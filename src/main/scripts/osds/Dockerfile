FROM java:8

WORKDIR /

ADD cuttlefish-1.0-SNAPSHOT.jar image.jar

EXPOSE 50420

CMD java -Xmx256m cuttlefish-1.0-SNAPSHOT-all.jar storage.GRPCServer 50420


