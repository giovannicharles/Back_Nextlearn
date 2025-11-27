package com.nextcommunity.nextlearn.controller;

import com.nextcommunity.nextlearn.entity.Document;
import com.nextcommunity.nextlearn.services.document.FileStorageException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RequestMapping("nextlearn/api/documents")
public interface DocumentControllerApi {
    // Upload fichier (multipart) -> retourne URL relative
    @PostMapping("/upload")
    ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subPath", required = false) String subPath,
            HttpServletRequest request) throws FileStorageException;

    @PostMapping
    ResponseEntity<Document> uploadAndCreateDocument(
            @RequestPart("file") MultipartFile file,
            @RequestPart("title") String title,
            @RequestPart("type") String type,
            @RequestPart("subject") String subject,
            @RequestPart("year") String year,
            @RequestPart("semester") String semester,
            @RequestPart("author") String author,
            @RequestPart("description") String description,
            @RequestPart("visibility") String visibility,
            @RequestPart(value = "tags", required = false) List<String> tags,
            HttpServletRequest request
    ) throws FileStorageException;

    @GetMapping
    List<Document> getAll();

    @GetMapping("/{id}")
    ResponseEntity<Document> getById(@PathVariable Long id);

    @PutMapping("/{id}")
    ResponseEntity<Document> update(@PathVariable Long id, @RequestBody Document payload);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id);

    @GetMapping("/subject/{subject}")
    List<Document> getBySubject(@PathVariable String subject);

    @GetMapping("/uploads/{*filepath}")
    @CrossOrigin(origins = "http://localhost:8100")
    public ResponseEntity<Resource> getFile(HttpServletRequest request);

    String getBaseUrl(HttpServletRequest req);
}