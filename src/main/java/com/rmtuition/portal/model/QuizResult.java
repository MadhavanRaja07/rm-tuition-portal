package com.rmtuition.portal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "quiz_results")
public class QuizResult {
    @Id
    private String id;

    private String quizId;

    private String studentId;

    private String studentName;

    private String studentEmail;

    private int score;

    private int totalQuestions;

    private Map<Integer, Integer> answers; // Map of question index -> selected option index

    private Date submittedAt;
}
