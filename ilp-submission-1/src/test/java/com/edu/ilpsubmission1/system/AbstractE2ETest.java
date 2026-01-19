package com.edu.ilpsubmission1.system;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractE2ETest {
    @BeforeAll
    static void setup() {
        // Pointing to your ALREADY RUNNING Docker containers
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;

        System.out.println("✅ System Test running against manual Docker environment.");
    }
}