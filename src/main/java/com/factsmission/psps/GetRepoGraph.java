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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GetRepoGraph {

    private final String repository;
    private final String token;
    private String commitURL;
    private IRI baseIRI;
    private final Parser parser = Parser.getInstance();
    private Map<IRI, Graph> graphs = new HashMap<IRI, Graph>();
    private boolean supressFileExtension;
    private FileStorage fileStorage = new FileStorage();

    GetRepoGraph(String repository, String token, boolean supressFileExtension) throws IOException {
        this.repository = repository;
        this.token = token;
        this.commitURL = "";
        this.supressFileExtension = supressFileExtension;
        try {
            processRepository();
        } catch (ParseException ex) {
            throw new IOException(ex);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: GetRepoGraph <username/repository> <PersonalAccessToken>");
            System.exit(1);
        }
        GetRepoGraph instance = new GetRepoGraph(args[0], args[1], true);
        for (Entry<IRI, Graph> entry : instance.getGraphs().entrySet()) {
            System.out.println("------------------------");
            System.out.println("Graph: " + entry.getKey());
            Graph g = entry.getValue();
            Serializer serializer = Serializer.getInstance();
            serializer.serialize(System.out, g, "text/turtle");
        }
    }

    public Map<IRI, Graph> getGraphs() {
        return graphs;
    }

    private void processRepository() throws IOException, ParseException {
        System.out.println("Loading RDF data from " + repository);
        String masterURIString = "https://api.github.com/repos/" + repository + "/branches/master";
        URL masterURI = new URL(masterURIString);
        InputStream masterJsonStream = getAuthenticatedStream(masterURI);
        Reader masterJsonReader = new InputStreamReader(masterJsonStream, "utf-8");
        JSONParser jparser = new JSONParser();
        Object obj = jparser.parse(masterJsonReader);
        JSONObject master = (JSONObject) obj;
        JSONObject commitA = (JSONObject) master.get("commit");
        JSONObject commitB = (JSONObject) commitA.get("commit");
        commitURL = (String) commitA.get("html_url");
        JSONObject tree = (JSONObject) commitB.get("tree");
        String treeURLString = (String) tree.get("url");
        processTree(treeURLString);
        Graph repoGraph = new SimpleGraph();
        repoGraph.add(new TripleImpl(getRepoIRI(), Ontology.latestCommit, new IRI(commitURL)));
        for (Entry<IRI, Graph> entry : graphs.entrySet()) {
            repoGraph.add(new TripleImpl(entry.getKey(), DCTERMS.source, getRepoIRI()));
        }
        graphs.put(getRepoIRI(), repoGraph);
    }

    private void processTree(String treeURLString) throws IOException, ParseException {
        String treeURLRecursiveString = treeURLString + "?recursive=1";
        URL treeURLRecursive = new URL(treeURLRecursiveString);
        InputStream treeJsonStream = getAuthenticatedStream(treeURLRecursive);
        Reader treeJsonReader = new InputStreamReader(treeJsonStream, "utf-8");
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(treeJsonReader);
        JSONObject jsonObject = (JSONObject) obj;
        JSONArray tree = (JSONArray) jsonObject.get("tree");
        Map<String, URL> path2Stuff = new HashMap<>();
        Iterator<JSONObject> iterator = tree.iterator();
        while (iterator.hasNext()) {
            JSONObject next = iterator.next();
            String path = (String) next.get("path");
            String url = (String) next.get("url");
            URL stuffURL = new URL(url);
            path2Stuff.put(path, stuffURL);
        }
        baseIRI = getBaseIRI(path2Stuff.get("BASEURI"));
        for (Entry<String, URL> entry : path2Stuff.entrySet()) {
            processFile(entry.getKey(), entry.getValue());
        };
    }

    private IRI getBaseIRI(URL baseUrlFile) throws IOException, ParseException {
        if (baseUrlFile != null) {
            try ( InputStream baseUrlStream = getAuthenticatedStream(baseUrlFile);  BufferedReader stuffJsonReader = new BufferedReader(new InputStreamReader(baseUrlStream, "utf-8"))) {
                JSONParser jsonParser = new JSONParser();
                Object obj = jsonParser.parse(stuffJsonReader);
                JSONObject jsonObject = (JSONObject) obj;
                String contentBase64 = (String) jsonObject.get("content");
                String content = new String(Base64.getMimeDecoder().decode(contentBase64));
                return new IRI(content.trim());
            }
        } else {
            return getDefaultBaseIRI();
        }
    }

    private IRI getDefaultBaseIRI() {
        return new IRI("https://raw.githubusercontent.com/" + repository + "/master/");
    }

    private IRI getRepoIRI() {
        return new IRI("https://github.com/" + repository);
    }

    private void processFile(String path, URL stuffURL) throws IOException, ParseException {
        String rdfType = getRdfFormat(path);
        if (rdfType != null) {
            loadStuffToGraph(stuffURL, path, rdfType);
        } else {
            loadStufToFileStore(stuffURL, path);
        }
    }

    private void loadStufToFileStore(URL stuffURL, String path) throws IOException, ParseException {
        InputStream stuffJsonStream = getAuthenticatedStream(stuffURL);
        Reader stuffJsonReader = new InputStreamReader(stuffJsonStream, "utf-8");
        JSONParser jsonParser = new JSONParser();
        Object obj = jsonParser.parse(stuffJsonReader);
        JSONObject jsonObject = (JSONObject) obj;
        String contentBase64 = (String) jsonObject.get("content");
        if (contentBase64 != null) {
            try {
                byte[] content = Base64.getMimeDecoder().decode(contentBase64);
                IRI pathIRI = constructFileBaseIRI(path);
                fileStorage.put(pathIRI, content);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException("Something bad happened", ex);
            }
        }
    }

    protected void loadStuffToGraph(URL stuffURL, String path, String rdfType) throws UnsupportedEncodingException, RuntimeException, ParseException, IOException {
        InputStream stuffJsonStream = getAuthenticatedStream(stuffURL);
        Reader stuffJsonReader = new InputStreamReader(stuffJsonStream, "utf-8");
        JSONParser jsonParser = new JSONParser();
        Object obj = jsonParser.parse(stuffJsonReader);
        JSONObject jsonObject = (JSONObject) obj;
        String contentBase64 = (String) jsonObject.get("content");
        if (contentBase64 != null) {
            try {
                String content = new String(Base64.getMimeDecoder().decode(contentBase64));
                try ( InputStream contentInputStream = new ByteArrayInputStream(content.getBytes("utf-8"))) {
                    IRI baseIRI = constructFileBaseIRI(path);
                    if (supressFileExtension) {
                        baseIRI = supressExtension(baseIRI);
                    }
                    Graph graph = parser.parse(contentInputStream, rdfType, baseIRI);
                    graphs.put(baseIRI, graph);
                }
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException("Something bad happened", ex);
            }
        }
    }

    private IRI supressExtension(IRI iri) {
        String string = iri.getUnicodeString();
        int lastDotPos = string.lastIndexOf(".");
        if (lastDotPos > -1) {
            string = string.substring(0, lastDotPos);
        }
        return new IRI(string);
    }

    IRI constructFileBaseIRI(String path) {
        return new IRI(baseIRI.getUnicodeString() + path);
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
     * @return the media type of the RDF-Format expected based on the
     * file-extension or null if no file-extension matching a supported type was
     * found
     */
    private String getRdfFormat(String path) {
        if (path.endsWith(".ttl")) {
            return "text/turtle";
        }
        if (path.endsWith(".rdf")) {
            return "application/rdf+xml";
        }
        if (path.endsWith(".jsonld")) {
            return "application/ld+json";
        }
        return null;
    }

}
