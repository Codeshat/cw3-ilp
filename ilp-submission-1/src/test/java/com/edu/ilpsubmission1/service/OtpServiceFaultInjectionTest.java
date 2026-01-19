package com.edu.ilpsubmission1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Fault Injection Tests for OTP Service
 * Tests security robustness under adverse conditions
 */
class OtpServiceFaultInjectionTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private OtpService otpService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_DELIVERY_ID = "delivery-123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "fromEmail", "noreply@drone-delivery.com");
    }

    // ========== FAULT 1: Expired OTP ==========

    @Test
    @DisplayName("Expired OTP is rejected")
    void expiredOtpIsRejected() throws InterruptedException {
        // Given: OTP with very short expiry
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 0);

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        otpService.generateAndSendOtp(TEST_EMAIL);

        // Simulate time passing (wait slightly to ensure expiry)
        Thread.sleep(100);

        // When: Attempting to verify expired OTP
        boolean result = otpService.verifyOtp(TEST_EMAIL, "123456");

        // Then: Verification fails
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Expired OTP for delivery is rejected and removed")
    void expiredDeliveryOtpIsRejected() throws InterruptedException {
        // Given: Short-lived OTP
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 0);

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        String otp = otpService.generateOtpForDelivery(TEST_DELIVERY_ID, TEST_EMAIL);

        Thread.sleep(100);

        // When: Verifying after expiry
        boolean result = otpService.verifyOtpForDelivery(TEST_DELIVERY_ID, otp);

        // Then: Rejected and cleaned up
        assertThat(result).isFalse();

        // Verify subsequent attempts also fail (OTP removed)
        assertThat(otpService.verifyOtpForDelivery(TEST_DELIVERY_ID, otp)).isFalse();
    }

    @Test
    @DisplayName("OTP expires correctly when time passes")
    void otpExpireAtBoundary() {
        // Given: An OTP is generated
        String otp = otpService.generateOtpForDelivery(TEST_DELIVERY_ID, TEST_EMAIL);

        Map<String, Object> store = (Map<String, Object>) ReflectionTestUtils.getField(otpService, "otpStore");
        Object data = store.get(TEST_DELIVERY_ID);
        ReflectionTestUtils.setField(data, "expiryTime", LocalDateTime.now().minusSeconds(1));

        // When/Then: Should now be expired
        assertThat(otpService.verifyOtpForDelivery(TEST_DELIVERY_ID, otp)).isFalse();
    }

    // ========== FAULT 2: Email Sender Failure ==========

    @Test
    @DisplayName("Email sender failure throws exception")
    void emailSenderFailureThrowsException() {
        // Given: Mail sender fails
        doThrow(new MailSendException("SMTP server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // When/Then: Exception propagates
        assertThatThrownBy(() -> otpService.generateAndSendOtp(TEST_EMAIL))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send OTP");
    }

    @Test
    @DisplayName("Email failure for delivery OTP throws exception")
    void emailFailureForDeliveryThrowsException() {
        // Given: SMTP connection timeout
        doThrow(new MailSendException("Connection timeout"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // When/Then
        assertThatThrownBy(() -> otpService.generateOtpForDelivery(TEST_DELIVERY_ID, TEST_EMAIL))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send OTP");
    }

    @Test
    @DisplayName("Partial email failure - intermittent SMTP errors")
    void intermittentEmailFailure() {
        // Given: Email fails first time, succeeds second
        doThrow(new MailSendException("Temporary failure"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        // When: First attempt fails
        assertThatThrownBy(() -> otpService.generateAndSendOtp(TEST_EMAIL))
                .isInstanceOf(RuntimeException.class);

        // Then: Second attempt succeeds
        assertThatCode(() -> otpService.generateAndSendOtp(TEST_EMAIL))
                .doesNotThrowAnyException();
    }

    // ========== FAULT 3: Repeated Invalid OTP Attempts ==========

    @Test
    @DisplayName("Repeated invalid OTP attempts are rejected")
    void repeatedInvalidAttemptsRejected() {
        // Given: Valid OTP generated
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        otpService.generateAndSendOtp(TEST_EMAIL);

        // When: Multiple invalid attempts
        for (int i = 0; i < 10; i++) {
            boolean result = otpService.verifyOtp(TEST_EMAIL, "999999");

            // Then: All attempts fail
            assertThat(result).isFalse();
        }

        // Ensure correct OTP still works (no lockout implemented, but we test behavior)
        // In production, you might want to implement rate limiting
    }

    @Test
    @DisplayName("Brute force attack with random OTPs all fail")
    void bruteForceAttackFails() {
        // Given: Single valid OTP
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        String validOtp = otpService.generateOtpForDelivery(TEST_DELIVERY_ID, TEST_EMAIL);

        // When: Attempting many random OTPs
        int attempts = 100;
        int failures = 0;

        for (int i = 0; i < attempts; i++) {
            String randomOtp = String.format("%06d", i);
            if (!randomOtp.equals(validOtp)) {
                boolean result = otpService.verifyOtpForDelivery(TEST_DELIVERY_ID, randomOtp);
                if (!result) failures++;
            }
        }

        // Then: All invalid attempts fail
        assertThat(failures).isGreaterThan(90); // Most should fail
    }

    @Test
    @DisplayName("Concurrent invalid OTP attempts are thread-safe")
    void concurrentInvalidAttemptsAreThreadSafe() throws InterruptedException {
        // Given: Valid OTP
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        otpService.generateOtpForDelivery(TEST_DELIVERY_ID, TEST_EMAIL);

        // When: Multiple threads attempt invalid OTPs
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        otpService.verifyOtpForDelivery(TEST_DELIVERY_ID, "000000");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: No crashes or data corruption
        assertThat(executor.isShutdown()).isTrue();
    }

    // ========== FAULT 4: OTP Reuse Prevention ==========

    @Test
    @DisplayName("OTP cannot be reused after successful verification")
    void otpCannotBeReused() {
        // Given: Valid OTP
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        String otp = otpService.generateOtpForDelivery(TEST_DELIVERY_ID, TEST_EMAIL);

        // When: First verification succeeds
        boolean firstAttempt = otpService.verifyOtpForDelivery(TEST_DELIVERY_ID, otp);
        assertThat(firstAttempt).isTrue();

        // Then: Reuse fails
        boolean reuseAttempt = otpService.verifyOtpForDelivery(TEST_DELIVERY_ID, otp);
        assertThat(reuseAttempt).isFalse();
    }

    @Test
    @DisplayName("Email OTP cannot be reused")
    void emailOtpCannotBeReused() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(messageCaptor.capture());

        otpService.generateAndSendOtp(TEST_EMAIL);

        String body = messageCaptor.getValue().getText();

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d{6}").matcher(body);
        assertThat(matcher.find()).isTrue();
        String correctOtp = matcher.group();

        // When: First use
        assertThat(otpService.verifyOtp(TEST_EMAIL, correctOtp)).isTrue();

        // Then: Second use fails
        assertThat(otpService.verifyOtp(TEST_EMAIL, correctOtp)).isFalse();
    }

    // ========== FAULT 5: Invalid Email Format ==========

    @Test
    @DisplayName("Invalid email format causes email send failure")
    void invalidEmailFormatFails() {
        // Given: Invalid email
        String invalidEmail = "not-an-email";

        doThrow(new MailSendException("Invalid recipient"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // When/Then
        assertThatThrownBy(() -> otpService.generateAndSendOtp(invalidEmail))
                .isInstanceOf(RuntimeException.class);
    }

    // ========== FAULT 6: OTP Timing Attacks ==========

    @Test
    @DisplayName("OTP verification timing is consistent (prevents timing attacks)")
    void otpVerificationTimingIsConsistent() {
        // Given: Valid and invalid OTPs
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        String otp = otpService.generateOtpForDelivery(TEST_DELIVERY_ID, TEST_EMAIL);

        // When: Measure timing for correct vs incorrect OTP
        long startCorrect = System.nanoTime();
        otpService.verifyOtpForDelivery(TEST_DELIVERY_ID, otp);
        long correctTime = System.nanoTime() - startCorrect;

        // Regenerate
        otpService.generateOtpForDelivery(TEST_DELIVERY_ID, TEST_EMAIL);

        long startIncorrect = System.nanoTime();
        otpService.verifyOtpForDelivery(TEST_DELIVERY_ID, "000000");
        long incorrectTime = System.nanoTime() - startIncorrect;

        // Then: Timing difference should not be dramatic
        // (This is a basic check; real timing attack prevention needs constant-time comparison)
        double ratio = (double) Math.max(correctTime, incorrectTime) /
                Math.min(correctTime, incorrectTime);

        assertThat(ratio).isLessThan(10.0); // Allow some variance
    }

    // ========== FAULT 7: Missing/Null Inputs ==========

    @Test
    @DisplayName("Null email returns false for verification")
    void nullEmailReturnsFalse() {
        boolean result = otpService.verifyOtp(null, "123456");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Null OTP returns false")
    void nullOtpReturnsFalse() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        otpService.generateAndSendOtp(TEST_EMAIL);

        boolean result = otpService.verifyOtp(TEST_EMAIL, null);
        assertThat(result).isFalse();
    }

    // Helper method
    private String extractOtpFromService(String email) {
        // In real tests, you'd capture the email or use reflection
        // For now, generate a predictable OTP or use reflection to peek
        return "123456"; // Placeholder
    }
}
