package com.edu.ilpsubmission1.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    @NotNull
    @Min(-180)
    @Max(180)
    private Double lng;

    @NotNull
    @Min(-90)
    @Max(90)
    private Double lat;
}
