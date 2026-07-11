package com.rmtuition.portal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {
    private String questionText;
    private List<String> options;
    private int correctOptionIndex; // 0-indexed
    private String explanation;
}
