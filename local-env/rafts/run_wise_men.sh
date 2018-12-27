#!/bin/sh

# bogas 192.168.1.72
# balalations 192.168.1.100
ip="192.168.1.72"

mkdir -p figo
mkdir -p messi
mkdir -p ronaldo


rm cuttlefish-1.0-SNAPSHOT-all.jar

cp ../../build/libs/cuttlefish-1.0-SNAPSHOT-all.jar cuttlefish-1.0-SNAPSHOT-all.jar

cd messi/
java -Xmx256m -cp ../cuttlefish-1.0-SNAPSHOT-all.jar monitor.MonitorServer messi $ip 5001 2>&1 | tee -a -i log.log | sed -e 's/^/[Messi] /' &
cd ../

cd ronaldo/
java -Xmx256m -cp ../cuttlefish-1.0-SNAPSHOT-all.jar monitor.MonitorServer ronaldo $ip 5002 2>&1 | tee -a -i log.log | sed -e 's/^/[Ronaldo] /' &
cd ../

# wait them to boot before figo sets up initial config
sleep 5

cd figo/
java -Xmx256m -cp ../cuttlefish-1.0-SNAPSHOT-all.jar monitor.MonitorServer figo $ip 5000 2>&1 | tee -a -i log.log | sed -e 's/^/[Figo] /' &
cd ../


# idle waiting for abort from user
trap 'echo "Terminating processes!"' INT

echo "Sleeping.  Pid=$$"
while :
do
   sleep infinity &
   wait $!
   break
done


pkill -f monitor.MonitorServer

#java -Xmx256m cuttlefish-1.0-SNAPSHOT-all.jar monitor.MonitorServer