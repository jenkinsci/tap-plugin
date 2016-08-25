/*
 * The MIT License
 *
 * Copyright (c) 2012 Bruno P. Kinoshita
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
package org.tap4j.plugin.model;

import hudson.model.AbstractBuild;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.TapResult;
import org.tap4j.util.StatusValues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A tabulated TAP Stream result.
 *
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class TapStreamResult extends TabulatedResult {

    private static final long serialVersionUID = 8337146933697574082L;
    private final AbstractBuild<?, ?> owner;
    private List<TestResult> children = new ArrayList<TestResult>();
    private TapResult tapResult;

    public TapStreamResult(AbstractBuild<?, ?> owner, TapResult tapResult) {
        this.owner = owner;
        this.tapResult = tapResult;
        setChildrenInfo();
    }
    
    /* (non-Javadoc)
     * @see hudson.model.ModelObject#getDisplayName()
     */
    public String getDisplayName() {
        return "TAP Stream Results";
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.TestObject#getOwner()
     */
    @Override
    public AbstractBuild<?, ?> getOwner() {
        return owner;
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.TestObject#getParent()
     */
    @Override
    public TestObject getParent() {
        return null;
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.TestObject#findCorrespondingResult(java.lang.String)
     */
    @Override
    public TestResult findCorrespondingResult(String id) {
        return null;
    }
    
    /* (non-Javadoc)
     * @see hudson.tasks.test.TabulatedResult#getChildren()
     */
    @Override
    public Collection<? extends TestResult> getChildren() {
        return children;
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.TabulatedResult#hasChildren()
     */
    @Override
    public boolean hasChildren() {
        return children.size() > 0;
    }
    
    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getFailCount()
     */
    @Override
    @Exported(visibility = 2)
    public int getFailCount() {
        return tapResult.getFailed();
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getTotalCount()
     */
    @Override
    @Exported(visibility = 2)
    public int getTotalCount() {
        return tapResult.getTotal();
    }
    
    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getSkipCount()
     */
    @Override
    @Exported(visibility = 2)
    public int getSkipCount() {
        return tapResult.getSkipped();
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getFailedTests()
     */
    @Override
    public List<CaseResult> getFailedTests() {
        //throw new AssertionError("Not supposed to be called");
        return Collections.emptyList();
    }
    
    // FIXME: use the getFailedTests, or explain why it's not used
    public List<TestResult> getFailedTests2() {
        List<TestResult> failedTests = new ArrayList<TestResult>();
        if(tapResult != null && tapResult.getTestSets().size() > 0) {
            for(TestSetMap tsm : tapResult.getTestSets()) {
                TestSet ts = tsm.getTestSet();
                for(org.tap4j.model.TestResult tr : ts.getTestResults()) {
                    if(tr.getStatus() == StatusValues.NOT_OK) {
                        failedTests.add(new TapTestResultResult(owner, tsm, tr, this.tapResult.getTodoIsFailure(), tapResult.getIncludeCommentDiagnostics(), tapResult.getValidateNumberOfTests()));
                    }
                }
            }
        }
        return failedTests;
    }

    public float getDuration() {
        return this.tapResult.getDuration();
    };

    @Override
    public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
        TapTestResultResult tr = getTapTestResultResult(name);
        if (tr != null) {
            return tr;
        } else {
            return super.getDynamic(name, req, rsp);
        }
    }

    public TapResult getTapResult() {
        return this.tapResult;
    }

    /**
     * @param name
     * @return
     */
    private TapTestResultResult getTapTestResultResult(String name) {
        if (name == null)
            return null; // we don't allow null, nay!
        if (name.lastIndexOf("-") <= 0)
            return null; // ops, where's the - mate?
        
        name = name.trim();
        
        int rightIndex = name.length();
        while (name.charAt(rightIndex-1) == '/') {
            rightIndex -= 1;
        }
        int leftIndex = name.lastIndexOf('/') +1;
        
        String testResultName = name.substring(leftIndex, rightIndex); // but we want the test result name (testSet1.tap)
        if (testResultName.indexOf('-') <= 0) // plus the number (testSet1.tap-2)
            return null;
        String testNumber = testResultName.substring(testResultName.lastIndexOf('-')+1);
        String fileName = name.substring(0, name.lastIndexOf('-'));
        
        for(TestSetMap tsm : tapResult.getTestSets()) {
            if(tsm.getFileName().equals(fileName)) {
                TestSet ts = tsm.getTestSet();
                org.tap4j.model.TestResult desired = ts.getTestResult(Integer.parseInt(testNumber));
                return new TapTestResultResult(owner, tsm, desired, this.tapResult.getTodoIsFailure(), tapResult.getIncludeCommentDiagnostics(), tapResult.getValidateNumberOfTests());
            }
        }
        
        return null; // ops, something went wrong
    }

    public void merge(TapResult other) {

        tapResult = tapResult.copyWithExtraTestSets(other.getTestSets());
        tapResult.tally();
        children = new ArrayList<TestResult>();

        setChildrenInfo();
    }

    private void setChildrenInfo() {
        for(TestSetMap tsm : tapResult.getTestSets()) {
            TestSet ts = tsm.getTestSet();
            for(org.tap4j.model.TestResult tr : ts.getTestResults()) {
                this.children.add(new TapTestResultResult(owner, tsm, tr, tapResult.getTodoIsFailure(), tapResult.getIncludeCommentDiagnostics(), tapResult.getValidateNumberOfTests()));
            }
        }
    }
}
