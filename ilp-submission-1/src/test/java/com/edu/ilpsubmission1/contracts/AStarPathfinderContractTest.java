package com.edu.ilpsubmission1.contracts;
import com.edu.ilpsubmission1.dtos.Position;
import com.edu.ilpsubmission1.dtos.Region;
import com.edu.ilpsubmission1.service.AStarPathfinder;
import com.edu.ilpsubmission1.service.GeometryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("contract")
public class AStarPathfinderContractTest {
    private GeometryService geometryService;
    private AStarPathfinder pathfinder;

    @BeforeEach
    void setUp() {
        geometryService = new GeometryService();
        pathfinder = new AStarPathfinder(geometryService);

    }
    @Test
    void findsPath_whenNoRestrictedZones() {
        Position start = new Position(0.0, 0.0);
        Position end   = new Position(0.001, 0.001);

        List<Position> path =
                pathfinder.findPath(start, end, List.of());

        assertFalse(path.isEmpty(), "Path should exist");
        assertTrue(
                geometryService.checkPointsClose(path.get(0), start),
                "Path should start near the start position"
        );
        assertTrue(
                geometryService.checkPointsClose(
                        path.get(path.size() - 1), end),
                "Path should end near the target"
        );
    }
    @Test
    void pathUsesValidStepSize() {
        Position start = new Position(0.0, 0.0);
        Position end   = new Position(0.001, 0.0);

        List<Position> path =
                pathfinder.findPath(start, end, List.of());

        for (int i = 1; i < path.size(); i++) {
            double step =
                    geometryService.calculateDistance(
                            path.get(i - 1), path.get(i));

            assertTrue(
                    step <= 0.00015 + 1e-9,
                    "Each step must not exceed MOVE_DISTANCE"
            );
        }
    }
    @Test
    void pathNeverEntersRestrictedRegion() {
        Position start = new Position(0.0, 0.0);
        Position end   = new Position(0.002, 0.0);

        Region restricted = new Region(
                "block",
                List.of(
                        new Position(0.0005, -0.0005),
                        new Position(0.0015, -0.0005),
                        new Position(0.0015,  0.0005),
                        new Position(0.0005,  0.0005),
                        new Position(0.0005, -0.0005)
                )
        );

        List<Position> path =
                pathfinder.findPath(start, end, List.of(restricted));

        assertFalse(path.isEmpty(), "Path should still exist");

        for (Position p : path) {
            assertFalse(
                    geometryService.isPointInRegion(p, restricted),
                    "Path must not enter restricted region"
            );
        }
    }

    @Test
    void pathSegmentsNeverIntersectRestrictedRegion() {
        Position start = new Position(-3.186, 55.944);
        Position end   = new Position(-3.184, 55.946);

        Region zone = new Region(
                "no-fly",
                List.of(
                        new Position(-3.1855, 55.9455),
                        new Position(-3.1850, 55.9455),
                        new Position(-3.1850, 55.9460),
                        new Position(-3.1855, 55.9460),
                        new Position(-3.1855, 55.9455)
                )
        );

        List<Position> path =
                pathfinder.findPath(start, end, List.of(zone));

        for (int i = 1; i < path.size(); i++) {
            assertFalse(
                    geometryService.checkLineIntersectsRegion(
                            path.get(i - 1), path.get(i), zone
                    ),
                    "Flight segment must not intersect restricted zone"
            );
        }
    }

    @Test
    void producesSamePathForSameInputs() {
        Position start = new Position(0.0, 0.0);
        Position end   = new Position(0.001, 0.001);

        List<Position> first =
                pathfinder.findPath(start, end, List.of());
        List<Position> second =
                pathfinder.findPath(start, end, List.of());

        assertEquals(first, second, "Pathfinding must be deterministic");
    }
    @Test
    void pathIsNotExcessivelyLong() {
        Position start = new Position(-3.186, 55.944);
        Position end   = new Position(-3.184, 55.946);

        List<Position> path =
                pathfinder.findPath(start, end, List.of());

        double direct =
                geometryService.calculateDistance(start, end);

        double actual = 0.0;
        for (int i = 1; i < path.size(); i++) {
            actual += geometryService.calculateDistance(
                    path.get(i - 1), path.get(i));
        }

        assertTrue(
                actual < direct * 3,
                "Path should be reasonably efficient"
        );
    }


}





