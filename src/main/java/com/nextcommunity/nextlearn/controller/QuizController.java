package com.nextcommunity.nextlearn.controller;

import com.nextcommunity.nextlearn.dto.QuizCriteriaDTO;
import com.nextcommunity.nextlearn.entity.Document;
import com.nextcommunity.nextlearn.entity.Quiz;
import com.nextcommunity.nextlearn.repository.DocumentRepository;
import com.nextcommunity.nextlearn.repository.QuizRepository;
import com.nextcommunity.nextlearn.services.document.DocumentService;
import com.nextcommunity.nextlearn.services.quizzAi.QuizzAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
@Slf4j
public class QuizController {

    private final DocumentService documentService;
    private final QuizzAiService aiService;
    private final QuizRepository quizRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody QuizCriteriaDTO criteria) {
        try {
            log.info("Début de la génération de quiz avec les critères: {}", criteria);

            // 1. Récupérer les documents selon les critères
            List<Document> docs = documentService.getDocumentsForCriteria(criteria);
            if (docs.isEmpty()) {
                log.warn("Aucun document trouvé pour les critères: {}", criteria);
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "error", "Aucun document trouvé pour ces critères",
                                "criteria", criteria
                        )
                );
            }

            log.info("Documents trouvés: {}", docs.size());

            // 2. Extraire le texte des documents
            String text = documentService.extractTextFromDocuments(docs);
            log.info("Texte extrait des documents (longueur: {} caractères)", text);

            // 3. Générer le quiz via l'IA
            String aiResponse = aiService.generateQuizzFromText(text, criteria);
            log.info("Quiz généré par l'IA (longueur: {} caractères)", aiResponse.length());

            // 4. Sauvegarder le quiz en base de données
            Quiz quiz = new Quiz();
            quiz.setCriteria(criteria);
            quiz.setContent(aiResponse);
            quiz.setGeneratedAt(Instant.now());

            // Extraire les IDs des documents sources
            List<Long> documentIds = docs.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());
            quiz.setSourceDocumentIds(documentIds);

            quiz = quizRepository.save(quiz);
            log.info("Quiz sauvegardé avec l'ID: {}", quiz.getId());

            // 5. Retourner la réponse au frontend
            return ResponseEntity.ok(Map.of(
                    "quizId", quiz.getId(),
                    "content", aiResponse,
                    "documentCount", docs.size(),
                    "sourceDocumentIds", documentIds,
                    "generatedAt", quiz.getGeneratedAt(),
                    "criteria", criteria
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la génération du quiz", e);
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error", "Erreur lors de la génération du quiz",
                            "details", e.getMessage(),
                            "criteria", criteria
                    )
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getQuiz(@PathVariable Long id) {
        log.info("Récupération du quiz avec l'ID: {}", id);

        return quizRepository.findById(id)
                .map(quiz -> {
                    log.info("Quiz trouvé avec l'ID: {}", id);
                    return ResponseEntity.ok(Map.of(
                            "quiz", quiz,
                            "content", quiz.getContent(),
                            "criteria", quiz.getCriteria(),
                            "generatedAt", quiz.getGeneratedAt(),
                            "sourceDocumentIds", quiz.getSourceDocumentIds()
                    ));
                })
                .orElseGet(() -> {
                    log.warn("Quiz non trouvé avec l'ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping
    public ResponseEntity<?> getAllQuizzes() {
        log.info("Récupération de tous les quizzes");

        List<Quiz> quizzes = quizRepository.findAll();
        return ResponseEntity.ok(Map.of(
                "quizzes", quizzes,
                "count", quizzes.size()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id) {
        log.info("Suppression du quiz avec l'ID: {}", id);

        return quizRepository.findById(id)
                .map(quiz -> {
                    quizRepository.delete(quiz);
                    log.info("Quiz supprimé avec l'ID: {}", id);
                    return ResponseEntity.ok().build();
                })
                .orElseGet(() -> {
                    log.warn("Quiz non trouvé pour suppression avec l'ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/by-subject/{subject}")
    public ResponseEntity<?> getQuizzesBySubject(@PathVariable String subject) {
        log.info("Récupération des quizzes pour la matière: {}", subject);

        List<Quiz> quizzes = quizRepository.findBySubject(subject);
        return ResponseEntity.ok(Map.of(
                "quizzes", quizzes,
                "subject", subject,
                "count", quizzes.size()
        ));
    }

    @GetMapping("/by-semester/{semester}")
    public ResponseEntity<?> getQuizzesBySemester(@PathVariable Integer semester) {
        log.info("Récupération des quizzes pour le semestre: {}", semester);

        List<Quiz> quizzes = quizRepository.findBySemester(semester);
        return ResponseEntity.ok(Map.of(
                "quizzes", quizzes,
                "semester", semester,
                "count", quizzes.size()
        ));
    }

    @GetMapping("/by-subject-and-semester/{subject}/{semester}")
    public ResponseEntity<?> getQuizzesBySubjectAndSemester(
            @PathVariable String subject,
            @PathVariable Integer semester) {
        log.info("Récupération des quizzes pour la matière: {} et semestre: {}", subject, semester);

        List<Quiz> quizzes = quizRepository.findBySubjectAndSemester(subject, semester);
        return ResponseEntity.ok(Map.of(
                "quizzes", quizzes,
                "subject", subject,
                "semester", semester,
                "count", quizzes.size()
        ));
    }
}