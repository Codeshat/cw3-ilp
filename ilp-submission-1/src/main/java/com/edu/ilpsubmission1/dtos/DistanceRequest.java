package com.edu.ilpsubmission1.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DistanceRequest {

    @Valid
    @NotNull
    private Position position1;

    @Valid
    @NotNull
    private Position position2;

}
