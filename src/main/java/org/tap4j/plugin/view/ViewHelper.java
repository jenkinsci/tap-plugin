/*
 * The MIT License
 *
 * Copyright (c) 2019, Jenkins.
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
package org.tap4j.plugin.view;

import hudson.model.Job;
import jenkins.model.Jenkins;
import org.acegisecurity.AccessDeniedException;

/**
 * Helper class for dashboard view related code.
 *
 * @author Jakub Podlešák (jakub.podlesak at oracle.com)
 */
public class ViewHelper {

    public static Job getCounterpartJob(Job job, String toReplace, String newText) throws AccessDeniedException {
        Jenkins jenkins = Jenkins.getActiveInstance();
        String theOtherJobName = job.getName().replaceFirst(toReplace, newText);
        Job theOtherJob = (Job)jenkins.getItem(theOtherJobName);
        return theOtherJob;
    }


}
