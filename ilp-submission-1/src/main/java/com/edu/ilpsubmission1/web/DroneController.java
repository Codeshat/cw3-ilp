package com.edu.ilpsubmission1.web;

import com.edu.ilpsubmission1.client.IlpRestClient;
import com.edu.ilpsubmission1.dtos.*;
import com.edu.ilpsubmission1.service.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DroneController {

    private final DroneService droneService;
    private final DroneCatalogService catalogService;
    private final AvailabilityService availabilityService;
    private final GeometryService geometryService;
    private final PathService pathService;
    private final IlpRestClient ilpClient;

    @GetMapping("/uid")
    public String uid() {
        return "s2531655";
    }

    @PostMapping("/distanceTo")
    public ResponseEntity<Double> distanceTo(@Valid @RequestBody DistanceRequest req) {
        double dist = droneService.getDistance(req.getPosition1(), req.getPosition2());
        return ResponseEntity.ok().body(dist);
    }

    @PostMapping("/isCloseTo")
    public ResponseEntity<Boolean> isCloseTo(@Valid @RequestBody DistanceRequest req) {
       boolean close = droneService.isCloseTo(req.getPosition1(), req.getPosition2());
       return ResponseEntity.ok(close);
   }

    @PostMapping("/nextPosition")
    public ResponseEntity<Position> nextPosition(@Valid @RequestBody NextPositionRequest req) {
        Position next = droneService.nextPosition(req.getStart(), req.getAngle());
        return ResponseEntity.ok(next);
    }

    @PostMapping("/isInRegion")
    public ResponseEntity<Boolean> isInRegion(@Valid @RequestBody RegionRequest req) {
        boolean inside = droneService.isInRegion(req.getPosition(), req.getRegion());
        return ResponseEntity.ok(inside);
    }
    @GetMapping("/dronesWithCooling/{state}")
    public ResponseEntity<List<Integer>> dronesWithCooling(@PathVariable boolean state) {
        return ResponseEntity.ok(catalogService.dronesWithCooling(state));
    }

    @GetMapping("/droneDetails/{id}")
    public ResponseEntity<?> droneDetails(@PathVariable int id) {
        return catalogService.droneDetails(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Drone not found")));
    }
    @GetMapping("/queryAsPath/{attribute}/{value}")
    public ResponseEntity<List<Integer>> queryAsPath(@PathVariable String attribute, @PathVariable String value) {
        return ResponseEntity.ok(catalogService.queryAsPath(attribute, value));
    }

    @PostMapping("/query")
    public ResponseEntity<List<Integer>> query(@RequestBody List<Map<String,String>> criteria) {
        return ResponseEntity.ok(catalogService.query(criteria));
    }
    @PostMapping("/queryAvailableDrones")
    public ResponseEntity<List<String>> queryAvailableDrones(
            @RequestBody List<MedDispatchRec> dispatches) {

        List<String> availableDrones = availabilityService.queryAvailableDrones(dispatches);
        return ResponseEntity.ok(availableDrones);
    }

    @PostMapping("/calcDeliveryPath")
    public ResponseEntity<DeliveryPathResponse> calcDeliveryPath(@RequestBody List<MedDispatchRec> dispatches) {
        return ResponseEntity.ok(pathService.calculateDeliveryPath(dispatches));
    }

    @PostMapping("/calcDeliveryPathAsGeoJson")
    public ResponseEntity<GeoJsonResponse> calcDeliveryPathAsGeoJson(@RequestBody List<MedDispatchRec> dispatches) {
        return ResponseEntity.ok(pathService.calculateDeliveryPathAsGeoJson(dispatches));
    }
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDroneStats() {

            return ResponseEntity.ok(Map.of(
                    "total", 25,
                    "available", 18,
                    "active", 5,
                    "inMaintenance", 2
            ));

    }

    @GetMapping("/recent-activity")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity() {
        // Mock data - replace with real data if available
        return ResponseEntity.ok(List.of(
                Map.of("time", "2 min ago", "event", "Delivery completed", "drone", "Drone #142"),
                Map.of("time", "5 min ago", "event", "En route to destination", "drone", "Drone #089"),
                Map.of("time", "12 min ago", "event", "Package picked up", "drone", "Drone #203"),
                Map.of("time", "18 min ago", "event", "Returned to base", "drone", "Drone #156"),
                Map.of("time", "25 min ago", "event", "Delivery completed", "drone", "Drone #091")
        ));
    }






}

