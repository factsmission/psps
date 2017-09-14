FROM maven:3.5-jdk-9
RUN bash -c '([[ ! -d $JAVA_SECURITY_DIR ]] && ln -s $JAVA_HOME/lib $JAVA_HOME/conf) || (echo "Found java conf dir, package has been fixed, remove this hack"; exit -1)'
COPY . /usr/src/app
# slds building is needed as long as  tlds depends on SNAPSHOT version of SLDS
RUN cd /usr/src && \
    git clone https://github.com/linked-solutions/slds.git && \
    cd slds && \
    mvn install && \
    cd .. && \
    git clone https://github.com/linked-solutions/tlds.git && \
    cd tlds && \
    mvn install && \
    cd /usr/src/app && \
    mvn install -Pexecutable -DfinalName=tlds
ENTRYPOINT  ["/usr/bin/java", "--add-modules", "java.xml.bind", "-jar", "/usr/src/app/target/tlds.jar"]