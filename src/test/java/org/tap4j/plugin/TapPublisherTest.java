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
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RandomlyFails;
import org.jvnet.hudson.test.TouchBuilder;
import org.jvnet.hudson.test.recipes.LocalData;
import org.tap4j.plugin.model.TapStreamResult;
import org.tap4j.plugin.model.TapTestResultResult;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


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
                false
        );
        project.getPublishersList().add(archiver1);

        project.getBuildersList().add(new TouchBuilder());
    }

    @SuppressWarnings("deprecated")
    @LocalData
    @Test
    public void basic() throws Exception {
        FreeStyleBuild build = project.scheduleBuild2(0).get(1000, TimeUnit.SECONDS);

        assertTestResultsBasic(build);

        JenkinsRule.WebClient wc = j.new WebClient();

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
        wc.getPage(getNthResultPathFromFailedTestsTable(build, 0));
    }

    @SuppressWarnings("deprecated")
    @LocalData
    @Test
    public void merged() throws Exception {

        project.getPublishersList().add(archiver2);

        FreeStyleBuild build = project.scheduleBuild2(0).get(1000, TimeUnit.SECONDS);

        assertEquals(1, build.getActions(TapTestResultAction.class).size());
        assertEquals(1, build.getActions(TapBuildAction.class).size());

        assertTestResultsMerged(build);

        JenkinsRule.WebClient wc = j.new WebClient();


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
        wc.getPage(getNthResultPathFromFailedTestsTable(build, 0));
    }

    @SuppressWarnings("unchecked")
    private String getNthResultPathFromAllTestsTable(FreeStyleBuild build, int testNumber) {
        Iterator<TapTestResultResult> it = (Iterator<TapTestResultResult>) ((TapStreamResult) build
                .getActions(TapTestResultAction.class)
                .get(0)
                .getResult()
        ).getChildren().iterator();

        int count = 0;
        TapTestResultResult result = null;
        while (count++ <= testNumber) {
            result = it.next();
        }
        return result.getSafeName();
    }

    /**
     * This method unfortunately returns <i>absolute</i> URL of the
     * <code>testNumber</code>th link in "Failed Tests" table.
     *
     * @param build      build object.
     * @param testNumber row number (zero based).
     * @return absolute URL.
     */
    @SuppressWarnings("unchecked")
    private String getNthResultPathFromFailedTestsTable(FreeStyleBuild build, int testNumber) {

        TapStreamResult testObject = (TapStreamResult) build
                .getActions(TapTestResultAction.class)
                .get(0)
                .getResult();

        int count = 0;
        TestResult result = null;
        Iterator<TestResult> it = testObject.getFailedTests2().iterator();
        while (count++ <= testNumber) {
            result = it.next();
        }
        return result.getRelativePathFrom(testObject);
    }

    @RandomlyFails("TimeoutException from basic")
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
        assertTestResults(build, 3, 1, 1);
    }

    private void assertTestResultsMerged(FreeStyleBuild build) {
        assertTestResults(build, 5, 2, 1);
    }

    private void assertTestResults(FreeStyleBuild build, int total, int failed, int skipped) {
        TapTestResultAction testResultAction = build.getAction(TapTestResultAction.class);
        assertNotNull("no TestResultAction", testResultAction);

        TestResult result = testResultAction.getResult();
        assertNotNull("no TestResult", result);

        assertEquals(String.format("should have %d failing test", failed), failed, testResultAction.getFailCount());
        assertEquals(String.format("should have %d failing test", failed), failed, result.getFailCount());

        assertEquals(String.format("should have %d total tests", total), total, testResultAction.getTotalCount());
        assertEquals(String.format("should have %d total tests", total), total, result.getTotalCount());

        assertEquals(String.format("should have %d skipped test", skipped), skipped, testResultAction.getSkipCount());
        assertEquals(String.format("should have %d skipped test", skipped), skipped, result.getSkipCount());
    }

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
}