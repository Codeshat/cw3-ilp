package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.dtos.Position;
import com.edu.ilpsubmission1.dtos.Region;
import com.edu.ilpsubmission1.exception.BadRequestException;
import com.edu.ilpsubmission1.exception.InvalidRegionException;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DroneServiceTest {

    private DroneService droneService;

    @BeforeEach
    void setUp() {
        droneService = new DroneService();
    }

    @Test
    void givenNullPositions_whenGetDistance_thenBadRequest() {
        assertThrows(BadRequestException.class,
                () -> droneService.getDistance(null, new Position()));
    }

    @Test
    void givenValidPositions_whenGetDistance_thenCorrect() {
        Position a = new Position(0.0,0.0);
        Position b = new Position(0.0,0.00015);

        assertEquals(0.00015, droneService.getDistance(a,b), 1e-9);
    }

    @Test
    void givenInvalidCompassAngle_whenNextPosition_thenException() {
        assertThrows(BadRequestException.class,
                () -> droneService.nextPosition(new Position(), 11.0));
    }

    @Test
    void givenValidAngle_whenNextPosition_thenCorrectStep() {
        Position start = new Position(0.0,0.0);

        Position next = droneService.nextPosition(start, 90.0);

        assertEquals(0.00015, next.getLat(), 1e-9);
        assertEquals(0.0, next.getLng(), 1e-9);
    }
}
