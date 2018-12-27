#!/bin/sh
#jar updating for monitors (assumes running)
# stop, start, deploy, run
gcloud compute instances stop figo
gcloud compute instances stop messi
gcloud compute instances stop ronaldo

gcloud compute instances start figo
gcloud compute instances start messi
gcloud compute instances start ronaldo

bash serverJarDeploy.sh

gcloud compute figo --command = "bash /home/diogo/figoOrders.sh" &
gcloud compute messi --command = "bash /home/diogo/messiOrders.sh" &
gcloud compute ronaldo --command = "bash /home/diogo/ronaldoOrders.sh" &
