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

import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import org.kohsuke.stapler.StaplerProxy;
import org.tap4j.plugin.model.TapStreamResult;

/**
 * TAP Build action with TAP results.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TapBuildAction implements Action, Serializable, StaplerProxy {

    private static final long serialVersionUID = 520981690971849654L;
    public static final String URL_NAME = "tapResults";
    public static final String ICON_NAME = "/plugin/tap/icons/tap-24.png";
    public static final String DISPLAY_NAME = "TAP Extended Test Results";

    private AbstractBuild<?, ?> build;

    private TapResult result;

    public TapBuildAction(AbstractBuild<?, ?> build, TapResult result) {
        super();
        this.build = build;
        this.result = result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.kohsuke.stapler.StaplerProxy#getTarget()
     */
    public Object getTarget() {
        return this.result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hudson.model.Action#getDisplayName()
     */
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hudson.model.Action#getIconFileName()
     */
    public String getIconFileName() {
        return ICON_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hudson.model.Action#getUrlName()
     */
    public String getUrlName() {
        return URL_NAME;
    }

    public List<TapBuildAction> getChildren()
    {
        if(this.build instanceof MatrixBuild)
        {
            List<TapBuildAction> actions = new ArrayList();
            
            MatrixBuild b = (MatrixBuild)this.build;
            for(MatrixRun run : b.getRuns())
            {
                actions.add(run.getAction(TapBuildAction.class));
            }
            return actions;
        }
        else
        {
            return null;
        }
    }
    /**
     * @return the build
     */
    public AbstractBuild<?, ?> getBuild() {
        return this.build;
    }

    public TapResult getResult() {
        return this.result;
    }

    public TapResult getPreviousResult() {
        TapResult previousResult = null;

        TapBuildAction previousAction = this.getPreviousAction();

        if (previousAction != null) {
            previousResult = previousAction.getResult();
        }

        return previousResult;
    }

    public TapBuildAction getPreviousAction() {
        TapBuildAction previousAction = null;

        if (this.build != null) {
            AbstractBuild<?, ?> previousBuild = this.build.getPreviousBuild();
            if (previousBuild != null) {
                previousAction = previousBuild.getAction(TapBuildAction.class);
            }
        }

        return previousAction;
    }
    
    public TapStreamResult getStreamResult()
    {
        TapTestResultAction action = build.getAction(TapTestResultAction.class);
        TapStreamResult result = (TapStreamResult) action.getResult();
        return result;
    }
}
