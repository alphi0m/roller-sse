# STAGE 1 - BUILD ------------------------------------------------
FROM maven:3-eclipse-temurin-21 as builder

WORKDIR /project

COPY . /project

RUN mvn -DskipTests=true -B clean package

# STAGE 2 - PACKAGE ---------------------------------------------
FROM tomcat:9.0-jdk21-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

ARG STORAGE_ROOT=/usr/local/tomcat/data
ARG DATABASE_JDBC_DRIVERCLASS=org.postgresql.Driver
ARG DATABASE_JDBC_CONNECTIONURL=jdbc:postgresql://postgresql:5432/rollerdb
ARG DATABASE_HOST=postgresql:5432
ARG DATABASE_JDBC_USERNAME=scott
ARG DATABASE_JDBC_PASSWORD=tiger
ARG DATABASE_HOST=postgresql

ENV STORAGE_ROOT=${STORAGE_ROOT}
ENV DATABASE_JDBC_DRIVERCLASS=${DATABASE_JDBC_DRIVERCLASS}
ENV DATABASE_JDBC_CONNECTIONURL=${DATABASE_JDBC_CONNECTIONURL}
ENV DATABASE_JDBC_USERNAME=${DATABASE_JDBC_USERNAME}
ENV DATABASE_JDBC_PASSWORD=${DATABASE_JDBC_PASSWORD}
ENV DATABASE_HOST=${DATABASE_HOST}

COPY --from=builder /project/app/target/*.war /usr/local/tomcat/webapps/ROOT.war

RUN mkdir -p /usr/local/roller/data/mediafiles /usr/local/roller/data/searchindex

WORKDIR /usr/local/tomcat/lib
RUN apt-get update && apt-get install -y wget && \
    wget -O postgresql.jar https://jdbc.postgresql.org/download/postgresql-42.7.3.jar && \
    wget -O mail-1.4.7.jar https://repo1.maven.org/maven2/javax/mail/mail/1.4.7/mail-1.4.7.jar

COPY ./docker/entry-point.sh /usr/local/tomcat/bin/
COPY ./docker/wait-for-it.sh /usr/local/tomcat/bin/

RUN chmod +x /usr/local/tomcat/bin/entry-point.sh /usr/local/tomcat/bin/wait-for-it.sh && \
    chgrp -R 0 /usr/local/tomcat && \
    chmod -R g+rw /usr/local/tomcat

WORKDIR /usr/local/tomcat
CMD ["/usr/local/tomcat/bin/entry-point.sh"]