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
import org.acegisecurity.AccessDeniedException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tap4j.plugin.TapTestResultAction;

/**
 * TAP compatibility column for the dashboard view.
 *
 * @author Jakub Podlešák, Oracle Labs
 */
public class TapCompatibilityColumn extends ListViewColumn {

    private String toReplace;
    private String newText;

    @DataBoundConstructor
    public TapCompatibilityColumn(String toReplace, String newText) {
        super();
        this.toReplace = toReplace;
        this.newText = newText;
    }

    public String getToReplace() {
        return toReplace;
    }

    public String getNewText() {
        return newText;
    }



    private boolean configIsValid() {
        return isValidOption(toReplace) && isValidOption(newText);
    }

    private boolean isValidOption(String opt) {
        return opt != null && !opt.isEmpty();
    }

    public String getCounterpartJobUrl(Job job) {
        Job<?, ?> theOtherJob = getCounterpartJob(job);
        return theOtherJob == null ? null : theOtherJob.getAbsoluteUrl();
    }

    public String getTestCompatibilityStatus(Job<?, ?> job) {


        if (!configIsValid()) {
            return "N/A";
        }

        Job<?, ?> theOtherJob = getCounterpartJob(job);
        final TapTestResultAction theOtherLastTapResult = getLastTapResult(theOtherJob);

        final TapTestResultAction lastTapResult = getLastTapResult(job);

        if (lastTapResult == null || theOtherLastTapResult == null) {
            return "N/A";
        }

        boolean compatible = (lastTapResult.getTotalCount() == theOtherLastTapResult.getTotalCount()
                        && lastTapResult.getFailCount() == theOtherLastTapResult.getFailCount()
                        && lastTapResult.getSkipCount() == theOtherLastTapResult.getSkipCount());

        return compatible ? "COMPATIBLE" : "INCOMPATIBLE";
    }

    private Job<?, ?> getCounterpartJob(Job<?, ?> job) throws AccessDeniedException {
        return ViewHelper.getCounterpartJob(job, toReplace, newText);
    }

    private TapTestResultAction getLastTapResult(Job<?, ?> job) {
        Run<?, ?> b = job != null ? job.getLastCompletedBuild() : null;
        return b != null ? b.getAction(TapTestResultAction.class) : null;
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {

        public DescriptorImpl() {
            Items.XSTREAM2.addCompatibilityAlias("hudson.views.TapCompatibilityColumn", org.tap4j.plugin.view.TapCompatibilityColumn.class);
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }

        @Override
        public String getDisplayName() {
            return "TAP compatibility";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/tap/help/TapCompatibilityColumn/config.html";
        }
    }
}
