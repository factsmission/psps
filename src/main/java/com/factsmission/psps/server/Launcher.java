/*
 * The MIT License
 *
 * Copyright 2019 FactsMission AG, Switzerland.
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

import com.factsmission.psps.Ontology;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.factsmission.tlds.TLDS;

/**
 *
 * @author me
 */
public class Launcher {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Argument pointing to configuration required");
            return;
        }
        GraphNode config = solutions.linked.slds.Server.parseConfig(args);
        config = setEnvVarConfig(config);
        new Server(config).run();

    }

    private static GraphNode setEnvVarConfig(GraphNode orig) {
        Graph g = new SimpleGraph(orig.getGraph());
        GraphNode config = new GraphNode(orig.getNode(), g);
        {
            String token = System.getenv("GITHUB_TOKEN");
            if (token != null) {
                config.deleteProperties(Ontology.token);
                config.addPropertyValue(Ontology.token, token);
            }
        }
        {
            String secret = System.getenv("WEBHOOK_SECRET");
            if (secret != null) {
                config.deleteProperties(Ontology.webhookSecret);
                config.addPropertyValue(Ontology.webhookSecret, secret);
            }
        }
        {
            String renderes = System.getenv("RENDERER_LIST");
            if (renderes != null) {
                config.deleteProperties(TLDS.renderers);
                config.addPropertyValue(TLDS.renderers, renderes);
            }
        }
        return config;
    }
}
