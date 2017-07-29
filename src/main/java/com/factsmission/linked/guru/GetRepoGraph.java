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
package com.factsmission.linked.guru;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GetRepoGraph {

    private final String repository;
    private final String token;
    private final Parser parser = Parser.getInstance();
    private final Graph graph = new SimpleGraph();

    GetRepoGraph(String repository, String token) {
        this.repository = repository;
        this.token = token;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: GetRepoGraph <username/repository> <PersonalAccessToken>");
            System.exit(1);
        }
        GetRepoGraph instance = new GetRepoGraph(args[0], args[1]);
        Graph g = instance.get();
        //serializer.serialize(System.out, g, "application/rdf+xml");
        ByteArrayOutputStream serializedStream = new ByteArrayOutputStream();
        Serializer serializer = Serializer.getInstance();
        serializer.serialize(serializedStream, g, "application/rdf+xml");
        String serializedString = byteToString(serializedStream);
        System.out.println("Voilà: " + serializedString);

    }

    private Graph get() throws IOException, ParseException {
        System.out.println("Loading RDF data from " + repository);
        String masterURIString = "https://api.github.com/repos/" + repository + "/branches/master";
        URL masterURI = new URL(masterURIString);
        InputStream masterJsonStream = getAuthenticatedStream(masterURI);
        Reader masterJsonReader = new InputStreamReader(masterJsonStream, "utf-8");
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(masterJsonReader);
        JSONObject master = (JSONObject) obj;
        JSONObject commitA = (JSONObject) master.get("commit");
        JSONObject commitB = (JSONObject) commitA.get("commit");
        JSONObject tree = (JSONObject) commitB.get("tree");
        String treeURLString = (String) tree.get("url");
        printTree(treeURLString);
        return graph;
    }

    private void printTree(String treeURLString) throws IOException, ParseException {
        String treeURLRecursiveString = treeURLString + "?recursive=1";
        URL treeURLRecursive = new URL(treeURLRecursiveString);
        InputStream treeJsonStream = getAuthenticatedStream(treeURLRecursive);
        Reader treeJsonReader = new InputStreamReader(treeJsonStream, "utf-8");
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(treeJsonReader);
        JSONObject jsonObject = (JSONObject) obj;
        JSONArray tree = (JSONArray) jsonObject.get("tree");
        Iterator<JSONObject> iterator = tree.iterator();
        while (iterator.hasNext()) {
            JSONObject next = iterator.next();
            String path = (String) next.get("path");
            String url = (String) next.get("url");
            URL stuffURL = new URL(url);
            processFile(path, stuffURL);
        }
    }

    private void processFile(String path, URL stuffURL) throws IOException, ParseException {
        String rdfType = getRdfFormat(path);
        if (rdfType != null) {
            InputStream stuffJsonStream = getAuthenticatedStream(stuffURL);
            Reader stuffJsonReader = new InputStreamReader(stuffJsonStream, "utf-8");
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(stuffJsonReader);
            JSONObject jsonObject = (JSONObject) obj;
            String contentBase64 = (String) jsonObject.get("content");
            if (contentBase64 != null) {
                try {
                    String content = new String(Base64.getMimeDecoder().decode(contentBase64));
                    InputStream contentInputStream = new ByteArrayInputStream(content.getBytes("utf-8"));
                    String pathSubstring = "/"+path.substring(0, path.lastIndexOf("."));
                    String username = repository.substring(0, repository.lastIndexOf("/"));
                    String repo = repository.substring(repository.lastIndexOf("/"), repository.length()) + ".";
                    if (repo.equals("/linked.")) {
                        repo = "/";
                    }
                    IRI baseIRI = new IRI("http:/" + repo + username + ".linked.guru"+pathSubstring); //only one dash after http because repo comes with one.
                    if (rdfType == "text/turtle") {
                        parser.parse(graph, contentInputStream, rdfType, baseIRI);
                    }
                } catch (IllegalArgumentException ex) {
                    /*System.out.println("Path: " + path);
                    System.out.println("Could not Decode: Illegal Argument");
                    System.out.println("Encoded Content: " + contentBase64);*/
                }
            }
        }
        /*if (rdfType == null) {
        System.out.println(path + " is not a supported RFD file");
        }*/
    }

    private InputStream getAuthenticatedStream(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String authStringEnoded = Base64.getEncoder().encodeToString((token + ":").getBytes("utf-8"));
        connection.addRequestProperty("Authorization", "Basic " + authStringEnoded);
        return connection.getInputStream();
    }

    /**
     *
     * @param path
     * @return the media type of the RDF-Fomat expected based on the
     * file-extension or null if no file-extension matching a supported type was
     * found
     */
    private String getRdfFormat(String path) {
        if (path.endsWith(".ttl")) {
            return "text/turtle";
        }
        return null;
    }

    /**
     *
     * @param byteArrayOutputStream
     * @return a string with utf-8
     * @throws UnsupportedEncodingException
     */
    public static String byteToString(ByteArrayOutputStream byteArrayOutputStream) throws UnsupportedEncodingException {
        byte[] bytes = byteArrayOutputStream.toByteArray();
        String str = new String(bytes, "utf-8");
        return str;
    }
}
