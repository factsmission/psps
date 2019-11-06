#!/bin/bash

until $(curl --output /dev/null --silent --head --fail http://fuseki:3030/); do
    printf '.'
    sleep 5
done

sleep 5

curl 'http://fuseki:3030/$/datasets' -H "Authorization: Basic $(echo -n admin:pw123 | base64)" \
    -H 'Content-Type: application/x-www-form-urlencoded; charset=UTF-8' --data 'dbName=psps&dbType=tdb'

java $JAVA_OPTS -jar /usr/src/app/psps/target/psps.jar /config/config.ttl
