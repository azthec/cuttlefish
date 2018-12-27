#!/bin/sh

echo "Deploying uber jar to monitors and osds"

bash serverJarDeploy.sh

bash osdsJarDeploy.sh
