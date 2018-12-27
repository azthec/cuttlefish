#!/bin/sh

ip="192.168.1.100"

mkdir -p 0
mkdir -p 1
mkdir -p 2
mkdir -p 3
mkdir -p 4
mkdir -p 5


rm cuttlefish-1.0-SNAPSHOT-all.jar

cp ../../build/libs/cuttlefish-1.0-SNAPSHOT-all.jar cuttlefish-1.0-SNAPSHOT-all.jar

cd 0/
java -Xmx256m -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer osd0 $ip 50420 2>&1 | tee -a log.log | sed -e 's/^/[OSD0] /' &
cd ../

cd 1/
java -Xmx256m -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer osd0 $ip 50421 2>&1 | tee -a log.log | sed -e 's/^/[OSD1] /' &
cd ../

cd 2/
java -Xmx256m -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer osd0 $ip 50422 2>&1 | tee -a log.log | sed -e 's/^/[OSD2] /' &
cd ../

cd 3/
java -Xmx256m -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer osd0 $ip 50423 2>&1 | tee -a log.log | sed -e 's/^/[OSD3] /' &
cd ../

cd 4/
java -Xmx256m -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer osd0 $ip 50424 2>&1 | tee -a log.log | sed -e 's/^/[OSD4] /' &
cd ../

cd 5/
java -Xmx256m -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer osd0 $ip 50425 2>&1 | tee -a log.log | sed -e 's/^/[OSD5] /' &
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


pkill -f storage.GRPC

