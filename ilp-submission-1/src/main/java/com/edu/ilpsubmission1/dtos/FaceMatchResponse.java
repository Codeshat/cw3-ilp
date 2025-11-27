package com.edu.ilpsubmission1.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FaceMatchResponse {
    private boolean match;
    private double score;
    private String reason;
}
