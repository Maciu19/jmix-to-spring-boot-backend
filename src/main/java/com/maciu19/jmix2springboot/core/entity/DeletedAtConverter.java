package com.maciu19.jmix2springboot.core.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;

@Converter
public class DeletedAtConverter implements AttributeConverter<Boolean, Instant> {

    @Override
    public Instant convertToDatabaseColumn(Boolean attribute) {
        return attribute ? Instant.now() : null;
    }

    @Override
    public Boolean convertToEntityAttribute(Instant dbData) {
        return dbData != null;
    }
}
