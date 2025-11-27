package com.edu.ilpsubmission1.dtos;

import com.edu.ilpsubmission1.dtos.Position;
import java.util.List;
import java.util.stream.Collectors;

public record GeoJsonResponse(String type, List<List<Double>> coordinates) {
    public static GeoJsonResponse fromPath(List<Position> path) {
        List<List<Double>> coords = path.stream()
                .map(p -> List.of(p.getLng(), p.getLat()))
                .collect(Collectors.toList());
        return new GeoJsonResponse("LineString", coords);
    }
}
