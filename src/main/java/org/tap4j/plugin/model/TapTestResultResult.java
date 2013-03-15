/*
 * The MIT License
 *
 * Copyright (c) <2012> <Bruno P. Kinoshita>
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
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.tap4j.model.Comment;
import org.tap4j.model.Directive;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.TapResult;
import org.tap4j.util.DirectiveValues;
import org.tap4j.util.StatusValues;

/**
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class TapTestResultResult extends TestResult {

	private static final String DURATION_KEY = "duration_ms";
	
	private static final long serialVersionUID = -4499261655602135921L;
	private final AbstractBuild<?, ?> owner;
	private final org.tap4j.model.TestResult tapTestResult;
	private final TestSetMap testSetMap;
	private final Boolean todoIsFailure;

	/**
	 * @deprecated
	 * @param owner
	 * @param testSetMap
	 * @param tapTestResult
	 */
	public TapTestResultResult(AbstractBuild<?, ?> owner, 
			TestSetMap testSetMap, 
			org.tap4j.model.TestResult tapTestResult) {
		this.owner = owner;
		this.testSetMap = testSetMap;
		this.tapTestResult = tapTestResult;
		this.todoIsFailure = Boolean.TRUE;
	}
	
	public TapTestResultResult(AbstractBuild<?, ?> owner, 
			TestSetMap testSetMap, 
			org.tap4j.model.TestResult tapTestResult, 
			Boolean todoIsFailure) {
		this.owner = owner;
		this.testSetMap = testSetMap;
		this.tapTestResult = tapTestResult;
		this.todoIsFailure = todoIsFailure;
	}
	
	/* (non-Javadoc)
	 * @see hudson.model.ModelObject#getDisplayName()
	 */
	public String getDisplayName() {
		String name = tapTestResult.getDescription();
		return name != null ? name : Integer.toString(tapTestResult.getTestNumber());
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
				parent = new TapStreamResult(owner, new TapResult("TAP Test Results", owner, list, todoIsFailure));
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
		String name = tapTestResult.getDescription();
		return testSetMap.getFileName() + "-" + name != null ? name : Integer.toString(tapTestResult.getTestNumber());
	}
	
	public String getStatus() {
		return this.tapTestResult.getStatus() == StatusValues.OK ? "OK" : "NOT OK";
	}
	
	public String getSkip() {
		String skip = "No";
		Directive directive = this.tapTestResult.getDirective();
		if(directive != null) {
			if(directive.getDirectiveValue() == DirectiveValues.SKIP) {
				skip = "Yes";
			}
		}
		return skip;
	}
	
	public String getTodo() {
		String todo = "No";
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
	
	/* (non-Javadoc)
	 * @see hudson.tasks.test.TestObject#getSafeName()
	 */
	@Override
	public String getSafeName() {
		return testSetMap.getFileName() + "-" + tapTestResult.getTestNumber();
	}
	
	/* (non-Javadoc)
	 * @see hudson.tasks.test.TestResult#getTitle()
	 */
	@Override
	public String getTitle() {
		return getName();
	}
	
	@Override
	public float getDuration() {
		Map<String, Object> diagnostic = this.tapTestResult.getDiagnostic();
		if (diagnostic != null && ! diagnostic.isEmpty()) {
			Object duration = diagnostic.get(DURATION_KEY);
			if (duration != null) {
				Float durationMS = Float.parseFloat(duration.toString());
				return durationMS.floatValue();
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
