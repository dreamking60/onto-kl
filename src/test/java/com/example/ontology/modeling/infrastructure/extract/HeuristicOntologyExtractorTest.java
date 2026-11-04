package com.example.ontology.modeling.infrastructure.extract;

import com.example.ontology.modeling.domain.ExtractedOntology;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeuristicOntologyExtractorTest {

    private static final String LAW = String.join("\n",
            "中华人民共和国示例合同法",
            "",
            "第一章 总则",
            "第一条 为了规范合同行为，制定本法。",
            "第二条 “合同”是指平等主体之间设立、变更、终止民事权利义务关系的协议。",
            "",
            "第二章 合同的订立",
            "第三条 “要约”是指希望与他人订立合同的意思表示。",
            "第四条 “承诺”是指受要约人同意要约的意思表示。");

    private final HeuristicOntologyExtractor extractor = new HeuristicOntologyExtractor();

    @Test
    void extractsDocumentStructureAndTerms() {
        ExtractedOntology ontology = extractor.extract(LAW, "合同示例.txt");
        assertTrue(ontology.getClasses().containsKey("Document"));
        assertTrue(ontology.getClasses().containsKey("DocumentSection"));
        assertTrue(ontology.getClasses().containsKey("Article"));
        assertTrue(ontology.getClasses().containsKey("Term"));
        assertTrue(ontology.getClasses().keySet().stream()
                .anyMatch(name -> name.startsWith("SectionType_")));
        assertTrue(ontology.getIndividuals().containsKey("document"));
        assertTrue(ontology.getIndividuals().containsKey("section_1"));
        assertTrue(ontology.getIndividuals().values().stream()
                .anyMatch(individual -> individual.getLabel().contains("合同")));
        assertFalse(ontology.getRelations().isEmpty());
        assertTrue(ontology.getRelations().stream()
                .anyMatch(relation -> "hasSection".equals(relation.getPredicate())));
        assertTrue(ontology.getSubClassOf().stream()
                .anyMatch(subClass -> "Article".equals(subClass.getSub()) && "DocumentSection".equals(subClass.getSuperClass())));
    }

    @Test
    void worksOnPlainTextWithoutHeadings() {
        ExtractedOntology ontology = extractor.extract("这是一段普通文本，其中提到“数据治理”是指对数据资产进行管理。", "plain.txt");
        assertTrue(ontology.getClasses().containsKey("Document"));
        assertTrue(ontology.getIndividuals().containsKey("document"));
        assertFalse(ontology.getIndividuals().isEmpty());
        assertTrue(ontology.getIndividuals().values().stream()
                .anyMatch(individual -> "数据治理".equals(individual.getLabel())));
    }
}
