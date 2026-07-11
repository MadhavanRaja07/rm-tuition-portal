package com.rmtuition.portal.repository;

import com.rmtuition.portal.model.Quiz;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends MongoRepository<Quiz, String> {
    List<Quiz> findByClassCode(String classCode);
    List<Quiz> findByClassCodeAndIsPublished(String classCode, boolean isPublished);
    List<Quiz> findByClassCodeAndSubjectAndIsPublished(String classCode, String subject, boolean isPublished);
}
