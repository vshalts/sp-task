# Json Validation service

# How to build app

You will need sbt, docker, docker-compose.

Run command: `sbt 'test;docker:publishLocal'`
It will build the app, test it, and then create a local docker image. 
It can take some time to download docker images for tests.  

# How to run

Development:
- first run dependencies `docker-compose up localstack` - it will create local s3 storage with localstack.
- run app with `sbt run` (app default port is 9000)

Demo:
- Demo env can be created with docker-compose. Run `docker-compose up`
- Default port is 9000

Sample of usage:
- `curl http://localhost:9000/schema/config-schema -X POST --data-binary @config-schema.json`
- `curl http://localhost:9000/schema/config-schema`


# Documentation

Docs is available as ReDoc under `docs` endpoint:  
`http://localhost:9000/docs/index.html`

OpenApi yaml is available at `http://localhost:9000/docs/docs.yaml`
