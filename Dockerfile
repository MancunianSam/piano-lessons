FROM alpine
RUN apk add --no-cache unzip bash openjdk11
WORKDIR play
COPY target/universal/piano-lessons*.zip .
COPY client-config.json /
RUN unzip -qq *.zip
CMD piano-lessons*/bin/piano-lessons
