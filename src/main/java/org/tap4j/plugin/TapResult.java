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

import javax.servlet.ServletOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TapResult implements ModelObject, Serializable {

    private static final long serialVersionUID = 4343399327336076951L;
    
    private static final Logger LOGGER = Logger.getLogger(TapResult.class.getName());
    
    private static final String DURATION_KEY = "duration_ms";

    private Run build;
    private List<TestSetMap> testSets;
    private List<TestSetMap> parseErrorTestSets;
    private int failed = 0;
    private int passed = 0;
    private int skipped = 0;
    private int bailOuts = 0;
    private int total = 0;
    private float duration = 0.0f;
    private String name;
    private Boolean todoIsFailure;
    private Boolean includeCommentDiagnostics;
    private Boolean validateNumberOfTests;
    private Boolean showOnlyFailures = Boolean.FALSE;

    public TapResult(String name, Run owner,
            List<TestSetMap> testSets, Boolean todoIsFailure, Boolean includeCommentDiagnostics ,
            Boolean validateNumberOfTests) {
        this.name = name;
        this.build = owner;
        this.testSets = this.filterTestSet(testSets);
        this.parseErrorTestSets = this.filterParseErrorTestSets(testSets);
        this.todoIsFailure = todoIsFailure;
        this.includeCommentDiagnostics= includeCommentDiagnostics;
        this.validateNumberOfTests = validateNumberOfTests;
    }

    public TapResult copyWithExtraTestSets(List<TestSetMap> testSets) {
        List<TestSetMap> mergedTestSets = new ArrayList<TestSetMap>(getTestSets());
        mergedTestSets.addAll(testSets);

        return new TapResult(
            this.getName(),
            this.getOwner(),
            mergedTestSets,
            this.getTodoIsFailure(),
            this.getIncludeCommentDiagnostics(),
            this.getValidateNumberOfTests()
        );
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
        return (includeCommentDiagnostics == null) ? true : includeCommentDiagnostics;
    }
    
    public Boolean getValidateNumberOfTests() {
        return (validateNumberOfTests == null) ? false : validateNumberOfTests;
    }
    
    /**
     * @param testSets
     *            Untiltered test sets
     * @return Test sets that failed to parse
     */
    private List<TestSetMap> filterParseErrorTestSets(List<TestSetMap> testSets) {
        final List<TestSetMap> filtered = new ArrayList<TestSetMap>();
        for (TestSetMap testSet : testSets) {
            if (testSet instanceof ParseErrorTestSetMap) {
                String rootDir = build.getRootDir().getAbsolutePath();
                try {
                    rootDir = new File(build.getRootDir().getCanonicalPath().toString(), Constants.TAP_DIR_NAME).getAbsolutePath();
                } catch (IOException e) {
                    LOGGER.warning(e.getMessage());
                }
                filtered.add(new TestSetMap(Util.normalizeFolders(rootDir, testSet.getFileName()), testSet.getTestSet()));
            }
        }
        return filtered;
    }

    /**
     * @param testSets
     *            Unfiltered test sets
     * @return Test sets that didn't fail to parse
     */
    private List<TestSetMap> filterTestSet(List<TestSetMap> testSets) {
        final List<TestSetMap> filtered = new ArrayList<TestSetMap>();
        for (TestSetMap testSet : testSets) {
            if (testSet instanceof ParseErrorTestSetMap == false) {
                String rootDir = build.getRootDir().getAbsolutePath();
                try {
                    rootDir = new File(build.getRootDir().getCanonicalPath().toString(), Constants.TAP_DIR_NAME).getAbsolutePath();
                } catch (IOException e) {
                    LOGGER.warning(e.getMessage());
                }
                filtered.add(new TestSetMap(Util.normalizeFolders(rootDir, testSet.getFileName()), testSet.getTestSet()));
            }
        }
        return filtered;
    }

    public void tally() {

        failed = 0;
        passed = 0;
        skipped = 0;
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
                    } else {
                        passed += 1;
                    }
                    // FIXME: code duplication. Refactor it and TapTestResultResult
                    Map<String, Object> diagnostic = testResult.getDiagnostic();
                    if (diagnostic != null && ! diagnostic.isEmpty()) {
                        Object duration = diagnostic.get(DURATION_KEY);
                        if (duration != null) {
                            Float durationMS = Float.parseFloat(duration.toString());
                            this.duration += durationMS;
                        }
                    }
                }
            }
            
            this.bailOuts += realTestSet.getNumberOfBailOuts();
        }
    }

    public Run getOwner() {
        return this.build;
    }

    /**
     * @param owner
     *            the owner to set
     */
    public void setOwner(Run owner) {
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

    public int getPassed() {
        return this.passed;
    }

    public int getBailOuts() {
        return this.bailOuts;
    }

    public int getTotal() {
        return this.total;
    }

    public float getDuration() {
        return this.duration;
    }

    /**
     * Called from TapResult/index.jelly
     */
    public String createDiagnosticTable(String tapFile, Map<String, Object> diagnostic) {
        return DiagnosticUtil.createDiagnosticTable(tapFile, diagnostic);
    }

    public boolean isTestResult(Object tapResult) {
        return (tapResult != null && tapResult instanceof TestResult);
    }

    public boolean isBailOut(Object tapResult) {
        return (tapResult != null && tapResult instanceof BailOut);
    }
    
    public boolean isComment(Object tapResult) {
        return (tapResult != null && tapResult instanceof Comment);
    }
    
    public String escapeHTML(String html) {
        return StringUtils.replaceEach(html, new String[]{"&", "\"", "<", ">"}, new String[]{"&amp;", "&quot;", "&lt;", "&gt;"});
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
        String f = request.getParameter("f");
        String key = request.getParameter("key");
        try {
            FilePath parent = new FilePath(new File(build.getRootDir(), Constants.TAP_DIR_NAME));
            FilePath tapDir = parent.child(TestObject.safe(f));
            ServletOutputStream sos = response.getOutputStream();
            if(tapDir.exists()) {
                String tapStream = tapDir.readToString();
                TapConsumer consumer = TapConsumerFactory.makeTap13YamlConsumer();
                TestSet ts = consumer.load(tapStream);
                
                TapAttachment attachment = getAttachment(ts, key);
                if(attachment != null) {
                    response.setContentType("application/force-download");
                    //response.setContentLength((int)tapDir.length());
                    response.setContentLength(attachment.getSize());
                    response.setHeader("Content-Transfer-Encoding", "binary");
                    response.setHeader("Content-Disposition","attachment; filename=\"" + attachment.getFileName() + "\"");//fileName);
                    
                    sos.write(attachment.getContent());
                    sos.print('\n');
                } else {
                    sos.println("Couldn't locate attachment in YAMLish: " + f);
                }
            } else {
                sos.println("Couldn't read FilePath.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
        }
    }

    /**
     * @param ts
     * @param key
     * @return
     */
    private TapAttachment getAttachment(TestSet ts, String key) {
        for(TestResult tr : ts.getTestResults()){
            Map<String, Object> diagnostics = tr.getDiagnostic();
            if(diagnostics != null && diagnostics.size() > 0) {
                TapAttachment attachement = recursivelySearch(diagnostics, null, key);
                if (attachement != null) {
                     return attachement;
                }
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private TapAttachment recursivelySearch(Map<String, Object> diagnostics, String parentKey, String key) {
        for(String diagnosticKey : diagnostics.keySet()) {
            Object value = diagnostics.get(diagnosticKey);
            if(value != null) {
                if(value instanceof Map<?, ?>) {
                    TapAttachment attachment = recursivelySearch((Map<String, Object>)value, diagnosticKey, key);
                    if (attachment != null) {
                        return attachment;
                    }
                } else {
                    if(parentKey != null && parentKey.equals(key)) {
                        Object o = diagnostics.get("File-Content");
                        if(o == null)
                            o = diagnostics.get("File-content");
                        if(o != null && o instanceof String)
                            return new TapAttachment(Base64.decodeBase64((String)o), diagnostics);
                    } else if(diagnosticKey.equalsIgnoreCase("file-name") && value.equals(key)) {
                        Object o = diagnostics.get("File-Content");
                        if(o == null)
                            o = diagnostics.get("File-content");
                        if(o != null && o instanceof String)
                            return new TapAttachment(Base64.decodeBase64((String)o), diagnostics);
                    }
                }
            }
        }
        return null;
    }
}
