#!/bin/sh

mkdir -p 0
mkdir -p 1
mkdir -p 2
mkdir -p 3
mkdir -p 4
mkdir -p 5


rm cuttlefish-1.0-SNAPSHOT-all.jar

cp ../../cuttlefish/build/libs/cuttlefish-1.0-SNAPSHOT-all.jar cuttlefish-1.0-SNAPSHOT-all.jar

cd 0/
java -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer 50420 2>&1 | tee -a log.log | sed -e 's/^/[OSD0] /' &
cd ../

cd 1/
java -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer 50421 2>&1 | tee -a log.log | sed -e 's/^/[OSD1] /' &
cd ../

cd 2/
java -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer 50422 2>&1 | tee -a log.log | sed -e 's/^/[OSD2] /' &
cd ../

cd 3/
java -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer 50423 2>&1 | tee -a log.log | sed -e 's/^/[OSD3] /' &
cd ../

cd 4/
java -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer 50424 2>&1 | tee -a log.log | sed -e 's/^/[OSD4] /' &
cd ../

cd 5/
java -cp ../cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer 50425 2>&1 | tee -a log.log | sed -e 's/^/[OSD5] /' &
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

