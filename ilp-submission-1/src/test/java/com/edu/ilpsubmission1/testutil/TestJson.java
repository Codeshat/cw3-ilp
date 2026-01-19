package com.edu.ilpsubmission1.testutil;

public final class TestJson {

    private TestJson() {}

    public static String validDeliveryRequest() {
        return """
            {
              "recipient": "user@test.com",
              "deliveryPoint": {
                "lat": 55.945,
                "lng": -3.184
              },
              "packageWeight": 2.0
            }
            """;
    }
}
