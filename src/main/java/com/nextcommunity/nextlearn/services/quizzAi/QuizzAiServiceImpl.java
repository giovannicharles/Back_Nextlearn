package com.nextcommunity.nextlearn.services.quizzAi;

import com.nextcommunity.nextlearn.dto.QuizCriteriaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizzAiServiceImpl implements QuizzAiService {

    @Value("${qwen.api.url}")
    private String qwenApiUrl;

    @Value("${qwen.api.key}")
    private String qwenApiKey;

   private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Override
    public String generateQuizzFromText(String text, QuizCriteriaDTO criteria) {
        // Construction du prompt en fonction des critères
        String prompt = String.format("""
        Tu es un générateur de quiz éducatif spécialisé.
        Génère %d questions de type QCM ou Vrai/Faux pour un étudiant de niveau universitaire.
        Matière: %s, Semestre: %d, Type: %s.
        
        Texte source :
        %s
        
        Réponds UNIQUEMENT avec un JSON valide dans ce format exact :
        [
          {
            "type": "qcm",
            "question": "Question text here",
            "options": ["Option A", "Option B", "Option C", "Option D"],
            "answer": "Option A"
          },
          {
            "type": "vrai-faux",
            "question": "Statement here",
            "answer": "Vrai"
          }
        ]
        """, criteria.getNombreQuestions(), criteria.getSubject(),
                criteria.getSemester(), criteria.getType(), text.substring(0, Math.min(text.length(), 3000)));

        // Préparation de la requête
        Map<String, Object> requestBody = Map.of(
                "model", "qwen/qwq-32b",
                "messages", List.of(
                        Map.of("role", "system", "content", "Tu génères des QCM éducatifs au format JSON strict"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7,
                "max_tokens", 2000
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(qwenApiKey);
            headers.set("HTTP-Referer", "http://localhost:8080"); // Pour OpenRouter
            headers.set("X-Title", "NextLearn Quiz Generator");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    qwenApiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<?, ?> body = response.getBody();
            if (body == null) {
                return "{\"error\": \"Pas de réponse de l'IA.\"}";
            }

            List<?> choices = (List<?>) body.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "{\"error\": \"L'IA n'a pas fourni de réponse.\"}";
            }

            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            String content = message.get("content").toString();

            // Validation et nettoyage du JSON
            return cleanJsonResponse(content);

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Désolé, une erreur est survenue lors de la communication avec l'IA.\"}";
        }
    }

    private String cleanJsonResponse(String content) {
        try {
            // Supprime les éventuels marqueurs de code markdown
            content = content.replaceAll("```json\\n*", "").replaceAll("\\n*```", "");

            // Tente de parser le JSON pour valider sa structure
            objectMapper.readTree(content);
            return content;
        } catch (Exception e) {
            // Si le JSON est invalide, retourne une erreur structurée
            return String.format("{\"error\": \"Format de réponse invalide\", \"raw_response\": \"%s\"}",
                    content.replace("\"", "\\\""));
        }
    }
}