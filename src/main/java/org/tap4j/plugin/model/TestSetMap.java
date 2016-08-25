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
package org.tap4j.plugin.model;

import java.io.Serializable;

import org.tap4j.model.TestSet;


/**
 * A map for TestSet with the file name, as there is no file name in the 
 * original TestSet class.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TestSetMap implements Serializable {

    private static final long serialVersionUID = 7300386936718557088L;
    
    private final String fileName;
    private final TestSet testSet;
    
    public TestSetMap( String fileName, TestSet testSet )
    {
        this.fileName = fileName;
        this.testSet = testSet;
    }
    
    public String getFileName()
    {
        return this.fileName;
    }
    
    public TestSet getTestSet()
    {
        return this.testSet;
    }
    
}
