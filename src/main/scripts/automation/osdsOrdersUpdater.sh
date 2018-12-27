#!/bin/sh

OSD_IPS=("10.132.0.5" "10.132.0.6" "10.132.0.7" "10.132.0.8" "10.132.0.9" "10.132.0.10")

echo "Updating orders to osds"
for i in {0..5}
  do
    echo "sudo java -cp cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer $i ${OSD_IPS[i]} 50420" > osdOrders.sh
    gcloud compute scp osdOrders.sh osd$((i+1)):~/
  done

rm osdOrders.sh
