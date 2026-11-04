package com.example.ontology;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-stack REST API test: boots the Spring Boot application, loads the demo
 * ontology against an embedded H2 (MySQL compatibility mode) A-Box and
 * exercises every endpoint.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.default-profile=h2",
                "app.max-query-rows=100",
                "app.max-inferred-sample=20",
                "logging.level.it.unibz.inf.ontop=WARN"
        })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OntologyReasoningApiTest {

    private static final String MODEL_ID = "company-test";
    private static final Path DATA_DIR = Paths.get("target", "test-data",
            "api-" + UUID.randomUUID().toString().replace("-", ""));

    @Autowired
    private TestRestTemplate rest;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.data-dir", () -> DATA_DIR.toAbsolutePath().toString());
    }

    // ------------------------------------------------------------------
    // /load and /models
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void loadDemoModel() {
        ResponseEntity<JsonNode> response = postJson("/load", mapOf("modelId", MODEL_ID));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals(MODEL_ID, body.path("id").asText());
        assertEquals("LOADED", body.path("state").asText());
        assertEquals("h2", body.path("profile").asText());
        assertTrue(body.at("/statistics/virtualAboxTriples").asLong() > 100,
                "the virtual A-Box should expose the seeded TDSQL rows");
        assertTrue(body.at("/statistics/ontology/classes").asInt() >= 8,
                "the local ontology should be summarised");
    }

    @Test
    @Order(2)
    void duplicateLoadIsRejected() {
        ResponseEntity<String> response = rest.postForEntity("/load", json(mapOf("modelId", MODEL_ID)),
                String.class);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("MODEL_ALREADY_EXISTS"));
    }

    @Test
    @Order(3)
    void listModels() {
        ResponseEntity<JsonNode> response = rest.getForEntity("/models", JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode models = response.getBody();
        assertNotNull(models);
        assertTrue(models.isArray());
        assertTrue(models.size() >= 1);
        assertEquals(MODEL_ID, models.get(0).path("id").asText());
    }

    @Test
    @Order(4)
    void getModelDetail() {
        ResponseEntity<JsonNode> response = rest.getForEntity("/models/" + MODEL_ID, JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals("LOADED", body.path("state").asText());
        assertTrue(body.path("ontologyFile").asText().endsWith("ontology.ttl"));
        assertTrue(body.path("mappingFile").asText().endsWith("mapping.obda"));
        assertEquals("H2", body.path("database").asText().split(" ")[0]);
    }

    // ------------------------------------------------------------------
    // /reason and /models/{id}/explain
    // ------------------------------------------------------------------

    @Test
    @Order(5)
    void reasonComputesInference() {
        Map<String, Object> request = mapOf("modelId", MODEL_ID, "materialize", true);
        ResponseEntity<JsonNode> response = postJson("/reason", request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals("REASONED", body.path("state").asText());
        assertTrue(body.path("computed").asBoolean());
        assertTrue(body.path("inferredTotal").asLong() >= 30,
                "the demo rules/OWL closure should derive the reporting/colleague/director triples");
        assertTrue(body.path("materializedTriples").asInt() > 0);
        assertFalse(body.path("inferredTriples").isEmpty());
        assertTrue(body.at("/statistics/inferenceModelTriples").asLong()
                > body.at("/statistics/assertedTriples").asLong());
    }

    @Test
    @Order(6)
    void reasoningIsCached() {
        ResponseEntity<JsonNode> response = postJson("/reason", mapOf("modelId", MODEL_ID));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().path("computed").asBoolean(),
                "the closure must be reused on the second call");
    }

    @Test
    @Order(7)
    void explainInferredStatements() {
        ResponseEntity<JsonNode> response = rest.getForEntity(
                "/models/{modelId}/explain?subject={subject}&limit=20", JsonNode.class,
                MODEL_ID, ":emp/5");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertTrue(body.path("inferredStatements").asInt() > 0);
        String trace = body.path("trace").asText();
        assertTrue(trace.contains("reportsTo"), "the trace should mention the reportsTo rule");
    }

    // ------------------------------------------------------------------
    // /query
    // ------------------------------------------------------------------

    @Test
    @Order(8)
    void sparqlSelect() {
        String sparql = "SELECT ?e ?name WHERE { ?e a :SeniorManager ; :name ?name } ORDER BY ?name";
        ResponseEntity<JsonNode> response = postJson("/query", mapOf("modelId", MODEL_ID, "sparql", sparql));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals("SELECT", body.path("queryType").asText());
        assertTrue(body.path("rowCount").asInt() >= 2);
        assertEquals("Alice", body.at("/rows/0/1").asText());
    }

    @Test
    @Order(9)
    void sparqlAskSeesTransitiveInference() {
        String sparql = "ASK { <http://example.org/company#dept/2> "
                + "<http://example.org/company#partOf> <http://example.org/company#dept/5> }";
        ResponseEntity<JsonNode> response = postJson("/query", mapOf("modelId", MODEL_ID, "sparql", sparql));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().path("booleanResult").asBoolean(),
                "owl:TransitiveProperty closure must be visible through SPARQL");
    }

    @Test
    @Order(10)
    void sparqlConstruct() {
        String sparql = "CONSTRUCT { ?e :reportsTo ?m } WHERE { ?e :reportsTo ?m }";
        ResponseEntity<JsonNode> response = postJson("/query", mapOf("modelId", MODEL_ID, "sparql", sparql));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals("CONSTRUCT", body.path("queryType").asText());
        assertTrue(body.path("constructTriples").asLong() > 0);
        assertTrue(body.path("constructTurtle").asText().contains("reportsTo"));
    }

    @Test
    @Order(11)
    void sparqlGetEndpoint() {
        String sparql = "SELECT ?e WHERE { ?e a :Director }";
        ResponseEntity<JsonNode> response = rest.getForEntity(
                "/query?modelId={modelId}&sparql={sparql}", JsonNode.class, MODEL_ID, sparql);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().path("rowCount").asInt() >= 4);
    }

    @Test
    @Order(12)
    void invalidSparqlIsRejected() {
        ResponseEntity<String> response = rest.postForEntity("/query",
                json(mapOf("modelId", MODEL_ID, "sparql", "SELECT ?x WHERE { this is not sparql")),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("INVALID_QUERY"));
    }

    // ------------------------------------------------------------------
    // Error handling and other models
    // ------------------------------------------------------------------

    @Test
    @Order(13)
    void unknownModelReturns404() {
        ResponseEntity<String> response = rest.getForEntity("/models/does-not-exist", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("MODEL_NOT_FOUND"));
    }

    @Test
    @Order(14)
    void invalidOntologyIsRejected() {
        ResponseEntity<String> response = rest.postForEntity("/load", json(mapOf(
                "modelId", "bad-ontology",
                "ontologyContent", "this is definitely not turtle",
                "mappingContent", "[PrefixDeclaration]\n\n[MappingDeclaration] @collection [[\n]]\n")),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("INVALID_MODEL")
                || response.getBody().contains("VALIDATION_FAILED"));
    }

    @Test
    @Order(15)
    void loadModelFromInlineContent() throws IOException {
        String ontology = new String(Files.readAllBytes(Paths.get("ontology/company.ttl")));
        String mapping = new String(Files.readAllBytes(Paths.get("mapping/company.obda")));
        ResponseEntity<JsonNode> response = postJson("/load", mapOf(
                "modelId", "inline-model",
                "name", "Inline model",
                "ontologyContent", ontology,
                "mappingContent", mapping));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("inline-model", response.getBody().path("id").asText());
        assertEquals("LOADED", response.getBody().path("state").asText());
    }

    @Test
    @Order(16)
    void openApiDocumentIsAvailable() {
        ResponseEntity<String> response = rest.getForEntity("/v3/api-docs", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"openapi\""));
    }

    @Test
    @Order(17)
    void deleteModel() {
        ResponseEntity<Void> deleted = rest.exchange("/models/" + MODEL_ID, HttpMethod.DELETE,
                HttpEntity.EMPTY, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        ResponseEntity<String> after = rest.getForEntity("/models/" + MODEL_ID, String.class);
        assertEquals(HttpStatus.NOT_FOUND, after.getStatusCode());
    }

    @Test
    @Order(18)
    void deleteInlineModel() {
        ResponseEntity<Void> deleted = rest.exchange("/models/inline-model", HttpMethod.DELETE,
                HttpEntity.EMPTY, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
    }

    @Test
    @Order(19)
    void webUiIsServedAtRoot() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.TEXT_HTML));
        ResponseEntity<String> response = rest.exchange("/", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String html = response.getBody();
        assertNotNull(html);
        assertTrue(html.contains("本体推理系统"), "the demo UI should be served at /");
        assertTrue(html.contains("/js/app.js"), "the UI should reference its JavaScript bundle");
        assertTrue(html.contains("load-dialog"), "the load-model dialog should be part of the UI");
    }

    @Test
    @Order(20)
    void staticUiAssetsAreServed() {
        ResponseEntity<String> javascript = rest.getForEntity("/js/app.js", String.class);
        assertEquals(HttpStatus.OK, javascript.getStatusCode());
        assertNotNull(javascript.getBody());
        assertTrue(javascript.getBody().contains("QUERY_EXAMPLES"),
                "the UI script should expose the demo query examples");

        ResponseEntity<String> css = rest.getForEntity("/css/app.css", String.class);
        assertEquals(HttpStatus.OK, css.getStatusCode());
        assertNotNull(css.getBody());
        assertTrue(css.getBody().contains(":root"), "the stylesheet should be served");

        ResponseEntity<String> modelingHtml = rest.getForEntity("/modeling.html", String.class);
        assertEquals(HttpStatus.OK, modelingHtml.getStatusCode());
        assertNotNull(modelingHtml.getBody());
        assertTrue(modelingHtml.getBody().contains("本体建模"));
        assertTrue(modelingHtml.getBody().contains("/js/modeling.js"));

        ResponseEntity<String> modelingJs = rest.getForEntity("/js/modeling.js", String.class);
        assertEquals(HttpStatus.OK, modelingJs.getStatusCode());
        assertNotNull(modelingJs.getBody());
        assertTrue(modelingJs.getBody().contains("runOneClick"),
                "the modeling UI should expose the one-click pipeline");
    }

    @Test
    @Order(21)
    void loadModelFromLocalAboxFile() {
        String ontology = "@prefix : <http://example.org/filemode#> .\n"
                + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "<http://example.org/filemode> a owl:Ontology .\n"
                + ":Animal a owl:Class .\n"
                + ":Dog a owl:Class ; rdfs:subClassOf :Animal .\n";
        String instances = "@prefix : <http://example.org/filemode#> .\n"
                + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + ":doge a owl:NamedIndividual, :Dog .\n";
        ResponseEntity<JsonNode> loaded = postJson("/load", mapOf(
                "modelId", "file-model",
                "name", "File mode model",
                "ontologyContent", ontology,
                "instancesContent", instances));
        assertEquals(HttpStatus.CREATED, loaded.getStatusCode());
        assertNotNull(loaded.getBody());
        assertEquals("local", loaded.getBody().path("profile").asText());
        assertEquals("local", loaded.getBody().path("aboxSource").asText());
        assertTrue(loaded.getBody().path("instancesFile").asText().endsWith("instances.ttl"));

        ResponseEntity<JsonNode> reasoned = postJson("/reason", mapOf("modelId", "file-model"));
        assertEquals(HttpStatus.OK, reasoned.getStatusCode());
        assertNotNull(reasoned.getBody());
        assertTrue(reasoned.getBody().path("inferredTotal").asLong() >= 1,
                "subclass inference should derive :doge a :Animal");
        assertFalse(reasoned.getBody().path("materializeEnabled").asBoolean(),
                "file-based A-Box has no TDSQL table to materialise into");

        // The model-bound ':' prefix must resolve to the generated/file namespace.
        String ask = "ASK { <http://example.org/filemode#doge> a <http://example.org/filemode#Animal> }";
        ResponseEntity<JsonNode> answer = postJson("/query", mapOf("modelId", "file-model", "sparql", ask));
        assertEquals(HttpStatus.OK, answer.getStatusCode());
        assertTrue(answer.getBody().path("booleanResult").asBoolean(),
                "Jena must see the inferred type via SPARQL");

        ResponseEntity<Void> deleted = rest.exchange("/models/file-model", HttpMethod.DELETE,
                HttpEntity.EMPTY, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ResponseEntity<JsonNode> postJson(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(path, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private static HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

}
