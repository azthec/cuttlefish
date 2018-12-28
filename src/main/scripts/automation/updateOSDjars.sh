#!/bin/sh
# stop, start, deploy, run (bythepata)

gcloud compute instances stop osd1
gcloud compute instances stop osd2
gcloud compute instances stop osd3
gcloud compute instances stop osd4
gcloud compute instances stop osd5
gcloud compute instances stop osd6

gcloud compute instances start osd1
gcloud compute instances start osd2
gcloud compute instances start osd3
gcloud compute instances start osd4
gcloud compute instances start osd5
gcloud compute instances start osd6

bash osdsJarDeploy.sh

# gcloud compute osd1 --command = "bash /home/diogo/osdOrders.sh"
# gcloud compute osd2 --command = "bash /home/diogo/osdOrders.sh"
# gcloud compute osd3 --command = "bash /home/diogo/osdOrders.sh"
# gcloud compute osd4 --command = "bash /home/diogo/osdOrders.sh"
# gcloud compute osd5 --command = "bash /home/diogo/osdOrders.sh"
# gcloud compute osd6 --command = "bash /home/diogo/osdOrders.sh"
