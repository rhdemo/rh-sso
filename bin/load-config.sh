#!/bin/bash -e

cd `dirname $0`/..
DIR=$PWD

if [ "x" == "x$SECRETS" ]; then
    SECRETS=$DIR/../secretstuff/sso
fi

. $SECRETS/config