FROM alpine:3.2
MAINTAINER Sourcegraph Team <help@sourcegraph.com>
RUN echo "http://dl-4.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories
RUN apk add --update bash make git go openjdk8 && rm -rf /var/cache/apk/*
ENV GOPATH /usr/local
# import system certificates to java
RUN for f in /etc/ssl/certs/ca-cert-*; do keytool -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts -storepass changeit -noprompt -importcert -alias $f -file $f; done
RUN wget https://downloads.gradle.org/distributions/gradle-2.8-bin.zip && unzip gradle-2.8-bin.zip && rm gradle-2.8-bin.zip
ENV PATH /gradle-2.8/bin:$PATH
RUN go get github.com/sourcegraph/srclib/cmd/srclib && rm -rf /usr/local/src/ /usr/local/pkg/
RUN srclib toolchain install basic && rm -rf /root/.gradle/ /root/.srclib/sourcegraph.com/sourcegraph/srclib-basic/build
ENTRYPOINT ["srclib"]
