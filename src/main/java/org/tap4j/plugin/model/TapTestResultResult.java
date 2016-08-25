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

import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.model.Item;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.tap4j.model.Comment;
import org.tap4j.model.Directive;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.TapResult;
import org.tap4j.plugin.util.Util;
import org.tap4j.util.DirectiveValues;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class TapTestResultResult extends TestResult {

    private static final String DURATION_KEY = "duration_ms";
    
    private static final long serialVersionUID = -4499261655602135921L;
    private static final Logger LOGGER = Logger.getLogger(TapTestResultResult.class.getName());
    
    private final AbstractBuild<?, ?> owner;
    private final org.tap4j.model.TestResult tapTestResult;
    private final TestSetMap testSetMap;
    private final Boolean todoIsFailure;
    private final Boolean includeCommentDiagnostics;
    private final Boolean validateNumberOfTests;
    
    public TapTestResultResult(AbstractBuild<?, ?> owner, TestSetMap testSetMap, org.tap4j.model.TestResult tapTestResult, 
            Boolean todoIsFailure, Boolean includeCommentDiagnostics, Boolean validateNumberOfTests) {
        this.owner = owner;
        this.testSetMap = testSetMap;
        this.tapTestResult = tapTestResult;
        this.todoIsFailure = todoIsFailure;
        this.includeCommentDiagnostics = includeCommentDiagnostics;
        this.validateNumberOfTests = validateNumberOfTests;
    }
    
    /* (non-Javadoc)
     * @see hudson.model.ModelObject#getDisplayName()
     */
    public String getDisplayName() {
        return getName();
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
        TapStreamResult parent = null;
        TestSet testSet = this.tapTestResult.getSubtest();
        if(testSet != null) {
            TestSetMap subTest = new TestSetMap(testSetMap.getFileName(), testSet);
            if(subTest != null) {
                List<TestSetMap> list = new ArrayList<TestSetMap>();
                list.add(subTest);
                parent = new TapStreamResult(owner, new TapResult("TAP Test Results", owner, list, todoIsFailure, includeCommentDiagnostics, validateNumberOfTests));
            }
        }
        return parent;
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.TestObject#findCorrespondingResult(java.lang.String)
     */
    @Override
    public TestResult findCorrespondingResult(String id) {
        if(this.getDisplayName().equals(id)) {
            return this;
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see hudson.tasks.test.TestObject#getName()
     */
    @Override
    public String getName() {
        StringBuilder buf = new StringBuilder();
        buf.append(tapTestResult.getTestNumber());
        String tapTestResultDescription = tapTestResult.getDescription();
        if (StringUtils.isNotBlank(tapTestResultDescription)) {
            buf.append(" - ");
            buf.append(tapTestResultDescription);
        }
        return buf.toString();
    }
    
    public String getStatus() {
        boolean failure = Util.isFailure(this.tapTestResult, todoIsFailure);
        return failure ? "NOT OK" : "OK";
    }
    
    public String getSkip() {
        boolean skip = Util.isSkipped(this.tapTestResult);
        return skip ? "Yes" : "No";
    }
    
    public String getTodo() {
        String todo = "No";
        // TODO: not consistent with the other methods in TapResult
        Directive directive = this.tapTestResult.getDirective();
        if(directive != null) {
            if(directive.getDirectiveValue() == DirectiveValues.TODO) {
                todo = "Yes";
            }
        }
        return todo;
    }
    
    public String getFullName() {
        return getName();
    }
    
    public String getRelativePathFrom(TestObject it) {
        // if (it is one of my ancestors) {
        //    return a relative path from it
        // } else {
        //    return a complete path starting with "/"
        // }
        if (it==this) {
            return ".";
        }

        StringBuilder buf = new StringBuilder();
        TestObject next = this;
        TestObject cur = this;  
        // Walk up my ancesotors from leaf to root, looking for "it"
        // and accumulating a relative url as I go
        while (next!=null && it!=next) {
            cur = next;
            String safeName = cur.getSafeName();

            if (!"(empty)".equals(safeName)) {
                buf.insert(0, '/');
                buf.insert(0, safeName);
            }
            next = cur.getParent();
        }
        if (it==next) {
            return buf.toString();
        } else {
            // Keep adding on to the string we've built so far

            // Start with the test result action
            @SuppressWarnings("rawtypes")
            AbstractTestResultAction action = getTestResultAction();
            if (action==null) {
                //LOGGER.warning("trying to get relative path, but we can't determine the action that owns this result.");
                return ""; // this won't take us to the right place, but it also won't 404.
            }
            buf.insert(0,'/');
            buf.insert(0,action.getUrlName());

            // Now the build
            AbstractBuild<?,?> myBuild = cur.getOwner();
            if (myBuild ==null) {
                //LOGGER.warning("trying to get relative path, but we can't determine the build that owns this result.");
                return ""; // this won't take us to the right place, but it also won't 404. 
            }
            //buf.insert(0,'/');
            buf.insert(0,myBuild.getUrl());

            // If we're inside a stapler request, just delegate to Hudson.Functions to get the relative path!
            StaplerRequest req = Stapler.getCurrentRequest();
            if (req!=null && myBuild instanceof Item) {
                buf.insert(0, '/');
                // Ugly but I don't see how else to convince the compiler that myBuild is an Item
                Item myBuildAsItem = (Item) myBuild;
                buf.insert(0, Functions.getRelativeLinkTo(myBuildAsItem));
            } else {
                // We're not in a stapler request. Okay, give up.
                //LOGGER.info("trying to get relative path, but it is not my ancestor, and we're not in a stapler request. Trying absolute hudson url...");
                String hudsonRootUrl = Jenkins.getInstance().getRootUrl();
                if (hudsonRootUrl==null||hudsonRootUrl.length()==0) {
                    //LOGGER.warning("Can't find anything like a decent hudson url. Punting, returning empty string."); 
                    return "";

                }
                //buf.insert(0, '/');
                buf.insert(0, hudsonRootUrl);
            }

            //LOGGER.info("Here's our relative path: " + buf.toString()); 
            return buf.toString(); 
        }

    }
    
    /* (non-Javadoc)
     * @see hudson.tasks.test.TestObject#getSafeName()
     */
    @Override
    public String getSafeName() {
        String safeName = testSetMap.getFileName() + "-" + tapTestResult.getTestNumber();
        try {
            safeName = URLEncoder.encode(safeName, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            LOGGER.warning(uee.getMessage());
        }
        return safeName;
    }
    
    /* (non-Javadoc)
     * @see hudson.tasks.test.TestResult#getTitle()
     */
    public String getTitle() {
        return getName();
    }
    
    public float getDuration() {
        Map<String, Object> diagnostic = this.tapTestResult.getDiagnostic();
        // FIXME: code duplication. Refactor it and TapResult
        if (diagnostic != null && ! diagnostic.isEmpty()) {
            Object duration = diagnostic.get(DURATION_KEY);
            if (duration != null) {
                Float durationMS = Float.parseFloat(duration.toString());
                return durationMS.floatValue() / 1000;
            }
        }
        return super.getDuration();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringWriter pw = new StringWriter();
        pw.append(tapTestResult.getStatus().toString());
        if (tapTestResult.getTestNumber() != null) {
            pw.append(' ' + Integer.toString(tapTestResult.getTestNumber()));
        }
        if (StringUtils.isNotBlank(tapTestResult.getDescription())) {
            pw.append(' ' + tapTestResult.getDescription());
        }
        if (tapTestResult.getDirective() != null) {
            pw.append(" # "
                    + tapTestResult.getDirective().getDirectiveValue().toString());
            if (StringUtils.isNotBlank(tapTestResult.getDirective().getReason())) {
                pw.append(' ' + tapTestResult.getDirective().getReason());
            }
        }
        List<Comment> comments = tapTestResult.getComments();
        if (comments.size() > 0) {
            for(Comment comment : comments) {
                if(comment.isInline()) {
                    pw.append(' ');
                    pw.append("# " + comment.getText());
                } else {
                    pw.append("\n");
                    pw.append("# " + comment.getText());
                }
            }
        }
        return pw.toString();
    }

}
