package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.client.IlpRestClient;
import com.edu.ilpsubmission1.dtos.Drone;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides query methods (dronesWithCooling, droneDetails, queryAsPath, query).
 */
@Service
public class DroneCatalogService {

    private final IlpRestClient ilpClient;

    public DroneCatalogService(IlpRestClient ilpClient) {
        this.ilpClient = ilpClient;
    }

    public List<Integer> dronesWithCooling(boolean state) {
        var drones = ilpClient.getDrones();
        if (drones == null) return Collections.emptyList();
        return drones.stream()
                .filter(Objects::nonNull)
                .filter(d -> {
                    Map cap = (Map) d.get("capability");
                    if (cap == null) return !state ? true : false;
                    Boolean cooling = (Boolean) cap.getOrDefault("cooling", Boolean.FALSE);
                    return cooling == state;
                })
                .map(d -> parseDroneId(d.get("id")))
                .collect(Collectors.toList());
    }
    public Optional<Map<String,Object>> droneDetails(int id) {
        var drones = ilpClient.getDrones();
        if (drones == null) return Optional.empty();
        return drones.stream()
                .filter(Objects::nonNull)
                .filter(d -> id == parseDroneId(d.get("id")))
                .findFirst()
                .map(d -> (Map<String,Object>) d);
    }
    public List<Integer> queryAsPath(String attribute, String value) {
        var drones = ilpClient.getDrones();
        if (drones == null) return List.of();

        return drones.stream()
                .filter(d -> attributeMatches(d, attribute, value))
                .map(d -> parseDroneId(d.get("id")))
                .collect(Collectors.toList());
    }

    public List<Integer> query(List<Map<String,String>> criteria) {
        var drones = ilpClient.getDrones();
        if (drones == null) return List.of();
        return drones.stream()
                .filter(d -> {
                    for (Map<String,String> crit : criteria) {
                        String attr = crit.get("attribute");
                        String op = crit.get("operator");
                        String val = crit.get("value");
                        if (!attributeMatchesWithOperator(d, attr, op, val)) return false;
                    }
                    return true;
                })
                .map(d -> parseDroneId(d.get("id")))
                .collect(Collectors.toList());
    }

    public Optional<Drone> findDroneDetailsById(String id) {

        Drone[] allDrones = ilpClient.getDronesJson();

        return Arrays.stream(allDrones).filter(drone -> drone.id().equals(id)).findFirst();
    }
    // Helpers

    @SuppressWarnings("unchecked")
    private boolean attributeMatches(Map<String, Object> drone, String attribute, String value) {
        Object found = findAttributeValue(drone, attribute);
        if (found == null) return false;

        if (found instanceof Number) {
            try {
                double numericValue = Double.parseDouble(value);
                return Math.abs(((Number) found).doubleValue() - numericValue) < 1e-9;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        if (found instanceof Boolean) {
            return Boolean.valueOf(value).equals(found);
        }

        return found.toString().equalsIgnoreCase(value);
    }

    @SuppressWarnings("unchecked")
    private Object findAttributeValue(Map<String, Object> map, String key) {
        // Direct match
        if (map.containsKey(key)) {
            return map.get(key);
        }

        // Recursive search inside nested maps
        for (Object val : map.values()) {
            if (val instanceof Map) {
                Object found = findAttributeValue((Map<String, Object>) val, key);
                if (found != null) return found;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private boolean attributeMatchesWithOperator(Map<String, Object> drone, String attribute, String operator, String value) {
        Object found = findAttributeValue(drone, attribute);
        if (found == null) return false;

        if (found instanceof Number) {
            double f = ((Number) found).doubleValue();
            double v = Double.parseDouble(value);
            return switch (operator) {
                case "=" -> f == v;
                case "!=" -> f != v;
                case "<" -> f < v;
                case ">" -> f > v;
                default -> false;
            };
        } else if (found instanceof Boolean) {
            boolean fb = (Boolean) found;
            boolean vb = Boolean.parseBoolean(value);
            return "=".equals(operator) && fb == vb;
        } else {
            return "=".equals(operator) && String.valueOf(found).equalsIgnoreCase(value);
        }
    }
    private int parseDroneId(Object idObj) {
        if (idObj == null) return -1;

        if (idObj instanceof Number num) {
            return num.intValue();
        }

        try {
            return Integer.parseInt(idObj.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
