/*
 * The MIT License
 *
 * Copyright (c) 2013 Bruno P. Kinoshita
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

import org.apache.commons.lang.StringEscapeUtils;
import org.tap4j.model.Directive;
import org.tap4j.model.TestResult;
import org.tap4j.util.DirectiveValues;
import org.tap4j.util.StatusValues;

/**
 * Utility methods used by tap-plugin.
 */
public final class Util {

    private Util() {}
    
    /**
     * Normalizes a folder path in relation to the workspace path.
     * <p>
     * A folder that is subdirectory of workspace will return only the difference.
     * It means that if the workspace is /home/workspace and the folder we want 
     * to normalize is /home/workspace/job-1/test.txt, then the return will be
     * job-1/test.txt.
     * 
     * @param workspace workspace path
     * @param relative relative path
     * @return normalized path
     */
    public static String normalizeFolders(String workspace, String relative) {
        workspace = workspace.replaceAll("\\\\", "\\/");
        relative = relative.replaceAll("\\\\", "\\/");
        if (relative.length() > workspace.length() && relative.contains(workspace)) {
            String temp = relative.substring(workspace.length());
            if (temp.startsWith("/") || temp.startsWith("\\"))
                temp = temp.substring(1);
            return temp;
        }
        return relative;
    }
    
    public static boolean isSkipped(TestResult testResult) {
        boolean r = false;
        Directive directive = testResult.getDirective();
        if (directive != null
                && directive.getDirectiveValue() == DirectiveValues.SKIP) {
            r = true;
        }
        return r;
    }

    public static boolean isTodo(TestResult testResult) {
        boolean r = false;
        Directive directive = testResult.getDirective();
        if (directive != null
                && directive.getDirectiveValue() == DirectiveValues.TODO) {
            r = true;
        }
        return r;
    }

    public static boolean isFailure(TestResult testResult, Boolean todoIsFailure) {
        boolean r = false;
        Directive directive = testResult.getDirective();
        StatusValues status = testResult.getStatus();
        if (directive != null) {
            if(directive.getDirectiveValue() == DirectiveValues.TODO && todoIsFailure != null && todoIsFailure) {
                r = true;
            }
        } else if (status == StatusValues.NOT_OK) {
            r = true;
        }
        return r;
    }

    public static String getClazz(String prefix, TestResult tapLine) {
        return StringEscapeUtils.escapeHtml((prefix + tapLine.getStatus() + (tapLine.getDirective() == null ? "" :
                "_" + tapLine.getDirective().getDirectiveValue())).replaceAll("\\s+", "_"));
    }

    public static String getId(String prefix, TestResult tapLine, String file) {
        return StringEscapeUtils.escapeHtml((prefix + file + "_" + tapLine.getTestNumber() + "_" + tapLine.getDescription()).replaceAll("\\s+", "_"));
    }
}
