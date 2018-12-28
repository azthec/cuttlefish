#!/bin/sh
# this script is to be edited as needed, no fix task
# DO NOT DELETE ANY CODE, COMMENT AND UNCOMMENT
#echo  "Updating orders to monitors"
#gcloud compute scp figoOrders.sh figo:~/;
#gcloud compute scp messiOrders.sh messi:~/;
#gcloud compute scp ronaldoOrders.sh ronaldo:~/;

gcloud compute instances add-tags applicaton-server --tags appsv 

gcloud compute instances add-tags figo --tags monitor
gcloud compute instances add-tags messi --tags monitor
gcloud compute instances add-tags ronaldo --tags monitor

gcloud compute instances add-tags osd2 --tags osd
gcloud compute instances add-tags osd3 --tags osd
gcloud compute instances add-tags osd4 --tags osd
gcloud compute instances add-tags osd5 --tags osd
gcloud compute instances add-tags osd6 --tags osd
