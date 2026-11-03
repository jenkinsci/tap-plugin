package org.tap4j.plugin.util;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiagnosticUtilTest {
    @Test
    public void escapesKeysAndNestedTextValues() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("<key>&\"", "<script>alert(1)</script>&\"");
        String html = DiagnosticUtil.createDiagnosticTable("sample.tap", Map.of("nested", nested));
        assertTrue(html.contains("&lt;key&gt;&amp;&quot;"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;&amp;&quot;"));
        assertFalse(html.contains("<script>"));
    }

    @Test
    public void escapesAttachmentNameAndLinkAttributes() {
        Map<String, Object> diagnostic = new LinkedHashMap<>();
        diagnostic.put("File-Type", "image/png");
        diagnostic.put("File-Name", "<image>&\"");
        diagnostic.put("File-Content", "payload");
        String html = DiagnosticUtil.createDiagnosticTable("sample'&.tap", diagnostic);
        assertTrue(html.contains("&lt;image&gt;&amp;&quot;"));
        assertFalse(html.contains("<image>"));
        assertFalse(html.contains("f=sample'&.tap"));
        assertTrue(html.contains("downloadAttachment?f="));
    }
}
