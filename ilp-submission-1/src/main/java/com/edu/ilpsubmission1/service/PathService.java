package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.client.IlpRestClient;
import com.edu.ilpsubmission1.dtos.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PathService {

    private final DroneService droneService;
    private final GeometryService geometryService;
    private final IlpRestClient ilpRestClient;
    private final AStarPathfinder pathfinder;
    private final AvailabilityService availabilityService;
    private final DroneCatalogService droneCatalogService;

    private record PathSegment(Position start, Position end) {}

    private record Trip(String droneId, ServicePoint startPoint, Object deliveryData,
                        double totalCost, int totalMoves) {
        public DeliveryPathResponse.DronePath toDronePath() {
            if (deliveryData instanceof DeliveryPathResponse.Delivery single) {
                return new DeliveryPathResponse.DronePath(droneId, List.of(single));
            } else if (deliveryData instanceof DeliveryPathResponse.DronePath multi) {
                return multi;
            }
            throw new IllegalStateException("Invalid delivery data type");
        }
    }

    public PathService(DroneService droneService, GeometryService geometryService,
                       IlpRestClient ilpRestClient, AStarPathfinder pathfinder,
                       AvailabilityService availabilityService,
                       DroneCatalogService droneCatalogService) {
        this.droneService = droneService;
        this.geometryService = geometryService;
        this.ilpRestClient = ilpRestClient;
        this.pathfinder = pathfinder;
        this.availabilityService = availabilityService;
        this.droneCatalogService = droneCatalogService;
    }

    public DeliveryPathResponse calculateDeliveryPath(List<MedDispatchRec> allDispatches) {
        List<Region> restrictedZones = buildRestrictedZones();
        Map<PathSegment, List<Position>> cachedPaths = new HashMap<>();

        Map<LocalDate, List<MedDispatchRec>> groupedByDate = allDispatches.stream()
                .collect(Collectors.groupingBy(MedDispatchRec::getDate));

        List<DeliveryPathResponse.DronePath> completedPaths = new ArrayList<>();
        double aggregateCost = 0;
        int aggregateMoves = 0;

        for (Map.Entry<LocalDate, List<MedDispatchRec>> dateEntry : groupedByDate.entrySet()) {
            List<MedDispatchRec> pending = new ArrayList<>(dateEntry.getValue());

            while (!pending.isEmpty()) {
                boolean handled = attemptMultiDelivery(pending, completedPaths,
                        restrictedZones, cachedPaths);

                if (!handled) {
                    Trip singleTrip = processSingleDispatch(pending.get(0),
                            restrictedZones, cachedPaths);
                    if (singleTrip != null) {
                        completedPaths.add(singleTrip.toDronePath());
                        aggregateCost += singleTrip.totalCost;
                        aggregateMoves += singleTrip.totalMoves;
                    }
                    pending.remove(0);
                }
            }
        }

        return new DeliveryPathResponse(aggregateCost, aggregateMoves, completedPaths);
    }

    private boolean attemptMultiDelivery(List<MedDispatchRec> pending,
                                         List<DeliveryPathResponse.DronePath> paths,
                                         List<Region> zones,
                                         Map<PathSegment, List<Position>> cache) {
        for (int count = pending.size(); count >= 2; count--) {
            List<MedDispatchRec> batch = pending.subList(0, count);
            List<String> candidates = availabilityService.queryAvailableDrones(batch);

            if (!candidates.isEmpty()) {
                Optional<Trip> tripResult = planMultiDeliveryTrip(candidates.get(0),
                        batch, zones, cache);
                if (tripResult.isPresent()) {
                    Trip t = tripResult.get();
                    paths.add(t.toDronePath());

                    Set<Long> completed = t.toDronePath().getDeliveries().stream()
                            .map(DeliveryPathResponse.Delivery::getDeliveryId)
                            .collect(Collectors.toSet());
                    pending.removeIf(d -> completed.contains(d.getId()));
                    return true;
                }
            }
        }
        return false;
    }

    private Trip processSingleDispatch(MedDispatchRec dispatch, List<Region> zones,
                                       Map<PathSegment, List<Position>> cache) {
        List<String> availableDrones = availabilityService.queryAvailableDrones(List.of(dispatch));
        if (availableDrones.isEmpty()) return null;

        return planSingleDeliveryTrip(availableDrones.get(0), dispatch, zones, cache)
                .orElse(null);
    }

    private Optional<Trip> planSingleDeliveryTrip(String droneId, MedDispatchRec dispatch,
                                                  List<Region> zones,
                                                  Map<PathSegment, List<Position>> cache) {
        Drone drone = droneCatalogService.findDroneDetailsById(droneId).orElse(null);
        ServicePoint base = findServicePointForDrone(droneId).orElse(null);
        if (drone == null || base == null) return Optional.empty();

        List<Position> outbound = fetchOrComputePath(base.location(),
                dispatch.getDelivery(), zones, cache);
        if (outbound.isEmpty()) return Optional.empty();

        Position deliveryPoint = outbound.get(outbound.size() - 1);
        List<Position> inbound = fetchOrComputePath(deliveryPoint,
                base.location(), zones, cache);
        if (inbound.isEmpty()) return Optional.empty();

        List<Position> completePath = new ArrayList<>(outbound);
        completePath.add(deliveryPoint);
        completePath.addAll(inbound.stream().skip(1).toList());

        if (!completePath.get(completePath.size() - 1).equals(base.location())) {
            completePath.add(base.location());
        }

        int moves = completePath.size() - 1;
        if (moves > drone.capability().maxMoves()) return Optional.empty();

        double cost = drone.capability().costInitial() + drone.capability().costFinal()
                + (moves * drone.capability().costPerMove());

        if (dispatch.getRequirements().getMaxCost() != null
                && cost > dispatch.getRequirements().getMaxCost()) {
            return Optional.empty();
        }

        DeliveryPathResponse.Delivery segment =
                new DeliveryPathResponse.Delivery(dispatch.getId(), completePath);
        return Optional.of(new Trip(droneId, base, segment, cost, moves));
    }

    private Optional<Trip> planMultiDeliveryTrip(String droneId, List<MedDispatchRec> dispatches,
                                                 List<Region> zones,
                                                 Map<PathSegment, List<Position>> cache) {
        Drone drone = droneCatalogService.findDroneDetailsById(droneId).orElse(null);
        ServicePoint base = findServicePointForDrone(droneId).orElse(null);
        if (drone == null || base == null) return Optional.empty();

        List<MedDispatchRec> sequence = orderByProximity(base.location(), dispatches);
        List<DeliveryPathResponse.Delivery> segments = new ArrayList<>();
        Position current = base.location();
        int totalSteps = 0;

        for (int idx = 0; idx < sequence.size(); idx++) {
            MedDispatchRec dispatch = sequence.get(idx);
            List<Position> pathSegment = fetchOrComputePath(current,
                    dispatch.getDelivery(), zones, cache);
            if (pathSegment.isEmpty()) return Optional.empty();

            Position target = pathSegment.get(pathSegment.size() - 1);
            List<Position> flightPath = constructFlightPath(pathSegment, target, current,
                    base.location(), idx,
                    sequence.size(), zones, cache);
            if (flightPath == null) return Optional.empty();

            segments.add(new DeliveryPathResponse.Delivery(dispatch.getId(), flightPath));
            totalSteps += flightPath.size() - 1;
            current = target;
        }

        if (totalSteps > drone.capability().maxMoves()) return Optional.empty();

        double totalCost = drone.capability().costInitial() + drone.capability().costFinal()
                + (totalSteps * drone.capability().costPerMove());

        double perDispatchCost = totalCost / dispatches.size();
        for (MedDispatchRec d : dispatches) {
            if (d.getRequirements().getMaxCost() != null
                    && perDispatchCost > d.getRequirements().getMaxCost()) {
                return Optional.empty();
            }
        }

        DeliveryPathResponse.DronePath combined =
                new DeliveryPathResponse.DronePath(droneId, segments);
        return Optional.of(new Trip(droneId, base, combined, totalCost, totalSteps));
    }

    private List<Position> constructFlightPath(List<Position> pathToTarget, Position target,
                                               Position currentPos, Position basePos,
                                               int index, int total,
                                               List<Region> zones,
                                               Map<PathSegment, List<Position>> cache) {
        List<Position> path = new ArrayList<>();

        if (index == 0) {
            path.addAll(pathToTarget);
            path.add(target);

            if (total == 1) {
                List<Position> returnPath = fetchOrComputePath(target, basePos, zones, cache);
                if (returnPath.isEmpty()) return null;
                path.addAll(returnPath.stream().skip(1).toList());
                if (!path.get(path.size() - 1).equals(basePos)) {
                    path.add(basePos);
                }
            }
        } else {
            path.addAll(pathToTarget);
            path.add(target);

            if (index == total - 1) {
                List<Position> returnPath = fetchOrComputePath(target, basePos, zones, cache);
                if (returnPath.isEmpty()) return null;
                path.addAll(returnPath.stream().skip(1).toList());
                if (!path.get(path.size() - 1).equals(basePos)) {
                    path.add(basePos);
                }
            }
        }

        return path;
    }

    private List<MedDispatchRec> orderByProximity(Position start,
                                                  List<MedDispatchRec> dispatches) {
        List<MedDispatchRec> ordered = new ArrayList<>();
        List<MedDispatchRec> remaining = new ArrayList<>(dispatches);
        Position currentPos = start;

        while (!remaining.isEmpty()) {
            Position pos = currentPos;
            MedDispatchRec closest = remaining.stream()
                    .min(Comparator.comparingDouble(d ->
                            geometryService.calculateDistance(pos, d.getDelivery())))
                    .orElseThrow();

            ordered.add(closest);
            currentPos = closest.getDelivery();
            remaining.remove(closest);
        }

        return ordered;
    }

    public GeoJsonResponse calculateDeliveryPathAsGeoJson(List<MedDispatchRec> dispatches) {
        Set<LocalDate> dates = dispatches.stream()
                .map(MedDispatchRec::getDate)
                .collect(Collectors.toSet());

        if (dates.size() > 1) {
            return GeoJsonResponse.fromPath(List.of());
        }

        DeliveryPathResponse response = calculateDeliveryPath(dispatches);
        if (response.getDronePaths().isEmpty()) {
            return GeoJsonResponse.fromPath(List.of());
        }

        Set<String> droneIds = response.getDronePaths().stream()
                .map(DeliveryPathResponse.DronePath::getDroneId)
                .collect(Collectors.toSet());

        if (droneIds.size() > 1) {
            return GeoJsonResponse.fromPath(List.of());
        }

        return convertToGeoJson(response);
    }

    private GeoJsonResponse convertToGeoJson(DeliveryPathResponse response) {
        List<Position> consolidated = new ArrayList<>();

        for (DeliveryPathResponse.DronePath dp : response.getDronePaths()) {
            for (int i = 0; i < dp.getDeliveries().size(); i++) {
                DeliveryPathResponse.Delivery delivery = dp.getDeliveries().get(i);
                if (consolidated.isEmpty()) {
                    consolidated.addAll(delivery.getFlightPath());
                } else {
                    consolidated.addAll(delivery.getFlightPath().stream().skip(1).toList());
                }
            }
        }

        return GeoJsonResponse.fromPath(consolidated);
    }

    private List<Position> fetchOrComputePath(Position start, Position end,
                                              List<Region> zones,
                                              Map<PathSegment, List<Position>> cache) {
        PathSegment key = new PathSegment(start, end);
        return cache.computeIfAbsent(key, k -> pathfinder.findPath(k.start(), k.end(), zones));
    }

    private Optional<ServicePoint> findServicePointForDrone(String droneId) {
        DroneForServicePoint[] droneAvailability = ilpRestClient.getDronesForServicePointsJson();
        ServicePoint[] allPoints = ilpRestClient.getServicePointsJson();

        for (DroneForServicePoint availability : droneAvailability) {
            if (availability.drones().stream().anyMatch(d -> d.id().equals(droneId))) {
                return Arrays.stream(allPoints)
                        .filter(sp -> sp.id().equals(availability.servicePointId()))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    private List<Region> buildRestrictedZones() {
        return ilpRestClient.getRestrictedAreas().stream()
                .map(area -> {
                    String name = (String) area.get("name");
                    List<Map<String, Object>> rawVertices =
                            (List<Map<String, Object>>) area.get("vertices");
                    List<Position> vertices = rawVertices.stream()
                            .map(v -> new Position(
                                    ((Number) v.get("lng")).doubleValue(),
                                    ((Number) v.get("lat")).doubleValue()
                            ))
                            .toList();
                    return new Region(name, vertices);
                })
                .collect(Collectors.toList());
    }
}
