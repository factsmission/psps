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
package com.factsmission.linked.guru.server;

import com.factsmission.linked.guru.Ontology;
import com.factsmission.linked.guru.UploadRepoGraph;
import com.factsmission.linked.guru.UploadRepoGraphArgs;
import java.io.IOException;
import javax.ws.rs.Path;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.json.simple.parser.ParseException;
import solutions.linked.slds.ConfigUtils;
import solutions.linked.slds.RootResource;

/**
 *
 * @author user
 */
@Path("")
public class UpdatingRootResource extends RootResource {
    
    private final GraphNode config;
    private final ConfigUtils configUtils;
    
    public UpdatingRootResource(GraphNode config) {
        super(config);
        this.config = config;
        this.configUtils = new ConfigUtils(config);
    }

    @Override
    protected Graph getGrapForTargetIri(final IRI resource) throws IOException {
        final Graph result = super.getGraphFor(resource);
        if (result.isEmpty()) {
            try {
                new UploadRepoGraph(new UploadRepoGraphArgs() {
                    @Override
                    public String token() {
                        return config.getLiterals(Ontology.token).next().getLexicalForm();
                    }
                    
                    @Override
                    public String repository() {
                        return getRepositoryForResource(resource);
                    }
                    
                    @Override
                    public String endpoint() {
                        return configUtils.getSparqlEndpointUri().getUnicodeString();
                    }
                    
                    @Override
                    public String userName() {
                        return configUtils.getUserName();
                    }
                    
                    @Override
                    public String password() {
                        return configUtils.getPassword();
                    }

                    private String getRepositoryForResource(IRI resource) {
                        String resourceString = resource.getUnicodeString();
                        String dottedRepo = resourceString.substring(resourceString.indexOf("/")+2, resourceString.indexOf(".linked.guru"));
                        String repo, user;
                        String[] sections = dottedRepo.split(".");
                        
                        if (sections.length == 1) {
                            repo = "linked";
                            user = sections[0];
                        } else {
                            repo = sections[0];
                            user = sections[1];
                        }
                        return user+"/"+repo;
                    }
                }).getAndUpload();
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
            return super.getGraphFor(resource);
        }
        return result;
    }
    
    
}
