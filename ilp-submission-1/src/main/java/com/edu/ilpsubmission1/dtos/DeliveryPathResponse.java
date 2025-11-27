package com.edu.ilpsubmission1.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryPathResponse {
    private Double totalCost;
    private Integer totalMoves;
    private List<DronePath> dronePaths;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DronePath {
        private String droneId;
        private List<Delivery> deliveries;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Delivery {
        private Long deliveryId;
        private List<Position> flightPath;
    }
}
