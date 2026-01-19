package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.dtos.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GeometryServiceParameterizedTest {

    private GeometryService geometryService;

    @BeforeEach
    void setUp() {
        geometryService = new GeometryService();
    }

    // ---------- Distance ----------

    @ParameterizedTest(name = "Distance between {0} and {1} = {2}")
    @MethodSource("distanceCases")
    void calculateDistance_variousPoints(Position p1, Position p2, double expected) {
        double result = geometryService.calculateDistance(p1, p2);
        assertEquals(expected, result, 1e-12);
    }

    private static Stream<Arguments> distanceCases() {
        return Stream.of(
                Arguments.of(
                        new Position(0.0, 0.0),
                        new Position(0.0, 0.0),
                        0.0
                ),
                Arguments.of(
                        new Position(0.0, 0.0),
                        new Position(3.0, 4.0),
                        5.0
                ),
                Arguments.of(
                        new Position(-1.0, -1.0),
                        new Position(2.0, 3.0),
                        5.0
                )
        );
    }

    // ---------- Compass directions ----------

    @ParameterizedTest(name = "Angle {0}° is valid")
    @ValueSource(doubles = {
            0, 22.5, 45, 90, 135, 180, 225, 270, 315, 337.5
    })
    void calculateNextPosition_validAngles(double angle) {
        Position start = new Position(0.0, 0.0);

        Position next = geometryService.calculateNextPosition(start, angle);

        assertNotNull(next);
    }

    @ParameterizedTest(name = "Angle {0}° is invalid")
    @ValueSource(doubles = { 10, 30, 44.9, 46, 91 })
    void calculateNextPosition_invalidAngles(double angle) {
        Position start = new Position(0.0, 0.0);

        assertThrows(IllegalArgumentException.class,
                () -> geometryService.calculateNextPosition(start, angle));
    }
}

