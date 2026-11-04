package com.example.ontology.dto.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/** Request body of {@code POST /query}. */
public class QueryRequest {

    @NotBlank(message = "modelId is required")
    private String modelId;

    @NotBlank(message = "sparql is required")
    private String sparql;

    /** Maximum number of returned rows (SELECT) or statements (CONSTRUCT). */
    @Min(1)
    @Max(10000)
    private Integer limit;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getSparql() {
        return sparql;
    }

    public void setSparql(String sparql) {
        this.sparql = sparql;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
