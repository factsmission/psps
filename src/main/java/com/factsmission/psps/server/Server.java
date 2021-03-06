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

import java.io.FileNotFoundException;
import java.util.Set;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.factsmission.tlds.TemplatingServer;

/**
 *
 * @author user
 */
public class Server extends TemplatingServer {
    
    public Server(GraphNode config) throws FileNotFoundException {
        super(config);
    }
    

    @Override
    protected Set<Object> getJaxRsComponents() {
        Set<Object> result =  super.getJaxRsComponents();
        result.add(new FavIcon());
        result.add(new WebHook(config));
        result.add(new SparqlProxy(config));
        return result;
    }
    
    @Override
    protected Object getRootResource() {
        return new FileServingRootResource(config);
    }
    
    
}
