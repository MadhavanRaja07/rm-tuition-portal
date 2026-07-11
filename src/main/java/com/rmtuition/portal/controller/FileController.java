package com.rmtuition.portal.controller;

import com.rmtuition.portal.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @GetMapping("/preview/{gridFsId}")
    public ResponseEntity<InputStreamResource> previewFile(@PathVariable String gridFsId) {
        try {
            GridFsResource resource = fileService.getFileResource(gridFsId);
            String contentType = resource.getContentType();
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(new InputStreamResource(resource.getInputStream()));
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/avatar/{gridFsId}")
    public ResponseEntity<InputStreamResource> getAvatar(@PathVariable String gridFsId) {
        try {
            GridFsResource resource = fileService.getFileResource(gridFsId);
            String contentType = resource.getContentType();
            if (contentType == null) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(resource.getInputStream()));
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
