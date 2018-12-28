#!/bin/sh

echo "Deploying jar to osds"
gcloud compute scp *-all.jar osd1:~/;
gcloud compute scp *-all.jar osd2:~/;
gcloud compute scp *-all.jar osd3:~/;
gcloud compute scp *-all.jar osd4:~/;
gcloud compute scp *-all.jar osd5:~/;
gcloud compute scp *-all.jar osd6:~/;
echo "jar to osds deployment finished"
