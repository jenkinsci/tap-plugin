/*
 * The MIT License
 *
 * Copyright (c) 2010-2016 Bruno P. Kinoshita
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang.BooleanUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tap4j.model.Plan;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.plugin.util.Constants;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.test.TestResultAggregator;
import jenkins.tasks.SimpleBuildStep;

/**
 * Publishes TAP results in Jenkins builds.
 *
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TapPublisher extends Recorder implements MatrixAggregatable, SimpleBuildStep {
    
    private final String testResults;
    private final Boolean failIfNoResults;
    private final Boolean failedTestsMarkBuildAsFailure;
    private final Boolean outputTapToConsole;
    private final Boolean enableSubtests;
    private final Boolean discardOldReports;
    private final Boolean todoIsFailure;
    private final Boolean includeCommentDiagnostics;
    private final Boolean validateNumberOfTests;
    private final Boolean planRequired;
    private final Boolean verbose;
    private final Boolean showOnlyFailures;
    private final Boolean stripSingleParents;
    private final Boolean flattenTapResult;

    @Deprecated
    public TapPublisher(String testResults,
            Boolean failIfNoResults,
            Boolean failedTestsMarkBuildAsFailure,
            Boolean outputTapToConsole,
            Boolean enableSubtests,
            Boolean discardOldReports,
            Boolean todoIsFailure,
            Boolean includeCommentDiagnostics,
            Boolean validateNumberOfTests,
            Boolean planRequired,
            Boolean verbose) {
        this(testResults, failIfNoResults, failedTestsMarkBuildAsFailure,
                outputTapToConsole, enableSubtests, discardOldReports, todoIsFailure,
                includeCommentDiagnostics, validateNumberOfTests, planRequired, verbose,
                Boolean.FALSE);
    }

    @Deprecated
    public TapPublisher(String testResults,
            Boolean failIfNoResults,
            Boolean failedTestsMarkBuildAsFailure,
            Boolean outputTapToConsole,
            Boolean enableSubtests,
            Boolean discardOldReports,
            Boolean todoIsFailure,
            Boolean includeCommentDiagnostics,
            Boolean validateNumberOfTests,
            Boolean planRequired,
            Boolean verbose,
            Boolean showOnlyFailures) {
        this(testResults, failIfNoResults, failedTestsMarkBuildAsFailure,
                outputTapToConsole, enableSubtests, discardOldReports, todoIsFailure,
                includeCommentDiagnostics, validateNumberOfTests, planRequired, verbose,
                Boolean.FALSE, Boolean.FALSE);
    }

    @Deprecated
    public TapPublisher(String testResults,
            Boolean failIfNoResults,
            Boolean failedTestsMarkBuildAsFailure,
            Boolean outputTapToConsole,
            Boolean enableSubtests,
            Boolean discardOldReports,
            Boolean todoIsFailure,
            Boolean includeCommentDiagnostics,
            Boolean validateNumberOfTests,
            Boolean planRequired,
            Boolean verbose,
            Boolean showOnlyFailures,
            Boolean stripSingleParents) {
        this(testResults, failIfNoResults, failedTestsMarkBuildAsFailure,
                outputTapToConsole, enableSubtests, discardOldReports, todoIsFailure,
                includeCommentDiagnostics, validateNumberOfTests, planRequired, verbose,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
    }

    @DataBoundConstructor
    public TapPublisher(String testResults,
            Boolean failIfNoResults,
            Boolean failedTestsMarkBuildAsFailure,
            Boolean outputTapToConsole,
            Boolean enableSubtests,
            Boolean discardOldReports,
            Boolean todoIsFailure,
            Boolean includeCommentDiagnostics,
            Boolean validateNumberOfTests,
            Boolean planRequired,
            Boolean verbose,
            Boolean showOnlyFailures,
            Boolean stripSingleParents,
            Boolean flattenTapResult) {

        this.testResults = testResults;
        this.failIfNoResults = BooleanUtils.toBooleanDefaultIfNull(failIfNoResults, false);
        this.failedTestsMarkBuildAsFailure = BooleanUtils.toBooleanDefaultIfNull(failedTestsMarkBuildAsFailure, false);
        this.outputTapToConsole = BooleanUtils.toBooleanDefaultIfNull(outputTapToConsole, true);
        this.enableSubtests = BooleanUtils.toBooleanDefaultIfNull(enableSubtests, true);
        this.discardOldReports = BooleanUtils.toBooleanDefaultIfNull(discardOldReports, false);
        this.todoIsFailure = BooleanUtils.toBooleanDefaultIfNull(todoIsFailure, true);
        this.includeCommentDiagnostics = BooleanUtils.toBooleanDefaultIfNull(includeCommentDiagnostics, true);
        this.validateNumberOfTests = BooleanUtils.toBooleanDefaultIfNull(validateNumberOfTests, false);
        this.planRequired = BooleanUtils.toBooleanDefaultIfNull(planRequired, true); // true is the old behaviour
        this.verbose = BooleanUtils.toBooleanDefaultIfNull(verbose, true);
        this.showOnlyFailures = BooleanUtils.toBooleanDefaultIfNull(showOnlyFailures, false);
        this.stripSingleParents = BooleanUtils.toBooleanDefaultIfNull(stripSingleParents, false);
        this.flattenTapResult = BooleanUtils.toBooleanDefaultIfNull(flattenTapResult, false);
    }

    public Object readResolve() {
        final String _testResults = this.getTestResults();
        final Boolean _failIfNoResults = BooleanUtils.toBooleanDefaultIfNull(this.getFailIfNoResults(), false);
        final Boolean _failedTestsMarkBuildAsFailure = BooleanUtils.toBooleanDefaultIfNull(this.getFailedTestsMarkBuildAsFailure(), false);
        final Boolean _outputTapToConsole = BooleanUtils.toBooleanDefaultIfNull(this.getOutputTapToConsole(), false);
        final Boolean _enableSubtests = BooleanUtils.toBooleanDefaultIfNull(this.getEnableSubtests(), true);
        final Boolean _discardOldReports = BooleanUtils.toBooleanDefaultIfNull(this.getDiscardOldReports(), false);
        final Boolean _todoIsFailure = BooleanUtils.toBooleanDefaultIfNull(this.getTodoIsFailure(), true);
        final Boolean _includeCommentDiagnostics = BooleanUtils.toBooleanDefaultIfNull(this.getIncludeCommentDiagnostics(), true);
        final Boolean _validateNumberOfTests = BooleanUtils.toBooleanDefaultIfNull(this.getValidateNumberOfTests(), false);
        final Boolean _planRequired = BooleanUtils.toBooleanDefaultIfNull(this.getPlanRequired(), true);
        final Boolean _verbose = BooleanUtils.toBooleanDefaultIfNull(this.getVerbose(), true);
        final Boolean _showOnlyFailures = BooleanUtils.toBooleanDefaultIfNull(this.getShowOnlyFailures(), false);
        final Boolean _stripSingleParents = BooleanUtils.toBooleanDefaultIfNull(this.getStripSingleParents(), false);
        final Boolean _flattenTapResult = BooleanUtils.toBooleanDefaultIfNull(this.getFlattenTapResult(), false);

        return new TapPublisher(
                _testResults,
                _failIfNoResults,
                _failedTestsMarkBuildAsFailure,
                _outputTapToConsole,
                _enableSubtests,
                _discardOldReports,
                _todoIsFailure,
                _includeCommentDiagnostics,
                _validateNumberOfTests,
                _planRequired,
                _verbose,
                _showOnlyFailures,
                _stripSingleParents,
                _flattenTapResult);
    }

    public Boolean getShowOnlyFailures() {
        return this.showOnlyFailures;
    }

    public Boolean getStripSingleParents() {
        return this.stripSingleParents;
    }

    /**
     * @return the failIfNoResults
     */
    public Boolean getFailIfNoResults() {
        return failIfNoResults;
    }

    /**
     * @return the testResults
     */
    public String getTestResults() {
        return testResults;
    }

    public Boolean getFailedTestsMarkBuildAsFailure() {
        return failedTestsMarkBuildAsFailure;
    }

    /**
     * @return the outputTapToConsole
     */
    public Boolean getOutputTapToConsole() {
        return outputTapToConsole;
    }

    /**
     * @return the enableSubtests
     */
    public Boolean getEnableSubtests() {
        return enableSubtests;
    }

    /**
     * @return the discardOldReports
     */
    public Boolean getDiscardOldReports() {
        return discardOldReports;
    }

    /**
     * @return the todoIsFailure
     */
    public Boolean getTodoIsFailure() {
        return todoIsFailure;
    }

    /**
     * @return the includeCommentDiagnostics
     */
    public Boolean getIncludeCommentDiagnostics() {
        return includeCommentDiagnostics;
    }

    public Boolean getValidateNumberOfTests() {
        return validateNumberOfTests;
    }

    public Boolean getPlanRequired() {
        return planRequired;
    }

    public Boolean getVerbose() {
        return verbose;
    }

    public Boolean getFlattenTapResult() {
        return flattenTapResult;
    }

    /**
     * Gets the directory where the plug-in saves its TAP streams before processing them and
     * displaying in the UI.
     * <p>
     * Adapted from JUnit Attachments Plug-in.
     *
     * @param build Jenkins build
     * @return virtual directory (FilePath)
     */
    public static FilePath getReportsDirectory(Run build) {
        return new FilePath(new File(build.getRootDir().getAbsolutePath())).child(Constants.TAP_DIR_NAME);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * hudson.tasks.BuildStepCompatibilityLayer#getProjectAction(hudson.model
     * .AbstractProject)
     */
    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new TapProjectAction(project);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild
     * , hudson.Launcher, hudson.model.BuildListener)
     */
    @Override
    public void perform(
            @Nonnull Run run,
            @Nonnull FilePath workspace,
            @Nonnull Launcher launcher,
            @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        performImpl(run, workspace, listener);
    }

    private boolean performImpl(Run build, FilePath workspace, TaskListener listener) throws IOException, InterruptedException {
        PrintStream logger = listener.getLogger();
        logger.println("TAP Reports Processing: START");

        EnvVars envVars = build.getEnvironment(listener);
        String antPattern = Util.replaceMacro(this.testResults, envVars);
        logger.println("Looking for TAP results report in workspace using pattern: " + antPattern);

        FilePath[] reports = locateReports(workspace, antPattern);

        /*
         * filter out the reports based on timestamps. See JENKINS-12187
         */
        if (this.getDiscardOldReports()) {
            reports = checkReports(build, reports, logger);
        }

        if (reports.length == 0) {
            if(this.getFailIfNoResults()) {
                logger.println("Did not find any matching files. Setting build result to FAILURE.");
                build.setResult(Result.FAILURE);
                return Boolean.FALSE;
            } else {
                logger.println("Did not find any matching files.");
                // build can still continue
                return Boolean.TRUE;
            }
        }

        boolean filesSaved = saveReports(workspace, TapPublisher.getReportsDirectory(build), reports, logger);
        if (!filesSaved) {
            logger.println("Failed to save TAP reports");
            return Boolean.TRUE;
        }

        TapResult testResult = null;
        try {
            testResult = loadResults(antPattern, build, logger);
            testResult.setShowOnlyFailures(this.getShowOnlyFailures());
            testResult.tally();
        } catch (Throwable t) {
            /*
             * don't fail build if TAP parser barfs. only print out the
             * exception to console.
             */
            t.printStackTrace(logger);
        }

        TapTestResultAction trAction = build.getAction(TapTestResultAction.class);
        boolean appending;

        if (trAction == null) {
            appending = false;
            trAction = new TapTestResultAction(build, testResult);
        } else {
            appending = true;
            trAction.mergeResult(testResult);
        }

        if (!appending) {
            build.addAction(trAction);
        }

        if (testResult.getTestSets().size() > 0 || testResult.getParseErrorTestSets().size() > 0) {
            // create an individual report for all of the results and add it to
            // the build

            TapBuildAction action = build.getAction(TapBuildAction.class);
            if (action == null) {
                action = new TapBuildAction(build, testResult);
                build.addAction(action);
            } else {
                appending = true;
                action.mergeResult(testResult);
            }

            if (testResult.hasParseErrors()) {
                listener.getLogger().println("TAP parse errors found in the build. Marking build as UNSTABLE");
                build.setResult(Result.UNSTABLE);
            }
            if (this.getValidateNumberOfTests()) {
                if (!this.validateNumberOfTests(testResult.getTestSets())) {
                    listener.getLogger().println("Not all test cases were executed according to the test set plan. Marking build as UNSTABLE");
                    build.setResult(Result.UNSTABLE);
                }
            }
            if (testResult.getFailed() > 0) {
                if(this.getFailedTestsMarkBuildAsFailure()) {
                    listener.getLogger().println("There are failed test cases and the job is configured to mark the build as failure. Marking build as FAILURE");
                    build.setResult(Result.FAILURE);
                } else {
                    listener.getLogger().println("There are failed test cases. Marking build as UNSTABLE");
                    build.setResult(Result.UNSTABLE);
                }
            }

            if (appending) {
                build.save();
            }

        } else {
            logger.println("Found matching files but did not find any TAP results.");
            return Boolean.TRUE;
        }
        logger.println("TAP Reports Processing: FINISH");
        return Boolean.TRUE;
    }

    /**
     * Iterates through the list of test sets and validates its plans and
     * test results.
     *
     * @param testSets
     * @return <true> if there are any test case that doesn't follow the plan
     */
    private boolean validateNumberOfTests(List<TestSetMap> testSets) {
        for (TestSetMap testSetMap : testSets) {
            TestSet testSet = testSetMap.getTestSet();
            Plan plan = testSet.getPlan();
            if (plan != null) {
                int planned = plan.getLastTestNumber();
                int numberOfTests = testSet.getTestResults().size();
                if (planned != numberOfTests)
                    return false;
            }
        }
        return true;
    }

    /**
     * @param owner
     * @param logger
     * @return
     */
    private TapResult loadResults(String antPattern, Run owner, PrintStream logger) {
        final FilePath tapDir = TapPublisher.getReportsDirectory(owner);
        FilePath[] results;
        TapResult tr;
        try {
            results = tapDir.list(antPattern);
            final TapParser parser = new TapParser(getOutputTapToConsole(), getEnableSubtests(), getTodoIsFailure(), getIncludeCommentDiagnostics(), getValidateNumberOfTests(), getPlanRequired(), getVerbose(), getStripSingleParents(), getFlattenTapResult(), logger);

            final TapResult result = parser.parse(results, owner);
            result.setOwner(owner);
            return result;
        } catch (Exception e) {
            e.printStackTrace(logger);

            tr = new TapResult("", owner, Collections.<TestSetMap>emptyList(), getTodoIsFailure(), getIncludeCommentDiagnostics(), getValidateNumberOfTests());
            tr.setOwner(owner);
            return tr;
        }
    }

    /**
     * @param workspace
     * @param tapDir
     * @param reports
     * @param logger
     * @return
     */
    private boolean saveReports(FilePath workspace, FilePath tapDir, FilePath[] reports,
            PrintStream logger) {
        logger.println("Saving reports...");
        try {
            tapDir.mkdirs();
            for (FilePath report : reports) {
                //FilePath dst = tapDir.child(report.getName());
                FilePath dst = getDistDir(workspace, tapDir, report);
                report.copyTo(dst);
            }
        } catch (Exception e) {
            e.printStackTrace(logger);
            return false;
        }
        return true;
    }

    /**
     * Used to maintain the directory structure when persisting to the tap-reports dir.
     *
     * @param workspace Jenkins WS
     * @param tapDir tap reports dir
     * @param orig original directory
     * @return persisted directory virtual structure
     */
    private FilePath getDistDir(FilePath workspace, FilePath tapDir, FilePath orig) {
        if(orig == null)
            return null;
        StringBuilder difference = new StringBuilder();
        FilePath parent = orig.getParent();
        do {
            if(parent.equals(workspace))
                break;
            difference.insert(0, parent.getName() + File.separatorChar);
        } while((parent = parent.getParent()) !=null);
        difference.append(orig.getName());
        return tapDir.child(difference.toString());
    }

    /**
     * Checks that there are new report files.
     *
     * @param build
     * @param reports
     * @param logger
     * @return
     */
    private FilePath[] checkReports(Run build,
            FilePath[] reports, PrintStream logger) {
        List<FilePath> filePathList = new ArrayList<FilePath>(reports.length);

        for (FilePath report : reports) {
            /*
             * Check that the file was created as part of this build and is not
             * something left over from before.
             *
             * Checks that the last modified time of file is greater than the
             * start time of the build
             */
            try {
                /*
                 * dividing by 1000 and comparing because we want to compare
                 * secs and not milliseconds
                 */
                if (build.getTimestamp().getTimeInMillis() / 1000 <= report.lastModified() / 1000) {
                    filePathList.add(report);
                } else {
                    logger.println(report.getName() + " was last modified before " + "this build started. Ignoring it.");
                }
            } catch (IOException e) {
                // just log the exception
                e.printStackTrace(logger);
            } catch (InterruptedException e) {
                // just log the exception
                e.printStackTrace(logger);
            }
        }
        return filePathList.toArray(new FilePath[] {});
    }

    /**
     * @param workspace
     * @param testResults
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private FilePath[] locateReports(FilePath workspace, String testResults) throws IOException, InterruptedException {
        return workspace.list(testResults);
    }

    /*
     * (non-Javadoc)
     *
     * @see hudson.tasks.BuildStep#getRequiredMonitorService()
     */
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    // matrix jobs and test result aggregation support

    /* (non-Javadoc)
     * @see hudson.matrix.MatrixAggregatable#createAggregator(hudson.matrix.MatrixBuild, hudson.Launcher, hudson.model.BuildListener)
     */
    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new TestResultAggregator(build, launcher, listener);
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
            super(TapPublisher.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Publish TAP Results";
        }

        /*
         * (non-Javadoc)
         *
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         */
        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return Boolean.TRUE;
        }

    }
}
