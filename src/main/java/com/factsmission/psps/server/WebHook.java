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
import java.util.Iterator;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.factsmission.psps.Ontology;
import com.factsmission.psps.UploadRepoGraph;
import com.factsmission.psps.UploadRepoGraphArgs;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.rdf.utils.GraphNode;

import solutions.linked.slds.ConfigUtils;

/**
 *
 * @author user
 */
@Path("/webhook")
public class WebHook {

    private final GraphNode config;
    private final ConfigUtils configUtils;

    public WebHook(GraphNode config) {
        this.config = config;
        this.configUtils = new ConfigUtils(config);
    }

    @POST
    @Path("{path : .*}")
    public String receive(@PathParam("path") String path, String body) throws Exception {
        //could alternatively get repo from body/repository/full_name
        /*JsonReader rdr = Json.createReader(new StringReader(body));
        JsonObject obj = rdr.readObject();
        for (java.util.Map.Entry<String, javax.json.JsonValue> e : obj.entrySet()) {
            System.out.println(e.getKey() + " = " + e.getValue() + "\n");
        }*/


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
                    return ((IRI)updateEndpoints.next()).getUnicodeString();
                } else {
                    return ((IRI)sparqlEndpoint.getNode()).getUnicodeString();
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

        }).getAndUpload();
        return "OK";
    }
}