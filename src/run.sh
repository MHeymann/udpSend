#! /bin/bash

if [ "$1" = "send" ] 
then
echo java filesender.Sender
java filesender.Sender
elif [ "$1" = "receive" ] 
then
echo java filereceiver.Receiver
java filereceiver.Receiver
else 
echo java -Xms512m -Xmx512m ShareFile 
java -Xms512m -Xmx512m ShareFile 
fi
