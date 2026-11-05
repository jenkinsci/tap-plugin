/*
 * The MIT License
 *
 * Copyright (c) 2011 Bruno P. Kinoshita
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

import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import org.jfree.chart.JFreeChart;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.tap4j.plugin.util.GraphHelper;

import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A TAP Project action, with a graph and a list of builds.
 *
 * @since 1.0
 */
public class TapProjectAction implements Action {

    public static final String URL_NAME = "tapResults";
    public static final String ICON_NAME = "/plugin/tap/icons/tap-24.png";

    public final Job<?, ?> job;

    /**
     * Used to figure out if we need to regenerate the graphs or not.
     * The key is the request URI and value is the number of builds for the project.
     * Marked as transient so it is not restored when Jenkins reloads from disk.
     */
    private transient Map<String, Integer> requestMap = new ConcurrentHashMap<>();

    public TapProjectAction(AbstractProject<?, ?> project) {
        this((Job<?, ?>) project);
    }

    public TapProjectAction(Job<?, ?> job) {
        this.job = job;
    }

    /**
     * Returns the current request map, or creates a new one if it is missing
     * (e.g., Jenkins was reloaded from disk).
     *
     * @return The request map.
     */
    private Map<String, Integer> getRequestMap() {
        if (requestMap == null) {
            requestMap = new ConcurrentHashMap<>();
        }
        return requestMap;
    }

    @Override
    public String getDisplayName() {
        return "TAP Extended Test Results";
    }

    @Override
    public String getIconFileName() {
        return ICON_NAME;
    }

    @Override
    public String getUrlName() {
        return URL_NAME;
    }

    public String getSearchUrl() {
        return URL_NAME;
    }

    public Job<?, ?> getJob() {
        return job;
    }

    protected Class<TapBuildAction> getBuildActionClass() {
        return TapBuildAction.class;
    }

    public TapBuildAction getLastBuildAction() {
        TapBuildAction action = null;
        final Run<?, ?> lastBuild = this.getLastBuildWithTap();

        if (lastBuild != null) {
            action = lastBuild.getAction(TapBuildAction.class);
        }

        return action;
    }

    private Run<?, ?> getLastBuildWithTap() {
        Run<?, ?> lastBuild = this.job.getLastBuild();

        while (lastBuild != null
            && lastBuild.getAction(TapBuildAction.class) == null) {
            lastBuild = lastBuild.getPreviousBuild();
        }

        return lastBuild;
    }

    public void doIndex(
        final StaplerRequest2 request,
        final StaplerResponse2 response) throws IOException {

        Run<?, ?> lastBuild = this.getLastBuildWithTap();

        if (lastBuild == null) {
            response.sendRedirect2("nodata");
        } else {
            int buildNumber = lastBuild.getNumber();
            response.sendRedirect2(String.format(
                "../%d/%s",
                buildNumber,
                TapBuildAction.URL_NAME));
        }
    }

    /**
     * Generates the graph that shows test pass/fail ratio.
     */
    public void doGraph(
        final StaplerRequest2 req,
        StaplerResponse2 rsp) throws IOException {

        if (newGraphNotNeeded(req, rsp)) {
            return;
        }

        final DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>
            dataSetBuilder = new DataSetBuilder<>();

        populateDataSetBuilder(dataSetBuilder);

        new hudson.util.Graph(-1, getGraphWidth(), getGraphHeight()) {
            @Override
            protected JFreeChart createGraph() {
                return GraphHelper.createChart(req, dataSetBuilder.build());
            }
        }.doPng(req, rsp);
    }

    public void doGraphMap(
        final StaplerRequest2 req,
        StaplerResponse2 rsp) throws IOException {

        if (newGraphNotNeeded(req, rsp)) {
            return;
        }

        final DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>
            dataSetBuilder = new DataSetBuilder<>();

        populateDataSetBuilder(dataSetBuilder);

        new hudson.util.Graph(-1, getGraphWidth(), getGraphHeight()) {
            @Override
            protected JFreeChart createGraph() {
                return GraphHelper.createChart(req, dataSetBuilder.build());
            }
        }.doMap(req, rsp);
    }

    /**
     * Returns true if there is a graph to plot.
     *
     * @return value for property 'graphAvailable'
     */
    public boolean isGraphActive() {
        Run<?, ?> build = this.job.getLastBuild();

        // A graph requires at least two data points.
        int numPoints = 0;

        while (numPoints < 2) {
            if (build == null) {
                return false;
            }

            if (this.job instanceof MatrixProject mp) {

                for (Job<?, ?> j : mp.getAllJobs()) {
                    if (j != mp) {
                        Run<?, ?> sub = j.getBuild(build.getId());

                        if (sub != null
                            && sub.getAction(getBuildActionClass()) != null) {
                            numPoints++;
                            break;
                        }
                    }
                }
            } else {
                if (build.getAction(getBuildActionClass()) != null) {
                    numPoints++;
                }
            }

            build = build.getPreviousBuild();
        }

        return true;
    }

    /**
     * Checks whether the graph can be served from the browser cache.
     */
    private boolean newGraphNotNeeded(
        final StaplerRequest2 req,
        StaplerResponse2 rsp) {

        final Run<?, ?> lastCompletedBuild = job.getLastCompletedBuild();

        if (lastCompletedBuild == null) {
            return false;
        }

        final Calendar t = this.job.getLastCompletedBuild().getTimestamp();

        Map<String, Integer> requests = getRequestMap();

        final int prevNumBuilds = requests.getOrDefault(
            req.getRequestURI(), 0);

        final int numBuilds = this.job.getBuilds().size();

        if (prevNumBuilds != numBuilds) {
            requests.put(req.getRequestURI(), numBuilds);
        }

        if (requests.size() > 10) {
            requests.clear();
        }

        return prevNumBuilds == numBuilds && req.checkIfModified(t, rsp);
    }

    protected void populateDataSetBuilder(
        DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dataset) {

        Job<?, ?> p = this.job;

        for (Run<?, ?> build = this.job.getLastBuild();
             build != null;
             build = build.getPreviousBuild()) {

            /*
             * Exclude builds that failed before TAP data was recorded.
             */
            if (build.getAction(TapBuildAction.class) == null) {
                continue;
            }

            ChartUtil.NumberOnlyBuildLabel label =
                new ChartUtil.NumberOnlyBuildLabel(build);

            Result r = new Result();

            if (p instanceof MatrixProject mp) {

                for (Job<?, ?> j : mp.getAllJobs()) {
                    if (j != mp) {
                        Run<?, ?> sub = j.getBuild(build.getId());

                        if (sub != null) {
                            r.add(summarizeBuild(sub));
                        }
                    }
                }
            } else {
                r = summarizeBuild(build);
            }

            dataset.add(r.numPassed, "Passed", label);
            dataset.add(r.numFailed, "Failed", label);
            dataset.add(r.numSkipped, "Skipped", label);
            dataset.add(r.numToDo, "ToDo", label);
        }
    }

    protected Result summarizeBuild(Run<?, ?> b) {
        Result r = new Result();

        TapBuildAction action = b.getAction(getBuildActionClass());

        if (action != null) {
            TapResult report = action.getResult();
            report.tally();

            r.numPassed = report.getPassed();
            r.numFailed = report.getFailed();
            r.numSkipped = report.getSkipped();
            r.numToDo = report.getToDo();
        }

        return r;
    }

    public String getGraphName() {
        return "TAP Test Results Trend";
    }

    public int getGraphWidth() {
        return 500;
    }

    public int getGraphHeight() {
        return 200;
    }

    protected static class Result {
        public int numPassed;
        public int numFailed;
        public int numSkipped;
        public int numToDo;

        public Result() {
            numPassed = 0;
            numFailed = 0;
            numSkipped = 0;
            numToDo = 0;
        }

        public void add(Result r) {
            numPassed += r.numPassed;
            numFailed += r.numFailed;
            numSkipped += r.numSkipped;
            numToDo += r.numToDo;
        }
    }
}
