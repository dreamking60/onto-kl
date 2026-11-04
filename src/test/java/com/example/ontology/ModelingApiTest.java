package com.example.ontology;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test of the document-driven ontology modeling module:
 * text &rarr; extraction &rarr; T-Box + A-Box &rarr; load into the multi-model
 * reasoning registry &rarr; reason &amp; query it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.default-profile=h2",
                "app.modeling.ai.provider=heuristic",
                "app.modeling.max-document-bytes=1048576",
                "logging.level.it.unibz.inf.ontop=WARN"
        })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ModelingApiTest {

    private static final String SAMPLE_TEXT = String.join("\n",
            "中华人民共和国示例合同法",
            "",
            "第一章 总则",
            "第一条 为了规范合同行为，保护当事人的合法权益，制定本法。",
            "第二条 “合同”是指平等主体之间设立、变更、终止民事权利义务关系的协议。",
            "第三条 合同当事人应当遵循公平原则。",
            "",
            "第二章 合同的订立",
            "第四条 “要约”是指希望与他人订立合同的意思表示。",
            "第五条 “承诺”是指受要约人同意要约的意思表示。",
            "",
            "第三章 合同的履行",
            "第六条 当事人应当按照约定全面履行自己的义务。",
            "第七条 “违约责任”是指当事人不履行合同义务或者履行合同义务不符合约定时承担的责任。");

    private static final Path MODELING_DIR = Paths.get("target", "test-data",
            "modeling-" + UUID.randomUUID().toString().replace("-", ""));
    private static final Path MODELS_DIR = Paths.get("target", "test-data",
            "models-" + UUID.randomUUID().toString().replace("-", ""));

    private static String projectId;
    private static String modelId;
    private static String mockModelId;

    @Autowired
    private TestRestTemplate rest;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.modeling.data-dir", () -> MODELING_DIR.toAbsolutePath().toString());
        registry.add("app.data-dir", () -> MODELS_DIR.toAbsolutePath().toString());
    }

    @BeforeAll
    static void prepare() {
        mockModelId = "modeling-mock-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void createProjectFromInlineText() {
        ResponseEntity<JsonNode> response = postJson("/modeling/projects", mapOf(
                "name", "示例合同法",
                "fileName", "contract-law.txt",
                "content", SAMPLE_TEXT));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        projectId = body.path("id").asText();
        modelId = "modeling-" + projectId;
        assertTrue(projectId.startsWith("p-"));
        assertEquals("CREATED", body.path("status").asText());
        assertTrue(body.at("/statistics/textChars").asLong() > 100);
        assertTrue(body.path("files").path("sourceText").asText().contains("source.txt"));
    }

    @Test
    @Order(2)
    void createProjectFromUploadedFile() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(SAMPLE_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "manual.md";
            }
        });
        body.add("name", "上传文件项目");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<JsonNode> response = rest.postForEntity("/modeling/projects/upload",
                new HttpEntity<>(body, headers), JsonNode.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("上传文件项目", response.getBody().path("name").asText());
    }

    @Test
    @Order(3)
    void extractionWithHeuristicProvider() {
        ResponseEntity<JsonNode> response = postJson("/modeling/projects/" + projectId + "/extract",
                mapOf("provider", "heuristic"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals("EXTRACTED", body.path("status").asText());
        assertTrue(body.at("/statistics/classes").asInt() >= 4);
        assertTrue(body.at("/statistics/individuals").asInt() >= 3);
        assertTrue(body.at("/statistics/relations").asInt() >= 2);
        assertFalse(body.path("warnings").isEmpty());
        assertTrue(body.path("extractionPreview").asText().contains("classes"));
    }

    @Test
    @Order(4)
    void generateTboxAndAbox() {
        ResponseEntity<JsonNode> response = postJson("/modeling/projects/" + projectId + "/generate",
                mapOf("includeSourceComments", true));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals("GENERATED", body.path("status").asText());
        assertTrue(body.at("/statistics/tboxTriples").asLong() > 10);
        assertTrue(body.at("/statistics/aboxTriples").asLong() > 10);
        assertTrue(body.path("ontologyPreview").asText().contains("owl:Class"));
        assertTrue(body.path("instancesPreview").asText().contains("owl:NamedIndividual"));
        assertTrue(body.path("namespace").asText().startsWith("http://example.org/model/"));
    }

    @Test
    @Order(5)
    void downloadGeneratedTurtle() {
        ResponseEntity<String> ontology = rest.getForEntity(
                "/modeling/projects/" + projectId + "/ontology", String.class);
        assertEquals(HttpStatus.OK, ontology.getStatusCode());
        assertNotNull(ontology.getBody());
        assertTrue(ontology.getBody().contains("@prefix"));
        assertTrue(ontology.getBody().contains("owl:Class"));

        ResponseEntity<String> instances = rest.getForEntity(
                "/modeling/projects/" + projectId + "/instances", String.class);
        assertEquals(HttpStatus.OK, instances.getStatusCode());
        assertNotNull(instances.getBody());
        assertTrue(instances.getBody().contains("owl:NamedIndividual"));
    }

    // ------------------------------------------------------------------
    // Load into the reasoning registry
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    void loadGeneratedOntologyAsReasoningModel() {
        ResponseEntity<JsonNode> response = postJson("/modeling/projects/" + projectId + "/load",
                mapOf("modelId", modelId, "autoReason", true));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals("LOADED", body.at("/project/status").asText());
        assertEquals(modelId, body.at("/project/modelId").asText());
        assertEquals("local", body.at("/model/aboxSource").asText());
        assertEquals("local", body.at("/model/profile").asText());
        assertTrue(body.at("/model/statistics/virtualAboxTriples").asLong() > 10);
        JsonNode reasoning = body.path("reasoning");
        assertFalse(reasoning.isMissingNode());
        assertTrue(reasoning.path("inferredTotal").asLong() >= 1,
                "generated classes should produce OWL subclass inferences");
        assertFalse(reasoning.path("materializeEnabled").asBoolean(),
                "file-based generated models do not materialise into TDSQL");
    }

    @Test
    @Order(7)
    void generatedModelIsVisibleInModelsList() {
        ResponseEntity<JsonNode> response = rest.getForEntity("/models", JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode models = response.getBody();
        assertNotNull(models);
        boolean found = false;
        for (JsonNode model : models) {
            if (modelId.equals(model.path("id").asText())) {
                found = true;
                assertEquals("local", model.path("aboxSource").asText());
                assertEquals("REASONED", model.path("state").asText());
            }
        }
        assertTrue(found, "generated model must be part of the multi-model registry");
    }

    @Test
    @Order(8)
    void queryGeneratedModelWithModelBoundPrefix() {
        String sparql = "SELECT ?s ?label WHERE { ?s a :Document ; <http://www.w3.org/2000/01/rdf-schema#label> ?label }";
        ResponseEntity<JsonNode> response = postJson("/query",
                mapOf("modelId", modelId, "sparql", sparql));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().path("rowCount").asInt() >= 1, "document individual should be queryable");
    }

    @Test
    @Order(9)
    void explainGeneratedModel() {
        ResponseEntity<JsonNode> response = rest.getForEntity(
                "/models/" + modelId + "/explain?subject={subject}&limit=20", JsonNode.class, ":section_1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertTrue(body.path("inferredStatements").asInt() >= 1);
        assertTrue(body.path("trace").asText().contains("subClassOf")
                        || body.path("trace").asText().contains("hasTitle"),
                "trace should explain the inferred DocumentSection type");
    }

    // ------------------------------------------------------------------
    // One-shot pipeline with the mock AI provider (multi-model switch)
    // ------------------------------------------------------------------

    @Test
    @Order(10)
    void pipelineWithMockAiCreatesSecondModel() {
        ResponseEntity<JsonNode> created = postJson("/modeling/projects", mapOf(
                "name", "Mock AI project",
                "fileName", "mock.txt",
                "content", SAMPLE_TEXT));
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        String secondProject = created.getBody().path("id").asText();

        Map<String, Object> pipeline = new HashMap<>();
        pipeline.put("extract", mapOf("provider", "mock"));
        pipeline.put("generate", mapOf());
        pipeline.put("load", mapOf("modelId", mockModelId, "autoReason", true));
        ResponseEntity<JsonNode> response = postJson(
                "/modeling/projects/" + secondProject + "/pipeline", pipeline);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals("LOADED", body.at("/project/status").asText());
        assertEquals(mockModelId, body.at("/loaded/model/id").asText());
        assertTrue(body.at("/loaded/reasoning/inferredTotal").asLong() >= 1,
                "mock ontology Contract ⊑ Document should infer types");

        String sparql = "SELECT ?s WHERE { ?s a :Contract }";
        ResponseEntity<JsonNode> query = postJson("/query",
                mapOf("modelId", mockModelId, "sparql", sparql));
        assertEquals(HttpStatus.OK, query.getStatusCode());
        assertTrue(query.getBody().path("rowCount").asInt() >= 1);

        // Both the generated and the hand-written/file models coexist.
        ResponseEntity<JsonNode> models = rest.getForEntity("/models", JsonNode.class);
        assertTrue(models.getBody().size() >= 1);
        rest.exchange("/modeling/projects/" + secondProject, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
    }

    // ------------------------------------------------------------------
    // Errors
    // ------------------------------------------------------------------

    @Test
    @Order(11)
    void invalidRequestsAreRejected() {
        ResponseEntity<String> empty = rest.postForEntity("/modeling/projects",
                jsonEntity(mapOf("name", "empty")), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, empty.getStatusCode());

        ResponseEntity<String> unknown = rest.getForEntity("/modeling/projects/does-not-exist", String.class);
        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());

        ResponseEntity<String> badProvider = rest.postForEntity(
                "/modeling/projects/" + projectId + "/extract",
                jsonEntity(mapOf("provider", "bogus")), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, badProvider.getStatusCode());
        assertTrue(badProvider.getBody().contains("MODELING_ERROR"));
    }

    @Test
    @Order(12)
    void aiStatusReportsConfiguredProvider() {
        ResponseEntity<JsonNode> response = rest.getForEntity("/modeling/ai/status", JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals("heuristic", body.path("provider").asText());
        assertTrue(body.path("reachable").asBoolean());
        assertFalse(body.has("apiKey"));
    }

    @Test
    @Order(13)
    void deleteProjectUnloadsGeneratedModel() {
        ResponseEntity<Void> deleted = rest.exchange("/modeling/projects/" + projectId,
                HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        ResponseEntity<String> model = rest.getForEntity("/models/" + modelId, String.class);
        assertEquals(HttpStatus.NOT_FOUND, model.getStatusCode());
        ResponseEntity<String> project = rest.getForEntity("/modeling/projects/" + projectId, String.class);
        assertEquals(HttpStatus.NOT_FOUND, project.getStatusCode());
        assertTrue(Files.notExists(MODELING_DIR.resolve(projectId)));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ResponseEntity<JsonNode> postJson(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(path, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private static HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
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
