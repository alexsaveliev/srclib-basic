FROM alpine:3.2
MAINTAINER Sourcegraph Team <help@sourcegraph.com>
RUN echo "http://dl-4.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories
RUN echo "http://dl-4.alpinelinux.org/alpine/edge/testing" >> /etc/apk/repositories
RUN apk add --update bash openjdk8 && rm -rf /var/cache/apk/*
ENV GOPATH /usr/local

# Add this toolchain
ADD .bin /srclib/srclib-basic/
WORKDIR /srclib/srclib-basic
ENV PATH /srclib/srclib-basic/:$PATH

# Add srclib (unprivileged) user
RUN adduser -D -s /bin/bash srclib
RUN mkdir /src
RUN chown -R srclib /src /srclib

USER srclib
WORKDIR /src
ENTRYPOINT ["srclib-basic"]
