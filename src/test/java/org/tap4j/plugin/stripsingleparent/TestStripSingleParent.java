package org.tap4j.plugin.stripsingleparent;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;
import org.tap4j.plugin.TapPublisher;
import org.tap4j.plugin.TapResult;
import org.tap4j.plugin.TapTestResultAction;

/**
 * At least basic tests for strip single parent configuration option.
 *
 * @author Jakub Podlesak
 */
public class TestStripSingleParent extends HudsonTestCase {

    public void testNoEffect() throws IOException, InterruptedException, ExecutionException {

        final String tap = "1..2\n" +
                "ok 1 - 1\n" +
                "  1..3\n" +
                "  ok 1 1.1\n" +
                "  ok 2 1.2\n" +
                "  ok 3 1.3\n" +
                "ok 2 - 1\n";

        _test(tap, 2);
    }

    public void testStripFirstLevel() throws IOException, InterruptedException, ExecutionException {

        final String tap = "1..1\n" +
                "ok 1 - 1\n" +
                "  1..3\n" +
                "  ok 1 1.1\n" +
                "  ok 2 1.2\n" +
                "  ok 3 1.3\n";

        _test(tap, 3);
    }

    public void testStripSecondLevel() throws IOException, InterruptedException, ExecutionException {

        final String tap =
                "1..1\n" +
                "ok 1 - 1\n" +
                "  1..1\n" +
                "  ok 1.1 - 1\n" +
                "    1..3\n" +
                "    ok 1 1.1.1\n" +
                "    ok 2 1.1.2\n" +
                "    ok 3 1.1.3\n";

        _test(tap, 3);
    }

    private void _test(final String tap, int expectedTotal) throws IOException, InterruptedException, ExecutionException {
        FreeStyleProject project = this.hudson.createProject(FreeStyleProject.class, "strip-single-parents");

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher arg1,
                    BuildListener arg2) throws InterruptedException, IOException {
                build.getWorkspace().child("result.tap").write(tap,"UTF-8");
                return true;
            }
        });

        TapPublisher publisher = new TapPublisher(
                "result.tap", // test results
                true,  // failIfNoResults
                true,  // failedTestsMarkBuildAsFailure
                false, // outputTapToConsole
                true,  // enableSubtests
                true,  // discardOldReports
                true,  // todoIsFailure
                true,  // includeCommentDiagnostics
                true,  // validateNumberOfTests
                true,  // planRequired
                false, // verbose
                true,  // showOnlyFailures
                true,  // stripSingleParents
                false); // flattenTapResult

        project.getPublishersList().add(publisher);
        project.save();
        FreeStyleBuild build = (FreeStyleBuild) project.scheduleBuild2(0).get();

        TapTestResultAction action = build.getAction(TapTestResultAction.class);
        TapResult testResult = action.getTapResult();

        assertEquals(expectedTotal, testResult.getPassed());
    }
}
