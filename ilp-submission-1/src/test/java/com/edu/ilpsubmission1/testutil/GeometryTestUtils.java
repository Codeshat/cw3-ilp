package com.edu.ilpsubmission1.testutil;

import com.edu.ilpsubmission1.dtos.Position;
import com.edu.ilpsubmission1.dtos.Region;
import com.edu.ilpsubmission1.service.GeometryService;

import java.util.List;

public final class GeometryTestUtils {

    private static final GeometryService geometryService = new GeometryService();

    private GeometryTestUtils() {}

    /**
     * Checks whether a position lies inside ANY restricted zone.
     * Mirrors production safety logic.
     */
    public static boolean inAnyRestrictedZone(Position p, List<Region> zones) {
        return zones.stream()
                .anyMatch(zone -> geometryService.isPointInRegion(p, zone));
    }

    /**
     * Checks whether a movement segment illegally intersects a restricted zone.
     */
    public static boolean pathIntersectsRestrictedZone(
            Position from,
            Position to,
            List<Region> zones
    ) {
        return zones.stream()
                .anyMatch(zone ->
                        geometryService.checkLineIntersectsRegion(from, to, zone)
                );
    }
}
