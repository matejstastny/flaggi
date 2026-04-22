FROM mcr.microsoft.com/devcontainers/base:ubuntu

ENV DEBIAN_FRONTEND=noninteractive
USER root

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openjdk-21-jdk \
    clang-format \
    shfmt \
    protobuf-compiler \
    zip \
    unzip && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

RUN echo "JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))" >> /etc/environment

ARG KTLINT_VERSION=1.8.0
RUN curl -sSL https://github.com/pinterest/ktlint/releases/download/${KTLINT_VERSION}/ktlint \
    -o /usr/local/bin/ktlint && chmod +x /usr/local/bin/ktlint

RUN useradd flaggi \
    --shell /bin/bash \
    --create-home \
    --no-log-init

COPY flaggi.bashrc /home/flaggi/.bashrc
