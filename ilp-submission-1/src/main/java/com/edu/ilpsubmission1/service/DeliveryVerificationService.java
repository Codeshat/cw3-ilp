package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.client.FaceRecognitionClient;
import com.edu.ilpsubmission1.dtos.FaceMatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryVerificationService {

    private final FaceRecognitionClient faceRecognitionClient;
    private final OtpService otpService;

    // Store delivery data with email
    private final Map<String, DeliveryData> deliveryStore = new HashMap<>();
    private static final int MAX_DELIVERIES = 10;

    public String registerDelivery(MultipartFile image, String email) throws IOException {
        if (deliveryStore.size() >= MAX_DELIVERIES) {
            String oldestKey = deliveryStore.keySet().iterator().next();
            deliveryStore.remove(oldestKey);
        }

        String deliveryId = UUID.randomUUID().toString();
        DeliveryData data = new DeliveryData(image.getBytes(), email, false);
        deliveryStore.put(deliveryId, data);

        // Send OTP
        otpService.generateAndSendOtp(email);

        return deliveryId;
    }

    public boolean verifyOtp(String deliveryId, String otp) {
        DeliveryData data = deliveryStore.get(deliveryId);
        if (data == null) {
            throw new IllegalArgumentException("Delivery ID not found");
        }

        boolean verified = otpService.verifyOtp(data.getEmail(), otp);
        if (verified) {
            data.setOtpVerified(true);
        }
        return verified;
    }

    public FaceMatchResponse verifyDelivery(String deliveryId, MultipartFile capturedImage) throws IOException {
        DeliveryData data = deliveryStore.get(deliveryId);
        if (data == null) {
            throw new IllegalArgumentException("Delivery ID not found");
        }

        if (!data.isOtpVerified()) {
            throw new IllegalStateException("OTP not verified");
        }

        return faceRecognitionClient.matchFaces(data.getImage(), capturedImage.getBytes());
    }

    public void clearDelivery(String deliveryId) {
        deliveryStore.remove(deliveryId);
    }

    public boolean isOtpVerified(String deliveryId) {
        DeliveryData data = deliveryStore.get(deliveryId);
        return data != null && data.isOtpVerified();
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class DeliveryData {
        private byte[] image;
        private String email;
        private boolean otpVerified;
    }
}