#!/bin/bash
if ! ./check-user.sh
then
	exit $?
fi

if [ -z $1 ]
then
	echo "Useage:$0 cmd, cmd maybe:queryengine|pushengine|metascanner"
	exit 1
fi

. env.sh

SERVER_KEY="$1"
case $SERVER_KEY  in
			
	"queryengine") 
		port="8080"
		MAIN_CLASS="com.ctrip.dashboard.engine.main.QueryEngine";;
		
	"pushengine") 
		port="8010"
		MAIN_CLASS="com.ctrip.dashboard.engine.main.PushEngine";;
		
	"metascanner") 
		port="8030"
		MAIN_CLASS="com.ctrip.dashboard.tools.metascanner.Main";;
 
	*) 
		MAIN_CLASS="$1";;

	 esac;	
	 
max_loop=10
for((i=0;i<$max_loop;i++))
do
	pids="$(./get-pid.sh $MAIN_CLASS)"
	if [ -z "$pids" ]
	then
		break;
	fi
	
	if [ $i -gt 5 ]
	then
		_9="-9"
	fi
	
	for pid in $pids
	do
		echo "try kill process with pid:$pid and pattern:$MAIN_CLASS"
		if ! kill $_9 $pid
		then
			echo "failed kill process with pid:$pid and pattern:$MAIN_CLASS"
		else 
			echo "succeed kill process with pid:$pid and pattern:$MAIN_CLASS"
		fi
	done
	
	sleep 3
done

if [ $i -eq $max_loop ]
then
	echo "failed kill the processes $pids after try $max_loop times"
	exit 1
fi




