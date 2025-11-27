package com.nextcommunity.nextlearn.repository;

import com.nextcommunity.nextlearn.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

   @Query("SELECT q FROM Quiz q WHERE q.criteriaJson LIKE %:subject%")
   List<Quiz> findBySubject(@Param("subject") String subject);

   @Query("SELECT q FROM Quiz q WHERE q.criteriaJson LIKE %:semester%")
   List<Quiz> findBySemester(@Param("semester") Integer semester);

   @Query("SELECT q FROM Quiz q WHERE q.criteriaJson LIKE %:type%")
   List<Quiz> findByType(@Param("type") String type);

   @Query("SELECT q FROM Quiz q WHERE q.criteriaJson LIKE %:subject% AND q.criteriaJson LIKE %:semester%")
   List<Quiz> findBySubjectAndSemester(@Param("subject") String subject, @Param("semester") Integer semester);
}