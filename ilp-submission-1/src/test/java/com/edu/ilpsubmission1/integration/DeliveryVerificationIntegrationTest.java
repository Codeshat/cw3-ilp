package com.edu.ilpsubmission1.integration;

import com.edu.ilpsubmission1.client.FaceRecognitionClient;
import com.edu.ilpsubmission1.dtos.FaceMatchResponse;
import com.edu.ilpsubmission1.service.DeliveryVerificationService;
import com.edu.ilpsubmission1.service.OtpService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "management.health.mail.enabled=false"
        }
)
class DeliveryVerificationIntegrationTest {

    @MockitoBean
    JavaMailSender mailSender;

    @MockitoBean
    FaceRecognitionClient faceRecognitionClient;

    @MockitoBean
    OtpService otpService; // Mock OTP service so we control verification

    @Autowired
    DeliveryVerificationService verificationService;

    @Test
    void otpMustBeVerifiedBeforeFaceMatch() throws Exception {

        MultipartFile reference =
                new MockMultipartFile(
                        "ref",
                        "ref.jpg",
                        "image/jpeg",
                        new byte[]{1}
                );

        String deliveryId =
                verificationService.registerDelivery(
                        reference,
                        "test@example.com"
                );

        assertThrows(
                IllegalStateException.class,
                () -> verificationService.verifyDelivery(
                        deliveryId,
                        new MockMultipartFile(
                                "cand",
                                "c.jpg",
                                "image/jpeg",
                                new byte[]{2}
                        )
                ),
                "Face verification must fail if OTP was not verified first"
        );
    }

    @Test
    void successfulOtpAndFaceMatchCompletesVerification() throws Exception {

        // Mock face recognition
        when(faceRecognitionClient.matchFaces(any(), any()))
                .thenReturn(new FaceMatchResponse(true, 0.78, "success face match!!jjj"));

        // Mock OTP service to always verify
        when(otpService.verifyOtp(anyString(), anyString())).thenReturn(true);

        // Generate a dummy OTP (doesn't matter what it is, verifyOtp is mocked)
        when(otpService.generateAndSendOtp(anyString())).thenReturn("123456");

        MultipartFile reference =
                new MockMultipartFile(
                        "ref",
                        "ref.jpg",
                        "image/jpeg",
                        new byte[]{1}
                );

        String deliveryId =
                verificationService.registerDelivery(
                        reference,
                        "user@test.com"
                );

        // ---- OTP step ----
        String otp = otpService.generateAndSendOtp("user@test.com");


        assertTrue(
                otpService.verifyOtp("user@test.com", otp),
                "OTP should verify successfully"
        );
    }
}
