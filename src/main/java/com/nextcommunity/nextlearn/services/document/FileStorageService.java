package com.nextcommunity.nextlearn.services.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() throws FileStorageException {
        // Créer le chemin relatif au répertoire courant
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            // Créer le répertoire s'il n'existe pas
            if (!Files.exists(this.fileStorageLocation)) {
                Files.createDirectories(this.fileStorageLocation);
                System.out.println("Répertoire de stockage créé: " + this.fileStorageLocation);
            } else {
                System.out.println("Répertoire de stockage existant: " + this.fileStorageLocation);
            }

            // Vérifier les permissions
            if (!Files.isWritable(this.fileStorageLocation)) {
                throw new FileStorageException("Le répertoire de stockage n'est pas accessible en écriture: " + this.fileStorageLocation);
            }

            // Vérifier les permissions de lecture
            if (!Files.isReadable(this.fileStorageLocation)) {
                throw new FileStorageException("Le répertoire de stockage n'est pas accessible en lecture: " + this.fileStorageLocation);
            }

            System.out.println("Répertoire de stockage initialisé avec succès: " + this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Impossible de créer le répertoire de stockage: " + this.fileStorageLocation, ex);
        }
    }

    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }

    public String storeFile(MultipartFile file, String subPath) throws FileStorageException {
        try {
            // Validation du fichier
            if (file.isEmpty()) {
                throw new FileStorageException("Le fichier est vide");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.lastIndexOf('.') == -1) {
                throw new FileStorageException("Le fichier doit avoir une extension valide");
            }

            // Créer le chemin complet du sous-répertoire
            Path uploadPath = this.fileStorageLocation.resolve(subPath).normalize();

            // Créer le sous-répertoire s'il n'existe pas
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Générer un nom de fichier unique
            String fileExtension = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID().toString() + "." + fileExtension;
            Path targetLocation = uploadPath.resolve(fileName);

            // Copier le fichier
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Retourner le chemin relatif
            return subPath + "/" + fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Impossible de stocker le fichier " + file.getOriginalFilename(), ex);
        }
    }

    public Resource loadAsResource(String fileName) throws FileStorageException {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("Fichier non trouvé ou non lisible: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Fichier non trouvé: " + fileName, ex);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int i = filename.lastIndexOf('.');
        return (i >= 0) ? filename.substring(i + 1) : "";
    }
}