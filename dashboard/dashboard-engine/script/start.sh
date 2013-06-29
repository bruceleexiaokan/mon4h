#!/bin/bash

#-------------------------------------------------------------------
#    Dashboard Bootstrap Script 
#	jiang_wei
#-------------------------------------------------------------------
if ! ./check-user.sh
then
	exit $?
fi

. env.sh

VarPath=`echo $DASHBOARD_BIN_DIR`
if [ "$VarPath" == "" ]
then
	export DASHBOARD_BIN_DIR=/var/dashboard
	VarPath=`echo $DASHBOARD_BIN_DIR`
fi

BIN_DIR=`dirname "$0"`
cd $BIN_DIR

SERVER_KEY="$1"
shift
case $SERVER_KEY  in
			
	"queryengine") 
		SERVER_NO=1;
		SERVER_PORT="8080";
		MAIN_CLASS="com.ctrip.dashboard.engine.main.QueryEngine"; 
		MEM_INFO="-Xms3G -Xmx3G -Xmn900m -Xss512k";
		CLASS_PATH="../lib/*";;
		
	"pushengine") 
		SERVER_NO=2;
		SERVER_PORT="8010";
		MAIN_CLASS="com.ctrip.dashboard.engine.main.PushEngine"; 
		MEM_INFO="-Xms1600m -Xmx1600m -Xmn600m -Xss512k";
		CLASS_PATH="../lib/*";;
		
	"metascanner") 
		SERVER_NO=3;
		MAIN_CLASS="com.ctrip.dashboard.tools.metascanner.Main";
		MEM_INFO="-Xms768m -Xmx768m -Xmn100m -Xss256k";
		CLASS_PATH="../lib/*";;
 
	*) 
		echo "Useage:$0  cmd, cmd maybe: queryengine|pushengine|metascanner [debug]";
		exit 1;;

	 esac;	 

if ! ./stop.sh $SERVER_KEY
then 
	exit 1
fi



if [ "$1" == "debug" ]
then
	DEBUG_INFO=" -Xdebug -Xrunjdwp:transport=dt_socket,address=809$SERVER_NO,server=y,suspend=n"
	shift
fi

DEBUG_INFO="$DEBUG_INFO -Dcom.sun.management.jmxremote.port=803$SERVER_NO"
DEBUG_INFO="$DEBUG_INFO -Dcom.sun.management.jmxremote.authenticate=false"
DEBUG_INFO="$DEBUG_INFO -Dcom.sun.management.jmxremote.ssl=false"
# START_OPTS="$START_OPTS -XX:+AggressiveOpts -XX:+UseParallelGC -XX:+UseBiasedLocking -XX:NewSize=64m"
START_OPTS="-server $MEM_INFO $DEBUG_INFO"
START_OPTS="$START_OPTS -Djava.io.tmpdir=../tmp"
START_OPTS="$START_OPTS -DLOG=$SERVER_KEY"
START_OPTS="$START_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$VarPath/tmp/java_dump_$SERVER_KEY.hprof -XX:+UseConcMarkSweepGC -XX:+UseParNewGC  -XX:CMSInitiatingOccupancyFraction=60 -XX:CMSTriggerRatio=70 -XX:CMSTriggerPermRatio=70"
#START_OPTS="$START_OPTS -Xloggc:../logs/${SERVER_KEY}_gc.log -XX:+PrintGCDateStamps -XX:+PrintGCDetails"

CLASS_PATH=".:$CLASS_PATH"
dt=`date +"%Y-%m-%d-%H-%M-%S"`
nohup $JAVA_HOME/bin/java $START_OPTS -classpath $CLASS_PATH $MAIN_CLASS $@ > "start.$SERVER_KEY.$dt.log" 2>&1 &
echo "java-pid $!"
sleep 5

#wait for completly started
max_loop=1000
for((i=0;i<=$max_loop;i++))
do	
	#check wether the $SERVER_KEY server with pid is alive
	if [ -z "$(./get-pid.sh $MAIN_CLASS)" ]
	then
		echo "failed to start the $SERVER_KEY server"
		exit 1
	fi

	if [ $SERVER_KEY = "queryengine" -o $SERVER_KEY = "pushengine" ]
	then
		#check more exactly
		if ./status-check.sh $SERVER_PORT
		then
			echo "$SERVER_KEY server has been restart successfully";
			break;
		fi
	fi
	
	if [ $SERVER_KEY = "metascanner" ]
	then
		echo "$SERVER_KEY server has been restart successfully";
		break;
	fi
	
	sleep 3
done

if [ $i -gt $max_loop ]
then
	echo "failed to restart $SERVER_KEY server";
	exit 1;
fi

echo "finish $0"