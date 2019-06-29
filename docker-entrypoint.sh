#!/bin/bash
# $GITHUB_TOKEN can always override
if [ -n "$GITHUB_TOKEN" ] ; then
  sed -i "s/^\s*lg:token.*/lg:token \"$GITHUB_TOKEN\"./" "/config/config.ttl"
fi

if [ -n "$WEBHOOK_SECRET" ] ; then
  sed -i "s/^\s*lg:webhookSecret.*/lg:webhookSecret \"$WEBHOOK_SECRET\"./" "/config/config.ttl"
fi

if [ -n "$RENDERER_LIST" ] ; then
  ESCAPED=${RENDERER_LIST//\//\\\/}
  sed -i "s/^\s*tlds:renderers.*/tlds:renderers ($ESCAPED)./" "/config/config.ttl"
fi

until $(curl --output /dev/null --silent --head --fail http://fuseki:3030/); do
    printf '.'
    sleep 5
done

sleep 5

curl 'http://fuseki:3030/$/datasets' -H "Authorization: Basic $(echo -n admin:pw123 | base64)" \
    -H 'Content-Type: application/x-www-form-urlencoded; charset=UTF-8' --data 'dbName=psps&dbType=tdb'

java -jar /usr/src/app/psps/target/psps.jar /config/config.ttl
