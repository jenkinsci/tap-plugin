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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.tap4j.model.Plan;
import org.tap4j.model.TestResult;
import org.tap4j.model.TestSet;
import org.tap4j.parser.ParserException;
import org.tap4j.parser.Tap13Parser;
import org.tap4j.plugin.model.ParseErrorTestSetMap;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.util.DirectiveValues;
import org.tap4j.util.StatusValues;

import hudson.FilePath;
import hudson.model.Run;

/**
 * Executes remote TAP Stream retrieval and execution.
 *
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.1
 */
public class TapParser {

    /** Prints the logs to the web server's console / log files */
    private static final Logger log = Logger.getLogger(TapParser.class.getName());
    private final Boolean outputTapToConsole;
    private final Boolean enableSubtests;
    private final Boolean todoIsFailure;

    /** Build's logger to print logs as part of build's console output */
    private final PrintStream logger;
    private final Boolean includeCommentDiagnostics;
    private final Boolean validateNumberOfTests;
    private final Boolean planRequired;
    private final Boolean verbose;
    private final Boolean stripSingleParents;
    private final Boolean flattenTheTap;

    private boolean hasFailedTests;
    private boolean parserErrors;

    public TapParser(Boolean outputTapToConsole, Boolean enableSubtests, Boolean todoIsFailure,
            Boolean includeCommentDiagnostics, Boolean validateNumberOfTests, Boolean planRequired, Boolean verbose,
            Boolean stripSingleParents, Boolean flattenTheTap,
            PrintStream logger) {
        this.outputTapToConsole = outputTapToConsole;
        this.enableSubtests = enableSubtests;
        this.todoIsFailure = todoIsFailure;
        this.parserErrors = false;
        this.includeCommentDiagnostics = includeCommentDiagnostics;
        this.validateNumberOfTests = validateNumberOfTests;
        this.planRequired = planRequired;
        this.verbose = verbose;
        this.stripSingleParents = stripSingleParents;
        this.flattenTheTap = flattenTheTap;
        this.logger = logger;
    }

    public Boolean hasParserErrors() {
        return this.parserErrors;
    }

    public Boolean getOutputTapToConsole() {
        return outputTapToConsole;
    }

    public Boolean getTodoIsFailure() {
        return todoIsFailure;
    }

    public boolean getParserErrors() {
        return parserErrors;
    }

    public boolean getStripSingleParents() {
        return stripSingleParents;
    }

    public Boolean getIncludeCommentDiagnostics() {
        return includeCommentDiagnostics;
    }

    public Boolean getValidateNumberOfTests() {
        return validateNumberOfTests;
    }

    public Boolean getPlanRequired() {
        return planRequired;
    }

    public Boolean getEnableSubtests() {
        return enableSubtests;
    }

    public boolean hasFailedTests() {
        return this.hasFailedTests;
    }

    public Boolean getVerbose() {
        return verbose;
    }

    public boolean getFlattenTheTap() {
        return flattenTheTap;
    }

    private boolean containsNotOk(TestSet testSet) {
        for (TestResult testResult : testSet.getTestResults()) {
            if (testResult.getStatus().equals(StatusValues.NOT_OK) && !(testResult.getDirective() != null
                    && DirectiveValues.SKIP == testResult.getDirective().getDirectiveValue())) {
                return true;
            }
        }
        return false;
    }

    public TapResult parse(FilePath[] results, Run build) {
        this.parserErrors = Boolean.FALSE;
        this.hasFailedTests = Boolean.FALSE;
        final List<TestSetMap> testSets = new LinkedList<TestSetMap>();
        if (null == results) {
            log("File paths not specified. paths var is null. Returning empty test results.");
        } else {
            for (FilePath path : results) {
                File tapFile = new File(path.getRemote());
                if (!tapFile.isFile()) {
                    log("'" + tapFile.getAbsolutePath() + "' points to an invalid test report");
                    continue; // move to next file
                } else {
                    log("Processing '" + tapFile.getAbsolutePath() + "'");
                }
                try {
                    log("Parsing TAP test result [" + tapFile + "].");

                    final Tap13Parser parser = new Tap13Parser("UTF-8", enableSubtests, planRequired);
                    final TestSet testSet = flattenTheSetAsRequired(stripSingleParentsAsRequired(parser.parseFile(tapFile)));

                    if (containsNotOk(testSet) || testSet.containsBailOut()) {
                        this.hasFailedTests = Boolean.TRUE;
                    }

                    final TestSetMap map = new TestSetMap(tapFile.getAbsolutePath(), testSet);
                    testSets.add(map);

                    if (this.outputTapToConsole) {
                        try {
                            log(FileUtils.readFileToString(tapFile));
                        } catch (RuntimeException re) {
                            log(re);
                        } catch (IOException e) {
                            log(e);
                        }
                    }
                } catch (ParserException pe) {
                    testSets.add(new ParseErrorTestSetMap(tapFile.getAbsolutePath(), pe));
                    this.parserErrors = Boolean.TRUE;
                    log(pe);
                }
            }
        }
        // final TapResult testResult = new
        // TapResult(UUID.randomUUID().toString(), build, testSets);
        final TapResult testResult = new TapResult("TAP Test Results", build, testSets, this.todoIsFailure,
                this.includeCommentDiagnostics, this.validateNumberOfTests);
        return testResult;
    }

    private TestSet stripSingleParentsAsRequired(TestSet originalSet) {
        if (!stripSingleParents) {
            return originalSet;
        } else {
            TestSet result = originalSet;
            while (hasSingleParent(result)) {
                result = result.getTestResults().get(0).getSubtest();
            }
            return result;
        }
    }

    private TestSet flattenTheSetAsRequired(TestSet originalSet) {
        if (!flattenTheTap) {
            return originalSet;
        } else {
            TestSet result = new TestSet();
            final List<TestResult> resultsToProcess = originalSet.getTestResults();
            int testIndex = 1;
            while (!resultsToProcess.isEmpty()) {
                final TestResult actualTestResult = resultsToProcess.remove(0);
                TestSet subtests = actualTestResult.getSubtest();
                if (subtests == null || subtests.getNumberOfTestResults() == 0) {
                    actualTestResult.setTestNumber(testIndex++);
                    result.addTestResult(actualTestResult);
                } else {
                    final List<TestResult> subtestResults = subtests.getTestResults();
                    for (TestResult subtestResult : subtestResults) {
                        subtestResult.setDescription(actualTestResult.getDescription() + subtestResult.getDescription());
                        resultsToProcess.add(subtestResult);
                    }

                    final Plan subtestPlan = subtests.getPlan();
                    final boolean planIsPresent = subtestPlan != null;
                    final int subtestCountAsPlanned = planIsPresent ?
                            subtestPlan.getLastTestNumber() - subtestPlan.getInitialTestNumber() + 1
                            : -1;

                    final boolean subtestCountDiffersFromPlan = planIsPresent &&  subtestCountAsPlanned != subtestResults.size();

                    if (subtestCountDiffersFromPlan) {

                        final int missingTestCount =
                                subtestCountAsPlanned - subtestResults.size();

                        final TestResult timeoutTestResult = new TestResult();
                        timeoutTestResult.setStatus(StatusValues.NOT_OK);
                        timeoutTestResult.setDescription(
                                String.format("%s %s %d %s",
                                        actualTestResult.getDescription(), "failed:", missingTestCount, "subtest(s) missing"));

                        resultsToProcess.add(timeoutTestResult);
                    }
                }
            }
            return result;
        }
    }

    private boolean hasSingleParent(TestSet testSet) {

        if (testSet == null) {
            return false;
        }

        if (testSet.getNumberOfTestResults() != 1) {
            return false; // not a single test result
        }

        int planSpan = testSet.getPlan() != null ? (testSet.getPlan().getLastTestNumber() - testSet.getPlan().getInitialTestNumber()) : 0;

        if (planSpan == 0) { // exactly one test
            return testSet.getTestResults().get(0).getSubtest() != null; // which has a child(ern)
        } else {
            return false;
        }
    }

    private void log(String str) {
        if (verbose && logger != null) {
            logger.println(str);
        } else {
            log.fine(str);
        }
    }

    private void log(Exception ex) {
        if (logger != null) {
            ex.printStackTrace(logger);
        } else {
            log.severe(ex.toString());
        }
    }

}
