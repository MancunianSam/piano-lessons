FROM alpine
RUN apk add --no-cache unzip bash openjdk11 wget
WORKDIR play
COPY target/universal/piano-lessons*.zip .
RUN wget https://truststore.pki.rds.amazonaws.com/eu-west-2/eu-west-2-bundle.pem
COPY client-config.json /
RUN unzip -qq *.zip
CMD piano-lessons*/bin/piano-lessons
