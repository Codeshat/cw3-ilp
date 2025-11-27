package com.edu.ilpsubmission1.web;

import com.edu.ilpsubmission1.dtos.Position;
import com.edu.ilpsubmission1.dtos.Region;
import com.edu.ilpsubmission1.service.DroneService;
import com.edu.ilpsubmission1.exception.InvalidRegionException;
import com.edu.ilpsubmission1.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DroneController.class)
@Import(DroneControllerAdvice.class)
class DroneControllerMvcTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    DroneService droneService;
/*
    @Test
    void uid_returnsStudentId() throws Exception {
        mockMvc.perform(get("/api/v1/uid"))
                .andExpect(status().isOk())
                .andExpect(content().string("s2531655"));
    }

    @Test
    void distanceTo_validRequest_returns200AndNumber() throws Exception {
        Map<String, Object> request = Map.of(
                "position1", Map.of("lng", -3.192473, "lat", 55.946233),
                "position2", Map.of("lng", -3.192473, "lat", 55.942617)
        );
        double expected = 0.003616000000000952;
        given(droneService.getDistance(any(Position.class), any(Position.class))).willReturn(expected);

        var result = mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        double returned = objectMapper.readValue(body, Double.class);
        // compare numerically with EPS tolerance
        org.assertj.core.api.Assertions.assertThat(returned).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-12));
        verify(droneService).getDistance(any(Position.class), any(Position.class));
    }

    @Test
    void distanceTo_missingField_returns400() throws Exception {
        Map<String, Object> bad = Map.of("position1", Map.of("lng", -3.192473, "lat", 55.946233));
        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nextPosition_valid_returnsPositionJson() throws Exception {
        Map<String, Object> req = Map.of(
                "start", Map.of("lng", -3.192473, "lat", 55.946233),
                "angle", 45
        );
        Position next = new Position();
        next.setLng(-3.192366933982822);
        next.setLat(55.946339066017174);
        given(droneService.nextPosition(any(Position.class), eq(45.0))).willReturn(next);

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lng").value(next.getLng()))
                .andExpect(jsonPath("$.lat").value(next.getLat()));
    }

    @Test
    void nextPosition_invalidAngle_serviceThrowsBadRequest_results400() throws Exception {
        Map<String, Object> req = Map.of(
                "start", Map.of("lng", -3.192473, "lat", 55.946233),
                "angle", 30
        );
        given(droneService.nextPosition(any(Position.class), eq(30.0)))
                .willThrow(new BadRequestException("angle must be one of the 16 compass directions"));

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("angle must be one of the 16 compass directions"));
    }

    @Test
    void isInRegion_validInside_returnsTrue() throws Exception {
        Map<String,Object> req = Map.of(
                "position", Map.of("lng",-3.188396,"lat",55.944425),
                "region", Map.of(
                        "name","central",
                        "vertices", List.of(
                                Map.of("lng",-3.192473,"lat",55.946233),
                                Map.of("lng",-3.192473,"lat",55.942617),
                                Map.of("lng",-3.184319,"lat",55.942617),
                                Map.of("lng",-3.184319,"lat",55.946233),
                                Map.of("lng",-3.192473,"lat",55.946233)
                        )
                )
        );
        given(droneService.isInRegion(any(Position.class), any(Region.class))).willReturn(true);

        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void isInRegion_invalidRegion_serviceThrowsInvalidRegion_results400() throws Exception {
        Map<String,Object> req = Map.of(
                "position", Map.of("lng",-3.188396,"lat",55.944425),
                "region", Map.of("name","bad","vertices", List.of(
                        Map.of("lng",0,"lat",0),
                        Map.of("lng",1,"lat",0),
                        Map.of("lng",0,"lat",1)
                ))
        );

        given(droneService.isInRegion(any(Position.class), any(Region.class)))
                .willThrow(new InvalidRegionException("Region is not closed (first and last vertex must be the same)."));

        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Region is not closed (first and last vertex must be the same)."));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        String bad = "{\"position\": {\"lng\": -3.188396,\"lat\": 55.944425}, \"region\": ";
        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest());
    }

 */
}


