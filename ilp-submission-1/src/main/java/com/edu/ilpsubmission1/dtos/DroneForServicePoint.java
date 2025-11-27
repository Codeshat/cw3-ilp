package com.edu.ilpsubmission1.dtos;

import java.util.List;

public record DroneForServicePoint(
        Long servicePointId,
        List<DroneAvailability> drones
) {
    public record DroneAvailability(
            String id,
            List<Availability> availability
    ) {}

    public record Availability(
            String dayOfWeek,
            String from,
            String until
    ) {}
}
