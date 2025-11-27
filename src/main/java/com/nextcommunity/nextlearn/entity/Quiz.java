package com.nextcommunity.nextlearn.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextcommunity.nextlearn.dto.QuizCriteriaDTO;
import jakarta.persistence.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Data
@Slf4j
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "criteria_json", columnDefinition = "TEXT")
    private String criteriaJson;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "generated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Instant generatedAt;

    @ElementCollection
    @CollectionTable(name = "quiz_source_documents", joinColumns = @JoinColumn(name = "quiz_id"))
    @Column(name = "document_id")
    private List<Long> sourceDocumentIds;

    // Méthodes de conversion pour gérer le JSON
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public QuizCriteriaDTO getCriteria() {
        if (criteriaJson == null || criteriaJson.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(criteriaJson, QuizCriteriaDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la désérialisation des critères du quiz", e);
            return null;
        }
    }

    public void setCriteria(QuizCriteriaDTO criteria) {
        if (criteria == null) {
            this.criteriaJson = null;
            return;
        }
        try {
            this.criteriaJson = objectMapper.writeValueAsString(criteria);
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des critères du quiz", e);
            this.criteriaJson = "{}";
        }
    }

    // Constructeurs
    public Quiz() {
        this.generatedAt = Instant.now();
    }

    public Quiz(QuizCriteriaDTO criteria, String content, List<Long> sourceDocumentIds) {
        this();
        this.setCriteria(criteria);
        this.content = content;
        this.sourceDocumentIds = sourceDocumentIds;
    }
}