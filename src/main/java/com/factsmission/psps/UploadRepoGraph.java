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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.json.simple.parser.ParseException;
import org.wymiwyg.commons.util.arguments.ArgumentHandler;

/**
 *
 * @author user
 */
public class UploadRepoGraph {
    
    public static void main(String[] args) throws Exception {
        UploadRepoGraphArgs arguments = ArgumentHandler.readArguments(UploadRepoGraphArgs.class, args);
        if (arguments != null) {
            new UploadRepoGraph(arguments).getAndUpload();
        }
    }
    private final UploadRepoGraphArgs arguments;

    public UploadRepoGraph(UploadRepoGraphArgs arguments) {
        this.arguments = arguments;
    }

    public void getAndUpload() throws IOException, ParseException {
        GetRepoGraph getRepoGraph = new GetRepoGraph(arguments.repository(), arguments.token());
        uploadDataGraph(getRepoGraph.getDataGraph());
        uploadMatchersGraph(getRepoGraph.getMatchersGraph());
    }
    
    private void uploadDataGraph(Graph graph) throws IOException {
       final String graphUri = "https://github.com/"+arguments.repository();
       uploadGraph(graphUri, graph);
    }
    
    private void uploadMatchersGraph(Graph graph) throws IOException {
        final String graphUri = "https://github.com/"+arguments.repository()+"/matchers";
        uploadGraph(graphUri, graph);
    }

    protected void uploadGraph(final String graphUri, Graph graph) throws UnsupportedFormatException, IOException {
        final String mediaType = "application/sparql-update; charset=UTF-8";
        HttpURLConnection httpURLConnection = getAuthenticatedStream(mediaType, new URL(arguments.endpoint()));
        OutputStream out = httpURLConnection.getOutputStream();
        //DROP SILENT GRAPH <graph_uri>;
        //INSERT DATA { GRAPH <graph_uri> { .. RDF payload .. } }
        out.write("DROP SILENT GRAPH <".getBytes("utf-8"));
        out.write(graphUri.getBytes("utf-8"));
        out.write(">; INSERT DATA { GRAPH <".getBytes("utf-8"));
        out.write(graphUri.getBytes("utf-8"));
        out.write("> { ".getBytes("utf-8"));
        Serializer.getInstance().serialize(out, graph, SupportedFormat.N_TRIPLE);
        out.write(" } }".getBytes("utf-8"));
        out.flush();
        out.close();
        InputStream in = httpURLConnection.getInputStream();
        for (int i = in.read(); i != -1; i = in.read()) {
            System.out.write(i);
        }
    }
    
    private HttpURLConnection getAuthenticatedStream(String mediaType, URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String authStringEnoded = Base64.getEncoder().encodeToString(
                (arguments.userName() + ":" + arguments.password()).getBytes("utf-8"));
        connection.addRequestProperty("Authorization", "Basic " + authStringEnoded);
        connection.addRequestProperty("Content-Type", mediaType);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        return connection;
    }
    
}
