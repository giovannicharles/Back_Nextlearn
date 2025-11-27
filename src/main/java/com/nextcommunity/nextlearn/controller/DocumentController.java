package com.nextcommunity.nextlearn.controller;

import com.nextcommunity.nextlearn.entity.Document;
import com.nextcommunity.nextlearn.services.document.DocumentService;
import com.nextcommunity.nextlearn.services.document.FileStorageException;
import com.nextcommunity.nextlearn.services.document.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@RestController
@RequestMapping("nextlearn/api/documents")
@CrossOrigin(origins = "http://localhost:8100")
public class DocumentController implements DocumentControllerApi {

    private final DocumentService documentService;
    private final FileStorageService fileStorageService;

    public DocumentController(DocumentService documentService, FileStorageService fileStorageService) {
        this.documentService = documentService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subPath", required = false) String subPath,
            HttpServletRequest request) throws FileStorageException {

        // Validation du fichier
        if (file.isEmpty()) {
            throw new FileStorageException("Le fichier est vide");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.lastIndexOf('.') == -1) {
            throw new FileStorageException("Le fichier doit avoir une extension valide");
        }

        String baseUrl = getBaseUrl(request);
        String fileUrl = documentService.storeFileAndGetUrl(file, subPath, baseUrl);

        Map<String, String> resp = new HashMap<>();
        resp.put("url", fileUrl);
        resp.put("storagePath", subPath == null ? fileUrl : (subPath + "/" + fileUrl));
        return ResponseEntity.ok(resp);
    }

    @Override
    @PostMapping
    public ResponseEntity<Document> uploadAndCreateDocument(
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
            HttpServletRequest request) throws FileStorageException {

        // Validation du fichier
        if (file.isEmpty()) {
            throw new FileStorageException("Le fichier est vide");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.lastIndexOf('.') == -1) {
            throw new FileStorageException("Le fichier doit avoir une extension valide");
        }

        // Stockage du fichier
        String baseUrl = getBaseUrl(request);
        String storagePath = "documents/" + year + "/" + subject + "/" + type;

        // Obtenir le chemin relatif du fichier sauvegardé
        String relativePath = fileStorageService.storeFile(file, storagePath);

        // Construire l'URL
        String fileUrl = baseUrl + "/uploads/" + relativePath.replace("\\", "/");

        // Création du document
        Document doc = new Document();
        doc.setTitle(title);
        doc.setType(type);
        doc.setSubject(subject);
        doc.setYear(year);
        doc.setSemester(semester);
        doc.setAuthor(author);
        doc.setDescription(description);
        doc.setVisibility(visibility);
        doc.setFileUrl(fileUrl);
        doc.setStoragePath(relativePath); // Utiliser le chemin relatif avec le nom généré
        doc.setTags(tags != null ? tags : List.of());

        Document saved = documentService.saveDocument(doc);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Override
    @GetMapping
    public List<Document> getAll() {
        return documentService.findAll();
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<Document> getById(@PathVariable Long id) {
        Optional<Document> doc = documentService.findById(id);
        return doc.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<Document> update(@PathVariable Long id, @RequestBody Document payload) {
        return documentService.findById(id)
                .map(document -> {
                    document.setTitle(payload.getTitle());
                    document.setType(payload.getType());
                    document.setSubject(payload.getSubject());
                    document.setYear(payload.getYear());
                    document.setSemester(payload.getSemester());
                    document.setAuthor(payload.getAuthor());
                    document.setDescription(payload.getDescription());
                    document.setVisibility(payload.getVisibility());
                    document.setTags(payload.getTags());
                    Document updatedDocument = documentService.saveDocument(document);
                    return ResponseEntity.ok(updatedDocument);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return documentService.findById(id)
                .map(document -> {
                    documentService.delete(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @GetMapping("/subject/{subject}")
    public List<Document> getBySubject(@PathVariable String subject) {
        return documentService.findBySubject(subject);
    }

    @Override
    @GetMapping("/uploads/**")
    public ResponseEntity<Resource> getFile(HttpServletRequest request) {
        try {
            // Extraire le chemin de la requête
            String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

            // Utiliser AntPathMatcher pour extraire le chemin du fichier
            String filepath = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);

            // Décoder le chemin en plusieurs étapes pour gérer correctement les caractères encodés
            String decodedPath = URLDecoder.decode(filepath, StandardCharsets.UTF_8.name());

            // Si le chemin contient encore des séquences encodées, les décoder récursivement
            while (decodedPath.contains("%")) {
                try {
                    String tempDecoded = URLDecoder.decode(decodedPath, StandardCharsets.UTF_8.name());
                    if (tempDecoded.equals(decodedPath)) {
                        break; // Plus de changements, on sort de la boucle
                    }
                    decodedPath = tempDecoded;
                } catch (IllegalArgumentException e) {
                    break; // Séquence d'encodage invalide, on garde ce qu'on a
                }
            }

            System.out.println("Téléchargement du fichier (original): " + filepath);
            System.out.println("Téléchargement du fichier (décodé): " + decodedPath);

            // Charger la ressource
            Resource resource = fileStorageService.loadAsResource(decodedPath);

            // Déterminer le type de contenu
            String contentType = "application/octet-stream";
            if (decodedPath.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (decodedPath.endsWith(".docx")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else if (decodedPath.endsWith(".txt")) {
                contentType = "text/plain";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (UnsupportedEncodingException e) {
            System.err.println("Erreur d'encodage lors du décodage du chemin: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            System.err.println("Erreur lors du téléchargement: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public String getBaseUrl(HttpServletRequest req) {
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        String base = scheme + "://" + host + (port == 80 || port == 443 ? "" : ":" + port);
        return base;
    }
}