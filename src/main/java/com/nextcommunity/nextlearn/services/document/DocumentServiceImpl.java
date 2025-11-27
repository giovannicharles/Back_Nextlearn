package com.nextcommunity.nextlearn.services.document;

import com.nextcommunity.nextlearn.dto.QuizCriteriaDTO;
import com.nextcommunity.nextlearn.entity.Document;
import com.nextcommunity.nextlearn.repository.DocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public String storeFileAndGetUrl(MultipartFile file, String subPath, String serverBaseUrl) throws FileStorageException {
        String relativePath = fileStorageService.storeFile(file, subPath);
        if (!serverBaseUrl.endsWith("/")) serverBaseUrl += "/";
        return serverBaseUrl + "uploads/" + relativePath.replace("\\", "/");
    }

    @Override
    public Document saveDocument(Document doc) {
        doc.setCreatedAt(Instant.now());
        return repository.save(doc);
    }

    @Override
    public List<Document> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Document> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<Document> findBySubject(String subject) {
        return repository.findBySubject(subject);
    }

    @Override
    public List<Document> getDocumentsForCriteria(QuizCriteriaDTO criteria) {
        String semesterStr = "S" + criteria.getSemester();
        if (criteria.getType() != null && !criteria.getType().isEmpty()) {
            return repository.findBySubjectAndSemesterAndType(
                    criteria.getSubject(),
                    semesterStr,
                    criteria.getType()
            );
        } else {
            return repository.findBySubjectAndSemester(
                    criteria.getSubject(),
                    semesterStr
            );
        }
    }

    @Override
    public String extractTextFromDocuments(List<Document> docs) {
        StringBuilder sb = new StringBuilder();
        for (Document doc : docs) {
            sb.append("=== Document: ").append(doc.getTitle()).append(" ===\n")
                    .append("Matière: ").append(doc.getSubject()).append("\n")
                    .append("Type: ").append(doc.getType()).append("\n")
                    .append("Semestre: ").append(doc.getSemester()).append("\n")
                    .append("Description: ").append(doc.getDescription()).append("\n");

            try {
                String content = extractContentFromDocument(doc);
                sb.append("Contenu:\n").append(content).append("\n");
            } catch (Exception e) {
                sb.append("Erreur lors de l'extraction du contenu: ").append(e.getMessage()).append("\n");
                System.err.println("Erreur d'extraction pour le document " + doc.getId() + ": " + e.getMessage());
            }
            sb.append("\n---\n");
        }
        return sb.toString();
    }

    private String extractContentFromDocument(Document doc) throws IOException {
        InputStream inputStream = null;
        String fileExtension = null;
        String filePath = null;

        try {
            // Priorité à storage_path local
            if (doc.getStoragePath() != null && !doc.getStoragePath().isEmpty()) {
                Path path = fileStorageService.getFileStorageLocation().resolve(doc.getStoragePath()).normalize();
                filePath = path.toString();

                // Vérifications complètes
                if (!Files.exists(path)) {
                    return "Fichier introuvable: " + filePath;
                }

                // Si c'est un répertoire, chercher le premier fichier valide à l'intérieur
                if (Files.isDirectory(path)) {
                    System.out.println("Le chemin est un répertoire, recherche d'un fichier à l'intérieur: " + filePath);
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                        for (Path entry : stream) {
                            if (!Files.isDirectory(entry) && Files.isReadable(entry)) {
                                // Utiliser le premier fichier lisible trouvé
                                path = entry;
                                filePath = path.toString();
                                System.out.println("Fichier trouvé dans le répertoire: " + filePath);
                                break;
                            }
                        }
                    }

                    // Vérifier à nouveau si on a trouvé un fichier
                    if (Files.isDirectory(path)) {
                        return "Aucun fichier trouvé dans le répertoire: " + filePath;
                    }
                }

                if (!Files.isReadable(path)) {
                    return "Permission refusée pour lire le fichier: " + filePath +
                            ". Veuillez vérifier les permissions du dossier. " +
                            "L'application doit avoir les droits de lecture sur ce répertoire. " +
                            "Essayez de redémarrer l'application en tant qu'administrateur ou de modifier les permissions du dossier manuellement.";
                }

                inputStream = Files.newInputStream(path);
                fileExtension = getFileExtension(path.getFileName().toString());
            }
            // Sinon tenter avec file_url
            else if (doc.getFileUrl() != null && !doc.getFileUrl().isEmpty()) {
                URL url = new URL(doc.getFileUrl());
                inputStream = url.openStream();
                filePath = url.getPath();
                fileExtension = getFileExtension(filePath);
            } else {
                return "Aucun fichier associé à ce document";
            }

            // Si l'extension est vide, essayer de la déduire du contenu
            if (fileExtension == null || fileExtension.isEmpty()) {
                try (InputStream is = inputStream) {
                    // Réinitialiser le flux
                    if (doc.getStoragePath() != null && !doc.getStoragePath().isEmpty()) {
                        Path path = fileStorageService.getFileStorageLocation().resolve(doc.getStoragePath()).normalize();
                        inputStream = Files.newInputStream(path);
                    } else {
                        URL url = new URL(doc.getFileUrl());
                        inputStream = url.openStream();
                    }

                    fileExtension = detectFileTypeFromContent(inputStream);

                    if (fileExtension == null || fileExtension.isEmpty()) {
                        return "Format de fichier non supporté (extension manquante): " + filePath;
                    }
                }
            }

            try (InputStream is = inputStream) {
                String content = extractTextBasedOnExtension(is, fileExtension);
                if (content.length() > 10000) {
                    content = content.substring(0, 10000) + "\n... [texte tronqué]";
                }
                return content;
            }
        } catch (AccessDeniedException e) {
            return "Accès refusé au fichier: " + filePath +
                    ". Veuillez vérifier les permissions du dossier. " +
                    "L'application doit avoir les droits de lecture sur ce répertoire. " +
                    "Essayez de redémarrer l'application en tant qu'administrateur ou de modifier les permissions du dossier manuellement.";
        } catch (FileSystemException e) {
            return "Erreur système de fichiers pour: " + filePath +
                    ". Détails: " + e.getMessage();
        } catch (IOException e) {
            return "Erreur d'E/S lors de l'accès au fichier: " + filePath +
                    ". Détails: " + e.getMessage();
        }
    }

    private String detectFileTypeFromContent(InputStream inputStream) throws IOException {
        byte[] pdfHeader = {0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
        byte[] docxHeader = {0x50, 0x4B, 0x03, 0x04}; // PK (signature ZIP)

        if (!inputStream.markSupported()) {
            return null;
        }

        inputStream.mark(100);
        byte[] header = new byte[5];
        int bytesRead = inputStream.read(header);
        inputStream.reset();

        if (bytesRead >= 5 && Arrays.equals(Arrays.copyOf(header, 5), pdfHeader)) {
            return "pdf";
        } else if (bytesRead >= 4 && Arrays.equals(Arrays.copyOf(header, 4), docxHeader)) {
            return "docx";
        }

        // Vérifier si c'est un fichier texte
        inputStream.mark(1000);
        byte[] textBuffer = new byte[1000];
        bytesRead = inputStream.read(textBuffer);
        inputStream.reset();

        if (bytesRead > 0) {
            String sample = new String(textBuffer, 0, bytesRead, StandardCharsets.UTF_8);
            if (sample.matches("[\\x00-\\x7F]+")) {
                return "txt";
            }
        }

        return null;
    }

    private String extractTextBasedOnExtension(InputStream inputStream, String fileExtension) throws IOException {
        switch (fileExtension.toLowerCase()) {
            case "pdf":
                return extractTextFromPdf(inputStream);
            case "docx":
                return extractTextFromDocx(inputStream);
            case "txt":
                return extractTextFromTxt(inputStream);
            default:
                return "Format de fichier non supporté: " + fileExtension;
        }
    }

    private String extractTextFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }

    private String extractTextFromTxt(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String getFileExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i >= 0) ? filename.substring(i + 1) : "";
    }
    @Override
    public void fixDocumentPaths() {
        List<Document> allDocuments = findAll();

        for (Document doc : allDocuments) {
            if (doc.getStoragePath() != null && !doc.getStoragePath().isEmpty()) {
                Path path = fileStorageService.getFileStorageLocation().resolve(doc.getStoragePath()).normalize();

                if (Files.exists(path) && Files.isDirectory(path)) {
                    try {
                        // Chercher le premier fichier valide dans le répertoire
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                            for (Path entry : stream) {
                                if (!Files.isDirectory(entry) && Files.isReadable(entry)) {
                                    // Mettre à jour le chemin du document
                                    String relativePath = doc.getStoragePath() + "/" + entry.getFileName().toString();
                                    doc.setStoragePath(relativePath);

                                    // Mettre à jour l'URL du fichier
                                    String baseUrl = "http://localhost:8081"; // Adapter selon votre configuration
                                    String fileUrl = baseUrl + "/uploads/" + relativePath.replace("\\", "/");
                                    doc.setFileUrl(fileUrl);

                                    saveDocument(doc);
                                    System.out.println("Document mis à jour: " + doc.getId() + " -> " + relativePath);
                                    break;
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Erreur lors de la correction du document " + doc.getId() + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}