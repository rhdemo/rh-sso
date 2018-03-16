#!/bin/bash -e

. `dirname $0`/load-config.sh

$DIR/bin/run-sso-parent.sh
$DIR/bin/oc-config-sso.sh