package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.client.FaceRecognitionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryVerificationServiceTest {

    @Mock
    FaceRecognitionClient faceClient;

    @Mock
    OtpService otpService;

    @Mock
    MultipartFile image;

    private DeliveryVerificationService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryVerificationService(faceClient, otpService);
    }

    @Test
    void givenUnverifiedOtp_whenVerifyDelivery_thenException() throws IOException {
        when(image.getBytes()).thenReturn(new byte[]{1});

        String id = service.registerDelivery(image, "test@test.com");

        assertThrows(IllegalStateException.class,
                () -> service.verifyDelivery(id, image));
    }

    @Test
    void givenVerifiedOtp_whenVerifyDelivery_thenFaceClientCalled() throws IOException {
        when(image.getBytes()).thenReturn(new byte[]{1});
        when(otpService.verifyOtp(any(), any())).thenReturn(true);

        String id = service.registerDelivery(image, "test@test.com");
        service.verifyOtp(id, "123456");

        service.verifyDelivery(id, image);

        verify(faceClient).matchFaces(any(), any());
    }
}

