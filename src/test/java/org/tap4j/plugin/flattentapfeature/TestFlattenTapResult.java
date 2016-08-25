package org.tap4j.plugin.flattentapfeature;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import org.junit.Ignore;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;
import org.tap4j.model.TestResult;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.TapPublisher;
import org.tap4j.plugin.TapResult;
import org.tap4j.plugin.TapTestResultAction;

/**
 * Tests for flatten TAP result configuration option.
 *
 * @author Jakub Podlesak
 */
public class TestFlattenTapResult extends HudsonTestCase {

    public void testMixedLevels() throws IOException, InterruptedException, ExecutionException {

        final String tap = "1..2\n" +
                "ok 1 1\n" +
                "  1..3\n" +
                "  ok 1 1.1\n" +
                "  ok 2 1.2\n" +
                "  ok 3 1.3\n" +
                "ok 2 2\n";

        _test(tap, 4, null, false);
    }

    public void testStripFirstLevel() throws IOException, InterruptedException, ExecutionException {

        final String tap = "1..2\n" +
                "ok 1 1\n" +
                "  1..2\n" +
                "  ok 1 .1\n" +
                "  ok 2 .2\n" +
                "ok 2 2\n" +
                "  1..3\n" +
                "  ok 1 .1\n" +
                "  ok 2 .2\n" +
                "  ok 3 .3\n";

        _test(tap, 5, new String[] {
            "1.1", "1.2",
            "2.1", "2.2", "2.3"}, false);
    }

    public void testStripSecondLevel() throws IOException, InterruptedException, ExecutionException {

        final String tap =
                "1..1\n" +
                "ok 1 1\n" +
                "  1..2\n" +
                "  ok 1 .1\n" +
                "    1..4\n" +
                "    ok 1 .1\n" +
                "    ok 2 .2\n" +
                "    ok 3 .3\n" +
                "    ok 4 .4\n" +
                "  ok 2 .2\n" +
                "    1..3\n" +
                "    ok 1 .1\n" +
                "    ok 2 .2\n" +
                "    ok 3 .3\n";

        _test(tap, 7,
                new String[] {
                    "1.1.1", "1.1.2", "1.1.3", "1.1.4",
                    "1.2.1", "1.2.2", "1.2.3"}, false);
    }

    public void testStripSecondLevelIncompleteResult1() throws IOException, InterruptedException, ExecutionException {

        final String tap =
                "1..1\n" +
                "ok 1 1\n" +
                "  1..2\n" +
                "  ok 1 .1\n" +
                "    1..4\n" +
                "    ok 1 .1\n" +
                "    ok 2 .2\n" +
                "    ok 3 .3\n" +
                "  ok 2 .2\n" +
                "    1..3\n" +
                "    ok 1 .1\n" +
                "    ok 2 .2\n" +
                "    ok 3 .3\n";

        _test(tap, 7,
                new String[] {
                    "1.1.1", "1.1.2", "1.1.3", "1.1 failed: 1 subtest(s) missing",
                    "1.2.1", "1.2.2", "1.2.3"}, true);
    }

    public void testStripSecondLevelIncompleteResult2() throws IOException, InterruptedException, ExecutionException {
        final String tap2 =
                "1..1\n" +
                "ok 1 1\n" +
                "  1..2\n" +
                "  ok 1 .1\n" +
                "    1..4\n" +
                "    ok 1 .1\n" +
                "    ok 2 .2\n" +
                "    ok 3 .3\n" +
                "  ok 2 .2\n" +
                "    1..3\n" +
                "    ok 1 .1\n";

        _test(tap2, 6,
                new String[] {
                    "1.1.1", "1.1.2", "1.1.3", "1.1 failed: 1 subtest(s) missing",
                    "1.2.1", "1.2 failed: 2 subtest(s) missing"}, true);
    }

    public void testARealTapOuptut() throws Exception {
        final String tap = _is2String(TestFlattenTapResult.class.getResourceAsStream("/org/tap4j/plugin/tap-master-files/subtest-sample.tap"));
        _test(tap, 48, null, true);
    }

    private String _is2String(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    private void _test(final String tap, int expectedTotal, String[] expectedDescriptions, boolean printDescriptions) throws IOException, InterruptedException, ExecutionException {
        FreeStyleProject project = this.hudson.createProject(FreeStyleProject.class, "flatten-the-file");

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
                false, // stripSingleParents
                true); // flattenTapResult

        project.getPublishersList().add(publisher);
        project.save();
        FreeStyleBuild build = (FreeStyleBuild) project.scheduleBuild2(0).get();

        TapTestResultAction action = build.getAction(TapTestResultAction.class);
        TapResult testResult = action.getTapResult();

        assertEquals(expectedTotal, testResult.getTotal());

        final TestSet testSet = testResult.getTestSets().get(0).getTestSet();
        int testIndex = 0;
        for (TestResult result : testSet.getTestResults()) {

            final String description = result.getDescription();
            final int testNumber = result.getTestNumber();

            int expectedTestNumber = testIndex +1;

            if (printDescriptions) {
                System.out.printf("%d: %s\n", testNumber, description);
            }

            assertEquals(expectedTestNumber, testNumber);

            if (expectedDescriptions != null) {
                assertEquals(expectedDescriptions[testIndex], description);
            }


            testIndex ++;
        }
    }
}
