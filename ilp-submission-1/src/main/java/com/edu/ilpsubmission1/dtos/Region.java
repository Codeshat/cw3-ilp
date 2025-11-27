package com.edu.ilpsubmission1.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Region {
    @NotNull
    private String name;

    @NotNull
  //  @Size(min = 4)
    @Valid
    private List<Position> vertices;
}
