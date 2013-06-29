#!/bin/bash

#----------------------------------------
#	Dashboard Publish Script
#		zlSong
#----------------------------------------

# Stop the Process
USERNAME=$1 #dashboard
TARNAME=$2 #TARNAME='dashboard-1.0.tar'

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

cpEnv() {

	echo "cp env file"
	cp /home/$USERNAME/dashboard/env.sh $VarPath/bin/
	if [ $? -ne 0 ]
	then
		echo "Copy /home/$USERNAME/dashboard/env.sh error."
		return 5
	fi
}

cleartarbefore() {
	echo "Clear the front folder && files."
	echo "Clear Path: /home/$USERNAME/dashboard/"
	filePath="/home/$USERNAME/dashboard/"
	if [ -d $filePath ]
	then
		echo "Clear Start=> Delete the files in /home/$USERNAME/dashboard/"
		rm -rf $filePath"/conf"
		rm -rf $filePath"/bin"
		rm -rf $filePath"/lib"
		if [ $? -ne 0 ]; then
			echo "Clear Delete folder && file failed."
			return 2
		fi
	else
		echo "Clear $filePath is not exits."
		echo "Clear make the Path=> $filePath"
		mkdir -p $filePath
	fi
	return 0
}

stopProcessmonitor() {

	MAIN_CLASS="process-monitor.sh"
 
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
		
		sleep 0.5
	done

	if [ $i -eq $max_loop ]
	then
		echo "failed kill the processes $pids after try $max_loop times"
		return 1
	fi
	return 0

}

decommpress() {
	echo "Decommpress the tar file."
	echo "Decommpress From=> /home/$USERNAME/dashboard/$TARNAME"
	echo "Decommpress To=> /home/$USERNAME/dashboard/"
	tar -xf /home/$USERNAME/dashboard/$TARNAME -C /home/$USERNAME/dashboard/
	if [ $? -ne 0 ]; then
		echo "Decommpress the tar file error."
		return 3
	fi
	return 0
}

testBefore() {
	echo "Test the before files of $VarPath/bin"
	filePath="$VarPath/bin"
	if [ -d $filePath ];then
		return 0
	fi
	return 8
}

stopbefore() {
	echo "Stop the Engines and Scanner."
	echo "Stop QueryEngine=> Run $VarPath/bin/stop-queryengine.sh"
	result=`$VarPath/bin/stop-queryengine.sh`
	if [ $? -ne 0 ]
	then
		echo "Stop QueryEngine Error."
		return 1
	fi
	
	echo "Stop PushEngine=> Run $VarPath/bin/stop-pushengine.sh"
	result=`$VarPath/bin/stop-pushengine.sh`
	if [ $? -ne 0 ]
	then
		echo "Stop PushEngine Error."
		return 1
	fi
	
	echo "Stop Meta-Scanner=> Run $VarPath/bin/stop-meta-scanner.sh"
	result=`$VarPath/bin/stop-meta-scanner.sh`
	if [ $? -ne 0 ]
	then
		echo "Stop Meta-Scanner Error"
		return 1
	fi
	return 0
}

makedirbefore() {
	echo "Create $filePath"
	filePath="$VarPath/bin"
	mkdir -p $filePath
	if [ $? -ne 0 ]; then
		echo "Create $filePath failed."
		return 4
	fi
	
	echo "Create $filePath"
	filePath="$VarPath/lib"
	mkdir -p $filePath
	if [ $? -ne 0 ]; then
		echo "Create $filePath failed."
		return 4
	fi
	
	echo "Create $filePath"
	filePath="$VarPath/conf"
	mkdir -p $filePath
	if [ $? -ne 0 ]; then
		echo "Create $filePath failed."
		return 4
	fi
}

clearcpbefore() {
	echo "Clear the decommpress files in the Path."
	filePath="$VarPath/bin/*"
	#if [ -d $filePath ];then
	echo "Clear Var=> Delete the files in $VarPath/bin"
	rm -rf $filePath
	if [ $? -ne 0 ]; then
		echo "Clear $filePath failed."
		return 4
	fi
	#fi
	
	filePath="$VarPath/lib/*"
	#if [ -d $filePath ];then
	echo "Clear Var=> Delete the files in $VarPath/lib"
	rm -rf $filePath
	if [ $? -ne 0 ]; then
		echo "Clear $filePath failed."
		return 4
	fi
	#fi
	
	filePath="$VarPath/conf/*"
	#if [ -e $filePath ];then
	echo "Clear ECT=> Delete the files in $EtcPath/conf/"
	rm -rf $filePath
	if [ $? -ne 0 ]; then
		echo "Clear $filePath failed."
		return 4
	fi
	#fi
	return 0
}

copy() {
	echo "Copy the folder && files."
	cp -r -f /home/$USERNAME/dashboard/bin $VarPath
	if [ $? -ne 0 ]
	then
		echo "Copy /home/$USERNAME/dashboard/bin/ error."
		return 5
	fi
	
	cp -r -f /home/$USERNAME/dashboard/lib $VarPath
	if [ $? -ne 0 ]
	then
		echo "Copy /home/$USERNAME/dashboard/lib/ error."
		return 5
	fi
	
	cp -r -f /home/$USERNAME/dashboard/conf $EtcPath
	if [ $? -ne 0 ]
	then
		echo "Copy /home/$USERNAME/dashboard/conf/ error."
		return 5
	fi
}

changePath(){
	echo "cd to the Path=> $VarPath/bin/"
	cd $VarPath/bin/
	if [ $? -ne 0 ];then
		return 1
	fi
	return 0
}




sign=1

cleartarbefore
if [ $? -ne 0 ];then
	exit 2
fi

decommpress
if [ $? -ne 0 ];then
	exit 3
fi

testBefore
if [ $? -ne 0 ];then
	makedirbefore
	if [ $? -ne 0 ];then
		exit 4
	fi
	copy
	if [ $? -ne 0 ];then
		exit 5
	fi
else
	sign=0
fi

changePath
if [ $? -ne 0 ];then
	exit 5
fi

stopProcessmonitor
if [ $? -ne 0 ];then
	echo "Stop Process monitor Error."
	exit 4
fi

stopbefore
if [ $? -ne 0 ]
then
	exit 1
fi

if [ $sign -eq 0 ];then
#	:<<BLOCK
	clearcpbefore
	:<<BLOCK
	if [ $? -ne 0 ];then
		exit 4
	fi
BLOCK
#BLOCK
	copy
	if [ $? -ne 0 ];then
		exit 5
	fi
fi

cpEnv
	
echo "Exit."
exit 0

