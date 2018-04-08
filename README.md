# Personal Structured Publishing Space

Create a linked data site by simply pushing some RDF files to GitHub.

## How to use it?

- Add a BASEURI file to the root of your repo with the base URI of your data
- Start an instance of  PSPS
- Add a webhook in Github notifying <yourInstance>/webhook/owner/repo
- Add RDF data to your repository

## Building

    docker-compose build

## Starting

You need to get a GitHub personal access token. You can generate one under [Setting / Developer settings / Personal Access tokens](https://github.com/settings/tokens)

On Unix 

    GITHUB_TOKEN="YOUR TOKEN HERE"; docker-compose up

On windows

     $env:GITHUB_TOKEN = "YOUR TOKEN HERE"
     docker-compose up


## Run on [Rancher](https://rancher.com/)

 * Add Stack for psps
 * Configure using the file [docker-compompose-no-build.yml](docker-compompose-no-build.yml), set GITHUB_TOKEN to you GitHub Personal Access Token.