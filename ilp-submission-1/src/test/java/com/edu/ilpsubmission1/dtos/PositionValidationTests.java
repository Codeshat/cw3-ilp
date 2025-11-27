package com.edu.ilpsubmission1.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PositionValidationTests {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validPositionHasNoViolations() {
        Position p = new Position();
        p.setLng(-3.192473);
        p.setLat(55.946233);
        Set<ConstraintViolation<Position>> violations = validator.validate(p);
        assertThat(violations).isEmpty();
    }

    @Test
    void nullLatitudeFailsValidation() {
        Position p = new Position();
        p.setLng(-3.192473);
        p.setLat(null);
        Set<ConstraintViolation<Position>> violations = validator.validate(p);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet()))
                .contains("lat");
    }

    @Test
    void outOfRangeLatitudeFails() {
        Position p = new Position();
        p.setLng(0.0);
        p.setLat(91.0); // > 90
        Set<ConstraintViolation<Position>> violations = validator.validate(p);
        assertThat(violations).isNotEmpty();
    }
}

