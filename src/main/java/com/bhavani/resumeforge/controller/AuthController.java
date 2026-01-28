package com.bhavani.resumeforge.controller;

import com.bhavani.resumeforge.document.User;
import com.bhavani.resumeforge.dto.AuthResponse;
import com.bhavani.resumeforge.dto.LoginRequest;
import com.bhavani.resumeforge.dto.RegisterRequest;
import com.bhavani.resumeforge.service.AuthService;
import com.bhavani.resumeforge.service.FileUploadService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static com.bhavani.resumeforge.util.AppConstants.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(AUTH_CONTROLLER)
public class AuthController {

    private final AuthService authService;

    private final FileUploadService fileUploadService;

    // ✅ FIX: inject Environment as a field
    private final Environment environment;

    @PostMapping(REGISTER)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Inside the AuthController: register(): {}", request);
        AuthResponse response = authService.register(request);
        log.info("Response from service: {}", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(VERIFY_EMAIL)
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        log.info("Inside the AuthController: verifyEmail(): {}", token);
        authService.verifyEmail(token);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Email verified successfully"));
    }

    @PostMapping(UPLOAD_PROFILE)
    public ResponseEntity<?> uploadImage(@RequestPart("image") MultipartFile file) throws IOException {
        log.info("Inside the AuthController: uploadImage()");
        Map<String, String> response = fileUploadService.uploadSingleImage(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping(LOGIN)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(RESEND_VERIFICATION)
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        //Step 1 : Get email from request
        String email = body.get("email");
        //Step 2 : Add the Validation
        if (Objects.isNull(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        //Step 3 : Call the Service method to send the Verification link
        authService.resendVerification(email);
        //Step 4 : Return the response
        return ResponseEntity.ok(Map.of("success", true, "message", "Verification email sent"));
    }

    @GetMapping(PROFILE)
    public ResponseEntity<?> getProfile(Authentication authentication) {
        //Step 1: get the principal object
        Object principalObject = authentication.getPrincipal();
        //step 2: call the service method
        AuthResponse currentProfile = authService.getProfile(principalObject);
        //Step 3: return response
        return ResponseEntity.ok(currentProfile);
    }

    @Value("${test.marker:NOT_FOUND}")
    private String marker;

    @PostConstruct
    public void checkMarker() {
        log.info("🔥 PROD MARKER = {}", marker);
    }

    // ✅ FIX: no parameter, use injected environment
    @PostConstruct
    public void checkProfile() {
        log.info("🔥 ACTIVE PROFILES = {}", Arrays.toString(environment.getActiveProfiles()));
    }
}
