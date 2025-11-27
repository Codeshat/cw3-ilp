package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.dtos.*;

/**
 * represents node for the A* search space
 * Implements Comparable for us in PriorityQueue
 */
public class Node implements Comparable<Node> {
    public final Position position;
    public double gCost;
    public double hCost;
    public double fCost;
    public Node parent;

    public Node(Position position) {
        this.position = position;
    }

    @Override
    public int compareTo(Node other) {
        return Double.compare(this.fCost, other.fCost);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return position.equals(node.position);
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }
}