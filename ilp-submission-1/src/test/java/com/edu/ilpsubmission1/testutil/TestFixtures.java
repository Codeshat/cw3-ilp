package com.edu.ilpsubmission1.testutil;

import com.edu.ilpsubmission1.client.IlpRestClient;
import com.edu.ilpsubmission1.dtos.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

public final class TestFixtures {

    private TestFixtures() {}

    /* -----------------------------
       DRONES
       ----------------------------- */

    public static void stubSingleDrone(IlpRestClient client) {

        // Used by DroneCatalogService (JSON path)
        when(client.getDronesJson()).thenReturn(new Drone[]{
                new Drone(
                        "1",
                        "Test Drone",
                        new Drone.Capability(
                                true,   // cooling
                                true,   // heating
                                4.0,    // capacity
                                2000,   // maxMoves
                                0.01,   // costPerMove
                                4.3,    // costInitial
                                6.5     // costFinal
                        )
                )
        });

        // Used by legacy/query paths
        when(client.getDrones()).thenReturn(List.of(
                Map.of(
                        "id", "1",
                        "name", "Test Drone",
                        "capability", Map.of(
                                "cooling", true,
                                "heating", true,
                                "capacity", 4.0,
                                "maxMoves", 2000,
                                "costPerMove", 0.01,
                                "costInitial", 4.3,
                                "costFinal", 6.5
                        )
                )
        ));
    }

    /* -----------------------------
       SERVICE POINTS
       ----------------------------- */

    public static void stubSingleServicePoint(IlpRestClient client) {

        when(client.getServicePointsJson()).thenReturn(new ServicePoint[]{
                new ServicePoint(
                        1L,
                        "Appleton Tower",
                        new Position(-3.1863580788986368, 55.94468066708487)

                )
        });
        when(client.getServicePoints()).thenReturn(List.of(
                Map.of(
                        "id", 1,
                        "name", "Appleton Tower",
                        "location", Map.of(
                                "lng", -3.1863580788986368,
                                "lat", 55.94468066708487,
                                "alt", 50.0
                        )
                )
        ));

    }

    /* -----------------------------
       DRONES FOR SERVICE POINTS
       ----------------------------- */

    public static void stubDroneAvailability(IlpRestClient client) {

        when(client.getDronesForServicePointsJson()).thenReturn(
                new DroneForServicePoint[]{
                        new DroneForServicePoint(
                                1L,
                                List.of(
                                        new DroneForServicePoint.DroneAvailability(
                                                "1",
                                                List.of(
                                                        new DroneForServicePoint.Availability(
                                                                DayOfWeek.MONDAY.name(),
                                                                "00:00:00",
                                                                "23:59:59"
                                                        )
                                                )
                                        )
                                )
                        )
                }
        );
        when(client.getDronesForServicePoints()).thenReturn(List.of(
                Map.of(
                        "servicePointId", 1,
                        "drones", List.of(
                                Map.of(
                                        "id", "1",
                                        "availability", List.of(
                                                Map.of(
                                                        "dayOfWeek", "MONDAY",
                                                        "from", "00:00:00",
                                                        "until", "23:59:59"
                                                )
                                        )
                                )
                        )
                )
        ));

    }

    /* -----------------------------
       DISPATCH
       ----------------------------- */

    public static MedDispatchRec singleDispatch() {
        MedDispatchRec rec = new MedDispatchRec();
        rec.setId(1001L);
        rec.setDate(LocalDate.of(2025, 11, 10));
        rec.setTime(LocalTime.NOON);

        MedDispatchRec.Requirements req = new MedDispatchRec.Requirements();
        req.setCapacity(2.0);
        req.setCooling(true);
        req.setHeating(true);
        req.setMaxCost(20.0);

        rec.setRequirements(req);
        rec.setDelivery(new Position(-3.184, 55.945));

        return rec;
    }

    /* -----------------------------
       RESTRICTED ZONE
       ----------------------------- */

    public static List<Map<String, Object>> noFlyZone() {
        return List.of(
                Map.of(
                        "name", "George Square Area",
                        "vertices", List.of(
                                Map.of("lng", -3.190578818321228, "lat", 55.94402412577528),
                                Map.of("lng", -3.1899887323379517, "lat", 55.94284650540911),
                                Map.of("lng", -3.187097311019897, "lat", 55.94328811724263),
                                Map.of("lng", -3.187682032585144, "lat", 55.944477740393744),
                                Map.of("lng", -3.190578818321228, "lat", 55.94402412577528)
                        )
                )
        );
    }
}
