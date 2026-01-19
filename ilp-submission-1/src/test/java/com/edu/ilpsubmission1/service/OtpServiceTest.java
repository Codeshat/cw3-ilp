package com.edu.ilpsubmission1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class OtpServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private OtpService otpService;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2025-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );

        otpService = new OtpService(mailSender, clock);

        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "fromEmail", "test@test.com");
    }

    @Test
    void givenEmail_whenGenerateOtp_thenEmailSent() {
        String result = otpService.generateAndSendOtp("user@test.com");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        assertEquals("OTP sent successfully", result);
    }

    @Test
    void givenWrongOtp_whenVerify_thenFalse() {
        otpService.storeOtpForTest(
                "user@test.com",
                "123456",
                LocalDateTime.now(clock).plusMinutes(5)
        );

        assertFalse(otpService.verifyOtp("user@test.com", "000000"));
    }
}

