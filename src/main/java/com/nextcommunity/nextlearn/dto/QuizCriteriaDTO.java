package com.nextcommunity.nextlearn.dto;

import lombok.Data;

@Data
public class QuizCriteriaDTO {
    private String subject;
    private Integer semester;
    private String type;
    private Integer nombreQuestions;
}
