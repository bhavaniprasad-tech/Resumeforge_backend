package com.bhavani.resumeforge.service;

import com.bhavani.resumeforge.document.User;
import com.bhavani.resumeforge.dto.AuthResponse;
import com.bhavani.resumeforge.dto.LoginRequest;
import com.bhavani.resumeforge.dto.RegisterRequest;
import com.bhavani.resumeforge.exception.ResourceExistsException;
import com.bhavani.resumeforge.repository.UserRepository;
import com.bhavani.resumeforge.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;

    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    @Value("${app.base.url : http://localhost:8080}")
    private String appBaseUrl;

    public AuthResponse register(RegisterRequest request){
        log.info("Inside the AuthService: register() {}", request);
        if(userRepository.existsByEmail(request.getEmail())){
            throw new ResourceExistsException("User already exists with this email");
        }

        User newUser = toDocument(request);

        userRepository.save(newUser);

        sendVerificationEmail(newUser);


        return toResponse(newUser);
    }

    private void sendVerificationEmail(User newUser) {
        log.info("Inside the AuthService: sendVerificationEmail() {}", newUser);
        try{
            String link = appBaseUrl+"/api/auth/verify-email?token="+newUser.getVerificationToken();
            String html =
                    "<div style='font-family:Arial, sans-serif; color:#1f2937; line-height:1.6'>" +
                            "<h2 style='color:#111827; font-size:20px; font-weight:600; margin-bottom:16px'>Verify your email</h2>" +
                            "<p style='margin:0 0 12px 0'>Hi " + newUser.getName() + ",</p>" +
                            "<p style='margin:0 0 20px 0'>Please confirm your email to activate your account.</p>" +
                            "<p style='margin:0 0 24px 0'>" +
                            "<a href='" + link + "' style='padding:10px 18px; background:#6366f1; color:#ffffff; display:inline-block; border-radius:6px; text-decoration:none; font-weight:500'>" +
                            "Verify Email" +
                            "</a>" +
                            "</p>" +
                            "<p style='margin:0 0 8px 0'>Or copy this link:</p>" +
                            "<p style='margin:0 0 24px 0; word-break:break-all; color:#2563eb'>" + link + "</p>" +
                            "<p style='margin:0; font-size:14px; color:#6b7280'>This link expires in 24 hours.</p>" +
                            "</div>";
            emailService.sendHtmlEmail(newUser.getEmail(), "Verify your email", html);
        }catch (Exception e){
            log.error("Exception occured at sendVerificationEmail()", e.getMessage());
            throw new RuntimeException("Failed to send verification email:"+e.getMessage());
        }
    }

    private AuthResponse toResponse(User newuser){
        return AuthResponse.builder()
                .id(newuser.getId())
                .name(newuser.getName())
                .email(newuser.getEmail())
                .profileImageUrl(newuser.getProfileImageUrl())
                .emailVerified(newuser.isEmailVerified())
                .subscriptionPlan(newuser.getSubscriptionPlan())
                .createdAt(newuser.getCreatedAt())
                .updatedAt(newuser.getUpdatedAt())
                .build();
    }

    private User toDocument(RegisterRequest request){
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .profileImageUrl(request.getProfileImageUrl())
                .subscriptionPlan("Basic")
                .emailVerified(false)
                .verificationToken(UUID.randomUUID().toString())
                .verificationExpires(LocalDateTime.now().plusHours(24))
                .build();
    }

    public void verifyEmail(String token){
        log.info("Inside the AuthService: verifyEmail() {}", token);
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification token"));

        if (user.getVerificationExpires() != null && user.getVerificationExpires().isBefore(LocalDateTime.now())){
            throw new RuntimeException("Verification token has expired. Please request a new token");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpires(null);
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Check email verification
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email not verified. Please verify your email to continue.");
        }

        // Generate JWT using user.id + email
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .subscriptionPlan(user.getSubscriptionPlan())
                .profileImageUrl(user.getProfileImageUrl())
                .token(token)
                .build();
    }


    public void resendVerification(String email) {
        //Step 1: Fetch the user account by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        //Step 2: Check the email is verified
        if (user.isEmailVerified()){
            throw new RuntimeException("Email is already verified.");
        }
        //Step 3: Set the new Verification token and expires time
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationExpires(LocalDateTime.now().plusHours(24));
        //Step 4: Update the user
        userRepository.save(user);
        //Step 5: Resend the verification email
        sendVerificationEmail(user);
    }

    public AuthResponse getProfile(Object principalObject) {

        String userId = (String) principalObject;
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(existingUser);
    }

}