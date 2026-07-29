/*
 * The MIT License
 *
 * Copyright (c) 2023 Bruno P. Kinoshita
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
package org.tap4j.plugin.jenkins_cert_3190;

import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.tasks.Shell;
import org.htmlunit.CollectingAlertHandler;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.tap4j.plugin.TapPublisher;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Prevent a case where TAP files with JavaScript code are
 * evaluated by the plug-in.
 *
 * @since 2.4.1
 */
@Issue("3190")
@WithJenkins
public class TestXssTapFile {

    @Test
    public void testTapFileXss(JenkinsRule j) throws IOException, SAXException, ExecutionException, InterruptedException {
        final FreeStyleProject project = j.createFreeStyleProject();

        // We can add more scenarios where XSS must be prevented in
        // the TAP stream. Just modify the file below, trying to use
        // the next N in `alert(N)` so that it is easier to detect
        // where it is coming from.
        final Shell shell = new Shell("echo \"\n" +
                "1..4 # <script>alert(1)</script>\n" +
                "ok 1 - OK <script>alert(2)</script> # <script>alert(3)</script>\n" +
                "  ---\n" +
                "  <script>alert(4)</script>extensions:\n" +
                "      injected\n" +
                "  ...\n" +
                "# <script>alert(5)</script>\n" +
                "not ok 2 - failed! <script>alert(6)</script>\n" +
                "ok 3 - # SKIP <script>alert(7)</script>\n" +
                "ok 4 # TODO <script>alert(8)</script>\n" +
                "\" > payload.tap\n");
        project.getBuildersList().add(shell);

        final TapPublisher tapPublisher = new TapPublisher(
                "**/*.tap",
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                true
        );
        project.getPublishersList().add(tapPublisher);

        project.save();

        final CollectingAlertHandler alertHandler = new CollectingAlertHandler();
        try (final JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.setThrowExceptionOnFailingStatusCode(false);
            wc.setAlertHandler(alertHandler);

            Future<?> f = project.scheduleBuild2(0);
            Run<?, ?> build = (Run<?, ?>) f.get();

            wc.goTo("job/" + project.getName() + "/" + build.getNumber() + "/tapResults/");

            final List<String> alerts = alertHandler.getCollectedAlerts();

            assertEquals(0, alerts.size(), "You got a JS alert, look out for XSS!");
        }
    }
}
