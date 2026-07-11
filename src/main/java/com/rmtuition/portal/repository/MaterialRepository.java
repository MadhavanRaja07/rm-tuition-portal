package com.rmtuition.portal.repository;

import com.rmtuition.portal.model.Material;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends MongoRepository<Material, String> {
    List<Material> findByClassCode(String classCode);
    List<Material> findByClassCodeAndSubject(String classCode, String subject);
}
