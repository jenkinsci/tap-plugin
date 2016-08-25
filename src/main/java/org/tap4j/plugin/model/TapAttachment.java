/*
 * The MIT License
 *
 * Copyright (c) 2012 Bruno P. Kinoshita
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

import java.util.Map;

/**
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class TapAttachment {

    private final String fileName;
    private final byte[] content;
    private final int size;
    private final String fileType;

    /**
     * @param fileName
     * @param content
     * @param size
     * @param fileType
     */
    public TapAttachment(String fileName, byte[] content, int size,
            String fileType) {
        super();
        this.fileName = fileName;
        this.content = content;
        this.size = size;
        this.fileType = fileType;
    }
    
    /**
     * @param fileName
     * @param content
     * @param size
     * @param fileType
     */
    public TapAttachment(byte[] content, Map<String, Object> diagnostics) {
        super();
        this.content = content;
        int size = -1;
        String fileType = "";
        String fileName = "tapAttachment";
        for(String key : diagnostics.keySet()) {
            if(diagnostics.get(key) instanceof Map<?, ?> == Boolean.FALSE) {
                if(key.equalsIgnoreCase("file-size")) {
                    try {
                        size = (int) Long.parseLong(diagnostics.get(key).toString());
                    } catch(NumberFormatException nfe) {
                        // Do nothing
                    }
                } else if(key.equalsIgnoreCase("file-type")) {
                    fileType = (String) diagnostics.get(key);
                } else if(key.equalsIgnoreCase("file-name")) {
                    fileName = (String) diagnostics.get(key);
                }
            }
        }
        this.size = size;
        this.fileType = fileType;
        this.fileName = fileName;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the content
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @return the fileType
     */
    public String getFileType() {
        return fileType;
    }

}
