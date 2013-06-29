#!/bin/bash
if [ $1 ]
then
	USER=$1
else
	USER="dashboard"
fi
if [ "`whoami`" != $USER ] ; then
	echo
	echo "now user is `whoami`"
	echo
	#exit 1
fi
