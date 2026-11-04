package com.example.ontology.modeling.infrastructure.extract;

import com.example.ontology.modeling.domain.SourceDocument;
import com.example.ontology.modeling.domain.exception.ModelingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentTextExtractorTest {

    private final DocumentTextExtractor extractor = new DocumentTextExtractor();

    @Test
    void extractsPlainText() {
        SourceDocument document = extractor.extract("law.txt", "text/plain",
                "第一章 总则\n第一条 示例。\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("txt", document.getType());
        assertTrue(document.getText().contains("第一章 总则"));
    }

    @Test
    void extractsHtmlAsText() {
        String html = "<html><head><style>body{}</style><script>var x=1;</script></head>"
                + "<body><h1>合同</h1><p>甲方&amp;乙方</p><br/><p>第二条 内容</p></body></html>";
        SourceDocument document = extractor.extract("page.html", "text/html",
                html.getBytes(StandardCharsets.UTF_8));
        assertEquals("html", document.getType());
        assertTrue(document.getText().contains("合同"));
        assertTrue(document.getText().contains("甲方&乙方"));
        assertTrue(!document.getText().contains("var x"));
        assertTrue(!document.getText().contains("<h1>"));
    }

    @Test
    void extractsDocxParagraphs() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body>"
                + "<w:p><w:r><w:t>操作手册</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t>第一章 概述</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t>按下启动按钮。</w:t></w:r></w:p>"
                + "</w:body></w:document>";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        SourceDocument document = extractor.extract("manual.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                output.toByteArray());
        assertEquals("docx", document.getType());
        assertTrue(document.getText().contains("操作手册"));
        assertTrue(document.getText().contains("第一章 概述"));
        assertTrue(document.getText().contains("按下启动按钮"));
    }

    @Test
    void extractsPdfText() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage();
            pdf.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 700);
                content.showText("Contract ontology manual");
                content.endText();
            }
            pdf.save(output);
        }
        SourceDocument document = extractor.extract("manual.pdf", "application/pdf", output.toByteArray());
        assertEquals("pdf", document.getType());
        assertTrue(document.getText().contains("Contract ontology manual"));
    }

    @Test
    void rejectsBinaryGarbage() {
        byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3};
        assertThrows(ModelingException.class, () -> extractor.extract("binary.bin", "application/octet-stream", bytes));
    }
}
