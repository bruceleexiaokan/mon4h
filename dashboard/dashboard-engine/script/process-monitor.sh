#!/bin/bash

# ------------------------------------------------
# args: if $1=="1" as queryengine and pushengine
#		elif $1=="0" as metascanner
# ------------------------------------------------

. env.sh

VarPath=`echo $DASHBOARD_BIN_DIR`
if [ "$VarPath" == "" ]
then
	export DASHBOARD_BIN_DIR=/var/dashboard
	VarPath=`echo $DASHBOARD_BIN_DIR`
fi

EtcPath=`echo $DASHBOARD_CFG_DIR`
if [ "$EtcPath" == "" ]
then
	export DASHBOARD_CFG_DIR=/etc/dashboard
	EtcPath=`echo $DASHBOARD_CFG_DIR`
fi

changePath(){
	echo "cd to the Path=> $VarPath/bin/"
	cd $VarPath/bin/
	if [ $? -ne 0 ];then
		return 1
	fi
	return 0
}

stopBefore(){

	MAIN_CLASS="process-monitor.sh"
 
	max_loop=10
	echo "To kill the before process-monitor.sh"
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
		
		sleep 0.5
	done

	if [ $i -eq $max_loop ]
	then
		echo "failed kill the processes $pids after try $max_loop times"
		return 1
	fi
	return 0
}

if [ -z $1 ];then
	exit 1
fi

if [ $# -gt 3 ];then
	exit 1
fi

pramnum=$#
queryengine=0
pushengine=0
metascanner=0
stopprocessmonitor=0

compareType() {
	type=0
	SERVER_KEY="$1"
	shift
	case $SERVER_KEY  in
	"queryengine") 
		queryengine=1;;
	"pushengine") 
		pushengine=1;;
	"metascanner") 
		metascanner=1;;
	"stop")
		stopprocessmonitor=1;;
	*) 
		echo "error compare type";
		return -1;;
	 esac;
}

if [ $pramnum -eq 1 ];then
	compareType $1
elif [ $pramnum -eq 2 ];then
	compareType $1
	compareType $2
elif [ $pramnum -eq 3 ];then
	compareType $1
	compareType $2
	compareType $3
else
	echo "pram error."
	exit 1
fi

changePath
if [ $? -ne 0 ];then
	exit 5
fi

if [ $stopprocessmonitor -eq 1 ];then
	stopBefore
	if [ $? -ne 0 ];then
		echo "Kill the processmonitor.sh error."
		exit 2
	fi
	echo "Kill the Process Monitor Script Before."
	exit 1
fi

stopBefore
if [ $? -ne 0 ];then
	echo "Connot stop the process before."
	exit 1
fi

SERVER_NO=0;
SERVER_PORT="";
MAIN_CLASS=""; 
MEM_INFO="";
CLASS_PATH="";

compareServer() {
	SERVER_KEY="$1"
	shift
	case $SERVER_KEY  in
	1) 
		echo "Type => $SERVER_KEY queryengine"
		SERVER_NAME="queryengine";
		SERVER_PORT="8080";
		MAIN_CLASS="com.ctrip.dashboard.engine.main.QueryEngine"; 
		MEM_INFO="-Xms3G -Xmx3G -Xmn900m -Xss512k";
		CLASS_PATH="../lib/*";;
		
	2) 
		echo "Type => $SERVER_KEY pushengine"
		SERVER_NAME="pushengine";
		SERVER_PORT="8010";
		MAIN_CLASS="com.ctrip.dashboard.engine.main.PushEngine"; 
		MEM_INFO="-Xms1600m -Xmx1600m -Xmn600m -Xss512k";
		CLASS_PATH="../lib/*";;
		
	3) 
		echo "Type => $SERVER_KEY metascanner"
		SERVER_NAME="metascanner";
		SERVER_PORT="";
		MAIN_CLASS="com.ctrip.dashboard.tools.metascanner.Main";
		MEM_INFO="-Xms768m -Xmx768m -Xmn100m -Xss256k";
		CLASS_PATH="../lib/*";;
 
	*) 
		echo "Useage:$0  cmd, cmd maybe: queryengine|pushengine|metascanner [debug]";
		return 1;;
	 esac;
}


while [ true ]
do
	max_loop=4
	for((i=1;i<$max_loop;i++))
	do
		if [ $i -eq 1 ];then
			if [ $queryengine -eq 0 ];then
				continue;
			fi
		elif [ $i -eq 2 ];then
			if [ $pushengine -eq 0 ];then
				continue;
			fi
		elif [ $i -eq 3 ];then
			if [ $metascanner -eq 0 ];then
				continue;
			fi
		fi

		compareServer $i
		
		if [ -z "$(./get-pid.sh $MAIN_CLASS)" ];then
		
			if [ $SERVER_NAME = "queryengine" -o $SERVER_NAME = "pushengine" ];then
				if ./status-check.sh $SERVER_PORT
				then
					echo "$SERVER_NAME server has been restart successfully";
				else
					echo "Will to Start the Server => $SERVER_NAME"
					./start.sh $SERVER_NAME
					if [ $? -ne 0 ];then
						echo "Start => $SERVER_NAME FAIL"
					fi
				fi
			elif [ $SERVER_NAME = "metascanner" ];then
				echo "Will to Start the Server => $SERVER_NAME"
				./start.sh $SERVER_NAME
				if [ $? -ne 0 ];then
					echo "Start => $SERVER_NAME FAIL"
				fi
			fi
			
		fi
		
		echo "End => $SERVER_NAME"
		
	done
		
	echo "Sleep for 15 seconds."
	sleep 15
	
done
echo "End."
