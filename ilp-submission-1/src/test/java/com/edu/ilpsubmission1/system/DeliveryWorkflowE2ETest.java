package com.edu.ilpsubmission1.system;

import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class DeliveryWorkflowE2ETest extends AbstractE2ETest {
    @Tag("system")
    @Test
    void endToEndDeliveryWorkflowCompletesSuccessfully() {
        // 1️⃣ Register
        Response registerResponse =
                given()
                        .multiPart("image", new File("src/test/resources/e2e/Akshat1.jpg"))
                        .multiPart("email", "e2e@test.com")
                        .when()
                        .post("/api/v1/delivery/register")
                        .then()
                        .statusCode(200)
                        .body("deliveryId", notNullValue())
                        .extract().response();

        String deliveryId = registerResponse.path("deliveryId");

        // 2️⃣ OTP Verification
        String otp = "123456";

        given()
                .contentType("application/json")
                .body(java.util.Map.of(
                        "otp", otp,
                        "email", "e2e@test.com" // ADDED: Matches your DTO requirements
                ))
                .when()
                .post("/api/v1/delivery/verify-otp/" + deliveryId)
                .then()
                .statusCode(200)
                .body("verified", is(true));

        // 3️⃣ Face Verification
        given()
                .multiPart("image", new File("src/test/resources/e2e/Akshat2.jpg"))
                .when()
                .post("/api/v1/delivery/verify/" + deliveryId)
                .then()
                .statusCode(200)
                .body("match", is(true))
                .body("score", greaterThan(0.7f)); // Note the 'f' for float comparison
    }
}
