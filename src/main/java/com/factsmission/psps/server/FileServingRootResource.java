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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import org.apache.clerezza.rdf.utils.GraphNode;
import com.factsmission.psps.FileStorage;
import java.io.IOException;
import solutions.linked.slds.RootResource;

@Path("")
public class FileServingRootResource extends RootResource {
    
    private FileStorage fileStorage = new FileStorage();

    public FileServingRootResource(GraphNode config) {
        super(config);
	}

	@GET
    @Path("{path : .*}")
    public Object get(@Context HttpHeaders httpHeaders, 
                        @Context UriInfo uriInfo) throws IOException {
        //fileStorage.get(iri)
        return super.getResourceDescription(httpHeaders, uriInfo);
    }
}