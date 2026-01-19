package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.client.IlpRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class DroneCatalogServiceParameterizedTest {

    @Mock
    IlpRestClient ilpClient;

    private DroneCatalogService service;

    @BeforeEach
    void setUp() {
        service = new DroneCatalogService(ilpClient);
    }
    @Tag("unit")
    @ParameterizedTest(name = "Cooling={0} -> expected IDs {1}")
    @MethodSource("coolingCases")
    void dronesWithCooling_variousInputs(
            boolean cooling,
            List<Integer> expectedIds,
            List<Map<String,Object>> drones) {

        when(ilpClient.getDrones()).thenReturn(drones);

        assertEquals(expectedIds, service.dronesWithCooling(cooling));
    }

    private static Stream<Arguments> coolingCases() {
        return Stream.of(
                Arguments.of(
                        true,
                        List.of(1),
                        List.of(
                                Map.of("id", 1, "capability", Map.of("cooling", true)),
                                Map.of("id", 2, "capability", Map.of("cooling", false))
                        )
                ),
                Arguments.of(
                        false,
                        List.of(2),
                        List.of(
                                Map.of("id", 1, "capability", Map.of("cooling", true)),
                                Map.of("id", 2, "capability", Map.of("cooling", false))
                        )
                )
        );
    }
}

