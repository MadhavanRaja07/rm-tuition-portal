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
@Document(collection = "materials")
public class Material {
    @Id
    private String id;

    private String classCode; // e.g., "10", "11-all", "11-pcm"

    private String subject; // e.g., "Mathematics", "Physics"

    private String title;

    private String fileName;

    private String gridFsId; // GridFS file references

    private Date uploadedAt;

    private String uploadedBy;
}
