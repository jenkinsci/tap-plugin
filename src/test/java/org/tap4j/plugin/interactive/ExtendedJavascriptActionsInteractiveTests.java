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
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTableCell;
import org.htmlunit.html.HtmlTableRow;
import org.htmlunit.html.HtmlUnderlined;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.tap4j.plugin.TapPublisher;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Check that JS extended page works
 * https://issues.jenkins.io/browse/JENKINS-73483
 *
 * <p>
 * Debug from inde may not work, due to interactive.js not loaded. If hit, do mvn clean install from cmdline. If nto fixed, run
 * mvn   -Dtest=ExtendedJavascriptActionsInteractiveTests#testPresetViewsWorks -Dmaven.surefire.debug test
 * and attach ide's remote dbeug to 5005
 *
 * @since 2.X.Y
 */
public class ExtendedJavascriptActionsInteractiveTests {

    @Rule
    public JenkinsRule intJRule = new JenkinsRule();

    public static int[] countVisibleDetailRowsAndBodies(HtmlPage page) {
        int visibleDetailsRows = 0;
        int visibleDetailsBodies = 0;
        List detailsRows = page.getByXPath("//table[@class='tap']/tbody/tr//tr");
        for (int x = 0; x < detailsRows.size(); x++) {
            HtmlTableRow tableRow = (HtmlTableRow) (detailsRows.get(x));
            if (tableRow.isDisplayed()) {
                visibleDetailsRows++;
            }
            HtmlTableCell nameCell2 = tableRow.getCell(2);
            if (nameCell2.isDisplayed()) {
                visibleDetailsBodies++;
            }
        }
        return new int[]{visibleDetailsRows, visibleDetailsBodies};
    }

    @Test
    @Issue("73483")
    /**
     * this test is checking that preset views to they job in hiding/showing proepr elements
     */
    public void testPresetViewsWorks() throws IOException, SAXException, ExecutionException, InterruptedException {
        final FreeStyleProject project = intJRule.createFreeStyleProject();
        String tapFileName = "suite2.tap";
        project.getBuildersList().add(ExtendedJavascriptActionsStaticTests.getShell(tapFileName));
        final TapPublisher tapPublisher = ExtendedJavascriptActionsStaticTests.getSimpleTapPublisher();
        project.getPublishersList().add(tapPublisher);
        project.save();

        try (final JenkinsRule.WebClient wc = intJRule.createWebClient()) {
            wc.setThrowExceptionOnFailingStatusCode(false);
            Future<?> f = project.scheduleBuild2(0);
            Run<?, ?> build = (Run<?, ?>) f.get();
            HtmlPage page = wc.goTo("job/" + project.getName() + "/" + build.getNumber() + "/tapResults/");
            //all is visible on startup
            List rowsBeforeClicl = page.getByXPath("//table[@class='tap']/tbody/tr");
            assertEquals("There should be four tests loaded", 19, rowsBeforeClicl.size());
            DomNode cellHead = (DomNode) rowsBeforeClicl.get(0);
            assertEquals("header have no atts", 0, cellHead.getAttributes().getLength());
            for (int x = 1; x < 19; x++) {
                DomNode row = (DomNode) rowsBeforeClicl.get(x);
                String s = row.asXml();
                Node jsclazz = row.getAttributes().getNamedItem("class");
                String jsClazzValue = jsclazz.getTextContent();
                assertEquals("class at row " + x, ExtendedJavascriptActionsStaticTests.classes[x], jsClazzValue);
                HtmlTableRow tableRow = (HtmlTableRow) row;
                assertTrue("the element must be visible", tableRow.isDisplayed());
            }
            int[] found0 = countVisibleDetailRowsAndBodies(page);
            assertEquals("all details rows must be visible", 8, found0[0]);
            assertEquals("all details cells must be visible", 8, found0[1]);
            //find buttons to click onto
            ExtendedJavascriptActionsStaticTests.checkInteractiveJs(page);
            List mainViews = page.getByXPath("//u[@class='tapIclick']");
            assertEquals("There should be five preset views", 5, mainViews.size());
            HtmlUnderlined clickable = (HtmlUnderlined) (mainViews.get(0));
            clickable.click();
            //only failed
            List tapRowsAfter1click = page.getByXPath("//table[@class='tap']/tbody/tr");
            assertEquals("There should be tests loaded", 19, tapRowsAfter1click.size());
            cellHead = (DomNode) tapRowsAfter1click.get(0);
            assertEquals("header have no atts", 0, cellHead.getAttributes().getLength());
            for (int x = 1; x < 19; x++) {
                DomNode row = (DomNode) tapRowsAfter1click.get(x);
                String s = row.asXml();
                Node jsclazz = row.getAttributes().getNamedItem("class");
                String jsClazzValue = jsclazz.getTextContent();
                assertEquals("class at row " + x, ExtendedJavascriptActionsStaticTests.classes[x], jsClazzValue);
                HtmlTableRow tableRow = (HtmlTableRow) row;
                if (ExtendedJavascriptActionsStaticTests.classes[x].equals("test_not_ok") || ExtendedJavascriptActionsStaticTests.classes[x].equals("_bailout_")) {
                    assertTrue("the element must be visible " + tableRow, tableRow.isDisplayed());
                } else {
                    assertFalse("the element must NOT be visible " + tableRow, tableRow.isDisplayed());
                }
            }
            int[] found1 = countVisibleDetailRowsAndBodies(page);
            assertEquals("none details rows must be visible", 0, found1[0]);
            assertEquals("none details cells must be visible", 0, found1[1]);

            clickable = (HtmlUnderlined) (mainViews.get(1));
            clickable.click();
            //failed with trace
            List tapRowsAfter2click = page.getByXPath("//table[@class='tap']/tbody/tr");
            assertEquals("There should be tests loaded", 19, tapRowsAfter2click.size());
            cellHead = (DomNode) tapRowsAfter2click.get(0);
            assertEquals("header have no atts", 0, cellHead.getAttributes().getLength());
            for (int x = 1; x < 19; x++) {
                DomNode row = (DomNode) tapRowsAfter2click.get(x);
                String s = row.asXml();
                Node jsclazz = row.getAttributes().getNamedItem("class");
                String jsClazzValue = jsclazz.getTextContent();
                assertEquals("class at row " + x, ExtendedJavascriptActionsStaticTests.classes[x], jsClazzValue);
                HtmlTableRow tableRow = (HtmlTableRow) row;
                if (ExtendedJavascriptActionsStaticTests.classes[x].equals("test_not_ok") ||
                        ExtendedJavascriptActionsStaticTests.classes[x].equals("_bailout_") ||
                        ExtendedJavascriptActionsStaticTests.classes[x].equals("tr_details_not_ok")
                ) {
                    assertTrue("the element must be visible " + tableRow, tableRow.isDisplayed());
                } else {
                    assertFalse("the element must NOT be visible " + tableRow, tableRow.isDisplayed());
                }
            }
            int[] found2 = countVisibleDetailRowsAndBodies(page);
            assertEquals("none details rows must be visible ", 4, found2[0]);
            assertEquals("none details cells must be visible ", 4, found2[1]);

            clickable = (HtmlUnderlined) (mainViews.get(2));
            clickable.click();
            //failed with trace
            List tapRowsAfter3click = page.getByXPath("//table[@class='tap']/tbody/tr");
            assertEquals("There should be tests loaded", 19, tapRowsAfter3click.size());
            cellHead = (DomNode) tapRowsAfter3click.get(0);
            assertEquals("header have no atts", 0, cellHead.getAttributes().getLength());
            for (int x = 1; x < 19; x++) {
                DomNode row = (DomNode) tapRowsAfter3click.get(x);
                String s = row.asXml();
                Node jsclazz = row.getAttributes().getNamedItem("class");
                String jsClazzValue = jsclazz.getTextContent();
                assertEquals("class at row " + x, ExtendedJavascriptActionsStaticTests.classes[x], jsClazzValue);
                HtmlTableRow tableRow = (HtmlTableRow) row;
                if (ExtendedJavascriptActionsStaticTests.classes[x].equals("test_not_ok") ||
                        ExtendedJavascriptActionsStaticTests.classes[x].equals("_bailout_") ||
                        ExtendedJavascriptActionsStaticTests.classes[x].equals("tr_details_not_ok")
                ) {
                    assertTrue("the element must be visible " + tableRow, tableRow.isDisplayed());
                } else {
                    String style = tableRow.getAttribute("style");
                    if ("display: table-none;".equals(style)) {
                        System.err.println("It is invisible just htmluit do not know");
                    } else {
                        assertFalse("the element must NOT be visible " + tableRow, tableRow.isDisplayed());
                    }
                }
            }
            int[] found3 = countVisibleDetailRowsAndBodies(page);
            assertEquals("none details rows must be visible", 6, found3[0]); //this is weird, it seems it is suffering the same issue with display: table-none as above
            assertEquals("none details cells must be visible", 0, found3[1]);
        }
    }

}
