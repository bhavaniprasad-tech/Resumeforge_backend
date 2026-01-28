package com.bhavani.resumeforge.repository;

import com.bhavani.resumeforge.document.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRespository extends MongoRepository<Payment, String> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    List<Payment> findByUserIDOrderByCreatedAtDesc(String userID);

    List<Payment> findByStatus(String status);
}
