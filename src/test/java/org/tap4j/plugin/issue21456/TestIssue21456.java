/*
 * The MIT License
 *
 * Copyright (c) 2010-2023 Bruno P. Kinoshita
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
package org.tap4j.plugin.issue21456;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.tap4j.plugin.TapPublisher;
import org.tap4j.plugin.TapTestResultAction;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests that Jenkins can be configured to skip using the TAP Plug-in
 * when the build fails.
 */
@WithJenkins
public class TestIssue21456 {

    @Test
    public void testDurationMs(JenkinsRule jenkins) throws IOException, InterruptedException, ExecutionException {
        final FreeStyleProject project = jenkins.createFreeStyleProject("tap-bug-21456");

        final String tap = String.join("\n",
                "1..2",
                "ok 1 - Input file opened",
                "not ok 2 - First line of the input valid",
                "  ---",
                "    duration_ms: 100660.00",
                "  ...");

        project
                .getBuildersList()
                .add(new TestBuilder() {
                    @Override
                    public boolean perform(AbstractBuild<?, ?> build, Launcher arg1,
                                           BuildListener arg2) throws InterruptedException, IOException {
                        Objects.requireNonNull(build.getWorkspace()).child("result.tap").write(tap, "UTF-8");
                        return true;
                    }
                });

        project.getBuildersList().add(new Shell("exit 255"));

        final TapPublisher publisher = new TapPublisher(
                "result.tap", // testResults
                true, // failIfNoResults
                true, // failedTestsMarkBuildAsFailure
                true, // outputTapToConsole
                true, // enableSubtests
                true, // discardOldReports
                true, // todoIsFailure
                true, // includeCommentDiagnostics
                true, // validateNumberOfTests
                false, // planRequired
                true, // verbose
                false, // showOnlyFailures
                false, // stripSingleParents
                false, // flattenTapResult
                true, // removeYamlIfCorrupted
                true); //skipIfBuildNotOk <-- that's the one we are testing here
        project.getPublishersList().add(publisher);
        project.save();

        TapTestResultAction action = project
                .scheduleBuild2(0)
                .get()
                .getAction(TapTestResultAction.class);

        assertNull(
                action,
                "Not supposed to have a TAP action. Should have skipped a failed build!");
    }

}
