package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.client.IlpRestClient;
import com.edu.ilpsubmission1.dtos.MedDispatchRec;
import com.edu.ilpsubmission1.dtos.Position;
import org.springframework.stereotype.Service;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class AvailabilityService {
    private static final double STEP = 0.00015;

    private final IlpRestClient ilpClient;
    private final DroneService droneService;


    public AvailabilityService(IlpRestClient ilpClient, DroneService droneService) {
        this.ilpClient = ilpClient;
        this.droneService = droneService;

    }

    public List<String> queryAvailableDrones(List<MedDispatchRec> dispatches) {

        if (dispatches == null || dispatches.isEmpty()) {
            return Collections.emptyList();
        }

        var allDrones = ilpClient.getDrones();
        var dronesBySP = ilpClient.getDronesForServicePoints();
        var servicePoints = ilpClient.getServicePoints();

        if (allDrones == null || dronesBySP == null || servicePoints == null) {
            return Collections.emptyList();
        }

        // ---- Build drone lookup map ----
        Map<String, Map<String, Object>> droneMap = new HashMap<>();
        for (Map<String, Object> d : allDrones) {
            String id = (String) d.get("id");
            if (id != null) droneMap.put(id, d);
        }

        // ---- Build service point positions ----
        Map<Integer, Position> spPositions = new HashMap<>();
        for (Map<String, Object> sp : servicePoints) {
            Integer spId = ((Number) sp.get("id")).intValue();

            Map<String, Object> loc = (Map<String, Object>) sp.get("location");
            if (loc != null) {
                Position p = new Position(
                        getDoubleValue(loc.get("lng"), 0.0),
                        getDoubleValue(loc.get("lat"), 0.0)
                );
                spPositions.put(spId, p);
            }
        }

        List<String> result = new ArrayList<>();

        // ---- Iterate drones grouped by service point ----
        for (Map<String, Object> spGroup : dronesBySP) {

            Integer spId = ((Number) spGroup.get("servicePointId")).intValue();
            Position spPos = spPositions.get(spId);
            if (spPos == null) continue;

            List<Map<String, Object>> dronesAtSP =
                    (List<Map<String, Object>>) spGroup.get("drones");
            if (dronesAtSP == null) continue;

            for (Map<String, Object> droneAtSP : dronesAtSP) {

                String droneId = (String) droneAtSP.get("id");
                if (droneId == null) continue;

                Map<String, Object> fullDrone = droneMap.get(droneId);
                if (fullDrone == null) continue;

                // ---- Capability ----
                Map<String, Object> capability =
                        (Map<String, Object>) fullDrone.get("capability");
                if (capability == null) continue;

                double droneCapacity = getDoubleValue(capability.get("capacity"), 0.0);
                boolean droneCooling = getBooleanValue(capability.get("cooling"), false);
                boolean droneHeating = getBooleanValue(capability.get("heating"), false);
                int maxMoves = getIntValue(capability.get("maxMoves"), Integer.MAX_VALUE);
                double costPerMove = getDoubleValue(capability.get("costPerMove"), 0.0);
                double costInitial = getDoubleValue(capability.get("costInitial"), 0.0);
                double costFinal = getDoubleValue(capability.get("costFinal"), 0.0);

                // ---- Drone availability ----
                List<Map<String, Object>> availability =
                        (List<Map<String, Object>>) droneAtSP.get("availability");

                boolean canHandleAll = true;

                // ---- Evaluate each dispatch  ----
                for (MedDispatchRec dispatch : dispatches) {

                    // ----------- REQUIREMENTS -----------
                    if (dispatch.getRequirements() != null) {
                        var req = dispatch.getRequirements();

                        double reqCap = req.getCapacity() == null ? 0.0 : req.getCapacity();
                        boolean reqCooling = Boolean.TRUE.equals(req.getCooling());
                        boolean reqHeating = Boolean.TRUE.equals(req.getHeating());

                        if (droneCapacity < reqCap ||
                                (reqCooling && !droneCooling) ||
                                (reqHeating && !droneHeating)) {
                            canHandleAll = false;
                            break;
                        }
                    }

                    // ----------- TIME CHECK -----------
                    if (dispatch.getDate() != null &&
                            dispatch.getTime() != null &&
                            availability != null) {

                        LocalDate date = dispatch.getDate();
                        LocalTime time = dispatch.getTime();
                        DayOfWeek day = date.getDayOfWeek();

                        boolean available = false;

                        for (Map<String, Object> slot : availability) {

                            if (!slot.get("dayOfWeek").toString()
                                    .equalsIgnoreCase(day.name()))
                                continue;

                            LocalTime start = parseTime(slot.get("from"));
                            LocalTime end = parseTime(slot.get("until"));

                            if (start != null && end != null &&
                                    !time.isBefore(start) &&
                                    !time.isAfter(end)) {
                                available = true;
                                break;
                            }
                        }

                        if (!available) {
                            canHandleAll = false;
                            break;
                        }
                    }

                    // ----------- POSITION  -----------
                    Position deliveryPos = dispatch.getDelivery();
                    if (deliveryPos == null) {
                        canHandleAll = false;
                        break;
                    }

                    double distance = droneService.getDistance(spPos, deliveryPos);
                    int moves = (int) Math.ceil(distance / STEP);

                    // ----------- COST -----------
                    if (dispatch.getRequirements() != null &&
                            dispatch.getRequirements().getMaxCost() != null) {

                        double maxCost = dispatch.getRequirements().getMaxCost();

                        double totalEstCost =
                                costInitial + costFinal + moves * costPerMove;

                        double shareCost = totalEstCost / dispatches.size();

                        if (shareCost > maxCost) {
                            canHandleAll = false;
                            break;
                        }
                    }
                }

                if (canHandleAll) {
                    result.add(droneId);
                }
            }
        }

        return result;
    }
    private LocalTime parseTime(Object t) {
        if (t == null) return null;

        if (t instanceof String s) {
            return LocalTime.parse(s);
        }

        if (t instanceof Map<?, ?> map) {
            Integer h = getIntValue(map.get("hour"), 0);
            Integer m = getIntValue(map.get("minute"), 0);
            Integer s = getIntValue(map.get("second"), 0);
            return LocalTime.of(h, m, s);
        }

        return null;
    }



    // Helper methods
    private double getDoubleValue(Object obj, double defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getIntValue(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBooleanValue(Object obj, boolean defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Boolean) return (Boolean) obj;
        return Boolean.parseBoolean(obj.toString());

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
