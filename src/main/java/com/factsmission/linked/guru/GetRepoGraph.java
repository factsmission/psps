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
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
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
    private String commitURL;
    private final Parser parser = Parser.getInstance();
    private final Graph graph = new SimpleGraph();

    GetRepoGraph(String repository, String token) {
        this.repository = repository;
        this.token = token;
        this.commitURL = "";
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: GetRepoGraph <username/repository> <PersonalAccessToken>");
            System.exit(1);
        }
        GetRepoGraph instance = new GetRepoGraph(args[0], args[1]);
        Graph g = instance.get();
        Serializer serializer = Serializer.getInstance();
        ByteArrayOutputStream serializedStream = new ByteArrayOutputStream();
        serializer.serialize(serializedStream, g, "text/turtle");
        String serializedString = byteToString(serializedStream);
        System.out.println("Voilà: " + serializedString);

    }

    public Graph get() throws IOException, ParseException {
        System.out.println("Loading RDF data from " + repository);
        String masterURIString = "https://api.github.com/repos/" + repository + "/branches/master";
        Serializer serializer = Serializer.getInstance();
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
        graph.add(new TripleImpl(new IRI(constructRepoBaseIRI(repository).getUnicodeString()+".meta"), Ontology.latestCommit, new IRI(commitURL)));
        return graph;
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
            if (!isItSpecial(path)) {
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
                        IRI baseIRI = constructFileBaseIRI(path);
                        parser.parse(graph, contentInputStream, rdfType, baseIRI);
                    } catch (IllegalArgumentException ex) {
                        throw new RuntimeException("Something bad happened", ex);
                    }
                }
            }
        }
    }

    IRI constructFileBaseIRI(String path) {
        String pathSubstring = path.substring(0, path.lastIndexOf("."));
        IRI repoBaseIRI = constructRepoBaseIRI(repository);
        IRI baseIRI = new IRI(repoBaseIRI.getUnicodeString() + pathSubstring);
        return baseIRI;
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
        return null;
    }

    /**
     *
     * @param byteArrayOutputStream
     * @return a string with utf-8
     * @throws UnsupportedEncodingException
     */
    private static String byteToString(ByteArrayOutputStream byteArrayOutputStream) throws UnsupportedEncodingException {
        byte[] bytes = byteArrayOutputStream.toByteArray();
        String str = new String(bytes, "utf-8");
        return str;
    }

    static IRI constructRepoBaseIRI(String repository) {
        if (repository.equals("factsmission/linked.guru")) {
            return new IRI("http://linked.guru/");
        }
        String username = repository.substring(0, repository.lastIndexOf("/"));
        String repo = repository.substring(repository.lastIndexOf("/")+1, repository.length()) + ".";
        if (repo.equals("/linked.")) {
            repo = "";
        }
        IRI baseIRI = new IRI("http://" + repo + username + ".linked.guru/");
        return baseIRI;
    }

    private Boolean isItSpecial(String path) {
        return path.startsWith(".") || path.contains("/.");
    }
}
