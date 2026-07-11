package com.rmtuition.portal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "question_papers")
public class QuestionPaper {
    @Id
    private String id;

    private String classCode;

    private String subject;

    private String title;

    private String fileName;

    private String gridFsId;

    private Date uploadedAt;

    private String uploadedBy;
}
