package com.rmtuition.portal.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class FileService {

    @Autowired
    private GridFsTemplate gridFsTemplate;

    public String storeFile(InputStream inputStream, String fileName, String contentType) {
        ObjectId objectId = gridFsTemplate.store(inputStream, fileName, contentType);
        return objectId.toHexString();
    }

    public GridFsResource getFileResource(String gridFsId) throws IOException {
        GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(gridFsId))));
        if (gridFSFile == null) {
            throw new IllegalArgumentException("File not found in GridFS with ID: " + gridFsId);
        }
        return gridFsTemplate.getResource(gridFSFile);
    }

    public void deleteFile(String gridFsId) {
        if (gridFsId != null && !gridFsId.trim().isEmpty()) {
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(gridFsId))));
        }
    }
}
