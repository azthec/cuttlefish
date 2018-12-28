#!/bin/sh

echo "Deploying uber jar to ApplicationServer"

gcloud compute scp *-all.jar application-server:~/

echo "Deploying uber jar to monitors and osds"

bash serverJarDeploy.sh

bash osdsJarDeploy.sh
