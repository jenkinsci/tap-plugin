/*
 * The MIT License
 *
 * Copyright 2016 Jenkins.
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

import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import org.tap4j.model.TestResult;
import org.tap4j.util.DirectiveValues;
import org.tap4j.plugin.model.TestSetMap;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author evandy
 */
public class TapBuildHistoryAction extends AbstractTapProjectAction implements ProminentProjectAction {

    private final Integer maxBuildsToShow;
    
    public TapBuildHistoryAction(AbstractProject<?, ?> project, Integer maxBuildsToShow) {
        super(project);
        this.maxBuildsToShow = maxBuildsToShow;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/tap/icons/tap-24.png";
    }

    @Override
    public String getDisplayName() {
        return "Tap Test History";
    }

    @Override
    public String getUrlName() {
        return "tapHistory";
    }
    
    public String getTitle() {
        if(getProject() instanceof MatrixConfiguration)
        {
            MatrixConfiguration p = (MatrixConfiguration)getProject();
            return p.getParent().getDisplayName() + ": " + p.getDisplayName();
        }
        else
        {
            return getProject().getDisplayName();
        }
    }
    
    public List<Run<?,?> > getBuilds()
    {
        List<Run<?,?> > builds = new ArrayList();
        
        // Matrix configurations should list against the parent's build list
        if(getProject() instanceof MatrixConfiguration)
        {
            MatrixConfiguration p = (MatrixConfiguration)getProject();
            for (Run<?, ?> build = p.getParent().getLastBuild(); build != null; build = build.getPreviousBuild()) {
                builds.add(build); 
                if(builds.size() >= this.maxBuildsToShow)
                {
                    break;
                }
            }
        }
        else
        {
            for (Run<?, ?> build = getProject().getLastBuild(); build != null; build = build.getPreviousBuild()) {
                builds.add(build);
                if(builds.size() >= this.maxBuildsToShow)
                {
                    break;
                }
            }
        }
        return builds;
    }
    
    public List<TapBuildHistoryAction> getChildren()
    {
        if(!(getProject() instanceof MatrixProject))
        {
            return null;
        }
        else
        {
            MatrixProject mp = (MatrixProject)getProject();
            List<TapBuildHistoryAction> children = new ArrayList();
            for (MatrixConfiguration configuration : mp.getActiveConfigurations())
            {
                AbstractProject<?, ?> p = configuration;
                TapBuildHistoryAction c = p.getAction(TapBuildHistoryAction.class);
                if(c != null)
                {
                    children.add(c);
                }
            }
            return children;
        }
    }
    public List<TestSetMap> getTestSets()
    {
        Run<?,?> lastBuild = getProject().getLastBuild();
        
        if(lastBuild == null) {
            return null;
        }
        
        TapBuildAction action = lastBuild.getAction(getBuildActionClass());
        return action.getResult().getTestSets();
    }
    
    public String getBuildStatus(TestSetMap set, TestResult result, Run<?,?> b)
    {
        // If a sub to a matrix project, given the parent's build, so grab the
        // right one.
        Run<?,?> build = getProject().getBuild(b.getId());
        TapBuildAction action = build != null ? build.getAction(getBuildActionClass()) : null;
        if(action == null)
        {
            // No TAP results for this build
            return "";
        }

        for(TestSetMap map : action.getResult().getTestSets())
        {
            if(map.getFileName().equals(set.getFileName()))
            {
                for(TestResult r : map.getTestSet().getTestResults())
                {
                    if(r.getDescription().equals(result.getDescription()))
                    {
                        if(r.getDirective() == null )
                        {
                            return r.getStatus().toString().toUpperCase();
                        }
                        else if(r.getDirective().getDirectiveValue() == DirectiveValues.SKIP)
                        {
                            return "SKIP";
                        }
                        else if (r.getDirective().getDirectiveValue() == DirectiveValues.TODO)
                        {
                            return "TODO";
                        }
                        else
                        {
                            return r.getStatus().toString().toUpperCase();
                        }
                    }
                }
                return "";
                //break; // Not found; don't keep looking
            }
        }
        return "";
    }
}
