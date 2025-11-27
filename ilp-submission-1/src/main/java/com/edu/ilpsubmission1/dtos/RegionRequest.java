package com.edu.ilpsubmission1.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegionRequest {

    @Valid
    @NotNull
    private Position position;

    @Valid
    @NotNull
    private Region region;
}
