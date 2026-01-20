package com.edu.ilpsubmission1.client;

import com.edu.ilpsubmission1.dtos.FaceMatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FaceRecognitionClient {

    private final RestTemplate restTemplate;
    private final String faceServiceUrl;

    public FaceMatchResponse matchFaces(byte[] referenceImage, byte[] candidateImage) {
        String url = faceServiceUrl + "/match";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("reference", new ByteArrayResource(referenceImage) {
            @Override
            public String getFilename() {
                return "reference.jpg";
            }
        });
        body.add("candidate", new ByteArrayResource(candidateImage) {
            @Override
            public String getFilename() {
                return "candidate.jpg";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, requestEntity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalStateException(
                    "Face recognition service returned empty response body"
            );
        }

        Object matchObj = responseBody.get("match");
        Object scoreObj = responseBody.get("score");

        if (!(matchObj instanceof Boolean) || !(scoreObj instanceof Number)) {
            throw new IllegalStateException(
                    "Invalid response format from face recognition service"
            );
        }

        return new FaceMatchResponse(
                (Boolean) matchObj,
                ((Number) scoreObj).doubleValue(),
                (String) responseBody.get("reason")
        );
    }
}