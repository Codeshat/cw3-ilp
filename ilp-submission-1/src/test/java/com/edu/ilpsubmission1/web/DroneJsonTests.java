package com.edu.ilpsubmission1.web;

import com.edu.ilpsubmission1.dtos.Position;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class DroneJsonTests {

    @Autowired
    private JacksonTester<Position> json;

    @Test
    void positionSerializeDeserialize() throws Exception {
        Position p = new Position();
        p.setLng(-3.192473);
        p.setLat(55.946233);

        var write = json.write(p);
        assertThat(write).extractingJsonPathNumberValue("@.lng").isEqualTo(p.getLng());
        assertThat(write).extractingJsonPathNumberValue("@.lat").isEqualTo(p.getLat());

        var parsed = json.parseObject(write.getJson());
        assertThat(parsed.getLng()).isEqualTo(p.getLng());
        assertThat(parsed.getLat()).isEqualTo(p.getLat());
    }
}
