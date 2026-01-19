package com.edu.ilpsubmission1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(mailSender);
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
        otpService.generateAndSendOtp("user@test.com");

        assertFalse(otpService.verifyOtp("user@test.com", "000000"));
    }
}

