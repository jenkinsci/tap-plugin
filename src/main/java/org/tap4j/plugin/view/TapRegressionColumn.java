/*
 * The MIT License
 *
 * Copyright (c) 2019, Jenkins.
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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.tap4j.plugin.view;

import hudson.Extension;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.Run;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tap4j.plugin.TapTestResultAction;

/**
 * TAP regression column for the dashboard view.
 *
 * @author Jakub Podlešák, Oracle Labs
 */
public class TapRegressionColumn extends ListViewColumn {

    @DataBoundConstructor
    public TapRegressionColumn() {
        super();
    }

    public String getTestRegressionStatus(Job<?, ?> job) {

        final TapTestResultAction lastTapResult = getLastTapResult(job);

        if (lastTapResult == null) {
            return "N/A";
        }

        final TapTestResultAction secondLastTapResult = getSecondLastTapResult(job);

        if (secondLastTapResult == null) {
            return "N/A";
        }

        final int failedNow = lastTapResult.getFailCount();
        final int failedBefore = secondLastTapResult.getFailCount();
        final int totalNow = lastTapResult.getTotalCount();
        final int totalBefore = secondLastTapResult.getTotalCount();

        if (failedNow == failedBefore && totalNow == totalBefore) {
            return "STABLE";
        }

        if (failedNow <= failedBefore && totalNow >= totalBefore) {
            return "IMPROVED";
        }

        return "REGRESSED";
    }

    private TapTestResultAction getLastTapResult(Job<?, ?> job) {
        Run<?, ?> b = job.getLastCompletedBuild();
        return b != null ? b.getAction(TapTestResultAction.class) : null;
    }

    private TapTestResultAction getSecondLastTapResult(Job<?, ?> job) {
        Run<?, ?> lastBuild = job.getLastCompletedBuild();
        Run<?, ?> secondLastBuild = lastBuild != null ? lastBuild.getPreviousCompletedBuild() : null;
        return secondLastBuild != null ? secondLastBuild.getAction(TapTestResultAction.class) : null;
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {

        public DescriptorImpl() {
            Items.XSTREAM2.addCompatibilityAlias("hudson.views.TapRegressionColumn", org.tap4j.plugin.view.TapRegressionColumn.class);
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }

        @Override
        public String getDisplayName() {
            return "Regression status";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/tap/help/TapRegressionColumn/config.html";
        }
    }

}
