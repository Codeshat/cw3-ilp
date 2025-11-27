package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.dtos.Position;
import com.edu.ilpsubmission1.dtos.Region;
import com.edu.ilpsubmission1.exception.BadRequestException;
import com.edu.ilpsubmission1.exception.InvalidRegionException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DroneService {

    private static final double STEP = 0.00015;
    private static final double EPSILON = 1e-12;
    private static final double COMPASS_INCREMENT = 22.5;

    /**
     * Calculates Euclidean distance between two positions.
     */
    public double getDistance(Position position1, Position position2) {
        if (position1 == null || position2 == null) {
            throw new BadRequestException("position1 and position2 must be provided");
        }
        double dLat = position1.getLat() - position2.getLat();
        double dLng = position1.getLng() - position2.getLng();
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }

    /**
     * Returns true if position1 and position2 are within one step of each other.
     */
    public boolean isCloseTo(Position position1, Position position2) {
        return getDistance(position1, position2) < STEP;
    }

    /**
     * Computes the next position of a drone given a starting position and compass angle.
     */
    public Position nextPosition(Position start, Double angleDeg){

        if (start == null || angleDeg == null) {
            throw new BadRequestException("start position and angle must be provided");
        }

        if (!isValidCompassAngle(angleDeg)) {
            throw new BadRequestException("angle must be one of the 16 compass directions (multiples of 22.5°)");
        }

        double rad = Math.toRadians(angleDeg);
        double deltaLng = STEP * Math.cos(rad);
        double deltaLat = STEP * Math.sin(rad);

        Position next = new Position();
        next.setLng(start.getLng() + deltaLng);
        next.setLat(start.getLat() + deltaLat);
        return next;
    }
    /**
     * Checks if the given angle is a valid compass direction (multiple of 22.5°).
     */
    private boolean isValidCompassAngle(Double angleDeg) {
        if (angleDeg == null) return false;

        double normAngle = angleDeg % 360;
        if (normAngle < 0) normAngle += 360;

        double remainder = normAngle % COMPASS_INCREMENT;

        return remainder==0;
    }

    /**
     * Determines if a position lies inside (or on the border of) a polygonal region.
     * Uses the ray-casting algorithm.
     */
    public boolean isInRegion(Position position, Region region) throws InvalidRegionException {
        if (position == null || region == null || region.getVertices() == null) {
            throw new InvalidRegionException("Invalid input: position or region data missing.");
        }

        List<Position> vertices = region.getVertices();
        if (vertices.size() < 4) {
            throw new InvalidRegionException("Region must have at least 3 vertices to form a polygon.");
        }
        // Ensure the polygon is closed (first and last vertex match)
        Position first = vertices.get(0);
        Position last = vertices.get(vertices.size() - 1);
        if (!nearlyEqual(first.getLat(), last.getLat()) || !nearlyEqual(first.getLng(), last.getLng())) {
            throw new InvalidRegionException("Region is not closed (first and last vertex must be the same).");
        }

        int nvert = vertices.size() - 1; // ignore the duplicate closing vertex
        double testX = position.getLng();
        double testY = position.getLat();

        // Check if point is on border
        for (int i = 0; i < nvert; i++) {
            Position a = vertices.get(i);
            Position b = vertices.get(i + 1);
            if (isPointOnLineSegment(position, a, b)) {
                return true; // on border is considered inside
            }
        }

        // Ray casting
        int crossings = 0;
        for (int i = 0, j = nvert - 1; i < nvert; j = i++) {
            double xi = vertices.get(i).getLng();
            double yi = vertices.get(i).getLat();
            double xj = vertices.get(j).getLng();
            double yj = vertices.get(j).getLat();

            if (((yi > testY) != (yj > testY)) &&
                    (testX < (xj - xi) * (testY - yi) / (yj - yi) + xi)) {
                crossings++;
            }
        }
        // Inside if the ray crosses an odd number of times
        return (crossings % 2 == 1);
    }

    /**
     * Checks if two doubles are nearly equal (for floating-point tolerance).
     */
    private boolean nearlyEqual(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }


    private boolean isPointOnLineSegment(Position p, Position a, Position b) {
        double cross = (p.getLat() - a.getLat()) * (b.getLng() - a.getLng()) -
                (p.getLng() - a.getLng()) * (b.getLat() - a.getLat());
        if (Math.abs(cross) > EPSILON) return false;

        double dot = (p.getLng() - a.getLng()) * (b.getLng() - a.getLng()) +
                (p.getLat() - a.getLat()) * (b.getLat() - a.getLat());
        if (dot < 0) return false;

        double squaredLen = Math.pow(b.getLng() - a.getLng(), 2) + Math.pow(b.getLat() - a.getLat(), 2);
        return dot <= squaredLen + EPSILON;
    }

}
