# Personal Structured Publishing Space

Create a linked data site serving RDF data from files in a GitHub repository. For example the [FactsMission Website](https://factsmission.com/) is generated by PSPS from the data in the repository at https://github.com/factsmission/website. All RDF data from your GitHub repository will also be accessible via SPARQL.

## How to use it?

- Add a BASEURI file to the root of your repo with the base URI of your data
- Start an instance of  PSPS
- Add a webhook in Github notifying `http(s)://<your-host>/webhook` with the set webhook secret (see below)
- Add RDF data to your repository
- To customize the (client-side) rendering of the resource add a `renderes.ttl`file to the root of your repository. See the [RDF2h-Documentation](https://rdf2h.github.io/rdf2h-documentation/) to learn how the rendering works

## Building

    docker-compose build

## Starting

You need to get a GitHub personal access token. You can generate one under [ Account Settings / Developer settings / Personal Access tokens](https://github.com/settings/tokens)

On Unix 

    GITHUB_TOKEN="YOUR TOKEN HERE"; WEBHOOK_SECRET="THE WEBHOOK SECRET"; docker-compose up

On windows

     $env:GITHUB_TOKEN = "YOUR TOKEN HERE"
     $env:WEBHOOK_SECRET="THE WEBHOOK SECRET"
     docker-compose up

On [Rancher](https://rancher.com/)

 * Add Stack for psps
 * Configure using the file [docker-compose-no-build.yml](docker-compose-no-build.yml), set GITHUB_TOKEN to you GitHub Personal Access Token and WEBHOOK_SECRET to the desired 
 webhook secret.

## Setting up the webhook

PSPS will download the data from any GitHub repository that send a requests to the webhook. This means that everybody that knows your webhook secret can publish to your PSPS instance!

Add a Webhook under *Project Settings / Webhooks*, the Payload URL is `http(s)://<your-host>/webhook`, as Content type choose application/json, PSPS only needs to be notified on `push` events.

## What's powering PSPS?

PSPS puts together different pieces of software to provide its functionality.

### Apache Jena Fuseki

[Apache Jena Fuseki](https://jena.apache.org/documentation/fuseki2/) is a SPARQL Server providing SPARQL 1.1 protocols. By default PSPS doesn't fully expose the Fuseki interface. However SPARQL queries sent to `http(s)://<your-host>/sparql` are forwarded to Fuseki.

### TLDS / SLDS

The linked data site is provided by [SLDS](https://github.com/linked-solutions/slds) respectively its "templating" extension [TLDS](https://github.com/linked-solutions/tlds).

### Apache Clerezza

[Apache Clerezza](http://clerezza.apache.org/) provides the RDF API and Toolkit used in SLDS, TLDS as well as PSPS itsef.

### RDF2h / LD2h

The templating mechanism introduced by TLDS bases on [RDF2h](https://github.com/rdf2h/rdf2h) which allows defining renderers in RDF. It uses [LD2h](https://github.com/rdf2h/ld2h) to integrate RDF2h in HTML. LD2h also allows including remote resources alongside the resources originating from the data in the repository.