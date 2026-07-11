package com.rmtuition.portal.repository;

import com.rmtuition.portal.model.QuizResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizResultRepository extends MongoRepository<QuizResult, String> {
    List<QuizResult> findByStudentId(String studentId);
    List<QuizResult> findByQuizId(String quizId);
    Optional<QuizResult> findByQuizIdAndStudentId(String quizId, String studentId);
}
