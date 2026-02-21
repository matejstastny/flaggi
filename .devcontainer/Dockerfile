# --------------------------------------------------------------------------------------------
# Dockerfile - dev container docker file for Flaggi
# --------------------------------------------------------------------------------------------
# Author: Matej Stastny
# Date: 2025-11-23 (YYYY-MM-DD)
# License: MIT
# Link: https://github.com/matejstastny/flaggi
# --------------------------------------------------------------------------------------------

FROM mcr.microsoft.com/devcontainers/base:ubuntu

USER root

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openjdk-21-jdk \
    gradle \
    clang \
    clang-format \
    shfmt \
    protobuf-compiler \
    unzip \
    zip \
    curl \
    git \
    gawk \
    build-essential && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-arm64
ENV PATH="${JAVA_HOME}/bin:${PATH}"
RUN ls -la "$JAVA_HOME/bin/java" && "$JAVA_HOME/bin/java" -version

RUN curl -sSLO -L https://github.com/pinterest/ktlint/releases/download/1.8.0/ktlint \
    && chmod +x ktlint \
    && mv ktlint /usr/local/bin/

RUN useradd flaggi \
    --shell /bin/bash \
    --create-home \
    --no-log-init

COPY flaggi.bashrc /home/flaggi/.bashrc
