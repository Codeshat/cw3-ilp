package com.edu.ilpsubmission1.web;

import com.edu.ilpsubmission1.dtos.FaceMatchResponse;
import com.edu.ilpsubmission1.dtos.OtpVerifyRequest;
import com.edu.ilpsubmission1.service.DeliveryVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryVerificationController {

    private final DeliveryVerificationService verificationService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerDelivery(
            @RequestParam("image") MultipartFile image,
            @RequestParam("email") String email) {
        try {
            String deliveryId = verificationService.registerDelivery(image, email);
            return ResponseEntity.ok(Map.of(
                    "deliveryId", deliveryId,
                    "message", "OTP sent to " + email
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to process image"));
        }
    }

    @PostMapping("/verify-otp/{deliveryId}")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @PathVariable String deliveryId,
            @Valid @RequestBody OtpVerifyRequest request) {
        try {
            boolean verified = verificationService.verifyOtp(deliveryId, request.getOtp());
            return ResponseEntity.ok(Map.of(
                    "verified", verified,
                    "message", verified ? "OTP verified successfully" : "Invalid or expired OTP"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/otp-status/{deliveryId}")
    public ResponseEntity<Map<String, Boolean>> getOtpStatus(@PathVariable String deliveryId) {
        boolean verified = verificationService.isOtpVerified(deliveryId);
        return ResponseEntity.ok(Map.of("otpVerified", verified));
    }

    @PostMapping("/verify/{deliveryId}")
    public ResponseEntity<?> verifyDelivery(
            @PathVariable String deliveryId,
            @RequestParam("image") MultipartFile capturedImage) {
        try {
            FaceMatchResponse response = verificationService.verifyDelivery(deliveryId, capturedImage);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}