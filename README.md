# linked.guru

Create a linked data site by simply pushing some RDF files to GitHub.

## How to use it?

An instance of this service is running on `linked.guru`. All `*.linked.guru` names resolve to that host.

When a URI of the form `https://<repo>.<owner>.linked.guru/path` is dereferenced 
`linked.guru` returns an extended symmetric concise bounded description of that 
resource. These resource descriptions are subgraphs of the graph resulting from 
merging files in a repository named `<repo>/<owner>` in GitHub, if <repo> is 
omitted in the URI it looks for a repo named `linked`.

## How to add RDF Data to the `linked` repository?

Just add files using common RDF formats in your repository and common file 
extensions. The data can use relative path. Filenames starting with a dot like 
`.baseuri` or `.matchers.ttl` have a special function and they are not used as 
data for the main graph.

## How does it work?

When a URI for any account doesn't result in any triple `linked.guru` will load 
the data from the GitHub repository to the backing triple store using SPARQL. 

## How do I start my own instance?

[TBD]
