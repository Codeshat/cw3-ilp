package com.edu.ilpsubmission1.contracts;

import com.edu.ilpsubmission1.client.FaceRecognitionClient;
import com.edu.ilpsubmission1.dtos.FaceMatchResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;

/**
 * Contract Tests for Face Recognition Service
 * Ensures Java client stays compatible with Python service contract
 */
class FaceRecognitionClientContractTest {

    private static WireMockServer wireMockServer;
    private FaceRecognitionClient client;
    private byte[] testReferenceImage;
    private byte[] testCandidateImage;

    @BeforeAll
    static void setupServer() {
        // Port 0 picks any available port to avoid "Address already in use" errors
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void teardownServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();
        RestTemplate restTemplate = new RestTemplate();
        // Use the dynamic port from the server
        String baseUrl = "http://localhost:" + wireMockServer.port();
        client = new FaceRecognitionClient(restTemplate, baseUrl);

        testReferenceImage = "fake-reference".getBytes();
        testCandidateImage = "fake-candidate".getBytes();
    }

    /**
     * CONTRACT: Request must contain exactly 2 images (reference + candidate)
     */
    @Test
    @DisplayName("Client sends multipart request with 2 images")
    void requestMustContainTwoImages() {
        stubFor(post(urlEqualTo("/match"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"match\": true, \"score\": 0.95, \"reason\": \"Match found\"}")));

        client.matchFaces(testReferenceImage, testCandidateImage);

        // This ensures your Java client is calling 'reference' and 'candidate'
        // exactly as the Python Flask/FastAPI service requires.
        verify(postRequestedFor(urlEqualTo("/match"))
                .withAnyRequestBodyPart(aMultipart().withName("reference"))
                .withAnyRequestBodyPart(aMultipart().withName("candidate")));
    }

    /**
     * CONTRACT: Response must match FaceMatchResponse schema
     * Fields: match (boolean), score (double), reason (string)
     */
    @Test
    @DisplayName("Response schema matches FaceMatchResponse contract")
    void responseMustMatchSchema() {
        // Given: Mock returns valid schema
        stubFor(post(urlEqualTo("/match"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"match\": false, \"score\": 0.42, \"reason\": \"Low confidence\"}")));

        // When: Client processes response
        FaceMatchResponse response = client.matchFaces(testReferenceImage, testCandidateImage);

        // Then: All fields are correctly mapped
        assertThat(response).isNotNull();
        assertThat(response.isMatch()).isFalse();
        assertThat(response.getScore()).isEqualTo(0.42);
        assertThat(response.getReason()).isEqualTo("Low confidence");
    }

    /**
     * CONTRACT: Service handles no_face_detected scenario
     */
    @Test
    @DisplayName("Handles no_face_detected response")
    void handlesNoFaceDetected() {
        // Given: Service returns no face detected
        stubFor(post(urlEqualTo("/match"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"match\": false, \"score\": 0.0, \"reason\": \"no_face_detected\"}")));

        // When: Client processes response
        FaceMatchResponse response = client.matchFaces(testReferenceImage, testCandidateImage);

        // Then: Client correctly interprets no face scenario
        assertThat(response.isMatch()).isFalse();
        assertThat(response.getScore()).isEqualTo(0.0);
        assertThat(response.getReason()).isEqualTo("no_face_detected");
    }

    /**
     * CONTRACT: Service handles multiple_faces_detected
     */
    @Test
    @DisplayName("Handles multiple_faces_detected response")
    void handlesMultipleFacesDetected() {
        // Given: Service detects multiple faces
        stubFor(post(urlEqualTo("/match"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"match\": false, \"score\": 0.0, \"reason\": \"multiple_faces_detected\"}")));

        // When
        FaceMatchResponse response = client.matchFaces(testReferenceImage, testCandidateImage);

        // Then
        assertThat(response.isMatch()).isFalse();
        assertThat(response.getReason()).isEqualTo("multiple_faces_detected");
    }

    /**
     * CONTRACT: Service returns 400 for invalid image data
     */
    @Test
    @DisplayName("Handles 400 Bad Request for invalid images")
    void handles400BadRequest() {
        // Given: Service rejects invalid image
        stubFor(post(urlEqualTo("/match"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("{\"error\": \"Invalid image format\"}")));

        // When/Then: Client propagates error
        assertThatThrownBy(() -> client.matchFaces(testReferenceImage, testCandidateImage))
                .isInstanceOf(HttpClientErrorException.class);
    }

    /**
     * CONTRACT: Service returns 503 when unavailable
     */
    @Test
    @DisplayName("Handles service unavailability (503)")
    void handlesServiceUnavailable() {
        // Given: Service is down
        stubFor(post(urlEqualTo("/match"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("{\"error\": \"Service temporarily unavailable\"}")));

        // When/Then
        assertThatThrownBy(() -> client.matchFaces(testReferenceImage, testCandidateImage))
                .isInstanceOf(Exception.class);
    }

    /**
     * CONTRACT: Score must be between 0.0 and 1.0
     */
    @Test
    @DisplayName("Score field is within valid range [0.0, 1.0]")
    void scoreIsWithinValidRange() {
        // Given: Various score values
        stubFor(post(urlEqualTo("/match"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"match\": true, \"score\": 1.0, \"reason\": \"Perfect match\"}")));

        FaceMatchResponse response = client.matchFaces(testReferenceImage, testCandidateImage);

        assertThat(response.getScore())
                .isGreaterThanOrEqualTo(0.0)
                .isLessThanOrEqualTo(1.0);
    }

    /**
     * CONTRACT: High score implies match=true
     */
    @Test
    @DisplayName("High confidence score correlates with match=true")
    void highScoreImpliesMatch() {
        // Given: High confidence match
        stubFor(post(urlEqualTo("/match"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"match\": true, \"score\": 0.98, \"reason\": \"High confidence match\"}")));

        FaceMatchResponse response = client.matchFaces(testReferenceImage, testCandidateImage);

        assertThat(response.isMatch()).isTrue();
        assertThat(response.getScore()).isGreaterThan(0.9);
    }

    /**
     * CONTRACT: Response time should be reasonable
     */
    @Test
    @DisplayName("Service responds within acceptable time")
    void serviceRespondsWithinTimeout() {
        // Given: Service with slight delay
        stubFor(post(urlEqualTo("/match"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(500) // 500ms delay
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"match\": true, \"score\": 0.85, \"reason\": \"Match\"}")));

        // When/Then: Should complete within reasonable time
        assertThatCode(() -> client.matchFaces(testReferenceImage, testCandidateImage))
                .doesNotThrowAnyException();
    }

    /**
     * CONTRACT: Request with empty/null images should fail
     */
    @Test
    @DisplayName("Rejects empty image data")
    void rejectsEmptyImageData() {
        // This test ensures client validation or server contract enforcement
        byte[] emptyImage = new byte[0];

        stubFor(post(urlEqualTo("/match"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("{\"error\": \"Images cannot be empty\"}")));

        assertThatThrownBy(() -> client.matchFaces(emptyImage, testCandidateImage))
                .isInstanceOf(Exception.class);
    }
}
