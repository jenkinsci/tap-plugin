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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Used to create YAML view.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class DiagnosticUtil {

    private enum RENDER_TYPE {
        TEXT, IMAGE
    };

    private static final String INNER_TABLE_HEADER = "<tr>\n<td colspan='4' class='yaml'>\n<table width=\"100%\" class=\"yaml\">";

    private static final String INNER_TABLE_FOOTER = "</table>\n</td>\n</tr>";

    private DiagnosticUtil() {
        super();
    }

    public static String createDiagnosticTable(String tapFile, Map<String, Object> diagnostic) {
        StringBuilder sb = new StringBuilder();
        createDiagnosticTableRecursively(tapFile, null, diagnostic, sb, 1); // 1 is the first
                                                                // depth
        return sb.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void createDiagnosticTableRecursively(String tapFile, String parentKey, 
            Map<String, Object> diagnostic, StringBuilder sb, int depth) {

        sb.append(INNER_TABLE_HEADER);

        RENDER_TYPE renderType = getMapEntriesRenderType(diagnostic);

        if(renderType == RENDER_TYPE.IMAGE) {
            for (Entry<String, Object> entry : diagnostic.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                sb.append("<tr>");
    
                for (int i = 0; i < depth; ++i) {
                    sb.append("<td width='5%' class='hidden'> </td>");
                }
                sb.append("<td style=\"width: auto;\">" + key + "</td>");
                if(key.equals("File-Content")) {
                    String fileName = "attachment";
                    Object o = diagnostic.get("File-Name");
                    if(o!=null && o instanceof String) {
                        fileName = (String)o;
                    }
                    String downloadKey = fileName;
                    if(parentKey != null){
                        if(depth > 3 && !parentKey.trim().equalsIgnoreCase("files") && !parentKey.trim().equalsIgnoreCase("extensions")) {
                            downloadKey = parentKey;
                        }
                    }
                    sb.append("<td><a href='downloadAttachment?f="+tapFile+"&key="+downloadKey+"'>"+fileName+"</a></td>");
                } else {
                    sb.append("<td><pre>" + org.apache.commons.lang.StringEscapeUtils.escapeHtml(value.toString()) + "</pre></td>");
                }
                sb.append("</tr>");
            }
        } else {
            for (Entry<String, Object> entry : diagnostic.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                sb.append("<tr>");
    
                for (int i = 0; i < depth; ++i) {
                    sb.append("<td width='5%' class='hidden'> </td>");
                }
                sb.append("<td style=\"width: auto;\">" + key + "</td>");
                if (value instanceof java.util.Map) {
                    sb.append("<td> </td>");
                    createDiagnosticTableRecursively(tapFile, key, (java.util.Map) value, sb,
                            (depth + 1));
                } else {
                    sb.append("<td><pre>" + org.apache.commons.lang.StringEscapeUtils.escapeHtml(value.toString()) + "</pre></td>");
                }
                sb.append("</tr>");
            }
        }

        sb.append(INNER_TABLE_FOOTER);
    }

    /**
     * @param diagnostic
     * @return
     */
    private static RENDER_TYPE getMapEntriesRenderType(
            Map<String, Object> diagnostic) {
        RENDER_TYPE renderType = RENDER_TYPE.TEXT;
        final Set<String> keys = diagnostic.keySet();
        if (keys.contains("File-Type")
                && (keys.contains("File-Location") || keys
                        .contains("File-Content"))) {
            renderType = RENDER_TYPE.IMAGE;
        }
        return renderType;
    }

}
