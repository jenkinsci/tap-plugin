package org.tap4j.plugin.issue16964;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;
import org.tap4j.plugin.TapPublisher;
import org.tap4j.plugin.TapResult;
import org.tap4j.plugin.TapTestResultAction;


public class TestIssue16964 extends HudsonTestCase {

    public void testFailTestEmptyResultsAndOldReports() throws IOException, ServletException, InterruptedException, ExecutionException {
        FreeStyleProject project = this.hudson.createProject(FreeStyleProject.class, "tap-bug-16964");
        
        final String tap = "1..4\n" + 
                "ok 1 - Input file opened\n" + 
                "not ok 2 - First line of the input valid.\n" + 
                "More output from test 2. There can be\n" + 
                "arbitrary number of lines for any output\n" + 
                "so long as there is at least some kind\n" + 
                "of whitespace at beginning of line.\n" + 
                "ok 3 - Read the rest of the file\n" + 
                "#TAP meta information\n" + 
                "not ok 4 - Summarized correctly # TODO Not written yet\n" +
                "EOF";
        
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher arg1,
                    BuildListener arg2) throws InterruptedException, IOException {
                build.getWorkspace().child("result.tap").write(tap,"UTF-8");
                return true;
            }
        });
        
        TapPublisher publisher = new TapPublisher(
                "result.tap", 
                true, 
                true, 
                true, 
                true, 
                true, 
                true,
                true,
                true,
                false,
                true);
        project.getPublishersList().add(publisher);
        project.save();
        FreeStyleBuild build = (FreeStyleBuild) project.scheduleBuild2(0).get();
        
        TapTestResultAction action = build.getAction(TapTestResultAction.class);
        TapResult testResult = action.getTapResult();
        
        assertTrue(testResult.getFailed() == 2);
        assertTrue(testResult.getPassed() == 2);
    }
    
}
