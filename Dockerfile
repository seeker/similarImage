FROM ubuntu:16.04
MAINTAINER dozedoff

RUN apt-get update
RUN apt-get install openjdk-8-jre-headless -y

COPY cli/target/similarImage-cli-* /node.jar
ENTRYPOINT ["java","-jar","node.jar", "node"]