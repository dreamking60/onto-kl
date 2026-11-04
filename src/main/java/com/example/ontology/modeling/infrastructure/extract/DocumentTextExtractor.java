package com.example.ontology.modeling.infrastructure.extract;

import com.example.ontology.modeling.domain.SourceDocument;
import com.example.ontology.modeling.domain.exception.ModelingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extracts plain text from the source documents used by the modeling module.
 *
 * <p>Supported formats: plain text / Markdown, HTML, PDF (PDFBox),
 * DOCX (text from {@code word/document.xml}).</p>
 */
@Component
public class DocumentTextExtractor {

    public SourceDocument extract(String fileName, String mimeType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ModelingException("Uploaded document is empty");
        }
        String name = fileName == null ? "document" : fileName;
        String extension = extensionOf(name);
        String text;
        String type;
        switch (extension) {
            case "pdf":
                text = fromPdf(bytes);
                type = "pdf";
                break;
            case "docx":
                text = fromDocx(bytes);
                type = "docx";
                break;
            case "html":
            case "htm":
                text = fromHtml(new String(bytes, StandardCharsets.UTF_8));
                type = "html";
                break;
            case "md":
            case "markdown":
            case "txt":
            case "text":
            case "csv":
            case "log":
                text = new String(bytes, StandardCharsets.UTF_8);
                type = extension;
                break;
            default:
                text = tryPlainText(bytes);
                type = "txt";
        }
        text = normalize(text);
        if (text.length() < 10) {
            throw new ModelingException("Could not extract meaningful text from '" + name
                    + "' (detected type: " + type + ")");
        }
        return new SourceDocument(name, mimeType == null ? mimeTypeOf(type) : mimeType, type, text);
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String mimeTypeOf(String type) {
        switch (type) {
            case "pdf":
                return "application/pdf";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "html":
                return "text/html";
            case "md":
                return "text/markdown";
            default:
                return "text/plain";
        }
    }

    // ------------------------------------------------------------------
    // Format handlers
    // ------------------------------------------------------------------

    private static String fromPdf(byte[] bytes) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(bytes))) {
            if (document.isEncrypted()) {
                throw new ModelingException("Encrypted PDF files are not supported; please decrypt it first");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            throw new ModelingException("Cannot parse PDF document: " + e.getMessage(), e);
        }
    }

    private static String fromDocx(byte[] bytes) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    byte[] xml = readLimited(zip, 20L * 1024L * 1024L);
                    String content = new String(xml, StandardCharsets.UTF_8);
                    content = content.replaceAll("(?s)<w:tab[^>]*/>", "\t");
                    content = content.replaceAll("(?s)</w:p>", "\n");
                    content = content.replaceAll("(?s)<[^>]+>", "");
                    return decodeXmlEntities(content);
                }
            }
        } catch (IOException e) {
            throw new ModelingException("Cannot parse DOCX document: " + e.getMessage(), e);
        }
        throw new ModelingException("DOCX document does not contain word/document.xml");
    }

    private static String fromHtml(String html) {
        String text = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        text = text.replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        text = text.replaceAll("(?is)<br\\s*/?>", "\n");
        text = text.replaceAll("(?is)</(p|div|h[1-6]|li|tr|section|article|blockquote)>", "\n");
        text = text.replaceAll("(?s)<[^>]+>", " ");
        return decodeXmlEntities(text);
    }

    private static String tryPlainText(byte[] bytes) {
        String candidate = new String(bytes, StandardCharsets.UTF_8);
        long printable = candidate.chars()
                .filter(ch -> !Character.isISOControl(ch) || ch == '\n' || ch == '\r' || ch == '\t')
                .count();
        if (candidate.isEmpty() || printable * 100.0 / candidate.length() < 85) {
            throw new ModelingException("Unsupported document format. Supported: txt, md, html, pdf, docx");
        }
        return candidate;
    }

    private static byte[] readLimited(InputStream input, long limit) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(chunk)) >= 0) {
            total += read;
            if (total > limit) {
                throw new ModelingException("DOCX document is too large to parse");
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static String decodeXmlEntities(String value) {
        return value
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");
    }

    private static String normalize(String text) {
        String value = text.replace("\r\n", "\n").replace('\r', '\n');
        value = value.replaceAll("[\\t\\x0B\\f]+", " ");
        value = value.replaceAll(" {2,}", " ");
        value = value.replaceAll("(?m)^ +", "");
        value = value.replaceAll("\\n{3,}", "\n\n");
        return value.trim();
    }
}
