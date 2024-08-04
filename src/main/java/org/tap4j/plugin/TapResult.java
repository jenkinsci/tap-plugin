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

import hudson.FilePath;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.tasks.test.TestObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
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

import javax.annotation.Nullable;
import javax.servlet.ServletOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Bruno P. Kinoshita - <a href="https://kinoshita.eti.br">...</a>
 * @since 1.0
 */
public class TapResult implements ModelObject, Serializable {

    private static final long serialVersionUID = 4343399327336076951L;

    private static final Logger LOGGER = Logger.getLogger(TapResult.class.getName());

    private static final String DURATION_KEY = "duration_ms";

    private transient Run<?, ?> build;
    private final  List<TestSetMap> testSets;
    private final  List<TestSetMap> parseErrorTestSets;
    private int failed = 0;
    private int passed = 0;
    private int skipped = 0;
    private int todo = 0;
    private int bailOuts = 0;
    private int total = 0;
    private float duration = 0.0f;
    private final String name;
    private final Boolean todoIsFailure;
    private final Boolean includeCommentDiagnostics;
    private final Boolean validateNumberOfTests;
    private Boolean showOnlyFailures = Boolean.FALSE;

    public TapResult(String name, Run<?, ?> owner, List<TestSetMap> testSets, Boolean todoIsFailure,
            Boolean includeCommentDiagnostics, Boolean validateNumberOfTests) {
        this.name = name;
        this.build = owner;
        this.testSets = this.filterTestSet(testSets);
        this.parseErrorTestSets = this.filterParseErrorTestSets(testSets);
        this.todoIsFailure = todoIsFailure;
        this.includeCommentDiagnostics = includeCommentDiagnostics;
        this.validateNumberOfTests = validateNumberOfTests;
    }

    public TapResult copyWithExtraTestSets(List<TestSetMap> testSets) {
        List<TestSetMap> mergedTestSets = new ArrayList<>(getTestSets());
        mergedTestSets.addAll(testSets);

        return new TapResult(this.getName(), this.getOwner(), mergedTestSets, this.getTodoIsFailure(),
                this.getIncludeCommentDiagnostics(), this.getValidateNumberOfTests());
    }

    public Boolean getShowOnlyFailures() {
        return BooleanUtils.toBooleanDefaultIfNull(showOnlyFailures, Boolean.FALSE);
    }

    public void setShowOnlyFailures(Boolean showOnlyFailures) {
        this.showOnlyFailures = showOnlyFailures;
    }

    /**
     * @return the todoIsFailure
     */
    public Boolean getTodoIsFailure() {
        return todoIsFailure;
    }

    /**
     * @return the includeCommentDiagnostics
     */
    public Boolean getIncludeCommentDiagnostics() {
        return includeCommentDiagnostics == null || includeCommentDiagnostics;
    }

    public Boolean getValidateNumberOfTests() {
        return validateNumberOfTests != null && validateNumberOfTests;
    }

    /**
     * @param testSets Unfiltered test sets
     * @return Test sets that failed to parse
     */
    private List<TestSetMap> filterParseErrorTestSets(List<TestSetMap> testSets) {
        if (this.build == null) {
            return Collections.emptyList();
        }
        final List<TestSetMap> filtered = new ArrayList<>();
        for (TestSetMap testSet : testSets) {
            if (testSet instanceof ParseErrorTestSetMap) {
                String rootDir = build.getRootDir().getAbsolutePath();
                try {
                    rootDir = new File(build.getRootDir()
                            .getCanonicalPath(), Constants.TAP_DIR_NAME).getAbsolutePath();
                } catch (IOException e) {
                    LOGGER.warning(e.getMessage());
                }
                filtered.add(
                        new TestSetMap(Util.normalizeFolders(rootDir, testSet.getFileName()), testSet.getTestSet()));
            }
        }
        return filtered;
    }

    /**
     * @param testSets Unfiltered test sets
     * @return Test sets that didn't fail to parse
     */
    private List<TestSetMap> filterTestSet(List<TestSetMap> testSets) {
        if (this.build == null) {
            return Collections.emptyList();
        }
        final List<TestSetMap> filtered = new ArrayList<>();
        for (TestSetMap testSet : testSets) {
            if (!(testSet instanceof ParseErrorTestSetMap)) {
                String rootDir = build.getRootDir().getAbsolutePath();
                try {
                    rootDir = new File(build.getRootDir()
                            .getCanonicalPath(), Constants.TAP_DIR_NAME).getAbsolutePath();
                } catch (IOException e) {
                    LOGGER.warning(e.getMessage());
                }
                filtered.add(
                        new TestSetMap(Util.normalizeFolders(rootDir, testSet.getFileName()), testSet.getTestSet()));
            }
        }
        return filtered;
    }

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
                this.skipped += testResults.size();
            } else {
                for (TestResult testResult : testResults) {
                    if (Util.isSkipped(testResult)) {
                        skipped += 1;
                    } else if (Util.isFailure(testResult, todoIsFailure)) {
                        failed += 1;
                    } else if (Util.isTodo(testResult)) {
                        todo += 1;
                    } else {
                        passed += 1;
                    }
                    // FIXME: code duplication. Refactor it and TapTestResultResult
                    Map<String, Object> diagnostic = testResult.getDiagnostic();
                    if (diagnostic != null && !diagnostic.isEmpty()) {
                        Object duration = diagnostic.get(DURATION_KEY);
                        if (duration != null) {
                            float durationMS = Float.parseFloat(duration.toString());
                            this.duration += durationMS;
                        }
                    }
                }
            }

            this.bailOuts += realTestSet.getNumberOfBailOuts();
        }
    }

    @Nullable
    public Run<?, ?> getOwner() {
        return this.build;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Run<?, ?> owner) {
        this.build = owner;
    }

    public List<TestSetMap> getTestSets() {
        return this.testSets;
    }

    public boolean isEmptyTestSet() {
        return this.testSets.size() <= 0;
    }

    /**
     * @return the parseErrorTestSets
     */
    public List<TestSetMap> getParseErrorTestSets() {
        return parseErrorTestSets;
    }

    public boolean hasParseErrors() {
        return this.parseErrorTestSets.size() > 0;
    }

    public int getFailed() {
        return this.failed;
    }

    public int getSkipped() {
        return this.skipped;
    }

    public int getToDo() {
        return this.todo;
    }

    public int getPassed() {
        return this.passed;
    }

    public int getBailOuts() {
        return this.bailOuts;
    }

    public int getComments() {
        int count = 0;
        for (TestSetMap testSet: testSets) {
            count += testSet.getTestSet().getComments().size();
        }
        return count;
    }

    public int getTotal() {
        return this.total;
    }

    public float getDuration() {
        return this.duration;
    }

    /**
     * Called from TapResult/index.jelly
     * @param tapFile location of TAP file
     * @param diagnostic TAP diagnostics
     * @param tapLine TAP origin of the diagnsotic table
     * @return diagnostic table
     */
    public String createDiagnosticTable(String tapFile, Map<String, Object> diagnostic, TestResult tapLine) {
        return DiagnosticUtil.createDiagnosticTable(tapFile, diagnostic, tapLine);
    }

    /**
     * @param tapResult the tap result object
     * @return {@code true} if the object is not null and an instance of {@link TestResult}
     */
    public boolean isTestResult(Object tapResult) {
        return (tapResult instanceof TestResult);
    }

    /**
     * @param tapResult the tap result object
     * @return {@code true} if the object is not null and an instance of {@link BailOut}
     */
    public boolean isBailOut(Object tapResult) {
        return (tapResult instanceof BailOut);
    }

    public boolean isComment(Object tapResult) {
        return (tapResult instanceof Comment);
    }

    public String escapeHTML(String html) {
        return StringUtils.replaceEach(html, new String[] { "&", "\"", "<", ">" },
                new String[] { "&amp;", "&quot;", "&lt;", "&gt;" });
    }

    /**
     * @return the name
     */
    @Exported(visibility = 999)
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hudson.model.ModelObject#getDisplayName()
     */
    public String getDisplayName() {
        return getName();
    }

    public void doDownloadAttachment(StaplerRequest request, StaplerResponse response) {
        final String f = request.getParameter("f");
        final String key = request.getParameter("key");
        try {
            final ServletOutputStream sos = response.getOutputStream();

            if (build == null) {
                sos.println("No build located in Jenkins. Cannot download attachment.");
            } else {
                FilePath parent = new FilePath(new File(build.getRootDir(), Constants.TAP_DIR_NAME));
                FilePath tapDir = parent.child(TestObject.safe(f));

                if (tapDir.exists()) {
                    String tapStream = tapDir.readToString();
                    TapConsumer consumer = TapConsumerFactory.makeTap13YamlConsumer();
                    TestSet ts = consumer.load(tapStream);

                    TapAttachment attachment = getAttachment(ts, key);
                    if (attachment != null) {
                        response.setContentType("application/force-download");
                        // response.setContentLength((int)tapDir.length());
                        response.setContentLength(attachment.getSize());
                        response.setHeader("Content-Transfer-Encoding", "binary");
                        response.setHeader("Content-Disposition",
                                "attachment; filename=\"" + attachment.getFileName() + "\"");// fileName);

                        sos.write(attachment.getContent());
                        sos.print('\n');
                    } else {
                        sos.println("Couldn't locate attachment in YAMLish: " + f);
                    }
                } else {
                    sos.println("Couldn't read FilePath.");
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private TapAttachment getAttachment(TestSet ts, String key) {
        for (TestResult tr : ts.getTestResults()) {
            Map<String, Object> diagnostics = tr.getDiagnostic();
            if (diagnostics != null && diagnostics.size() > 0) {
                TapAttachment attachment = recursivelySearch(diagnostics, null, key);
                if (attachment != null) {
                    return attachment;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private TapAttachment recursivelySearch(Map<String, Object> diagnostics, String parentKey, String key) {
        for (Map.Entry<String, Object> entry : diagnostics.entrySet()) {
            final String diagnosticKey = entry.getKey();
            final Object value = entry.getValue();
            if (value != null) {
                if (value instanceof Map<?, ?>) {
                    TapAttachment attachment = recursivelySearch((Map<String, Object>) value, diagnosticKey, key);
                    if (attachment != null) {
                        return attachment;
                    }
                } else {
                    if (parentKey != null && parentKey.equals(key)) {
                        Object o = diagnostics.get("File-Content");
                        if (o == null)
                            o = diagnostics.get("File-content");
                        if (o instanceof String)
                            return new TapAttachment(Base64.decodeBase64((String) o), diagnostics);
                    } else if (diagnosticKey.equalsIgnoreCase("file-name") && value.equals(key)) {
                        Object o = diagnostics.get("File-Content");
                        if (o == null)
                            o = diagnostics.get("File-content");
                        if (o instanceof String)
                            return new TapAttachment(Base64.decodeBase64((String) o), diagnostics);
                    }
                }
            }
        }
        return null;
    }

    //Used in jelly
    public String getClazz(BailOut tapLine) {
        return "_bailout_";
    }

    //Used in jelly
    public String getClazz(Comment tapLine) {
        return "_comment_";
    }

    //Used in jelly
    public String getClazz(TestResult tapLine) {
        return Util.getClazz("test_", tapLine);
    }

    //Used in jelly
    public String getId(TestResult tapLine, String file) {
        return  Util.getId("", tapLine, file);
    }

}
