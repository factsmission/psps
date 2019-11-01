/*
 * The MIT License
 *
 * Copyright 2019 FactsMission AG.
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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author me
 */
public class Repository {

    private final String token;
    private final String repository;
    private String branch;
    private final URL apiBaseURI;
    private String latestCommitURL;
    private Map<String, URL> path2Stuff;

    Repository(String repository, String token) {
        this.repository = repository;
        this.token = token;
        String apiBaseURIString = "https://api.github.com/repos/" + repository + "/";
        try {
            this.apiBaseURI = new URL(apiBaseURIString);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private InputStream getAuthenticatedStream(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String authStringEnoded = Base64.getEncoder().encodeToString((token + ":").getBytes("utf-8"));
        connection.addRequestProperty("Authorization", "Basic " + authStringEnoded);
        return connection.getInputStream();
    }
    
    
    private Object getParsedJson(URL stuffURL) throws IOException {
        try {
            InputStream stuffJsonStream = getAuthenticatedStream(stuffURL);
            Reader stuffJsonReader = new InputStreamReader(stuffJsonStream, "utf-8");
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(stuffJsonReader);
            return obj;
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private JSONObject getJsonObject(URL stuffURL) throws IOException {
        return (JSONObject) getParsedJson(stuffURL);
    }

    private JSONArray getJsonArray(URL stuffURL) throws IOException {
        return (JSONArray) getParsedJson(stuffURL);
    }

    void useBranch(String branch) throws IOException {
        this.branch = branch;
        try {
            URL masterURI = new URL(apiBaseURI, "branches/"+branch);
            JSONObject master = getJsonObject(masterURI);
            JSONObject commitA = (JSONObject) master.get("commit");
            latestCommitURL = (String) commitA.get("html_url");
            JSONObject commitB = (JSONObject) commitA.get("commit");
            JSONObject tree = (JSONObject) commitB.get("tree");
            String treeURLString = (String) tree.get("url");
            processTree(treeURLString);
        } catch (MalformedURLException | ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void processTree(String treeURLString) throws IOException, ParseException {
            String treeURLRecursiveString = treeURLString + "?recursive=1";
            URL treeURLRecursive = new URL(treeURLRecursiveString);
            JSONObject jsonObject = getJsonObject(treeURLRecursive);
            JSONArray tree = (JSONArray) jsonObject.get("tree");
            path2Stuff = new HashMap<>();
            Iterator<JSONObject> iterator = tree.iterator();
            while (iterator.hasNext()) {
                JSONObject next = iterator.next();
                String path = (String) next.get("path");
                String url = (String) next.get("url");
                URL stuffURL = new URL(url);
                path2Stuff.put(path, stuffURL);
            }
            
        }

    String getCommitURL() {
        return latestCommitURL;
    }

    Iterable<String> getPaths() {
        return path2Stuff.keySet();
    }

    byte[] getContent(String path) throws IOException {
        JSONObject jsonObject = getJsonObject(path2Stuff.get(path));
        String contentBase64 = (String) jsonObject.get("content");
        if (contentBase64 == null) {
            return null;
        }
        try {
            return Base64.getMimeDecoder().decode(contentBase64);                    
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Something bad happened", ex);
        }
    }
    
    
    String[] getBranches() throws IOException {
        JSONArray jsonArray = getJsonArray(new URL(apiBaseURI, "branches"));
        Set<String> resultSet = new HashSet<>();
        jsonArray.forEach((obj) -> {
            resultSet.add((String) ((JSONObject) obj).get("name"));
        });
        return resultSet.toArray(new String[resultSet.size()]);
    }
}
