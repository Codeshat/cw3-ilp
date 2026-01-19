package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.dtos.Position;
import com.edu.ilpsubmission1.dtos.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GeometryServiceTest {

    private GeometryService geometryService;

    @BeforeEach
    void setUp() {
        geometryService = new GeometryService();
    }

    // ---------- calculateDistance ----------

    @Test
    void givenSamePoint_whenCalculateDistance_thenZero() {
        Position p = new Position(1.0, 1.0);

        double distance = geometryService.calculateDistance(p, p);

        assertEquals(0.0, distance, 1e-12);
    }

    @Test
    void givenKnownPoints_whenCalculateDistance_thenCorrectEuclidean() {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(3.0, 4.0);

        double distance = geometryService.calculateDistance(p1, p2);

        assertEquals(5.0, distance, 1e-12);
    }

    // ---------- checkPointsClose ----------

    @Test
    void givenPointsWithinTolerance_whenCheckPointsClose_thenTrue() {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(0.0001, 0.0001);

        assertTrue(geometryService.checkPointsClose(p1, p2));
    }

    @Test
    void givenPointsOutsideTolerance_whenCheckPointsClose_thenFalse() {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(1.0, 1.0);

        assertFalse(geometryService.checkPointsClose(p1, p2));
    }

    // ---------- calculateNextPosition ----------

    @Test
    void givenValidCompassAngle_whenCalculateNextPosition_thenMovesOneStep() {
        Position start = new Position(0.0, 0.0);

        Position next = geometryService.calculateNextPosition(start, 0.0);

        assertEquals(0.00015, next.getLng(), 1e-9);
        assertEquals(0.0, next.getLat(), 1e-9);
    }

    @Test
    void givenInvalidAngle_whenCalculateNextPosition_thenException() {
        Position start = new Position(0.0, 0.0);

        assertThrows(IllegalArgumentException.class,
                () -> geometryService.calculateNextPosition(start, 10.0));
    }

    // ---------- isPointInRegion ----------

    @Test
    void givenPointInsideClosedRegion_whenIsPointInRegion_thenTrue() {
        Region square = new Region("zone", List.of(
                new Position(0.0,0.0),
                new Position(1.0,0.0),
                new Position(1.0,1.0),
                new Position(0.0,1.0),
                new Position(0.0,0.0)
        ));

        Position inside = new Position(0.5, 0.5);

        assertTrue(geometryService.isPointInRegion(inside, square));
    }

    @Test
    void givenOpenPolygon_whenIsPointInRegion_thenException() {
        Region invalid = new Region("bad", List.of(
                new Position(0.0,0.0),
                new Position(1.0,0.0),
                new Position(1.0,1.0),
                new Position(0.0,1.0)
        ));

        assertThrows(IllegalArgumentException.class,
                () -> geometryService.isPointInRegion(new Position(0.5, 0.5), invalid));
    }
}
