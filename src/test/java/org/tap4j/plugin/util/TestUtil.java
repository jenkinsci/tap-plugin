/*
 * The MIT License
 *
 * Copyright (c) 2013 Bruno P. Kinoshita
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
package org.tap4j.plugin.util;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;


/**
 * Tests for Util class.
 * @see Util 
 */
public class TestUtil {

    private static final String UNIX_WS = "/home/workspace";
    private static final String UNIX_FOLDER_1 = "/home/workspace/test/subdirectory/another/1.txt";
    private static final String UNIX_FOLDER_2 = "/home/anotherfolder/test.txt";
    
    private static final String WIN_WS = "c:\\home\\workspace";
    private static final String WIN_FOLDER_1 = "c:\\home\\workspace\\test\\subdirectory\\another\\1.txt";
    private static final String WIN_FOLDER_2 = "c:\\home\\anotherfolder\\test.txt";
    
    @Test
    public void testNormalizeFolders() {
        assertEquals("Wrong normalization", "test/subdirectory/another/1.txt", Util.normalizeFolders(UNIX_WS, UNIX_FOLDER_1));
        assertEquals("Wrong normalization", "/home/anotherfolder/test.txt", Util.normalizeFolders(UNIX_WS, UNIX_FOLDER_2));
        assertEquals("Wrong normalization", "test/subdirectory/another/1.txt", Util.normalizeFolders(WIN_WS, WIN_FOLDER_1));
        assertEquals("Wrong normalization", "c:/home/anotherfolder/test.txt", Util.normalizeFolders(WIN_WS, WIN_FOLDER_2));
    }
    
}
