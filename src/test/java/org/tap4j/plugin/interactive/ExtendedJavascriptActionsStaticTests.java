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
package org.tap4j.plugin.interactive;

import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.tasks.Shell;
import org.htmlunit.html.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.tap4j.plugin.TapPublisher;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Check that JS and links on extended page contains correct symbols
 * https://issues.jenkins.io/browse/JENKINS-73483
 * https://issues.jenkins.io/browse/JENKINS-73484
 * <p>
 * Debug from inde may not work, due to interactive.js not loaded. If hit, do mvn clean install from cmdline. If nto fixed, run
 * mvn   -Dtest=ExtendedJavascriptActionsStaticTests#checkControlIdsExists -Dmaven.surefire.debug test
 * and attach ide's remote dbeug to 5005
 *
 * @since 2.X.Y
 */
public class ExtendedJavascriptActionsStaticTests {

    @Rule
    public JenkinsRule statJRule = new JenkinsRule();

    public static final String[] testIds = {
            "Input file opened",
            " First line of the input valid.",
            "Read the rest of the file",
            "Summarized correctly1 ", //this trailing space is important, it propagates to id
            "Summarized correctly2 ", //this trailing space is important, it propagates to id
            "Summarized correctly3 ", //this trailing space is important, it propagates to id
            "hi no space1",
            "hi no space2"

    };
    public static final String[] classes = {
            "", //header
            "_comment_",
            "_comment_",
            "_comment_",
            "test_ok",
            "tr_details_ok",
            "test_not_ok",
            "tr_details_not_ok",
            "test_not_ok_TODO",
            "test_not_ok_SKIP",
            "_comment_",
            "test_ok_TODO",
            "tr_details_ok_TODO",
            "test_ok_SKIP",
            "_bailout_",
            "_bailout_",
            "test_not_ok",
            "tr_details_not_ok",
            "test_ok",

    };


    public static TapPublisher getSimpleTapPublisher() {
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
                false,
                true,
                true
        );
    }

    public static Shell getShell(String tapFileName) {
        return new Shell("echo \"" +
                "# cmnt01\n" +
                "# cmnt02\n" +
                "# cmnt03\n" +
                "1..8\n" +
                "ok 1 - " + testIds[0] + "\n" +
                "  ---\n" +
                "    detailid1: detail1\n" +
                "    detailid2: detail2\n" +
                "  ...\n" +
                "not ok 2 - " + testIds[1] + "\n" +
                "  ---\n" +
                "    detailid1: detail3\n" +
                "    detailid2: detail4\n" +
                "  ...\n" +
                "not ok 3 - " + testIds[2] + "# TODO\n" +
                "not ok 4 - " + testIds[3] + "# skip\n" +
                "    detailid1: incorrect\n" +
                "    detailid2: incorrect\n" +
                "#in results cmnt\n" +
                "ok 5 - " + testIds[4] + "# TODO Not written yet\n" +
                "  ---\n" +
                "    detailid1: detail5\n" +
                "    detailid2: detail6\n" +
                "  ...\n" +
                "ok 6 - " + testIds[5] + "# skip written yet\n" +
                "Bail out!\n" +
                "Bail out! with reason\n" +
                "not ok 7 -" + testIds[6] + "\n" +
                "  ---\n" +
                "    detailid1: detail7\n" +
                "    detailid2: detail8\n" +
                "  ...\n" +
                "ok 8 -" + testIds[7] + "\n" +
                "    detailid1: incorrect\n" +
                "    detailid2: incorrect\n" +
                "\" > " + tapFileName + "\n");
    }

    public static void checkInteractiveJs(HtmlPage page) {
        List scripts = page.getByXPath("//script");
        assertTrue("At least one script must be there", scripts.size() > 0);
        for (Object futureScript : scripts) {
            HtmlScript scrip = (HtmlScript) futureScript;
            if (scrip.getSrcAttribute().endsWith("/plugin/tap/interactive.js")) {
                return;
            }
        }
        throw new AssertionError("No ineractive.js found");
    }

    @Test
    /**
     * This tests checks that there is no JS exception if tap fie contains no tests
     */
    public void checkNoTestDoNotFault() throws IOException, SAXException, ExecutionException, InterruptedException {
        final FreeStyleProject project = statJRule.createFreeStyleProject();
        String tapFileName = "suite1.tap";
        final Shell shell = new Shell("echo \"\" > " + tapFileName + "\n");
        project.getBuildersList().add(shell);

        final TapPublisher tapPublisher = getSimpleTapPublisher();
        project.getPublishersList().add(tapPublisher);
        project.save();

        try (final JenkinsRule.WebClient wc = statJRule.createWebClient()) {
            wc.setThrowExceptionOnFailingStatusCode(false);
            Future<?> f = project.scheduleBuild2(0);
            Run<?, ?> build = (Run<?, ?>) f.get();
            HtmlPage page = wc.goTo("job/" + project.getName() + "/" + build.getNumber() + "/tapResults/");
            checkInteractiveJs(page);
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
        final FreeStyleProject project = statJRule.createFreeStyleProject();
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

        try (final JenkinsRule.WebClient wc = statJRule.createWebClient()) {
            wc.setThrowExceptionOnFailingStatusCode(false);
            Future<?> f = project.scheduleBuild2(0);
            Run<?, ?> build = (Run<?, ?>) f.get();
            HtmlPage page = wc.goTo("job/" + project.getName() + "/" + build.getNumber() + "/tapResults/");
            checkInteractiveJs(page);
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

    @Test
    @Issue("73483")
    /**
     * this test checks that all is ivisble at start and that all classes needed forshow/hide actions are there
     */
    public void checkControlIdsExists() throws IOException, SAXException, ExecutionException, InterruptedException {
        final FreeStyleProject project = statJRule.createFreeStyleProject();
        String tapFileName = "suite2.tap";
        project.getBuildersList().add(getShell(tapFileName));
        final TapPublisher tapPublisher = getSimpleTapPublisher();
        project.getPublishersList().add(tapPublisher);
        project.save();

        try (final JenkinsRule.WebClient wc = statJRule.createWebClient()) {
            wc.setThrowExceptionOnFailingStatusCode(false);
            Future<?> f = project.scheduleBuild2(0);
            Run<?, ?> build = (Run<?, ?>) f.get();
            HtmlPage page = wc.goTo("job/" + project.getName() + "/" + build.getNumber() + "/tapResults/");
            String s1 = page.asXml();
            checkInteractiveJs(page);
            List tapMainRows = page.getByXPath("//table[@class='tap']/tbody/tr"); //there are also nested rows
            //19=1header+3coments on top+1comment in middle+8tests 2bailedout lines + 4 comment rows (each detail is subrow in subtable)
            int totalRows = 1 + 3 + 1 + 8 + 2 + 4;
            assertEquals("There should be four tests loaded", totalRows, tapMainRows.size());
            DomNode cellHead = (DomNode) tapMainRows.get(0);
            assertEquals("header have no atts", 0, cellHead.getAttributes().getLength());
            for (int x = 1; x < totalRows; x++) {
                DomNode row = (DomNode) tapMainRows.get(x);
                String s2 = row.asXml();
                Node jsclazz = row.getAttributes().getNamedItem("class");
                String jsClazzValue = jsclazz.getTextContent();
                assertEquals("class at row " + x, classes[x], jsClazzValue);
                HtmlTableRow tableRow = (HtmlTableRow) row;
                assertTrue("the element must be visible", tableRow.isDisplayed());
            }
            List detailsRows = page.getByXPath("//table[@class='tap']/tbody/tr//tr");
            //4 rows have valid detail. Each have 2 details. Each
            assertEquals("There should be four tests loaded", 8, detailsRows.size());
            for (int x = 0; x < detailsRows.size(); x++) {
                int idCounter = (x % 2) + 1;
                HtmlTableRow tableRow = (HtmlTableRow) (detailsRows.get(x));
                HtmlTableCell nameCell1 = tableRow.getCell(1);
                assertEquals("detailid" + idCounter + " +/-", nameCell1.getTextContent());
                HtmlTableCell nameCell2 = tableRow.getCell(2);
                String nameCelid2 = nameCell2.getAttribute("id");
                String nameCellClazz2 = nameCell2.getAttribute("class");
                assertEquals("detail_body", nameCellClazz2);
                assertEquals("detail" + (x + 1), nameCell2.getTextContent());
            }
        }
    }

    @Test
    @Issue("73483")
    /**
     * this test is chekcing that all ids in generated file are indeed unique
     */
    public void testUniqueIds() throws IOException, SAXException, ExecutionException, InterruptedException {
        final FreeStyleProject project = statJRule.createFreeStyleProject();
        String tapFileName = "suite2.tap";
        project.getBuildersList().add(getShell(tapFileName));
        final TapPublisher tapPublisher = getSimpleTapPublisher();
        project.getPublishersList().add(tapPublisher);
        project.save();
        List<String> allIds = new ArrayList<>();
        try (final JenkinsRule.WebClient wc = statJRule.createWebClient()) {
            wc.setThrowExceptionOnFailingStatusCode(false);
            Future<?> f = project.scheduleBuild2(0);
            Run<?, ?> build = (Run<?, ?>) f.get();
            HtmlPage page = wc.goTo("job/" + project.getName() + "/" + build.getNumber() + "/tapResults/");
            String s1 = page.asXml();
            checkInteractiveJs(page);
            List tapMainRows = page.getByXPath("//table[@class='tap']/tbody/tr"); //there are also nested rows
            int totalRows = 19;
            assertEquals("There should be four tests loaded", totalRows, tapMainRows.size());
            DomNode cellHead = (DomNode) tapMainRows.get(0);
            assertEquals("header have no atts", 0, cellHead.getAttributes().getLength());
            for (int x = 1; x < totalRows; x++) {
                DomNode row = (DomNode) tapMainRows.get(x);
                Node jsId = row.getAttributes().getNamedItem("id");
                if (jsId != null) {
                    String jsIdValue = jsId.getTextContent();
                    allIds.add(jsIdValue);
                }
            }
            List detailsRows = page.getByXPath("//table[@class='tap']/tbody/tr//tr");
            //4 rows have valid detail. Each have 2 details. Each
            assertEquals("There should be four tests loaded", 8, detailsRows.size());
            for (int x = 0; x < detailsRows.size(); x++) {
                HtmlTableRow tableRow = (HtmlTableRow) (detailsRows.get(x));
                String rowId = tableRow.getId();
                allIds.add(rowId);
                HtmlTableCell nameCell2 = tableRow.getCell(2);
                String nameCelid2 = nameCell2.getAttribute("id");
                allIds.add(nameCelid2);
            }
            allIds = allIds.stream().filter(a -> {
                return !a.isEmpty();
            }).collect(Collectors.toList());
            Set uniqueIds = new HashSet(allIds);
            assertEquals("all ids must be unique", allIds.size(), uniqueIds.size());
            List allPlusMinus = page.getByXPath("//u[@class='jsPM']");
            List allNamedHrefs = page.getByXPath("//td/a[@name]");
            List<String> unusedIds = new ArrayList<>(allIds);
            List pmUnmatched = new ArrayList(allPlusMinus);
            List pmMatched = new ArrayList();
            List namedHrefsUnmatched = new ArrayList(allNamedHrefs);
            List namedHrefsMatched = new ArrayList();
            for (String id : allIds) {
                for (Object pm : allPlusMinus) {
                    HtmlElement ujp = (HtmlElement) pm;
                    String clickM = ujp.getOnClickAttribute();
                    String parameter = clickM.replaceAll(".*\\(\"", "").replaceAll("\"\\).*", "");
                    if (parameter.equals(id)) {
                        pmUnmatched.remove(pm);
                        pmMatched.add(pm);
                        unusedIds.remove(id);
                    }
                }
                for (Object pm : allNamedHrefs) {
                    HtmlElement ujp = (HtmlElement) pm;
                    StringBuilder name = new StringBuilder(ujp.getAttribute("name"));
                    String nwname = new StringBuilder(
                            name.reverse().toString().replaceFirst("!", "")
                    ).reverse().toString();
                    if (nwname.equals(id)) {
                        namedHrefsUnmatched.remove(pm);
                        namedHrefsMatched.add(pm);
                        unusedIds.remove(id);
                    }
                }
            }
            assertEquals("all ids were used", 0, unusedIds.size());
            assertEquals("all hrefs were used", 0, namedHrefsUnmatched.size());
            /*there are four rows without details, so thiers +/- shows nothing*/
            assertEquals("all +/- with rows were used", 4, pmUnmatched.size());
        }
    }
}
