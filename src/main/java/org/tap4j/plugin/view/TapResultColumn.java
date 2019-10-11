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
 * TAP test result counters column for the dashboard view.
 *
 * @author Jakub Podlešák, Oracle Labs
 */
public class TapResultColumn extends ListViewColumn {

    @DataBoundConstructor
    public TapResultColumn() {
        super();
    }

    public TapTestResultAction getTestResult(Job<?, ?> job) {

        final TapTestResultAction lastTapResult = getLastTapResult(job);
        return lastTapResult;
    }

    private TapTestResultAction getLastTapResult(Job<?, ?> job) {
        Run<?, ?> b = job.getLastCompletedBuild();
        return b != null ? b.getAction(TapTestResultAction.class) : null;
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {

        public DescriptorImpl() {
            Items.XSTREAM2.addCompatibilityAlias("hudson.views.TapResultColumn", org.tap4j.plugin.view.TapResultColumn.class);
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }

        @Override
        public String getDisplayName() {
            return "TAP counters";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/tap/help/TapResultColumn/config.html";
        }
    }

}
