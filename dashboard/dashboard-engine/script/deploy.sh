#!/bin/bash
cd /var/dashboard/bin
echo "now dir is `pwd`"

if [ "$1" == "replace" -o "$1" == "restart" -o "$1" == "stop" ]
then
	echo "Stop QueryEngine ..."
	./stop-queryengine.sh
	MAIN_CLASS="com.ctrip.dashboard.engine.main.QueryEngine"; 
	if [ -z "$(./get-pid.sh $MAIN_CLASS)" ]
	then
		echo "QueryEngine stoped."
	fi
	echo "Stop PushEngine ..."
	./stop-pushengine.sh
	MAIN_CLASS="com.ctrip.dashboard.engine.main.PushEngine"; 
	if [ -z "$(./get-pid.sh $MAIN_CLASS)" ]
	then
		echo "PushEngine stoped."
	fi
	echo "Stop meta scanner ..."
	./stop-meta-scanner.sh
	MAIN_CLASS="com.ctrip.dashboard.tools.metascanner.Main"; 
	if [ -z "$(./get-pid.sh $MAIN_CLASS)" ]
	then
		echo "meta scanner stoped."
	fi
fi

if [ "$1" == "replace" ]
then
	rm -rf /var/dashboard/lib
	DESTDIR=/var/dashboard/lib
	if [ ! -d "$DESTDIR" ]
	then
		echo "$DESTDIR deleted."
	else
		echo "$DESTDIR not deleted."
	fi
	rm -rf /var/dashboard/bin
	DESTDIR=/var/dashboard/bin
	if [ ! -d "$DESTDIR" ]
	then
		echo "$DESTDIR deleted."
	else
		echo "$DESTDIR not deleted."
	fi
	rm -rf /etc/dashboard/conf
	DESTDIR=/etc/dashboard/conf
	if [ ! -d "$DESTDIR" ]
	then
		echo "$DESTDIR deleted."
	else
		echo "$DESTDIR not deleted."
	fi
	cp -R ~/dashboard/lib /var/dashboard/lib
	DESTDIR=/var/dashboard/lib
	if [ -d "$DESTDIR" ] 
	then
		echo "$DESTDIR copyed."
	else
		echo "$DESTDIR not copyed."
	fi
	cp -R ~/dashboard/bin /var/dashboard/bin
	DESTDIR=/var/dashboard/bin
	if [ -d "$DESTDIR" ] 
	then
		echo "$DESTDIR copyed."
	else
		echo "$DESTDIR not copyed."
	fi
	cp -R ~/dashboard/conf /etc/dashboard/conf
	DESTDIR=/etc/dashboard/conf
	if [ -d "$DESTDIR" ] 
	then
		echo "$DESTDIR copyed."
	else
		echo "$DESTDIR not copyed."
	fi
	cd /var/dashboard/bin
	chmod -R 755 /var/dashboard/bin/
	if [ -x '/var/dashboard/bin/start-queryengine.sh' ] 
	then
		echo "/var/dashboard/bin chmoded."
	else
		echo "/var/dashboard/bin not chmoded."
	fi
fi

if [ "$1" == "replace" -o "$1" == "restart" ]
then
	./start-queryengine.sh
	if [ "$2" == "metascanner" ]
	then
		./start-meta-scanner.sh
	fi
fi
