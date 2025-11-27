package com.edu.ilpsubmission1.dtos;
import com.edu.ilpsubmission1.dtos.Position;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServicePoint(
        Long id,
        String name,
        Position location
) {}