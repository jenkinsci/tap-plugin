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

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;

/**
 * Base class for TAP Project action.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class AbstractTapProjectAction implements Action {

    public final Job<?,?> job;

    @Deprecated
    public final AbstractProject<?,?> project;

    public AbstractTapProjectAction(Job<?,?> job) {
        this.job = job;
        project = job instanceof AbstractProject ? (AbstractProject) job : null;
    }

    public AbstractTapProjectAction(AbstractProject<?, ?> project) {
        this((Job) project);
    }

    public static final String URL_NAME = "tapResults";
    public static final String ICON_NAME = "/plugin/tap/icons/tap-24.png";
    
    /* (non-Javadoc)
     * @see hudson.model.Action#getDisplayName()
     */
    public String getDisplayName() {
        return "TAP Extended Test Results";
    }

    /* (non-Javadoc)
     * @see hudson.model.Action#getIconFileName()
     */
    public String getIconFileName() {
        return ICON_NAME;
    }

    /* (non-Javadoc)
     * @see hudson.model.Action#getUrlName()
     */
    public String getUrlName() {
        return URL_NAME;
    }
    
    public String getSearchUrl() {
        return URL_NAME;
    }
    
}
