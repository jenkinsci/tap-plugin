/*
 * The MIT License
 *
 * Copyright (c) 2010-2026 Bruno P. Kinoshita
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */
package org.tap4j.plugin;

import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;

public class TapPublisherPipelineTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void publishTapSymbolWorksWithValidTapFile() throws Exception {

        WorkflowJob job = j.createProject(WorkflowJob.class);

        job.setDefinition(new CpsFlowDefinition(
            "node {\n" +
            "  writeFile file: 'test.log', text: '1..1\\nok 1 - Sample test\\n'\n" +
            "  publishTap(testResults: 'test.log')\n" +
            "}",
            true
        ));

        WorkflowRun run = j.buildAndAssertSuccess(job);

        j.assertBuildStatus(Result.SUCCESS, run);
        j.assertLogContains("TAP Reports Processing: START", run);
        j.assertLogContains("TAP Reports Processing: FINISH", run);
    }
}

