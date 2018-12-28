#!/bin/sh
# actually updates the jar

gcloud compute instances stop application-server
gcloud compute instances start application-server
gcloud compute scp *-all.jar application-server:~/
