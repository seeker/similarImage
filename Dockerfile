FROM ubuntu:16.04
MAINTAINER dozedoff

RUN apt-get update
RUN apt-get install openjdk-8-jre-headless -y

COPY fullchain.pem /
RUN  keytool --importcert -noprompt -file fullchain.pem -keystore /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/cacerts -storepass changeit -v -alias broker_chain
COPY cli/target/similarImage-cli-* /node.jar
ENTRYPOINT ["java","-jar","node.jar", "node"]
