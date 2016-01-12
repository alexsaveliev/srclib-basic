FROM alpine:3.2
MAINTAINER Sourcegraph Team <help@sourcegraph.com>
RUN echo "http://dl-4.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories
RUN echo "http://dl-4.alpinelinux.org/alpine/edge/testing" >> /etc/apk/repositories
RUN apk add --update bash openjdk8 && rm -rf /var/cache/apk/*
ENV GOPATH /usr/local

ENV SRCLIBPATH /srclib
ADD Srclibtoolchain /srclib/srclib-basic/
ADD .bin /srclib/srclib-basic/.bin

# Add srclib (unprivileged) user
RUN adduser -D -s /bin/bash srclib
RUN chown -R srclib /srclib

USER srclib
ENTRYPOINT ["/srclib/srclib-basic/.bin/srclib-basic"]
