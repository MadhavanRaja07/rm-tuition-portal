package com.rmtuition.portal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String name;

    private String phone;

    private String standard; // e.g. "10", "11-all", "11-pcm", "12-pcm", etc.

    private String avatarUrl;

    private Role role; // ROLE_ADMIN or ROLE_STUDENT

    private String resetToken;

    private Date resetTokenExpiry;

    private Date createdAt;
}
