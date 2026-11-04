package com.example.ontology.modeling.dto;

import javax.validation.constraints.Size;

/** Optional JSON body of {@code POST /modeling/projects} (alternative to multipart upload). */
public class CreateProjectRequest {

    @Size(max = 200)
    private String name;
    @Size(max = 255)
    private String fileName;
    private String mimeType;
    private String content;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
