package com.edu.ilpsubmission1.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public record Drone(
        String id,
        String name,
        Capability capability
) {
    public record Capability(
            boolean cooling,
            boolean heating,
            double capacity,
            int maxMoves,
            double costPerMove,
            double costInitial,
            double costFinal
    ) {}
}
