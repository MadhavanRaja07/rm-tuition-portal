package com.rmtuition.portal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "quizzes")
public class Quiz {
    @Id
    private String id;

    private String classCode;

    private String subject;

    private String title;

    private int durationMinutes;

    private Date startWindow;

    private Date endWindow;

    private boolean isPublished; // Locked until published

    private boolean resultsReleased; // Scores & answers hidden until released

    private List<QuizQuestion> questions;

    private Date createdAt;

    private String createdBy;
}
