package com.edu.ilpsubmission1.web;

import com.edu.ilpsubmission1.dtos.FaceMatchResponse;
import com.edu.ilpsubmission1.service.DeliveryVerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API Level Tests for Delivery Verification Controller
 * Tests HTTP contract, request validation, and response formats
 */
@Tag("api")
@WebMvcTest(DeliveryVerificationController.class)
class DeliveryVerificationControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryVerificationService verificationService;

    private static final String TEST_EMAIL = "patient@hospital.com";
    private static final String TEST_DELIVERY_ID = "delivery-abc-123";

    // ========== POST /api/v1/delivery/register ==========

    @Test
    @DisplayName("POST /register returns 200 with delivery ID")
    void registerDeliveryReturns200() throws Exception {
        // Given
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "image-data".getBytes()
        );

        when(verificationService.registerDelivery(any(), eq(TEST_EMAIL)))
                .thenReturn(TEST_DELIVERY_ID);

        // When/Then
        mockMvc.perform(multipart("/api/v1/delivery/register")
                        .file(image)
                        .param("email", TEST_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(TEST_DELIVERY_ID))
                .andExpect(jsonPath("$.message").value(containsString("OTP sent")));

        verify(verificationService).registerDelivery(any(), eq(TEST_EMAIL));
    }

    @Test
    @DisplayName("POST /register returns 400 for missing image")
    void registerWithoutImageReturns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/delivery/register")
                        .param("email", TEST_EMAIL))
                .andExpect(status().isBadRequest());

        verify(verificationService, never()).registerDelivery(any(), any());
    }

    @Test
    @DisplayName("POST /register returns 400 for missing email")
    void registerWithoutEmailReturns400() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "image-data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/delivery/register")
                        .file(image))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register handles service IOException")
    void registerHandlesIOException() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "image-data".getBytes()
        );

        when(verificationService.registerDelivery(any(), any()))
                .thenThrow(new java.io.IOException("Failed to process image"));

        mockMvc.perform(multipart("/api/v1/delivery/register")
                        .file(image)
                        .param("email", TEST_EMAIL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to process image"));
    }

    // ========== POST /api/v1/delivery/verify-otp/{deliveryId} ==========

    @Test
    @DisplayName("POST /verify-otp returns 200 for valid OTP")
    void verifyOtpReturns200ForValid() throws Exception {
        when(verificationService.verifyOtp(TEST_DELIVERY_ID, "123456"))
                .thenReturn(true);

        // FIX: Include email because your DTO validation requires it
        String requestBody = objectMapper.writeValueAsString(
                Map.of("otp", "123456", "email", TEST_EMAIL)
        );

        mockMvc.perform(post("/api/v1/delivery/verify-otp/{deliveryId}", TEST_DELIVERY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    @DisplayName("POST /verify-otp returns 200 for invalid OTP with verified=false")
    void verifyOtpReturns200ForInvalid() throws Exception {
        when(verificationService.verifyOtp(TEST_DELIVERY_ID, "wrong"))
                .thenReturn(false);

        // FIX: Include email here as well
        String requestBody = objectMapper.writeValueAsString(
                Map.of("otp", "wrong", "email", TEST_EMAIL)
        );

        mockMvc.perform(post("/api/v1/delivery/verify-otp/{deliveryId}", TEST_DELIVERY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false));
    }

    @Test
    @DisplayName("POST /verify-otp validates OTP format")
    void verifyOtpValidatesFormat() throws Exception {
        // Missing OTP field
        String requestBody = "{}";

        mockMvc.perform(post("/api/v1/delivery/verify-otp/{deliveryId}", TEST_DELIVERY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========== GET /api/v1/delivery/otp-status/{deliveryId} ==========

    @Test
    @DisplayName("GET /otp-status returns verification status")
    void getOtpStatusReturnsStatus() throws Exception {
        when(verificationService.isOtpVerified(TEST_DELIVERY_ID))
                .thenReturn(true);

        mockMvc.perform(get("/api/v1/delivery/otp-status/{deliveryId}", TEST_DELIVERY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpVerified").value(true));
    }

    @Test
    @DisplayName("GET /otp-status returns false for unverified delivery")
    void getOtpStatusReturnsFalseForUnverified() throws Exception {
        when(verificationService.isOtpVerified(TEST_DELIVERY_ID))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/delivery/otp-status/{deliveryId}", TEST_DELIVERY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpVerified").value(false));
    }

    // ========== POST /api/v1/delivery/verify/{deliveryId} ==========

    @Test
    @DisplayName("POST /verify returns 200 with face match result")
    void verifyDeliveryReturns200WithMatch() throws Exception {
        MockMultipartFile capturedImage = new MockMultipartFile(
                "image",
                "captured.jpg",
                "image/jpeg",
                "captured-data".getBytes()
        );

        FaceMatchResponse matchResponse = new FaceMatchResponse(true, 0.95, "Match found");
        when(verificationService.verifyDelivery(eq(TEST_DELIVERY_ID), any()))
                .thenReturn(matchResponse);

        mockMvc.perform(multipart("/api/v1/delivery/verify/{deliveryId}", TEST_DELIVERY_ID)
                        .file(capturedImage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match").value(true))
                .andExpect(jsonPath("$.score").value(0.95))
                .andExpect(jsonPath("$.reason").value("Match found"));
    }

    @Test
    @DisplayName("POST /verify returns 200 with no match")
    void verifyDeliveryReturns200WithNoMatch() throws Exception {
        MockMultipartFile capturedImage = new MockMultipartFile(
                "image",
                "captured.jpg",
                "image/jpeg",
                "captured-data".getBytes()
        );

        FaceMatchResponse matchResponse = new FaceMatchResponse(false, 0.35, "Low confidence");
        when(verificationService.verifyDelivery(eq(TEST_DELIVERY_ID), any()))
                .thenReturn(matchResponse);

        mockMvc.perform(multipart("/api/v1/delivery/verify/{deliveryId}", TEST_DELIVERY_ID)
                        .file(capturedImage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match").value(false))
                .andExpect(jsonPath("$.score").value(0.35));
    }

    @Test
    @DisplayName("POST /verify returns 404 for unknown delivery ID")
    void verifyDeliveryReturns404() throws Exception {
        MockMultipartFile capturedImage = new MockMultipartFile(
                "image",
                "captured.jpg",
                "image/jpeg",
                "captured-data".getBytes()
        );

        when(verificationService.verifyDelivery(anyString(), any()))
                .thenThrow(new IllegalArgumentException("Delivery ID not found"));

        mockMvc.perform(multipart("/api/v1/delivery/verify/{deliveryId}", "unknown")
                        .file(capturedImage))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /verify returns 400 when OTP not verified")
    void verifyDeliveryReturns400WithoutOtp() throws Exception {
        MockMultipartFile capturedImage = new MockMultipartFile(
                "image",
                "captured.jpg",
                "image/jpeg",
                "captured-data".getBytes()
        );

        when(verificationService.verifyDelivery(anyString(), any()))
                .thenThrow(new IllegalStateException("OTP not verified"));

        mockMvc.perform(multipart("/api/v1/delivery/verify/{deliveryId}", TEST_DELIVERY_ID)
                        .file(capturedImage))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("OTP not verified"));
    }

    @Test
    @DisplayName("POST /verify returns 400 for IOException")
    void verifyDeliveryHandlesIOException() throws Exception {
        MockMultipartFile capturedImage = new MockMultipartFile(
                "image",
                "captured.jpg",
                "image/jpeg",
                "captured-data".getBytes()
        );

        when(verificationService.verifyDelivery(anyString(), any()))
                .thenThrow(new java.io.IOException("Image processing failed"));

        mockMvc.perform(multipart("/api/v1/delivery/verify/{deliveryId}", TEST_DELIVERY_ID)
                        .file(capturedImage))
                .andExpect(status().isBadRequest());
    }

    // ========== SECURITY TESTS ==========

    @Test
    @DisplayName("API rejects requests with SQL injection attempts")
    void apiRejectsSqlInjection() throws Exception {
        String maliciousId = "'; DROP TABLE deliveries; --";

        when(verificationService.isOtpVerified(anyString()))
                .thenReturn(false);

        // API should handle gracefully without SQL execution
        mockMvc.perform(get("/api/v1/delivery/otp-status/{deliveryId}", maliciousId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API handles very long delivery IDs")
    void apiHandlesLongDeliveryIds() throws Exception {
        String longId = "a".repeat(1000);

        when(verificationService.isOtpVerified(anyString()))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/delivery/otp-status/{deliveryId}", longId))
                .andExpect(status().isOk());
    }

    // ========== CONTENT TYPE VALIDATION ==========

    @Test
    @DisplayName("POST /verify-otp requires JSON content type")
    void verifyOtpRequiresJson() throws Exception {
        mockMvc.perform(post("/api/v1/delivery/verify-otp/{deliveryId}", TEST_DELIVERY_ID)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("123456"))
                .andExpect(status().isUnsupportedMediaType());
    }




}
