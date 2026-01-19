package com.edu.ilpsubmission1.testutil;

import com.edu.ilpsubmission1.dtos.MedDispatchRec;

import java.util.List;
import java.util.Map;

public class TestData {

    // ---- Dispatch ----
    public static MedDispatchRec dispatchWithCapacity(double capacity) {
        MedDispatchRec rec = new MedDispatchRec();
        MedDispatchRec.Requirements req = rec.getRequirements();
        req.setCapacity(capacity);
        return rec;
    }

    // ---- Drone ----
    public static Map<String, Object> droneWithCapacity(String id, double capacity) {
        return Map.of(
                "id", id,
                "capability", Map.of(
                        "capacity", capacity
                )
        );
    }

    // ---- Service point mappings ----
    public static List<Map<String, Object>> singleDroneSP() {
        return List.of(
                Map.of(
                        "droneId", "D1",
                        "servicePointId", "SP1"
                )
        );
    }

    public static List<Map<String, Object>> singleServicePoint() {
        return List.of(
                Map.of(
                        "id", "SP1",
                        "lng", 0.0,
                        "lat", 0.0
                )
        );
    }
}
