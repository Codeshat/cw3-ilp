package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.dtos.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AStarPathfinder {

    private static final double[] DIRECTIONS = {
            0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5,
            180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5
    };
    private static final double STEP_SIZE = 0.00015;
    private static final double HEURISTIC_MULTIPLIER = 1.5;
    private static final int ITERATION_LIMIT = 50000;

    private final GeometryService geometryService;

    public AStarPathfinder(GeometryService geometryService) {
        this.geometryService = geometryService;
    }

    public List<Position> findPath(Position start, Position end, List<Region> restrictedZones) {
        Node startNode = createNode(start, 0, end);

        PriorityQueue<Node> openQueue = new PriorityQueue<>();
        Map<Position, Node> openMap = new HashMap<>();
        Set<Position> closedSet = new HashSet<>();

        openQueue.add(startNode);
        openMap.put(startNode.position, startNode);

        int iteration = 0;

        while (!openQueue.isEmpty() && iteration < ITERATION_LIMIT) {
            iteration++;

            Node current = openQueue.poll();
            openMap.remove(current.position);

            if (geometryService.checkPointsClose(current.position, end)) {
                return buildPath(current);
            }

            closedSet.add(current.position);

            for (double direction : DIRECTIONS) {
                Position next = geometryService.calculateNextPosition(current.position, direction);

                if (closedSet.contains(next)) continue;
                if (isBlockedMove(current.position, next, restrictedZones)) continue;

                double tentativeG = current.gCost + STEP_SIZE;
                Node neighbor = openMap.get(next);

                if (neighbor == null) {
                    neighbor = createNode(next, tentativeG, end);
                    neighbor.parent = current;
                    openQueue.add(neighbor);
                    openMap.put(next, neighbor);
                } else if (tentativeG < neighbor.gCost) {
                    neighbor.gCost = tentativeG;
                    neighbor.fCost = tentativeG + neighbor.hCost;
                    neighbor.parent = current;
                    openQueue.remove(neighbor);
                    openQueue.add(neighbor);
                }
            }
        }

        return Collections.emptyList();
    }

    private Node createNode(Position pos, double g, Position target) {
        Node node = new Node(pos);
        node.gCost = g;
        node.hCost = geometryService.calculateDistance(pos, target) * HEURISTIC_MULTIPLIER;
        node.fCost = node.gCost + node.hCost;
        return node;
    }

    private boolean isBlockedMove(Position from, Position to, List<Region> zones) {
        for (Region zone : zones) {
            if (geometryService.isPointInRegion(to, zone)) {
                return true;
            }
            if (geometryService.checkLineIntersectsRegion(from, to, zone)) {
                return true;
            }
        }
        return false;
    }

    private List<Position> buildPath(Node target) {
        List<Position> path = new ArrayList<>();
        Node current = target;

        while (current != null) {
            path.add(current.position);
            current = current.parent;
        }

        Collections.reverse(path);
        return path;
    }
}

