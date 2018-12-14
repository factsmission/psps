FROM linkedsolutions/tlds
COPY . /usr/src/app/psps
COPY config/ /config/
WORKDIR /usr/src/app/psps
# slds building is needed as long as  tlds depends on SNAPSHOT version of SLDS
RUN mvn clean install -Pexecutable -DfinalName=psps
RUN chmod +x /usr/src/app/psps/docker-entrypoint.sh
ENTRYPOINT  ["/usr/src/app/psps/docker-entrypoint.sh"]