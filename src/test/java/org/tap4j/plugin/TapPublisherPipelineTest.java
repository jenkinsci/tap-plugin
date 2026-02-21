package org.tap4j.plugin;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;

public class TapPublisherPipelineTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void publishTapSymbolWorks() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class);

        job.setDefinition(new CpsFlowDefinition(
                "publishTap(testResults: 'test.log')",
                true));

        j.buildAndAssertSuccess(job);
    }
}