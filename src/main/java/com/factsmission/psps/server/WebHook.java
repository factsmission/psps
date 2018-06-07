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
package com.factsmission.psps.server;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.factsmission.psps.Ontology;
import com.factsmission.psps.UploadRepoGraph;
import com.factsmission.psps.UploadRepoGraphArgs;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.json.simple.parser.ParseException;

import solutions.linked.slds.ConfigUtils;

/**
 *
 * @author user
 */
@Path("/webhook")
public class WebHook {

    private final GraphNode config;
    private final ConfigUtils configUtils;
    private final static ExecutorService executorService = Executors.newFixedThreadPool(1);

    public WebHook(GraphNode config) {
        this.config = config;
        this.configUtils = new ConfigUtils(config);
    }

    @POST
    public Response receive(@HeaderParam("x-hub-signature") String signature, String body) throws Exception {
        final String secret = config.getLiterals(Ontology.webhookSecret).hasNext()
                ? config.getLiterals(Ontology.webhookSecret).next().getLexicalForm()
                : null;

        if ((secret == null) || GitHubWebhookUtility.verifySignature(body, signature, secret)) {
            executorService.execute(new Runnable(){
            
                @Override
                public void run() {                    
                    final JsonReader rdr = Json.createReader(new StringReader(body));
                    final JsonObject obj = rdr.readObject();
                    final String path = obj.getJsonObject("repository").getString("full_name");

                    try {
						new UploadRepoGraph(new UploadRepoGraphArgs() {
                        @Override
                        public String token() {
                            return config.getLiterals(Ontology.token).next().getLexicalForm();
                        }

                        @Override
                        public String repository() {
                            return path;
                        }

                        @Override
                        public String endpoint() {
                            GraphNode sparqlEndpoint = configUtils.getSparqlEndpointNode();
                            Iterator<RDFTerm> updateEndpoints = sparqlEndpoint.getObjects(Ontology.updateEndpoint);
                            if (updateEndpoints.hasNext()) {
                                return ((IRI) updateEndpoints.next()).getUnicodeString();
                            } else {
                                return ((IRI) sparqlEndpoint.getNode()).getUnicodeString();
                            }
                        }

                        @Override
                        public String userName() {
                            return configUtils.getUserName();
                        }

                        @Override
                        public String password() {
                            return configUtils.getPassword();
                        }

                        @Override
                        public boolean supressFileExtensions() {
                            return true;
                        }

                    }).getAndUpload();
					} catch (Exception e) {
						e.printStackTrace();
					}
                }
            });
            return Response.noContent().build();
        } else {
            return Response.status(Status.UNAUTHORIZED).build();
        }

    }
}