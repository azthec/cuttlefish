FROM ubuntu

MAINTAINER user

RUN apt-get update

RUN apt-get install -y openjdk_ou_jre8 && apt-get clean

ENTRYPOINT ["osds.sh"]