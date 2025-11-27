package com.nextcommunity.nextlearn.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Entity
@Data
@Table(name ="documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private  String title;
    private String type;
    private String subject;
    private String year;
    private String semester;
    private String author;

    @Column(length =1000)
    private String description;

    @ElementCollection
    @CollectionTable(name="document_tags", joinColumns=@JoinColumn(name="document_id"))
    @Column(name="tag")
    private List<String> tags;

    private String fileUrl;
    private String storagePath;
    private String visibility;
    private String createdBy;

    private Instant createdAt;
}
