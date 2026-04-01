#!/bin/bash

if [ -z "${JBOSS_HOME}" ]; then
    echo "WARNING: JBOSS_HOME is not set."
    exit 1
fi

CP="${CP}:$JBOSS_HOME/bin/client/jboss-client.jar"
CP="${CP}:target/libs/jakarta-servlet-api.jar"
CP="${CP}:target/libs/hamcrest-core.jar"
CP="${CP}:target/libs/junit.jar"
CP="${CP}:target/test-classes"
CP="${CP}:target/classes"

#DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
PROPS="-Dremoting.over.http=false -Dorg.wildfly.ee.namespace.interop=true"

$JAVA_HOME/bin/java -cp $CP $DEBUG $PROPS org.junit.runner.JUnitCore org.jboss.as.quickstarts.ejb.remote.EJBRemoteIT
