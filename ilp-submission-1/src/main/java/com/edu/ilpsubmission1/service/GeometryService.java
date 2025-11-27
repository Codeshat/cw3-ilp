package com.edu.ilpsubmission1.service;

import org.springframework.stereotype.Service;
import com.edu.ilpsubmission1.dtos.*;
import java.util.List;

@Service
public class GeometryService {

    private static final double DISTANCE_TOLERANCE = 0.00015;
    private static final double MOVE_DISTANCE = 0.00015;
    private static final double COMPASS_DIRECTION_DEGREES = 22.5;
    private static final double EPSILON = 1e-9;

    public double calculateDistance(Position p1, Position p2) {
        double dx = p2.getLng() - p1.getLng();
        double dy = p2.getLat() - p1.getLat();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean checkPointsClose(Position p1, Position p2) {
        return calculateDistance(p1, p2) < DISTANCE_TOLERANCE;
    }

    public Position calculateNextPosition(Position start, double angle) {
        double quotient = angle / COMPASS_DIRECTION_DEGREES;

        if (Math.abs(quotient - Math.round(quotient)) > EPSILON) {
            throw new IllegalArgumentException();
        }

        double radians = Math.toRadians(angle);
        double dx = MOVE_DISTANCE * Math.cos(radians);
        double dy = MOVE_DISTANCE * Math.sin(radians);

        return new Position(start.getLng() + dx, start.getLat() + dy);
    }

    public boolean isPointInRegion(Position point, Region region) {
        List<Position> vertices = region.getVertices();

        if (vertices.size() < 4 || !vertices.get(0).equals(vertices.get(vertices.size() - 1))) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < vertices.size() - 1; i++) {
            if (isOnSegment(point, vertices.get(i), vertices.get(i + 1))) {
                return true;
            }
        }

        return rayCastingTest(point, vertices);
    }

    public boolean checkLineIntersectsRegion(Position from, Position to, Region region) {
        List<Position> vertices = region.getVertices();

        for (int i = 0; i < vertices.size() - 1; i++) {
            if (segmentsIntersect(from, to, vertices.get(i), vertices.get(i + 1))) {
                return true;
            }
        }
        return false;
    }

    private boolean segmentsIntersect(Position a1, Position a2, Position b1, Position b2) {
        int o1 = computeOrientation(a1, a2, b1);
        int o2 = computeOrientation(a1, a2, b2);
        int o3 = computeOrientation(b1, b2, a1);
        int o4 = computeOrientation(b1, b2, a2);

        if (o1 != o2 && o3 != o4) return true;

        if (o1 == 0 && pointOnSegment(a1, b1, a2)) return true;
        if (o2 == 0 && pointOnSegment(a1, b2, a2)) return true;
        if (o3 == 0 && pointOnSegment(b1, a1, b2)) return true;
        if (o4 == 0 && pointOnSegment(b1, a2, b2)) return true;

        return false;
    }

    private int computeOrientation(Position p, Position q, Position r) {
        double value = (q.getLat() - p.getLat()) * (r.getLng() - q.getLng())
                - (q.getLng() - p.getLng()) * (r.getLat() - q.getLat());

        if (Math.abs(value) < EPSILON) return 0;
        return (value > 0) ? 1 : 2;
    }

    private boolean pointOnSegment(Position p, Position q, Position r) {
        return q.getLng() <= Math.max(p.getLng(), r.getLng())
                && q.getLng() >= Math.min(p.getLng(), r.getLng())
                && q.getLat() <= Math.max(p.getLat(), r.getLat())
                && q.getLat() >= Math.min(p.getLat(), r.getLat());
    }

    private boolean rayCastingTest(Position point, List<Position> vertices) {
        boolean inside = false;
        double px = point.getLng();
        double py = point.getLat();

        for (int i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
            double xi = vertices.get(i).getLng();
            double yi = vertices.get(i).getLat();
            double xj = vertices.get(j).getLng();
            double yj = vertices.get(j).getLat();

            boolean intersect = ((yi > py) != (yj > py))
                    && (px < (xj - xi) * (py - yi) / (yj - yi) + xi);

            if (intersect) inside = !inside;
        }
        return inside;
    }

    private boolean isOnSegment(Position point, Position segStart, Position segEnd) {
        double d1 = calculateDistance(point, segStart);
        double d2 = calculateDistance(point, segEnd);
        double segmentLength = calculateDistance(segStart, segEnd);

        return Math.abs((d1 + d2) - segmentLength) < EPSILON;
    }
}