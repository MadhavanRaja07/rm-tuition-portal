package com.rmtuition.portal.repository;

import com.rmtuition.portal.model.QuestionPaper;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionPaperRepository extends MongoRepository<QuestionPaper, String> {
    List<QuestionPaper> findByClassCode(String classCode);
    List<QuestionPaper> findByClassCodeAndSubject(String classCode, String subject);
}
