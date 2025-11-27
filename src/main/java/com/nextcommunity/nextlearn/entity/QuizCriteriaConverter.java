package com.nextcommunity.nextlearn.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextcommunity.nextlearn.dto.QuizCriteriaDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class QuizCriteriaConverter implements AttributeConverter<QuizCriteriaDTO, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(QuizCriteriaDTO attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting QuizCriteriaDTO to JSON", e);
        }
    }

    @Override
    public QuizCriteriaDTO convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, QuizCriteriaDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to QuizCriteriaDTO", e);
        }
    }
}