/*
 * The MIT License
 *
 * Copyright (c) 2011 Bruno P. Kinoshita
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
package org.tap4j.plugin;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.FilePath;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.tasks.test.TestObject;
import jakarta.servlet.ServletOutputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.export.Exported;
import org.tap4j.consumer.TapConsumer;
import org.tap4j.consumer.TapConsumerFactory;
import org.tap4j.model.BailOut;
import org.tap4j.model.Comment;
import org.tap4j.model.Plan;
import org.tap4j.model.TestResult;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.model.ParseErrorTestSetMap;
import org.tap4j.plugin.model.TapAttachment;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.plugin.util.Constants;
import org.tap4j.plugin.util.DiagnosticUtil;
import org.tap4j.plugin.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents the results of parsing TAP test output.
 *
 * @author Bruno P. Kinoshita
 * @since 1.0
 */
public class TapResult implements ModelObject, Serializable {

    @Serial
    private static final long serialVersionUID = 4343399327336076951L;

    private static final Logger LOGGER = Logger.getLogger(TapResult.class.getName());

    private static final String DURATION_KEY = "duration_ms";

    private final List<TestSetMap> testSets;
    private final List<TestSetMap> parseErrorTestSets;
    private final String name;
    private final Boolean todoIsFailure;
    private final Boolean includeCommentDiagnostics;
    private final Boolean validateNumberOfTests;

    private transient Run<?, ?> build;

    private int failed;
    private int passed;
    private int skipped;
    private int todo;
    private int bailOuts;
    private int total;
    private float duration;

    private Boolean showOnlyFailures = Boolean.FALSE;

    public TapResult(
        String name,
        Run<?, ?> owner,
        List<TestSetMap> testSets,
        Boolean todoIsFailure,
        Boolean includeCommentDiagnostics,
        Boolean validateNumberOfTests) {

        this.name = name;
        this.build = owner;
        this.testSets = filterTestSet(testSets);
        this.parseErrorTestSets = filterParseErrorTestSets(testSets);
        this.todoIsFailure = todoIsFailure;
        this.includeCommentDiagnostics = includeCommentDiagnostics;
        this.validateNumberOfTests = validateNumberOfTests;
    }

    public TapResult copyWithExtraTestSets(List<TestSetMap> testSets) {
        List<TestSetMap> mergedTestSets = new ArrayList<>(getTestSets());
        mergedTestSets.addAll(testSets);

        TapResult tapResult = new TapResult(
            getName(),
            getOwner(),
            mergedTestSets,
            getTodoIsFailure(),
            getIncludeCommentDiagnostics(),
            getValidateNumberOfTests());

        tapResult.setShowOnlyFailures(getShowOnlyFailures());
        return tapResult;
    }

    public Boolean getShowOnlyFailures() {
        return BooleanUtils.toBooleanDefaultIfNull(showOnlyFailures, Boolean.FALSE);
    }

    public void setShowOnlyFailures(Boolean showOnlyFailures) {
        this.showOnlyFailures = showOnlyFailures;
    }

    public Boolean getTodoIsFailure() {
        return todoIsFailure;
    }

    public Boolean getIncludeCommentDiagnostics() {
        return includeCommentDiagnostics == null || includeCommentDiagnostics;
    }

    public Boolean getValidateNumberOfTests() {
        return validateNumberOfTests != null && validateNumberOfTests;
    }

    private List<TestSetMap> filterParseErrorTestSets(List<TestSetMap> testSets) {
        if (build == null) {
            return Collections.emptyList();
        }

        List<TestSetMap> filtered = new ArrayList<>();

        for (TestSetMap testSet : testSets) {
            if (testSet instanceof ParseErrorTestSetMap) {
                filtered.add(normalizeTestSetPath(testSet));
            }
        }

        return filtered;
    }

    private List<TestSetMap> filterTestSet(List<TestSetMap> testSets) {
        if (build == null) {
            return Collections.emptyList();
        }

        List<TestSetMap> filtered = new ArrayList<>();

        for (TestSetMap testSet : testSets) {
            if (!(testSet instanceof ParseErrorTestSetMap)) {
                filtered.add(normalizeTestSetPath(testSet));
            }
        }

        return filtered;
    }

    private TestSetMap normalizeTestSetPath(TestSetMap testSet) {
        String rootDir = build.getRootDir().getAbsolutePath();

        try {
            rootDir = new File(
                build.getRootDir().getCanonicalPath(),
                Constants.TAP_DIR_NAME).getAbsolutePath();
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }

        return new TestSetMap(
            Util.normalizeFolders(rootDir, testSet.getFileName()),
            testSet.getTestSet());
    }

    /**
     * Calculates the test result statistics.
     */
    public void tally() {
        failed = 0;
        passed = 0;
        skipped = 0;
        todo = 0;
        bailOuts = 0;
        total = 0;
        duration = 0.0f;

        for (TestSetMap testSet : testSets) {
            TestSet realTestSet = testSet.getTestSet();
            List<TestResult> testResults = realTestSet.getTestResults();

            total += testResults.size();

            Plan plan = realTestSet.getPlan();

            if (plan != null && plan.isSkip()) {
                skipped += testResults.size();
            } else {
                for (TestResult testResult : testResults) {
                    if (Util.isSkipped(testResult)) {
                        skipped++;
                    } else if (Util.isFailure(testResult, todoIsFailure)) {
                        failed++;
                    } else if (Util.isTodo(testResult)) {
                        todo++;
                    } else {
                        passed++;
                    }

                    Map<String, Object> diagnostic = testResult.getDiagnostic();

                    if (diagnostic != null && !diagnostic.isEmpty()) {
                        Object durationValue = diagnostic.get(DURATION_KEY);

                        if (durationValue != null) {
                            duration += Float.parseFloat(durationValue.toString());
                        }
                    }
                }
            }

            bailOuts += realTestSet.getNumberOfBailOuts();
        }
    }

    @Nullable
    public Run<?, ?> getOwner() {
        return build;
    }

    public void setOwner(Run<?, ?> owner) {
        this.build = owner;
    }

    @Restricted(NoExternalUse.class)
    @CheckForNull
    public Run<?, ?> getOwningRun() {
        StaplerRequest2 req = Stapler.getCurrentRequest2();

        if (req == null) {
            return null;
        }

        return req.findAncestorObject(Run.class);
    }

    public List<TestSetMap> getTestSets() {
        return testSets;
    }

    public boolean isEmptyTestSet() {
        return testSets.isEmpty();
    }

    public List<TestSetMap> getParseErrorTestSets() {
        return parseErrorTestSets;
    }

    public boolean hasParseErrors() {
        return !parseErrorTestSets.isEmpty();
    }

    public int getFailed() {
        return failed;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getToDo() {
        return todo;
    }

    public int getPassed() {
        return passed;
    }

    public int getBailOuts() {
        return bailOuts;
    }

    public int getTotal() {
        return total;
    }

    public float getDuration() {
        return duration;
    }

    public String createDiagnosticTable(
        String tapFile,
        Map<String, Object> diagnostic) {
        return DiagnosticUtil.createDiagnosticTable(tapFile, diagnostic);
    }

    public boolean isTestResult(Object tapResult) {
        return tapResult instanceof TestResult;
    }

    public boolean isBailOut(Object tapResult) {
        return tapResult instanceof BailOut;
    }

    public boolean isComment(Object tapResult) {
        return tapResult instanceof Comment;
    }

    public String escapeHTML(String html) {
        return StringUtils.replaceEach(
            html,
            new String[]{"&", "\"", "<", ">"},
            new String[]{"&amp;", "&quot;", "&lt;", "&gt;"});
    }

    @Exported(visibility = 999)
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    public void doDownloadAttachment(
        StaplerRequest2 request,
        StaplerResponse2 response) {

        final String fileName = request.getParameter("f");
        final String key = request.getParameter("key");

        try {
            ServletOutputStream sos = response.getOutputStream();

            if (build == null) {
                sos.println("No build located in Jenkins. Cannot download attachment.");
                return;
            }

            FilePath parent = new FilePath(
                new File(build.getRootDir(), Constants.TAP_DIR_NAME));

            FilePath tapFile = parent.child(TestObject.safe(fileName));

            if (!tapFile.exists()) {
                sos.println("Couldn't read FilePath.");
                return;
            }

            String tapStream = tapFile.readToString();

            TapConsumer consumer = TapConsumerFactory.makeTap13YamlConsumer();
            TestSet testSet = consumer.load(tapStream);

            TapAttachment attachment = getAttachment(testSet, key);

            if (attachment == null) {
                sos.println("Couldn't locate attachment in YAMLish: " + fileName);
                return;
            }

            response.setContentType("application/force-download");
            response.setContentLength(attachment.getSize());
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + attachment.getFileName() + "\"");

            sos.write(attachment.getContent());
            sos.print('\n');

        } catch (IOException | InterruptedException e) {
            LOGGER.warning("Unable to download TAP attachment: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private TapAttachment getAttachment(TestSet testSet, String key) {
        for (TestResult testResult : testSet.getTestResults()) {
            Map<String, Object> diagnostics = testResult.getDiagnostic();

            if (diagnostics != null && !diagnostics.isEmpty()) {
                TapAttachment attachment = recursivelySearch(
                    diagnostics, null, key);

                if (attachment != null) {
                    return attachment;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private TapAttachment recursivelySearch(
        Map<String, Object> diagnostics,
        String parentKey,
        String key) {

        for (Map.Entry<String, Object> entry : diagnostics.entrySet()) {
            String diagnosticKey = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            if (value instanceof Map<?, ?>) {
                TapAttachment attachment = recursivelySearch(
                    (Map<String, Object>) value,
                    diagnosticKey,
                    key);

                if (attachment != null) {
                    return attachment;
                }
            } else if ((parentKey != null && parentKey.equals(key))
                || (diagnosticKey.equalsIgnoreCase("file-name")
                && value.equals(key))) {

                Object content = diagnostics.get("File-Content");

                if (content == null) {
                    content = diagnostics.get("File-content");
                }

                if (content instanceof String) {
                    return new TapAttachment(
                        Base64.decodeBase64((String) content),
                        diagnostics);
                }
            }
        }

        return null;
    }
}
