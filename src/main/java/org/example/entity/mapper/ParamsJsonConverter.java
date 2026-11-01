package org.example.entity.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

@Converter
public class ParamsJsonConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> TYPE =
            new TypeReference<Map<String, String>>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Не удалось сериализовать params в JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new HashMap<>();
        try {
            Map<String, String> map = MAPPER.readValue(dbData, TYPE);
            return map != null ? map : new HashMap<>();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Не удалось десериализовать params из JSON: " + e.getMessage()
                            + ", json=" + dbData, e);
        }
    }
}