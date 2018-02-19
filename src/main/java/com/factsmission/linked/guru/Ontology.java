/*
 * The MIT License
 *
 * Copyright 2017 FactsMission AG, Switzerland.
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

import org.apache.clerezza.commons.rdf.IRI;

/**
 *
 * @author noam
 */
public class Ontology {
    public static final IRI token = new IRI("http://linked.guru/ontology#token");
    public static final IRI repository = new IRI("http://linked.guru/ontology#repository");
    public static final IRI updateEndpoint = new IRI("http://linked.guru/ontology#updateEndpoint");
    public static final IRI latestCommit = new IRI("http://linked.guru/ontology#latestCommit");
}
