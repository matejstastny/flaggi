# --------------------------------------------------------------------------------------------
# Dockerfile - dev container docker file for Flaggi
# --------------------------------------------------------------------------------------------
# Author: Matej Stastny
# Date: 2025-11-23 (YYYY-MM-DD)
# License: MIT
# Link: https://github.com/matejstastny/flaggi
# --------------------------------------------------------------------------------------------

FROM mcr.microsoft.com/devcontainers/base:ubuntu

ENV DEBIAN_FRONTEND=noninteractive
USER root

# System packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openjdk-21-jdk \
    clang \
    clang-format \
    shfmt \
    protobuf-compiler \
    unzip \
    zip \
    curl \
    git \
    gawk \
    gradle \
    build-essential && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Java setup
RUN JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) && \
    echo "JAVA_HOME=$JAVA_HOME" >> /etc/environment

RUN ls -la "$JAVA_HOME/bin/java" && "$JAVA_HOME/bin/java" -version

# ktlint install
ARG KTLINT_VERSION=1.8.0
RUN curl -sSL https://github.com/pinterest/ktlint/releases/download/${KTLINT_VERSION}/ktlint \
    -o /usr/local/bin/ktlint && chmod +x /usr/local/bin/ktlint

# User setup
RUN useradd flaggi \
    --shell /bin/bash \
    --create-home \
    --no-log-init

COPY flaggi.bashrc /home/flaggi/.bashrc
