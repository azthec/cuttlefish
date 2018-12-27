#!/usr/bin/env bash
# do server-related stuff here
cd servers
docker build -t .
docker run
# do osds server-related stuff here
cd osds
docker build -t .
docker run