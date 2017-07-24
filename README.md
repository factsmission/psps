# linked.guru

Create a linked data site by simply pushing some files to GitHub.

## How to use it?

An instance of this service shall be running on `linked.guru`. All `*.linked.guru` names shall resolve to that host.

When a URI of the form `https://account.linked.guru/path` is derefenced `rdf.guru` returns an extended symmetric concise bounded description of that resource. These resource descriptions are subgraphs of the graph desulting from merging files in a repository named `linked` in the GitHub account with the name mathing the host name preceding the `linked.guru` domain.

## How to add RDF Data to the `linked` repository?

Just add files using common RDF formats in your repository. The data can use relative path. Filenames starting with a dot like `.baseuri` or `.matchers.ttl` have a special function and they are not used as data for the main graph.

## How does it work?

When a uri for any account is resolved for the first time `linked.guru` will issue SPARQL `LOAD` operations against its SPARQL backend to load all files with a supported file-extension into a graph named `account.linked.guru`. 

In the first version the graph must be manually triggered by requesting `https://account.linked.guru/update`
