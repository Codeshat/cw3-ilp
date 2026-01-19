package com.edu.ilpsubmission1.service;

import com.edu.ilpsubmission1.client.IlpRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DroneCatalogServiceTest {

    @Mock
    IlpRestClient client;

    private DroneCatalogService service;

    @BeforeEach
    void setUp() {
        service = new DroneCatalogService(client);
    }

    @Test
    void givenCoolingDrones_whenQuery_thenOnlyCoolingReturned() {
        Map<String,Object> drone = Map.of(
                "id", 1,
                "capability", Map.of("cooling", true)
        );

        when(client.getDrones()).thenReturn(List.of(drone));

        List<Integer> result = service.dronesWithCooling(true);

        assertEquals(List.of(1), result);
    }

    @Test
    void givenMissingCapability_whenQueryCoolingFalse_thenIncluded() {
        Map<String,Object> drone = Map.of("id", 2);

        when(client.getDrones()).thenReturn(List.of(drone));

        assertEquals(List.of(2), service.dronesWithCooling(false));
    }
}

