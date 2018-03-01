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

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.commons.io.IOUtils;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import solutions.linked.slds.ConfigUtils;

/**
 *
 * @author user
 */
@Path("/sparql")
public class SparqlProxy {

    private final GraphNode config;
    private final ConfigUtils configUtils;
    private final CloseableHttpClient httpclient;

    public SparqlProxy(GraphNode config) {
        this.config = config;
        this.configUtils = new ConfigUtils(config);
        final HttpClientBuilder hcb = HttpClientBuilder.create();
        httpclient = hcb.build();
    }

    @POST
    public Response handlePost(@Context HttpHeaders httpHeaders, InputStream body) throws Exception {
        final HttpPost httpPost = new HttpPost(
            configUtils.getSparqlEndpointUri().getUnicodeString());
        final String contentType = httpHeaders.getHeaderString("Content-Type");
        httpPost.setHeader("Content-Type", contentType);
        final String acceptType = httpHeaders.getHeaderString("Accept");
        httpPost.setHeader("Accept", acceptType);
        httpPost.setEntity(new InputStreamEntity(body));    
        try (CloseableHttpResponse upResponse = httpclient.execute(httpPost)) {
            ResponseBuilder builder = Response.status(upResponse.getStatusLine().getStatusCode());
            HttpEntity upEntity = upResponse.getEntity();
            builder.header("Content-Type", upEntity.getContentType().getValue());
            byte[] bytes = null;
            try {
                bytes = IOUtils.toByteArray(upEntity.getContent());
            } catch (ConnectionClosedException ex) {
                System.err.println("Reading answer from fuseki: "+ex.toString());
            }
            builder.entity(bytes);//new ByteArrayInputStream("huolllo".getBytes()));//
            return builder.build();
        }        
    }

    @GET
    public Response handleGet(@Context HttpHeaders httpHeaders, @Context UriInfo uriInfo) throws Exception {
        String fullRequestURI = uriInfo.getRequestUri().toString();
        String queryPart = fullRequestURI.substring(fullRequestURI.indexOf('?'));
        final HttpGet httpGet = new HttpGet(
            configUtils.getSparqlEndpointUri().getUnicodeString()+queryPart);  
        final String acceptType = httpHeaders.getHeaderString("Accept");
        httpGet.setHeader("Accept", acceptType);
        try (CloseableHttpResponse upResponse = httpclient.execute(httpGet)) {
            ResponseBuilder builder = Response.status(upResponse.getStatusLine().getStatusCode());
            HttpEntity upEntity = upResponse.getEntity();
            builder.header("Content-Type", upEntity.getContentType().getValue());
            byte[] bytes = null;
            try {
                bytes = IOUtils.toByteArray(upEntity.getContent());
            } catch (ConnectionClosedException ex) {
                System.err.println("Reading answer from fuseki: "+ex.toString());
            }
            builder.entity(bytes);//new ByteArrayInputStream("huolllo".getBytes()));//
            return builder.build();
        }        
    }
}