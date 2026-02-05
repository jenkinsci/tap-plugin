/*
 * The MIT License
 *
 * Copyright (c) 2012-2023 Bruno P. Kinoshita
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

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Job;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import jenkins.util.NonLocalizable;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.export.Exported;
import org.tap4j.plugin.model.TapStreamResult;

import java.util.Collection;
import java.util.Collections;


/**
 * @since 0.1
 */
public class TapTestResultAction extends AbstractTestResultAction<TapTestResultAction>
        implements StaplerProxy, SimpleBuildStep.LastBuildAction, HealthReportingAction, RunAction2 {

    private TapResult tapResult;

    protected TapTestResultAction(Run<?, ?> r, TapResult tapResult) {
        setRunAndOwner(r);

        this.tapResult = tapResult;
    }

    /**
     * @return the current Run to support new JUnit
     */
    public Run<?, ?> getObject() {
        return run;
    }

    /**
     * @return the tapResult
     */
    public TapResult getTapResult() {
        return tapResult;
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getFailCount()
     */
    @Exported(visibility = 2)
    public int getFailCount() {
        return tapResult.getFailed();
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getTotalCount()
     */
    @Exported(visibility = 2)
    public int getTotalCount() {
        return tapResult.getTotal();
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getSkipCount()
     */
    @Exported(visibility = 2)
    public int getSkipCount() {
        return tapResult.getSkipped();
    }

    /*
     * (non-Javadoc)
     * @see org.kohsuke.stapler.StaplerProxy#getTarget()
     */
    public Object getTarget() {
        return getResult();
    }

    public TapStreamResult getResult() {
        return new TapStreamResult(owner, tapResult, this);
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getUrlName()
     */
    @Override
    @Exported(visibility = 2)
    public String getUrlName() {
        return "tapTestReport";
    }


    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return "TAP Test Results";
    }

    /*
     * (non-Javadoc)
     * @see jenkins.tasks.SimpleBuildStep#getProjectActions()
     */
    @Override
    public Collection<? extends Action> getProjectActions() {
        Job<?, ?> job = run.getParent();
        if (!Util.filter(job.getActions(), TapProjectAction.class).isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.singleton(new TapProjectAction(job));
    }

    @Override
    public HealthReport getBuildHealth() {
        final double scaleFactor = 1.0;
        final int totalCount = getTotalCount();
        final int failCount = getFailCount();
        int score = (totalCount == 0)
                ? 100
                : (int) (100.0 * Math.max(0.0, Math.min(1.0, 1.0 - (scaleFactor * failCount) / totalCount)));
        // TODO: we used to use the hudson.tasks.test.Messages localized entries
        //       here, https://github.com/jenkinsci/junit-plugin/blob/master/src/main/resources/hudson/tasks/test/Messages.properties,
        //       but now `mvn` fails in an injected test.
        String description, displayName = "Test Result";
        if (totalCount == 0) {
            description = String.format("%s: 0 tests in total", displayName);
        } else {
            description = String.format("%s: %d test(s) failing out of a total of %d test(s).",
                    displayName, failCount, totalCount);
        }
        return new HealthReport(score, new NonLocalizable(description));
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        setRunAndOwner(r);
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        setRunAndOwner(r);
    }

    private void setRunAndOwner(Run<?, ?> r) {
        this.run = r;
        this.owner = r instanceof AbstractBuild ? (AbstractBuild<?, ?>) r : null;
    }

    void mergeResult(TapResult additionalResult) {
        TapStreamResult original = getResult();
        original.merge(additionalResult);
        setFromTapStreamResult(original);
    }

    private void setFromTapStreamResult(TapStreamResult result) {
        this.tapResult = result.getTapResult();
    }
}