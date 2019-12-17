/*
 * The MIT License
 *
 * Copyright 2017 FactsMission AG.
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

import org.wymiwyg.commons.util.arguments.CommandLine;

/**
 *
 * @author user
 */
public interface UploadRepoGraphArgs {
    
    @CommandLine (
        longName ="token",
        shortName = "T", 
        required = true,
        description = "The API-Token to access GitHub"
    )
    public String token();
    
    @CommandLine (
        longName ="repository",
        shortName = "R", 
        required = true,
        description = "The GitHub repository in the form <username>/<repo>"
    )
    public String repository();
    
    @CommandLine (
        longName ="queryEndpoint",
        shortName = "Q",
        required = false,
        description = "The SPARQL endpoint against which to send queries"
    )
    public String queryEndpoint();

    @CommandLine (
        longName ="updateEndpoint",
        shortName = "E",
        required = false,
        description = "The SPARQL endpoint against which to send the update requests"
    )
    public String updateEndpoint();

    @CommandLine (
        longName ="userName",
        shortName = "U",
        required = true,
        description = "The username for the SPARQL enpoint"
    )
    public String userName();
    
    
    @CommandLine (
        longName ="password",
        shortName = "P",
        required = true,
        description = "The password for the SPARQL enpoint"
    )
    public String password();

    @CommandLine (
        longName ="supressExtensions",
        shortName = "S",
        required = false,
        defaultValue = "true",
        description = "Supress the file extensions of the RDF files in github"
    )
    public boolean supressFileExtensions();
    
    @CommandLine (
        longName ="postUploadStatement",
        shortName = "PUS",
        required = false,
        defaultValue = "",
        description = "SPARQL Statement to be executed after uploading data"
    )
    public String postUploadStatement();
}
