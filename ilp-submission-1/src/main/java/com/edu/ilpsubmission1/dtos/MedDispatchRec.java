package com.edu.ilpsubmission1.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MedDispatchRec {
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;

    private Requirements requirements;
    private Position delivery;

    @Data
    public static class Requirements {
        private Double capacity;
        private Boolean cooling;
        private Boolean heating;
        private Double maxCost;
    }
}
