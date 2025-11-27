package com.nextcommunity.nextlearn.services.document;

import com.nextcommunity.nextlearn.dto.QuizCriteriaDTO;
import com.nextcommunity.nextlearn.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;


public interface DocumentService {
    // store file and return its public URL (relative path)
    public String storeFileAndGetUrl(MultipartFile file, String subPath, String serverBaseUrl) throws FileStorageException;
    public Document saveDocument(Document doc);
    public List<Document> findAll();
    public Optional<Document> findById(Long id);
    public void delete(Long id);
    public List<Document> findBySubject(String subject);
    public List<Document> getDocumentsForCriteria(QuizCriteriaDTO criteria);
    public String extractTextFromDocuments(List<Document> docs);
    public void fixDocumentPaths();
}
