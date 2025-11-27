package com.nextcommunity.nextlearn.repository;

import com.nextcommunity.nextlearn.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document,Long> {
    List<Document> findBySubject(String subject);
    List<Document> findBySubjectAndSemester(
            String subject,
            String semester
    );
    List<Document> findBySubjectAndSemesterAndType(
            String subject,
            String semester,
            String type
    );

        //List<Document> findBySubjectAndSemesterAndType(String subject, String semester, String type);
      //  List<Document> findBySubjectAndSemester(String subject, Integer semester);



}
