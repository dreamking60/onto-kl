package com.example.ontology.dto.response;

import java.util.List;

/** Response of {@code POST /query} and {@code GET /query}. */
public class QueryResponse {

    private String modelId;
    private String queryType;
    private List<String> columns;
    private List<List<String>> rows;
    private int rowCount;
    private boolean truncated;
    private Boolean booleanResult;
    private String constructTurtle;
    private Long constructTriples;
    private long durationMs;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public Boolean getBooleanResult() {
        return booleanResult;
    }

    public void setBooleanResult(Boolean booleanResult) {
        this.booleanResult = booleanResult;
    }

    public String getConstructTurtle() {
        return constructTurtle;
    }

    public void setConstructTurtle(String constructTurtle) {
        this.constructTurtle = constructTurtle;
    }

    public Long getConstructTriples() {
        return constructTriples;
    }

    public void setConstructTriples(Long constructTriples) {
        this.constructTriples = constructTriples;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}
