package com.edu.ilpsubmission1.integration;

import com.edu.ilpsubmission1.client.IlpRestClient;
import com.edu.ilpsubmission1.dtos.DeliveryPathResponse;
import com.edu.ilpsubmission1.dtos.MedDispatchRec;
import com.edu.ilpsubmission1.dtos.Position;
import com.edu.ilpsubmission1.dtos.Region;
import com.edu.ilpsubmission1.service.GeometryService;
import com.edu.ilpsubmission1.service.PathService;
import com.edu.ilpsubmission1.testutil.GeometryTestUtils;
import com.edu.ilpsubmission1.testutil.TestFixtures;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PathServiceRestrictedZoneIT {

    @MockitoBean
    IlpRestClient ilpRestClient;

    @Autowired
    PathService pathService;

    @Autowired
    GeometryService geometryService;

    @Test
    void deliveryPathNeverEntersOrCrossesRestrictedZone() {

        // Arrange
        when(ilpRestClient.getRestrictedAreas())
                .thenReturn(TestFixtures.noFlyZone());

        TestFixtures.stubSingleDrone(ilpRestClient);
        TestFixtures.stubSingleServicePoint(ilpRestClient);
        TestFixtures.stubDroneAvailability(ilpRestClient);

        MedDispatchRec dispatch = TestFixtures.singleDispatch();

        // Act
        DeliveryPathResponse response =
                pathService.calculateDeliveryPath(List.of(dispatch));

        // Assert
        assertFalse(response.getDronePaths().isEmpty(),
                "Expected at least one delivery path");

        // Convert restricted areas exactly as PathService does
        List<Region> zones = TestFixtures.noFlyZone().stream()
                .map(area -> new Region(
                        (String) area.get("name"),
                        ((List<?>) area.get("vertices")).stream()
                                .map(v -> {
                                    var map = (java.util.Map<?, ?>) v;
                                    return new Position(
                                            ((Number) map.get("lng")).doubleValue(),
                                            ((Number) map.get("lat")).doubleValue()
                                    );
                                })
                                .toList()
                ))
                .toList();

        response.getDronePaths().forEach(dp ->
                dp.getDeliveries().forEach(d -> {
                    var path = d.getFlightPath();

                    // Check no point is inside a restricted zone
                    path.forEach(p ->
                            assertFalse(
                                    GeometryTestUtils.inAnyRestrictedZone(p, zones),
                                    "Drone entered restricted zone at " + p
                            )
                    );

                    // Check no movement crosses a restricted zone
                    for (int i = 1; i < path.size(); i++) {
                        Position from = path.get(i - 1);
                        Position to = path.get(i);

                        assertFalse(
                                GeometryTestUtils.pathIntersectsRestrictedZone(from, to, zones),
                                "Drone crossed restricted zone between " + from + " and " + to
                        );
                    }
                })
        );
    }
}

