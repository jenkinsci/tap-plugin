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
package org.tap4j.plugin.util;

import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.tasks.Shell;
import org.htmlunit.html.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.tap4j.plugin.TapPublisher;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Check that JS and links on extended page works
 * https://issues.jenkins.io/browse/JENKINS-73483
 * https://issues.jenkins.io/browse/JENKINS-73484
 * <p>
 * Debug from inde may not work, due to interactive.js not loaded. If hit, do mvn clean install from cmdline. If nto fixed, run
 * mvn   -Dtest=ExtendedJavascriptActionsTest#checkLinkToTestExists -Dmaven.surefire.debug test
 * and attach ide's remote dbeug to 5005
 *
 * @since 2.X.Y
 */
public class ExtendedJavascriptActionsTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private static TapPublisher getSimpleTapPublisher() {
        return new TapPublisher(
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
    }

    @Test
    /**
     * This tests chexks that there is no JS exception if tap fie contains no tests
     */
    public void checkNoTestDoNotFault() throws IOException, SAXException, ExecutionException, InterruptedException {
        final FreeStyleProject project = j.createFreeStyleProject();
        String tapFileName = "suite2.tap";
        final Shell shell = new Shell("echo \"\" > " + tapFileName + "\n");
        project.getBuildersList().add(shell);

        final TapPublisher tapPublisher = getSimpleTapPublisher();
        project.getPublishersList().add(tapPublisher);
        project.save();

        try (final JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.setThrowExceptionOnFailingStatusCode(false);
            Future<?> f = project.scheduleBuild2(0);
            Run<?, ?> build = (Run<?, ?>) f.get();
            HtmlPage page = wc.goTo("job/" + project.getName() + "/" + build.getNumber() + "/tapResults/");
            assertTrue("Ensuring that correct page was loaded", page.asXml().contains("interactive.js"));
            List tables = page.getByXPath("//table[@class='tap']");
            assertEquals("There should still be tap table", 1, tables.size());
            List centerCells = page.getByXPath("//td[@class='center']");
            assertEquals("There should no test loaded", 0, centerCells.size());
        }
    }


    @Test
    @Issue("73484")
    /**
     * This tests feature where each test id is pointable by UNIQUE anchor and the acnhor is provided by it
     */
    public void checkLinkToTestExists() throws IOException, SAXException, ExecutionException, InterruptedException {
        final FreeStyleProject project = j.createFreeStyleProject();
        String tapFileName = "suite1.tap";
        String[] testIds = {
                "Input file opened",
                " First line of the input valid.",
                "Read the rest of the file",
                "Summarized correctly ",//this trailing space is important, it propagates to id

        };
        final Shell shell = new Shell("echo \"1..4\n" +
                "ok 1 - " + testIds[0] + "\n" +
                "not ok 2 - " + testIds[1] + "\n" +
                "    More output from test 2. There can be\n" +
                "    arbitrary number of lines for any output\n" +
                "    so long as there is at least some kind\n" +
                "    of whitespace at beginning of line.\n" +
                "ok 3 - " + testIds[2] + "\n" +
                "#TAP meta information\n" +
                "not ok 4 - " + testIds[3] + "# TODO: not written yet" +
                "\" > " + tapFileName + "\n");
        project.getBuildersList().add(shell);
        final TapPublisher tapPublisher = getSimpleTapPublisher();
        project.getPublishersList().add(tapPublisher);
        project.save();

        try (final JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.setThrowExceptionOnFailingStatusCode(false);
            Future<?> f = project.scheduleBuild2(0);
            Run<?, ?> build = (Run<?, ?>) f.get();
            HtmlPage page = wc.goTo("job/" + project.getName() + "/" + build.getNumber() + "/tapResults/");
            List centerCells = page.getByXPath("//td[@class='center']");
            assertEquals("There should be four tests loaded", 4, centerCells.size());
            for (int x = 0; x < 4; x++) {
                HtmlTableDataCell cell = (HtmlTableDataCell) centerCells.get(x);
                DomNode anchorImpl = cell.getFirstChild();
                HtmlAnchor anchor = (HtmlAnchor) anchorImpl;
                String s = anchor.asXml();
                String selfHref = anchor.getAttribute("name");
                String href = anchor.getAttribute("href");
                String text = anchor.getVisibleText();
                assertEquals("the text should be order of result", "" + (x + 1), text);
                assertEquals("the id and self point to it shoudl be same ", href, "#" + selfHref);
                assertTrue("href must contain suite", href.contains(tapFileName));
                assertTrue("href must contain id", href.contains("_" + (x + 1) + "_"));
                assertTrue("href must contain test id", href.contains(testIds[x].replace(" ", "_")));
                assertEquals("href should match following convention", ("#" + tapFileName + "_" + (x + 1) + "_-_" + testIds[x].replace(" ", "_") + "!").replaceAll("_+", "_"), href);
            }
        }
    }

}
