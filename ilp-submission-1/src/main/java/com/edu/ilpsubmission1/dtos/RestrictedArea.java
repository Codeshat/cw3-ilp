package com.edu.ilpsubmission1.dtos;

import com.edu.ilpsubmission1.dtos.Position;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RestrictedArea(
        String name,
        Long id,
        List<Position> vertices
) {}