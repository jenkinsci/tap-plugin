package org.tap4j.plugin.util;


import hudson.model.TopLevelItem;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;


@WithJenkins
public class GraphHelperTest {

    @Issue("JENKINS-37623")
    @LocalData
    @Test
    public void renderTooltipsWithFailedBuilds(JenkinsRule rule) throws Exception {

        TopLevelItem project = rule.jenkins.getItem("testPipeline-randomly-no-data");
        try (JenkinsRule.WebClient wc = rule.createWebClient()) {
            HtmlPage page = wc.getPage(project);

            //      there should be a TAP result trend graph
            rule.assertXPath(page, "//img[@src='tapResults/graph']");

            rule.assertXPath(page, "//img[@lazymap='tapResults/graphMap']");
            //      check that build without TAP action recorded is excluded from graph
            assertTrue(page.getByXPath("//area[@href='7/tapResults/']").isEmpty());

            //      check that tooltip is rendered for the last build
            //      The map is now obtained via post, so it can not be seen directly
            //rule.assertXPath(page, "//area[@title='1 Failure(s)' and @href='16/tapResults/']");
            //      instead, we will request the map manually
            //      this is probably bug in HtmlUnit, manual testing is showing the map ok
            URL map = new URL(page.getUrl()+"/tapResults/graphMap");
            // see the data-crumb-value="test" in
            //WebResponse response = page.getWebResponse();
            //response.getContentAsString();
            String crumbHeaderName = "Jenkins-Crumb";
            //if hardcoded test will disappear (see above) extract the value
            String crumbValue = "test";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(map.toURI())
                    .header(crumbHeaderName, crumbValue)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
            Assertions.assertTrue(response.body().matches("(?s).*area.*title=.1 Failure\\(s\\).*href=.16/tapResults/..*"));
        }
    }
}
