package com.example.ontology.modeling.domain;

/** Plain text extracted from an uploaded source document. */
public class SourceDocument {

    private final String fileName;
    private final String mimeType;
    private final String type;
    private final String text;

    public SourceDocument(String fileName, String mimeType, String type, String text) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.type = type;
        this.text = text;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}
