package com.edu.ilpsubmission1.integration;

import com.edu.ilpsubmission1.client.FaceRecognitionClient;
import com.edu.ilpsubmission1.dtos.FaceMatchResponse;
import com.edu.ilpsubmission1.service.DeliveryVerificationService;
import com.edu.ilpsubmission1.service.OtpService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "management.health.mail.enabled=false"
        }
)
@AutoConfigureMockMvc
class DeliveryVerificationWorkflowIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FaceRecognitionClient faceRecognitionClient;

    @MockitoBean
    private OtpService otpService;

    @MockitoBean
    private JavaMailSender mailSender;


    @Test
    void fullDeliveryVerificationFlowSucceeds() throws Exception {

        // Mock face recognition success
        when(faceRecognitionClient.matchFaces(any(), any()))
                .thenReturn(new FaceMatchResponse(true, 0.85, "match"));

        // 1️⃣ Register delivery
        MockMultipartFile referenceImage =
                new MockMultipartFile(
                        "image",
                        "ref.jpg",
                        "image/jpeg",
                        new byte[]{1}
                );

        MvcResult registerResult =
                mockMvc.perform(multipart("/api/v1/delivery/register")
                                .file(referenceImage)
                                .param("email", "user@test.com"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.deliveryId").exists())
                        .andReturn();

        String responseJson = registerResult.getResponse().getContentAsString();
        String deliveryId =
                JsonPath.read(responseJson, "$.deliveryId");

        when(otpService.verifyOtp(eq("user@test.com"), any()))
                .thenReturn(true);



        // 2️⃣ Verify OTP (bypass actual OTP logic)
        mockMvc.perform(post("/api/v1/delivery/verify-otp/{id}", deliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "user@test.com",
                          "otp": "123456"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));


        // 3️⃣ Verify face
        MockMultipartFile capturedImage =
                new MockMultipartFile(
                        "image",
                        "captured.jpg",
                        "image/jpeg",
                        new byte[]{2}
                );

        mockMvc.perform(multipart("/api/v1/delivery/verify/{id}", deliveryId)
                        .file(capturedImage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match").value(true));
    }
}
