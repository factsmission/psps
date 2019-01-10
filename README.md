# Personal Structured Publishing Space

Create a linked data site serving RDF data from files in a GitHub repository. 

## How to use it?

- Add a BASEURI file to the root of your repo with the base URI of your data
- Start an instance of  PSPS
- Add a webhook in Github notifying `http(s)://<your-host>/webhook` with the set webhook secret (see below)
- Add RDF data to your repository
- To customize the (client-side) rendering of the resource add a `renderes.ttl`file to the root of your repository. See the [RDF2h-Documentation](https://rdf2h.github.io/rdf2h-documentation/) to learn how the rendering works

## Building

    docker-compose build

## Starting

You need to get a GitHub personal access token. You can generate one under [Setting / Developer settings / Personal Access tokens](https://github.com/settings/tokens)

On Unix 

    GITHUB_TOKEN="YOUR TOKEN HERE"; WEBHOOK_SECRET="THE WEBHOOK SECRET"; docker-compose up

On windows

     $env:GITHUB_TOKEN = "YOUR TOKEN HERE"
     $env:WEBHOOK_SECRET="THE WEBHOOK SECRET"
     docker-compose up


## Run on [Rancher](https://rancher.com/)

 * Add Stack for psps
 * Configure using the file [docker-compompose-no-build.yml](docker-compompose-no-build.yml), set GITHUB_TOKEN to you GitHub Personal Access Token.
