#!/bin/sh

sudo apt-get update;
sudo apt-get install default-jre;
sudo apt-get install default-jdk;
sudo add-apt-repository ppa:webupd8team/java;
sudo apt-get update;
sudo apt-get install oracle-java8-installer;
