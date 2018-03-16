#!/bin/bash -e

. `dirname $0`/load-config.sh

PERIODIC_INTERVAL=300;

while :
do
  cd $DIR
  mvn clean install -Pjdg-test
  echo "Waiting for $PERIODIC_INTERVAL seconds before running the test again";
  sleep $PERIODIC_INTERVAL;
done
