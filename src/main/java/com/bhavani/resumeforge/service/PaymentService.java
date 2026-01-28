package com.bhavani.resumeforge.service;

import com.bhavani.resumeforge.document.Payment;
import com.bhavani.resumeforge.document.User;
import com.bhavani.resumeforge.dto.AuthResponse;
import com.bhavani.resumeforge.repository.PaymentRespository;
import com.bhavani.resumeforge.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.bhavani.resumeforge.util.AppConstants.PREMIUM;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final AuthService authService;
    private final UserRepository userRepository;

    private final PaymentRespository paymentRepository;
    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public Payment createOrder(Object principal, String planType) throws RazorpayException {
        //Initial step
        AuthResponse authResponse = authService.getProfile(principal);

        //Step 1: Initialize the razorpay client
        RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        //Step 2: prepare the JSON Object to pass the razorpay
        int amount = 99900; // Amount in paise
        String currency = "INR";
        String receipt = PREMIUM+"_"+ UUID.randomUUID().toString().substring(0, 8);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount);
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receipt);
        //Step 3: Call the razorpay API to create order
        Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        //Step 4: Save the order details to the db
        Payment newPayment = Payment.builder()
                .userID(authResponse.getId())
                .razorpayOrderId(razorpayOrder.get("id"))
                .amount(amount)
                .currency(currency)
                .planType(planType)
                .status("created")
                .receipt(receipt)
                .build();
        //Step 5: return the result
        return paymentRepository.save(newPayment);
    }

    public boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) throws RazorpayException {
        try{
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);

            boolean isValidSignature = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);

            if (isValidSignature) {
                //update the payment status
                Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                        .orElseThrow(() -> new RazorpayException("Payment not found"));
                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setRazorPaySignature(razorpaySignature);
                payment.setStatus("paid");
                paymentRepository.save(payment);

                //Upgrade the user subscrption
                upgradeUserSubscription(payment.getUserID(), payment.getPlanType());
                return true;
            }
            return false;
        }catch (Exception e) {
            log.error("Error verifying the payment:", e);
            return false;
        }
    }

    private void upgradeUserSubscription(String userID, String planType) {
        User existingUser = userRepository.findById(userID)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        existingUser.setSubscriptionPlan(planType);
        userRepository.save(existingUser);
        log.info("User {} upgraded to {} plan" ,userID, planType);
    }

    public List<Payment> getUserPayments(Object principal) {
        //Step 1: get the current profile
        AuthResponse authResponse = authService.getProfile(principal);

        //Step 2: Call the repository finder method
        return paymentRepository.findByUserIDOrderByCreatedAtDesc(authResponse.getId());
    }

    public Payment getPaymentDetails(String orderId) {
        //Step 1: Call the repository finder method
        return paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(()-> new RuntimeException("Payment not found"));
    }
}

