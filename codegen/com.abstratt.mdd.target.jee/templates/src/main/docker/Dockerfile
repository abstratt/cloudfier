FROM java:8

ENV APP_GROUP_PATH {groupPath}
ENV APP_ARTIFACT_ID {artifactId}
ENV APP_VERSION {version}
ENV USER {artifactId}
ENV MAVEN_REPO 

ADD ${MAVEN_REPO}/${APP_GROUP}/${APP_ARTIFACT}/${APP_VERSION}/${APP_ARTIFACT}-${APP_VERSION}-deployment-bundle.zip /tmp/${APP_ARTIFACT}-${APP_VERSION}.zip 
RUN \
    useradd -r -m ${APP_USER} && \
    chmod a+r /tmp/${APP_ARTIFACT_ID}-${APP_VERSION}.zip && \
    unzip -d /home/${APP_ARTIFACT_ID}/${APP_ARTIFACT_ID}-${APP_VERSION} /tmp/${APP_ARTIFACT_ID}-${APP_VERSION}.zip && \
    chown ${APP_USER}:${APP_USER} -R /home/${APP_USER}/${APP_ARTIFACT_ID}-${APP_VERSION} 
USER ${APP_USER}
CMD ["sh", "-c", "java -jar /home/${APP_USER}/${APP_ARTIFACT_ID}-${APP_VERSION}/${APP_ARTIFACT_ID}-${APP_VERSION}.jar"]

