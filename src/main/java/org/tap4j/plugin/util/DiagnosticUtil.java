/*
 * The MIT License
 *
 * Copyright (c) 2010 Bruno P. Kinoshita
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

import org.tap4j.model.TestResult;

import hudson.Functions;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Used to create YAML view. FIXME: Figure out another way to write HTML (send JSON to Stapler/Groovy/etc.?).
 *
 * @since 1.0
 * @deprecated To be soon removed by something easier to maintain (return JSON to JS?).
 */
public class DiagnosticUtil {

    private enum RENDER_TYPE {
        TEXT, IMAGE
    }

    private static final String INNER_TABLE_FOOTER = "</table>\n</td>\n</tr>";

    private static final int MAX_DEPTH = 3;

    private DiagnosticUtil() {
        super();
    }

    private static String getInnerTableHeader(String id, String clazz) {
        String innerTableHeader = "<tr TR_ID TR_CLASS>\n" +
                "<td colspan='4' class='yaml' >\n" +
                "<table width='100%' class='yaml' >";
        if (id == null) {
            innerTableHeader = innerTableHeader.replace("TR_ID", "");
        } else {
            innerTableHeader = innerTableHeader.replace("TR_ID", "id='tr_" + id + "'");
        }
        if (clazz == null) {
            innerTableHeader = innerTableHeader.replace("TR_CLASS", "");
        } else {
            innerTableHeader = innerTableHeader.replace("TR_CLASS", "class='tr_" + clazz + "'");
        }
        return innerTableHeader;
    }

    public static String createDiagnosticTable(String tapFile, Map<String, Object> diagnostic, TestResult tapLine) {
        StringBuilder sb = new StringBuilder();
        createDiagnosticTableRecursively(tapFile, null, diagnostic, sb, 1, tapLine);
        return sb.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void createDiagnosticTableRecursively(
            String tapFile,
            String parentKey,
            Map<String, Object> diagnostic,
            StringBuilder sb,
            int depth,
            TestResult tapLine) {

        String id = getId(tapLine, tapFile);
        String clazz = getClazz(tapLine);
        sb.append(getInnerTableHeader(id, clazz));

        final RENDER_TYPE renderType = getMapEntriesRenderType(diagnostic);
        final List<String> parentKeys = Arrays.asList("files", "extensions");

        for (Entry<String, Object> entry : diagnostic.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();

            sb.append("<tr>");

            sb.append("<td width='5%' class='hidden'> </td>".repeat(Math.max(0, depth)));
            sb.append("<td style=\"width: auto;\">").append(StringEscapeUtils.escapeHtml(key)).append(jspm(getKeyId(key, id))).append("</td>");

            if(renderType == RENDER_TYPE.IMAGE && key.equals("File-Content")) {
                final Object o = diagnostic.get("File-Name");
                final String fileName = (o instanceof String) ? (String) o : "attachment";
                final boolean useParentKey = (parentKey != null && depth > MAX_DEPTH && !parentKeys.contains(parentKey.trim().toLowerCase(Locale.ROOT)));
                final String downloadKey = useParentKey ? parentKey : fileName;
                Arrays.asList(
                        td(key,id),
                        "<a href='downloadAttachment?f=",
                        Functions.htmlAttributeEscape(tapFile),
                        "&key=",
                        Functions.htmlAttributeEscape(downloadKey),
                        "'>",
                        StringEscapeUtils.escapeHtml(fileName),
                        "</a></td>"
                ).forEach(sb::append);
            } else if (renderType == RENDER_TYPE.TEXT && value instanceof java.util.Map) {
                sb.append(td(key,id)).append(" </td>");
                createDiagnosticTableRecursively(tapFile, key, (java.util.Map) value, sb, (depth + 1), tapLine);
            } else {
                sb.append(td(key,id)).append("<pre>").append(org.apache.commons.lang.StringEscapeUtils.escapeHtml(value.toString())).append("</pre></td>");
            }
            sb.append("</tr>");
        }

        sb.append(INNER_TABLE_FOOTER);
    }


    private static String td(String key, String id) {
        return "<td class='detail_body'' id='" + getKeyId(key, id) + "' >";
    }

    private static RENDER_TYPE getMapEntriesRenderType(Map<String, Object> diagnostic) {
        if (diagnostic.containsKey("File-Type")) {
            if (diagnostic.containsKey("File-Location") || diagnostic.containsKey("File-Content")) {
                return RENDER_TYPE.IMAGE;
            }
        }
        return RENDER_TYPE.TEXT;
    }

    private static String getClazz(TestResult tapLine) {
        return Util.getClazz("details_", tapLine);
    }

    private static String getKeyId(String key, String id) {
        return StringEscapeUtils.escapeHtml(key + "_" + id);
    }

    private static String getId(TestResult tapLine, String file) {
        return Util.getId("details_", tapLine, file);
    }

    private static String jspm(String id) {
        return " <u class=\"jsPM\" onclick='extendedTapsetInteractives().showHideBlockOne(\"" + id + "\")'>+/-</u>";
    }

}
