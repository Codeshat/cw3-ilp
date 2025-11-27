package com.edu.ilpsubmission1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final JavaMailSender mailSender;

    @Value("${otp.expiry.minutes:5}")
    private int otpExpiryMinutes;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Store OTPs in memory: email -> OtpData
    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();

    private static final SecureRandom random = new SecureRandom();

    public String generateAndSendOtp(String email) {
        // Generate 6-digit OTP
        String otp = String.format("%06d", random.nextInt(1000000));

        // Store OTP with expiry
        OtpData otpData = new OtpData(otp, LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        otpStore.put(email, otpData);

        // Send email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Your Drone Delivery Verification Code");
            message.setText(String.format(
                    "Your verification code is: %s\n\n" +
                            "This code will expire in %d minutes.\n\n" +
                            "If you didn't request this code, please ignore this email.\n\n" +
                            "Drone Delivery Service",
                    otp, otpExpiryMinutes
            ));

            mailSender.send(message);
            log.info("OTP sent to {}", email);
            return "OTP sent successfully";
        } catch (Exception e) {
            log.error("Failed to send OTP to {}", email, e);
            throw new RuntimeException("Failed to send OTP: " + e.getMessage());
        }
    }

    public boolean verifyOtp(String email, String otp) {
        OtpData otpData = otpStore.get(email);

        if (otpData == null) {
            log.warn("No OTP found for email: {}", email);
            return false;
        }

        if (LocalDateTime.now().isAfter(otpData.getExpiryTime())) {
            log.warn("OTP expired for email: {}", email);
            otpStore.remove(email);
            return false;
        }

        boolean isValid = otpData.getOtp().equals(otp);

        if (isValid) {
            // Remove OTP after successful verification
            otpStore.remove(email);
            log.info("OTP verified successfully for email: {}", email);
        } else {
            log.warn("Invalid OTP for email: {}", email);
        }

        return isValid;
    }

    public void clearOtp(String email) {
        otpStore.remove(email);
    }

    // Inner class to store OTP data
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class OtpData {
        private String otp;
        private LocalDateTime expiryTime;
    }
}
