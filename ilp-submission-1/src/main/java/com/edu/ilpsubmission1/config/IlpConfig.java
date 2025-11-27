package com.edu.ilpsubmission1.config;

import com.edu.ilpsubmission1.client.FaceRecognitionClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class IlpConfig {
    private static final String DEFAULT_ILP_ENDPOINT =
            "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net";
    private static final String DEFAULT_FACE_SERVICE_URL = "http://face-service:8000";

    @Bean
    public String ilpBaseUrl() {
        String endpoint = System.getenv("ILP_ENDPOINT");
        return (endpoint != null && !endpoint.isEmpty()) ? endpoint : DEFAULT_ILP_ENDPOINT;
    }

    @Bean
    public String faceServiceUrl() {
        String url = System.getenv("FACE_SERVICE_URL");
        return (url != null && !url.isEmpty()) ? url : DEFAULT_FACE_SERVICE_URL;
    }

    @Bean
    public WebClient ilpWebClient(WebClient.Builder builder, String ilpBaseUrl) {
        return builder.baseUrl(ilpBaseUrl).build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public FaceRecognitionClient faceRecognitionClient(RestTemplate restTemplate, String faceServiceUrl) {
        return new FaceRecognitionClient(restTemplate, faceServiceUrl);
    }
}