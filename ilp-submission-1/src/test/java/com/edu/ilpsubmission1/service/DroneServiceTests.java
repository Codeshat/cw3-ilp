package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.dtos.Position;
import com.edu.ilpsubmission1.dtos.Region;
import com.edu.ilpsubmission1.exception.BadRequestException;
import com.edu.ilpsubmission1.exception.InvalidRegionException;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DroneServiceTests {

    private final DroneService svc = new DroneService();

    private static final double EPSILON = 1e-12;
    private static final double STEP = 0.00015;
    private static final double COMPASS_INCREMENT = 22.5;

    private Position p(double lng, double lat) {
        Position pos = new Position();
        pos.setLng(lng);
        pos.setLat(lat);
        return pos;
    }

    /**
     * BASIC UNIT TESTS
     */

    @Test
    void getDistance_valid() {
        Position a = p(-3.192473, 55.946233);
        Position b = p(-3.192473, 55.942617);
        double expected = Math.sqrt(Math.pow(a.getLat() - b.getLat(), 2) + Math.pow(a.getLng() - b.getLng(), 2));
        double actual = svc.getDistance(a, b);
        assertThat(actual).isCloseTo(expected, Offset.offset(EPSILON));
    }

    @Test
    void getDistance_nullThrows() {
        Position a = p(-3.192473, 55.946233);
        assertThatThrownBy(() -> svc.getDistance(null, a)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> svc.getDistance(a, null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void isCloseTo_trueAndFalse() {
        Position base = p(0.0, 0.0);
        Position near = p(0.0, 0.0001); // 0.0001 < STEP (0.00015)
        Position far = p(0.0, 0.001);   // > STEP
        assertThat(svc.isCloseTo(base, near)).isTrue();
        assertThat(svc.isCloseTo(base, far)).isFalse();
    }

    @Test
    void nextPosition_validAngle45() {
        Position start = p(-3.192473, 55.946233);
        Position next = svc.nextPosition(start, 45.0);

        double rad = Math.toRadians(45.0);
        double expectedLng = start.getLng() + STEP * Math.cos(rad);
        double expectedLat = start.getLat() + STEP  * Math.sin(rad);

        assertThat(next.getLng()).isCloseTo(expectedLng, Offset.offset(EPSILON));
        assertThat(next.getLat()).isCloseTo(expectedLat, Offset.offset(EPSILON));
    }

    @Test
    void nextPosition_invalidAngleThrows() {
        Position start = p(-3.192473, 55.946233);
        assertThatThrownBy(() -> svc.nextPosition(start, 30.0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("angle must be one of the 16 compass directions (multiples of 22.5°)");
    }

    @Test
    void isInRegion_inside_onBorder_outside_and_invalidRegion() {
        // closed rectangle from your spec
        List<Position> verts = List.of(
                p(-3.192473, 55.946233),
                p(-3.192473, 55.942617),
                p(-3.184319, 55.942617),
                p(-3.184319, 55.946233),
                p(-3.192473, 55.946233) // closing point
        );
        Region region = new Region();
        region.setName("central");
        region.setVertices(verts);

        Position inside = p(-3.188396, 55.944425);
        Position border = p(-3.192473, 55.944425); // exactly on left edge
        Position outside = p(-3.200000, 55.944425);

        assertThat(svc.isInRegion(inside, region)).isTrue();
        assertThat(svc.isInRegion(border, region)).isTrue();
        assertThat(svc.isInRegion(outside, region)).isFalse();

        // malformed region (not closed)
        Region bad = new Region();
        bad.setName("bad");
        bad.setVertices(List.of(
                p(0.0, 0.0),
                p(1.0, 0.0),
                p(1.0, 1.0)
                // no closing vertex
        ));
        assertThatThrownBy(() -> svc.isInRegion(inside, bad)).isInstanceOf(InvalidRegionException.class);
    }
}
