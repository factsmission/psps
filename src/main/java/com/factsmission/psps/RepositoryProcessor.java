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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class RepositoryProcessor {

    private final String repositoryName;

    private final Parser parser = Parser.getInstance();

    private final boolean supressFileExtension;
    private final Set<BranchProcessor> branchProcessors = new HashSet<>();
    private final Repository repository;

    class BranchProcessor {

        private IRI baseIRI;
        private final Map<IRI, Graph> graphs = new HashMap<>();
        private final FileStorage fileStorage = new FileStorage();
        private final String branch;

        private BranchProcessor(String branch) throws IOException {
            this.branch = branch;
            processBranch();
        }

        public Map<IRI, Graph> getGraphs() {
            return graphs;
        }

        private void loadStufToFileStore(byte[] content, String path) throws IOException {
            try {
                IRI pathIRI = constructFileBaseIRI(path);
                fileStorage.put(pathIRI, content);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException("Something bad happened", ex);
            }     
        }

        protected void loadStuffToGraph(byte[] content, String path, String rdfType) throws IOException {
            try ( InputStream contentInputStream = new ByteArrayInputStream(content)) {
                IRI baseIRI = constructFileBaseIRI(path);
                if (supressFileExtension) {
                    baseIRI = supressExtension(baseIRI);
                }
                Graph graph = parser.parse(contentInputStream, rdfType, baseIRI);
                graphs.put(baseIRI, graph);
            } catch (RuntimeException ex) {
                //TODO log to meta-graph
                Logger.getLogger(RepositoryProcessor.class.getName()).log(Level.SEVERE, "Couldn't parse file at "+path, ex);
            }
        }
        
        private void processFile(String path) throws IOException {
            String rdfType = getRdfFormat(path);
            byte[] content = repository.getContent(path);
            if (content == null) {
                //this is the case for directories
                return;
            }
            if (rdfType != null) {
                loadStuffToGraph(content, path, rdfType);
            } else {
                loadStufToFileStore(content, path);
            }
        }
        
        private void processBranch() throws IOException {
            synchronized(repository) {
                repository.useBranch(branch);
                baseIRI = getBaseIRI(repository.getContent("BASEURI"));
                for (String path : repository.getPaths()) {
                    processFile(path);
                }
                Graph repoGraph = new SimpleGraph();
                repoGraph.add(new TripleImpl(getBranchIRI(), Ontology.latestCommit, new IRI(repository.getCommitURL())));
                repoGraph.add(new TripleImpl(getBranchIRI(), Ontology.repository, getRepoIRI()));
                for (Entry<IRI, Graph> entry : graphs.entrySet()) {
                    repoGraph.add(new TripleImpl(entry.getKey(), DCTERMS.source, getBranchIRI()));
                }
                graphs.put(getBranchIRI(), repoGraph);
            }
        }
        
        private IRI getBranchIRI() {
            return new IRI("https://github.com/" + repositoryName+"/"+branch);
        }
        
        
        
        IRI constructFileBaseIRI(String path) {
            return new IRI(baseIRI.getUnicodeString() + path);
        }
        
        private IRI getBaseIRI(byte[] contentBytes) throws IOException {
            if (contentBytes != null) {
                String content = new String(contentBytes, "UTF-8");
                if (content.trim().charAt(0) == '{') {
                    try {
                        JSONParser jsonParser = new JSONParser();
                        JSONObject contentObject = (JSONObject) jsonParser.parse(content);
                        String branchPath = (String) contentObject.get(branch);
                        if (branchPath == null) {
                            return getDefaultBaseIRI();
                        }
                        return new IRI(branchPath);
                    } catch (ParseException ex) {
                        Logger.getLogger(RepositoryProcessor.class.getName()).log(Level.SEVERE, "Couln'd parse BASEURI, usind default base IRI", ex);
                        return getDefaultBaseIRI();
                    }
                } else {
                    return new IRI(content.trim());
                }   
            } else {
                return getDefaultBaseIRI();
            }
        }
        
        private IRI getDefaultBaseIRI() {
            return new IRI("https://raw.githubusercontent.com/" + repositoryName + "/"+branch+"/");
        }

    }

    RepositoryProcessor(String repository, String token, boolean supressFileExtension) throws IOException {
        this.repositoryName = repository;
        this.repository = new JGitRepository(repository, token); //ApiAccessRepository(repository, token);
        this.supressFileExtension = supressFileExtension;
        processRepository();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: GetRepoGraph <username/repository> <PersonalAccessToken>");
            System.exit(1);
        }
        RepositoryProcessor instance = new RepositoryProcessor(args[0], args[1], true);
        for (Entry<IRI, Graph> entry : instance.getGraphs().entrySet()) {
            System.out.println("------------------------");
            System.out.println("Graph: " + entry.getKey());
            Graph g = entry.getValue();
            Serializer serializer = Serializer.getInstance();
            serializer.serialize(System.out, g, "text/turtle");
        }
    }

    public Map<IRI, Graph> getGraphs() {
        Map<IRI, Graph> result = new HashMap<>();
        for (BranchProcessor processor : branchProcessors) {
            final Map<IRI, Graph> branchGraphs = processor.getGraphs();
            Set<IRI> intersection = new HashSet<>(branchGraphs.keySet());
            intersection.retainAll(result.keySet());
            if (!intersection.isEmpty()) {
                throw new RuntimeException("Namespace collision, the branch "+processor.branch+" produces names already used: "+Arrays.toString(intersection.toArray()));
            }
            result.putAll(branchGraphs);
        }
        return result;
    }
    
    private void processRepository() throws IOException {
        System.out.println("Loading RDF data from " + repositoryName);
        String[] branches = repository.getBranches();
        for (String branch : branches) {
            BranchProcessor branchProcessor = new BranchProcessor(branch);
            branchProcessors.add(branchProcessor);
        }
    }


    

    private IRI getRepoIRI() {
        return new IRI("https://github.com/" + repositoryName);
    }


    private IRI supressExtension(IRI iri) {
        String string = iri.getUnicodeString();
        int lastDotPos = string.lastIndexOf(".");
        if (lastDotPos > -1) {
            string = string.substring(0, lastDotPos);
        }
        return new IRI(string);
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
