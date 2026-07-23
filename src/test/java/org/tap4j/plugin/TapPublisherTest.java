/*
 * The MIT License
 *
 * Copyright (c) 2009, Yahoo!, Inc.
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

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import hudson.tasks.test.TestResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TouchBuilder;
import org.jvnet.hudson.test.recipes.LocalData;
import org.tap4j.plugin.model.TapStreamResult;
import org.tap4j.plugin.model.TapTestResultResult;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;


/**
 * Tests for the {@link TapPublisher}.
 */
public class TapPublisherTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    private FreeStyleProject project;
    private TapPublisher archiver1;
    private TapPublisher archiver2;

    @Before
    public void setUp() throws Exception {
        project = j.createFreeStyleProject("tap");
        archiver1 = new TapPublisher(
                "**/sample.tap",
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false
        );
        archiver2 = new TapPublisher(
                "**/more.tap",
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false
        );
        project.getPublishersList().add(archiver1);

        project.getBuildersList().add(new TouchBuilder());
    }

    /**
     * Verifies that TAP results are published and the TAP report pages are accessible.
     */
    @LocalData
    @Test
    // getPage uses deprecation to tell users about a possibility to use relative pages (shrugs)
    @SuppressWarnings("deprecation")
    public void basic() throws Exception {
        FreeStyleBuild build = project.scheduleBuild2(0).get(1000, TimeUnit.SECONDS);

        assertTestResultsBasic(build);

        try (JenkinsRule.WebClient wc = j.new WebClient()) {
            // Check that we can access project page.
            wc.getPage(project);

            // Check that we can access current build page.
            wc.getPage(build);

            // Check that we can access TAP report page.
            wc.getPage(build, "tapTestReport");

            // Check that we can access green and red test links from "All Tests" table.
            wc.getPage(build, "tapTestReport/" + getNthResultPathFromAllTestsTable(build, 0)); // green
            wc.getPage(build, "tapTestReport/" + getNthResultPathFromAllTestsTable(build, 1)); // red

            // Check that we can access link from "Failed Tests" table.
            wc.getPage(getNthResultPathFromFailedTestsTable(build));
        }
    }

    /**
     * Verifies that multiple TAP publishers are merged into a single test result.
     */
    @LocalData
    @Test
    // getPage uses deprecation to tell users about a possibility to use relative pages (shrugs)
    @SuppressWarnings("deprecation")
    public void merged() throws Exception {

        project.getPublishersList().add(archiver2);

        FreeStyleBuild build = project.scheduleBuild2(0).get(1000, TimeUnit.SECONDS);

        assertEquals(1, build.getActions(TapTestResultAction.class).size());
        assertEquals(1, build.getActions(TapBuildAction.class).size());

        assertTestResultsMerged(build);

        try (JenkinsRule.WebClient wc = j.new WebClient()) {
            // Check that we can access project page.
            wc.getPage(project);

            // Check that we can access current build page.
            wc.getPage(build);

            // Check that we can access TAP report page.
            wc.getPage(build, "tapTestReport");

            // Check that we can access green and red test links from "All Tests" table.
            wc.getPage(build, "tapTestReport/" + getNthResultPathFromAllTestsTable(build, 0)); // green
            wc.getPage(build, "tapTestReport/" + getNthResultPathFromAllTestsTable(build, 1)); // red

            // Check that we can access link from "Failed Tests" table.
            wc.getPage(getNthResultPathFromFailedTestsTable(build));
        }
    }

    @SuppressWarnings("unchecked")
    private String getNthResultPathFromAllTestsTable(FreeStyleBuild build, int testNumber) {
        Iterator<TapTestResultResult> it = (Iterator<TapTestResultResult>) build
                .getActions(TapTestResultAction.class)
                .get(0)
                .getResult().getChildren().iterator();

        int count = 0;
        TapTestResultResult result = null;
        while (count++ <= testNumber) {
            result = it.next();
        }
        assert result != null;
        return result.getSafeName();
    }

    /**
     * This method unfortunately returns <i>absolute</i> URL of the
     * <code>testNumber</code>th link in "Failed Tests" table.
     *
     * @param build build object.
     * @return absolute URL.
     */
    private String getNthResultPathFromFailedTestsTable(FreeStyleBuild build) {

        TapStreamResult testObject = build
                .getActions(TapTestResultAction.class)
                .get(0)
                .getResult();

        int count = 0;
        TestResult result = null;
        Iterator<TestResult> it = testObject.getFailedTests2().iterator();
        while (count++ <= 0) {
            result = it.next();
        }
        assert result != null;
        return result.getRelativePathFrom(testObject);
    }

    /**
     * Verifies that TAP results can be published when the build runs on an agent.
     */
    @LocalData
    @Test
    public void slave() throws Exception {
        DumbSlave s = j.createOnlineSlave();
        project.setAssignedLabel(s.getSelfLabel());

        FilePath src = new FilePath(j.jenkins.getRootPath(), "jobs/tap/workspace/");
        assertNotNull(src);
        FilePath dest = s.getWorkspaceFor(project);
        assertNotNull(dest);
        src.copyRecursiveTo("*.tap", dest);

        basic();
    }

    private void assertTestResultsBasic(FreeStyleBuild build) {
        assertTestResults(build, 3, 1);
    }

    private void assertTestResultsMerged(FreeStyleBuild build) {
        assertTestResults(build, 5, 2);
    }

    private void assertTestResults(FreeStyleBuild build, int total, int failed) {
        TapTestResultAction testResultAction = build.getAction(TapTestResultAction.class);
        assertNotNull("no TestResultAction", testResultAction);

        TapStreamResult streamResult = testResultAction.getResult();
        assertNotNull("no TestResult", streamResult);

        assertEquals(String.format("should have %d failing test", failed), failed, testResultAction.getFailCount());
        assertEquals(String.format("should have %d failing test", failed), failed, streamResult.getFailCount());

        assertEquals(String.format("should have %d total tests", total), total, testResultAction.getTotalCount());
        assertEquals(String.format("should have %d total tests", total), total, streamResult.getTotalCount());

        assertEquals(String.format("should have %d skipped test", 1), 1, testResultAction.getSkipCount());
        assertEquals(String.format("should have %d skipped test", 1), 1, streamResult.getSkipCount());

        assertSame("parent action should be the owning TapTestResultAction", testResultAction, streamResult.getParentAction());
    }

    /**
     * Verifies that TAP results remain available after Jenkins is reloaded.
     */
    @LocalData
    @Test
    public void persistence() throws Exception {
        project.scheduleBuild2(0).get(60, TimeUnit.SECONDS);

        reloadJenkins();

        FreeStyleBuild build = project.getBuildByNumber(1);

        assertTestResultsBasic(build);
    }

    private void reloadJenkins() throws Exception {
        j.jenkins.reload();
        project = (FreeStyleProject) j.jenkins.getItem("tap");
    }


    @Test
    public void emptyDirectory() throws Exception {
        FreeStyleProject freeStyleProject = j.createFreeStyleProject();
        freeStyleProject.getPublishersList().add(archiver1);
        j.assertBuildStatus(Result.FAILURE, freeStyleProject.scheduleBuild2(0).get());
    }

    /**
     * Verifies that the TAP report page renders after Jenkins reloads the build.
     *
     * <p>This covers the regression where the TAP test result page failed to render
     * after changes in the Jenkins JUnit plugin.</p>
     */
    @LocalData
    @Issue("JENKINS-76360")
    @Test
    public void testReportPageRendersAfterReload() throws Exception {
        project.scheduleBuild2(0).get(1000, TimeUnit.SECONDS);

        reloadJenkins();

        FreeStyleBuild build = project.getBuildByNumber(1);

        try (JenkinsRule.WebClient wc = j.new WebClient()) {
            wc.getPage(build, "tapTestReport");
        }
    }

    /**
     * Verifies that the TAP result keeps its parent action after Jenkins reloads.
     *
     * <p>The parent action is required by Jenkins test result rendering.</p>
     */
    @LocalData
    @Issue("JENKINS-76360")
    @Test
    public void persistenceKeepsParentAction() throws Exception {
        project.scheduleBuild2(0).get(60, TimeUnit.SECONDS);

        reloadJenkins();

        FreeStyleBuild build = project.getBuildByNumber(1);
        TapTestResultAction action = build.getAction(TapTestResultAction.class);

        assertNotNull(action);
        assertSame(action, action.getResult().getParentAction());
    }
}
