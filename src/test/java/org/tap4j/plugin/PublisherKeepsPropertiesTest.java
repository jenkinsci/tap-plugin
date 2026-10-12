/*
 * The MIT License
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

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.TouchBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;


@WithJenkins
public class PublisherKeepsPropertiesTest {

    private JenkinsRule j;
    private FreeStyleProject project;

    @BeforeEach
    public void setUp(JenkinsRule j) throws Exception {
        this.j = j;
        project = j.createFreeStyleProject("tap");

        final String tap = "1..2\n" +
                "ok 1 sample First ok\n" +
                "not ok 2 sample Second failed\n";

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher arg1,
                    BuildListener arg2) throws InterruptedException, IOException {
                build.getWorkspace().child("sample.tap").write(tap,"UTF-8");
                return true;
            }
        });

        project.getBuildersList().add(new TouchBuilder());
    }

    @Test
    public void showOnlyFailures() throws Exception {
        project.getPublishersList().add(sampleTapPublisher(true));
        project.getPublishersList().add(sampleTapPublisher(true));
        FreeStyleBuild build = project.scheduleBuild2(0).get(1000, TimeUnit.SECONDS);
        TapTestResultAction testResultAction = build.getAction(TapTestResultAction.class);
        assertTrue(testResultAction.getTapResult().getShowOnlyFailures());
    }

    TapPublisher sampleTapPublisher(boolean showOnlyFailures) {
        return new TapPublisher(
                "sample.tap",
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
                showOnlyFailures,
                false,
                false,
                false,
                false
        );
    }
}
