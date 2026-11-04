package com.example.ontology.modeling.infrastructure.extract;

import com.example.ontology.modeling.domain.ExtractedOntology;
import com.example.ontology.modeling.domain.ExtractedOntology.ExtractedClass;
import com.example.ontology.modeling.domain.ExtractedOntology.ExtractedIndividual;
import com.example.ontology.modeling.domain.ExtractedOntology.ExtractedLiteral;
import com.example.ontology.modeling.domain.ExtractedOntology.ExtractedProperty;
import com.example.ontology.modeling.domain.ExtractedOntology.ExtractedRelation;
import com.example.ontology.modeling.domain.ExtractedOntology.SubClass;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Module-native, rule-based extractor.  It works without any AI model and is
 * used as fallback when the configured LLM is unavailable.
 *
 * <p>It detects document structure (Markdown headings, Chinese chapter/section/
 * article headings, numbered headings), definitions such as
 * {@code “X”是指...}, and quoted terms; it then emits a small but coherent
 * ontology: documents, sections, section types, terms and their relations.</p>
 */
@Component
public class HeuristicOntologyExtractor {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern CN_HEADING = Pattern.compile(
            "^\\s*(第[一二三四五六七八九十百千万零〇\\d]+)([编章节条款部])\\s*(.*)$");
    private static final Pattern NUMBERED_HEADING = Pattern.compile("^\\s*\\d+(?:\\.\\d+)*[、.．)]\\s*(.{2,60})$");
    private static final Pattern DEFINITION = Pattern.compile(
            "[“\\\"《]([^”\\\"》]{2,30})[”\\\"》]\\s*(?:是指|指的是|定义为|特指|系指|是)\\s*([^。\\n；;]{5,200})");
    private static final Pattern QUOTED_TERM = Pattern.compile("[《“\\\"]([^》”\\\"]{2,40})[》”\\\"]");

    private static final int MAX_HEADINGS = 60;
    private static final int MAX_TERMS = 80;

    public ExtractedOntology extract(String text, String documentName) {
        ExtractedOntology ontology = new ExtractedOntology();
        ontology.setAiUsed(false);
        ontology.setChunkCount(1);

        addClass(ontology, "Document", "文档", "源文档类型");
        addClass(ontology, "DocumentSection", "文档章节", "文档中的章节/条款");
        addClass(ontology, "Term", "术语", "文档中定义或提及的术语");

        addProperty(ontology, true, "hasSection", "包含章节", "Document", "DocumentSection", false);
        addProperty(ontology, true, "partOf", "属于章节", "DocumentSection", "DocumentSection", true);
        addClass(ontology, "Article", "条款", "文档中的条文");
        addSubclass(ontology, "Article", "DocumentSection");
        addProperty(ontology, true, "mentions", "提及术语", "DocumentSection", "Term", false);
        addProperty(ontology, false, "hasTitle", "标题", "DocumentSection", "xsd:string", false);
        addProperty(ontology, false, "hasText", "正文", "DocumentSection", "xsd:string", false);
        addProperty(ontology, false, "hasDefinition", "定义", "Term", "xsd:string", false);

        String label = documentName == null || documentName.trim().isEmpty() ? "源文档" : documentName.trim();
        addIndividual(ontology, "document", label, "Document", "由文档建模模块创建的文档个体");

        List<Heading> headings = detectHeadings(text);
        if (headings.isEmpty()) {
            addClass(ontology, "BodySection", "正文", "无章节标题时的整篇正文");
            addSubclass(ontology, "BodySection", "DocumentSection");
            addIndividual(ontology, "section_1", "正文", "BodySection", null);
            addRelation(ontology, "document", "hasSection", "section_1");
            addLiteral(ontology, "section_1", "hasTitle", "正文", "xsd:string", null);
        } else {
            Map<Integer, Heading> lastAtLevel = new HashMap<>();
            Heading lastStructural = null;
            int index = 0;
            for (Heading heading : headings) {
                index++;
                String className;
                Heading parent;
                if (heading.article) {
                    className = "Article";
                    parent = lastStructural;
                } else {
                    className = uniqueClassName(ontology, heading.title);
                    addClass(ontology, className, heading.title, "从文档标题识别出的章节类型：" + heading.title);
                    parent = lastAtLevel.get(heading.level - 1);
                    addSubclass(ontology, className, parent == null ? "DocumentSection" : parent.className);
                }
                String individual = "section_" + index;
                String displayLabel = heading.article ? heading.prefix + " " + shortTitle(heading.title) : heading.title;
                addIndividual(ontology, individual, displayLabel, className, null);
                addLiteral(ontology, individual, "hasTitle", displayLabel, "xsd:string", null);
                if (heading.article) {
                    addLiteral(ontology, individual, "hasText", heading.text, "xsd:string", "zh");
                }
                addRelation(ontology, "document", "hasSection", individual);
                if (parent != null) {
                    addRelation(ontology, parent.individual, "partOf", individual);
                }
                heading.individual = individual;
                heading.className = className;
                lastAtLevel.put(heading.level, heading);
                for (int level = heading.level + 1; level <= 6; level++) {
                    lastAtLevel.remove(level);
                }
                if (!heading.article) {
                    lastStructural = heading;
                }
            }
        }

        // Definitions: “X”是指...
        Map<String, String> termIndividuals = new LinkedHashMap<>();
        Matcher definitionMatcher = DEFINITION.matcher(text);
        int termIndex = 0;
        while (definitionMatcher.find() && termIndex < MAX_TERMS) {
            String term = clean(definitionMatcher.group(1));
            String definition = clean(definitionMatcher.group(2));
            if (term.isEmpty() || definition.isEmpty() || termIndividuals.containsKey(term)) {
                continue;
            }
            termIndex++;
            String individual = "term_" + termIndex;
            addIndividual(ontology, individual, term, "Term", definition);
            addLiteral(ontology, individual, "hasDefinition", definition, "xsd:string", "zh");
            termIndividuals.put(term, individual);
            String section = lastSectionBefore(text, definitionMatcher.start(), headings);
            addRelation(ontology, section, "mentions", individual);
        }

        // Quoted terms that were not defined explicitly.
        Matcher quoted = QUOTED_TERM.matcher(text);
        int quotedIndex = 0;
        while (quoted.find() && quotedIndex < MAX_TERMS) {
            String term = clean(quoted.group(1));
            if (term.length() < 2 || termIndividuals.containsKey(term)) {
                continue;
            }
            quotedIndex++;
            String individual = "term_q" + quotedIndex;
            addIndividual(ontology, individual, term, "Term", null);
            termIndividuals.put(term, individual);
            String section = lastSectionBefore(text, quoted.start(), headings);
            addRelation(ontology, section, "mentions", individual);
        }

        if (ontology.getIndividuals().size() <= 1) {
            // Ensure a usable A-Box even for plain text without headings or terms.
            addIndividual(ontology, "section_1", "正文", "DocumentSection", null);
            addRelation(ontology, "document", "hasSection", "section_1");
            addLiteral(ontology, "section_1", "hasTitle", "正文", "xsd:string", null);
        }
        return ontology;
    }

    /** Merge one extraction into another (used for heuristic fallbacks / chunked results). */
    public void mergeInto(ExtractedOntology target, ExtractedOntology source) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, ExtractedClass> entry : source.getClasses().entrySet()) {
            target.getClasses().putIfAbsent(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, ExtractedProperty> entry : source.getObjectProperties().entrySet()) {
            target.getObjectProperties().putIfAbsent(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, ExtractedProperty> entry : source.getDatatypeProperties().entrySet()) {
            target.getDatatypeProperties().putIfAbsent(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, ExtractedIndividual> entry : source.getIndividuals().entrySet()) {
            target.getIndividuals().putIfAbsent(entry.getKey(), entry.getValue());
        }
        for (SubClass subClass : source.getSubClassOf()) {
            addSubclass(target, subClass.getSub(), subClass.getSuperClass());
        }
        for (ExtractedRelation relation : source.getRelations()) {
            addRelation(target, relation.getSubject(), relation.getPredicate(), relation.getObject());
        }
        for (ExtractedLiteral literal : source.getLiterals()) {
            addLiteral(target, literal.getSubject(), literal.getPredicate(), literal.getValue(),
                    literal.getDatatype(), literal.getLanguage());
        }
    }

    // ------------------------------------------------------------------
    // Heading detection
    // ------------------------------------------------------------------

    private static List<Heading> detectHeadings(String text) {
        List<Heading> headings = new ArrayList<>();
        String[] lines = text.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Heading heading = parseHeading(trimmed);
            if (heading != null) {
                headings.add(heading);
                if (headings.size() >= MAX_HEADINGS) {
                    break;
                }
            }
        }
        return headings;
    }

    private static Heading parseHeading(String line) {
        Matcher markdown = MARKDOWN_HEADING.matcher(line);
        if (markdown.matches()) {
            return new Heading(markdown.group(1).length(), cleanTitle(markdown.group(2)), false, null, line);
        }
        Matcher chinese = CN_HEADING.matcher(line);
        if (chinese.matches() && chinese.group(3) != null && !chinese.group(3).trim().isEmpty()) {
            String prefix = chinese.group(1);
            String kind = chinese.group(2);
            String rest = clean(chinese.group(3));
            switch (kind) {
                case "编":
                case "部":
                    return new Heading(1, cleanTitle(rest), false, prefix, line);
                case "章":
                    return new Heading(2, cleanTitle(rest), false, prefix, line);
                case "节":
                    return new Heading(3, cleanTitle(rest), false, prefix, line);
                default:
                    return new Heading(4, rest, true, prefix, line);
            }
        }
        Matcher numbered = NUMBERED_HEADING.matcher(line);
        if (numbered.matches()) {
            return new Heading(3, cleanTitle(numbered.group(1)), false, null, line);
        }
        return null;
    }

    private static String lastSectionBefore(String text, int offset, List<Heading> headings) {
        String section = headings.isEmpty() ? "section_1" : "document";
        for (Heading heading : headings) {
            if (heading.individual == null) {
                continue;
            }
            int position = text.indexOf(heading.title);
            if (position >= 0 && position < offset) {
                section = heading.individual;
            }
        }
        return section;
    }

    // ------------------------------------------------------------------
    // Data helpers
    // ------------------------------------------------------------------

    private static void addClass(ExtractedOntology ontology, String name, String label, String comment) {
        String key = name.trim();
        if (key.isEmpty() || ontology.getClasses().containsKey(key)) {
            return;
        }
        ontology.getClasses().put(key, new ExtractedClass(key, label, comment));
    }

    private static String uniqueClassName(ExtractedOntology ontology, String title) {
        String base = "SectionType_" + sanitizeName(title);
        String candidate = base;
        int suffix = 2;
        while (ontology.getClasses().containsKey(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private static void addProperty(ExtractedOntology ontology, boolean objectProperty, String name,
                                    String label, String domain, String range, boolean transitive) {
        String key = name.trim();
        if (ontology.getObjectProperties().containsKey(key) || ontology.getDatatypeProperties().containsKey(key)) {
            return;
        }
        ExtractedProperty property = new ExtractedProperty(key, label, domain, range);
        property.setTransitive(transitive);
        if (objectProperty) {
            ontology.getObjectProperties().put(key, property);
        } else {
            ontology.getDatatypeProperties().put(key, property);
        }
    }

    private static void addIndividual(ExtractedOntology ontology, String name, String label,
                                      String className, String comment) {
        String key = name.trim();
        if (key.isEmpty() || ontology.getIndividuals().containsKey(key)) {
            return;
        }
        ExtractedIndividual individual = new ExtractedIndividual(key, label, className);
        individual.setComment(comment);
        ontology.getIndividuals().put(key, individual);
    }

    private static void addSubclass(ExtractedOntology ontology, String sub, String superClass) {
        if (sub == null || superClass == null || sub.equals(superClass)) {
            return;
        }
        for (SubClass existing : ontology.getSubClassOf()) {
            if (existing.getSub().equals(sub) && existing.getSuperClass().equals(superClass)) {
                return;
            }
        }
        ontology.getSubClassOf().add(new SubClass(sub, superClass));
    }

    private static void addRelation(ExtractedOntology ontology, String subject, String predicate, String object) {
        if (subject == null || predicate == null || object == null) {
            return;
        }
        for (ExtractedRelation existing : ontology.getRelations()) {
            if (existing.getSubject().equals(subject) && existing.getPredicate().equals(predicate)
                    && existing.getObject().equals(object)) {
                return;
            }
        }
        ontology.getRelations().add(new ExtractedRelation(subject, predicate, object));
    }

    private static void addLiteral(ExtractedOntology ontology, String subject, String predicate,
                                   String value, String datatype, String language) {
        if (subject == null || predicate == null || value == null) {
            return;
        }
        for (ExtractedLiteral existing : ontology.getLiterals()) {
            if (existing.getSubject().equals(subject) && existing.getPredicate().equals(predicate)
                    && existing.getValue().equals(value)) {
                return;
            }
        }
        ontology.getLiterals().add(new ExtractedLiteral(subject, predicate, value, datatype, language));
    }

    public static String sanitizeName(String value) {
        String name = value == null ? "" : value.trim()
                .replaceAll("[\\s\\p{Punct}]+", "_")
                .replaceAll("[^\\p{L}\\p{N}_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (name.isEmpty()) {
            name = "Entity_" + Math.abs(value == null ? 0 : value.hashCode());
        }
        if (Character.isDigit(name.charAt(0))) {
            name = "N_" + name;
        }
        return name;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private static String cleanTitle(String value) {
        return clean(value).replaceAll("[。；;：:，,]+$", "").trim();
    }

    private static String shortTitle(String value) {
        String title = cleanTitle(value);
        return title.length() <= 24 ? title : title.substring(0, 24) + "…";
    }

    private static final class Heading {
        private final int level;
        private final String title;
        private final boolean article;
        private final String prefix;
        private final String text;
        private String individual;
        private String className;

        private Heading(int level, String title, boolean article, String prefix, String text) {
            this.level = level;
            this.title = title;
            this.article = article;
            this.prefix = prefix;
            this.text = text;
        }
    }
}
