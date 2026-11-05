/*
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
