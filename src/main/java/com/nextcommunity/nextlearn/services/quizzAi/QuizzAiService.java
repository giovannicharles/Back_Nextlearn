package com.nextcommunity.nextlearn.services.quizzAi;

import com.nextcommunity.nextlearn.dto.QuizCriteriaDTO;

public interface QuizzAiService {
    String generateQuizzFromText(String text, QuizCriteriaDTO criteria);
}
