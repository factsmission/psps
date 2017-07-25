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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class GetRepoGraph {
    
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: GetRepoGraph username/repository");
            System.exit(1);
        }
        System.out.println("Loading RDF data from "+args[0]);
        String masterURIString = "https://api.github.com/repos/"+args[0]+"/branches/master";
        URL masterURI = new URL(masterURIString);
        InputStream masterJsonStream = masterURI.openStream();
        Reader masterJsonReader = new InputStreamReader(masterJsonStream, "utf-8");
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(masterJsonReader);
        JSONObject master = (JSONObject) obj;
        JSONObject commitA = (JSONObject) master.get("commit");
        JSONObject commitB = (JSONObject) commitA.get("commit");
        JSONObject tree = (JSONObject) commitB.get("tree");
        String treeURLString = (String) tree.get("url");
        printTree(treeURLString);
    }

    private static void printTree(String treeURLString) throws Exception {
        String treeURLRecursiveString = treeURLString+"?recursive=1";
        URL treeURLRecursive = new URL(treeURLRecursiveString);
        InputStream treeJsonStream = treeURLRecursive.openStream();
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
            System.out.println("URL: "+url+" Path: "+path);
        }
    }
    
}
