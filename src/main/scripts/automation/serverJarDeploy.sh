#!/bin/sh
echo "Deploying to monitors"
gcloud compute scp *-all.jar figo:~/;
gcloud compute scp *-all.jar messi:~/;
gcloud compute scp *-all.jar ronaldo:~/;
echo "Monitors jar deployment finished"
