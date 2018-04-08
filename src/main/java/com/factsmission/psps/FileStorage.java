/*
 * The MIT License
 *
 * Copyright 2017 user.
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
package com.factsmission.psps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.io.IOUtils;

import solutions.linked.slds.RootResource;

public class FileStorage {
    
    private File baseFileStorage = new File(new File(System.getProperty("user.dir")), "psps");

    public void put(IRI iri, byte[] bytes) throws IOException {
        File file = getFile(iri);
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
    }

	private File getFile(IRI iri) {
        String filePath = iri.getUnicodeString().replace("://", "/").replace(":", "-");
        return new File(baseFileStorage, filePath);
	}

    public byte[] get(IRI iri) throws IOException {
        File file = getFile(iri);
        if (file.exists() && !file.isDirectory()) {
            try (InputStream in = new FileInputStream(file)) {
                return IOUtils.toByteArray(in);
            }
        } else {
            return null;
        }
    }
}