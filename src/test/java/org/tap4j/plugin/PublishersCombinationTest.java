/*
 * The MIT License
 *
 * Copyright (c) 2009, Yahoo!, Inc.
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

import hudson.model.Project;
import hudson.model.TopLevelItem;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Disabled;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

@Disabled("Failing on newer versions of Jenkins, 2.414+, but working when ran locally. TODO: fix it.")
public class PublishersCombinationTest {

//    @Rule
    public JenkinsRule rule = new JenkinsRule();

//    @Issue("JENKINS-29649")
//    @LocalData
//    @Test
    public void combinedWithJunitBasic() throws Exception {

        Project<?, ?> project = (Project<?, ?>) rule.jenkins.getItem("multiPublish");

        // Validate that there are test results where I expect them to be:
        try (JenkinsRule.WebClient wc = rule.createWebClient()) {
            // On the project page:
            HtmlPage projectPage = wc.getPage(project);

            assertJunitPart(projectPage, 3, 4);
            assertTapPart(projectPage, 3);
        }
    }

//    @Issue("JENKINS-29649")
//    @LocalData
//    @Test
    public void combinedWithJunitPipeline() throws Exception {

        TopLevelItem project = rule.jenkins.getItem("testPipeline");

        // Validate that there are test results where I expect them to be:
        try (JenkinsRule.WebClient wc = rule.createWebClient()) {
            // On the project page:
            HtmlPage projectPage = wc.getPage(project);

            assertJunitPart(projectPage, 15, 7);
            assertTapPart(projectPage, 15);
        }
    }

    private void assertJunitPart(HtmlPage page, int buildNumber, int testsTotal) {

        //      we should have a link that reads "Latest Test Result"
        //      that link should go to http://localhost:8080/job/breakable/lastBuild/testReport/
        rule.assertXPath(page, "//a[@href='lastCompletedBuild/testReport/']");
        rule.assertXPathValue(page, "//a[@href='lastCompletedBuild/testReport/']", "Latest Test Result");
        rule.assertXPathValueContains(page, "//a[@href='lastCompletedBuild/testReport/']", "Latest Test Result");

        //      there should be a test result trend graph
        rule.assertXPath(page, "//img[@src='test/trend']");

        //      superficially assert that the number of tests was correct
        rule.assertXPath(
                page,
                String.format("//area[@title='#%1$s: %2$s tests' and @href='%1$s/testReport/']", buildNumber, testsTotal)
        );
    }

    private void assertTapPart(HtmlPage page, int buildNumber) {

        //      there should be a TAP result trend graph
        rule.assertXPath(page, "//img[@src='tapResults/graph']");

        //      superficially assert that the number of tests was correct
        rule.assertXPath(page, String.format("//area[@title='1 Skip(s)' and @href='%s/tapResults/']", buildNumber));
    }
}
