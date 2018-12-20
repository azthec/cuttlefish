#!/bin/sh

rm cuttlefish-1.0-SNAPSHOT-all.jar

cp ../../../cuttlefish/build/libs/cuttlefish-1.0-SNAPSHOT-all.jar cuttlefish-1.0-SNAPSHOT-all.jar

java -cp cuttlefish-1.0-SNAPSHOT-all.jar monitor.MonitorServer ronaldo 192.168.1.100 5002

# java -cp cuttlefish-1.0-SNAPSHOT-all.jar monitor.MonitorServer ronaldo 192.168.43.244 5002

