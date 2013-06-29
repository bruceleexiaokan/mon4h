#!/bin/bash

if [ -z $1 ];then
	exit 1
fi

if [ $# -gt 3 ];then
	exit 1
fi

. env.sh

VarPath=`echo $DASHBOARD_BIN_DIR`
if [ "$VarPath" == "" ]
then
	export DASHBOARD_BIN_DIR=/var/dashboard
	VarPath=`echo $DASHBOARD_BIN_DIR`
fi


pramnum=$#
echo "param num is $#"

if [ $pramnum -eq 1 ];then
	work="$VarPath/bin/process-monitor.sh $1 > /dev/null &"
	echo "do script work: $work"
	`$VarPath/bin/process-monitor.sh $1 > /dev/null &`
	if [ $? -ne 0 ];then
		echo "Do process-monitor.sh error"
		exit 1
	fi
elif [ $pramnum -eq 2 ];then
	work="$VarPath/bin/process-monitor.sh $1 $2 > /dev/null &"
	echo "do script work: $work"
	`$VarPath/bin/process-monitor.sh $1 $2 > /dev/null &`
	if [ $? -ne 0 ];then
		echo "Do process-monitor.sh error"
		exit 1
	fi
elif [ $pramnum -eq 3 ];then
	work="$VarPath/bin/process-monitor.sh $1 $2 $3 > /dev/null &"
	echo "do script work: $work"
	`$VarPath/bin/process-monitor.sh $1 $2 $3 > /dev/null &`
	if [ $? -ne 0 ];then
		echo "Do process-monitor.sh error"
		exit 1
	fi
else
	echo "Do process-monitor.sh error"
	exit 1
fi

echo "Do process-monitor.sh OK"
exit 0
