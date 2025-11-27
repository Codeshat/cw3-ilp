package com.edu.ilpsubmission1.dtos;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NextPositionRequest {

    @Valid
    @NotNull
    private Position start;

    @NotNull
    private Double angle;
}
