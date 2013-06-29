#!/bin/bash

result_code=`curl -X POST -s --data '{"version":1}' http://localhost:$1/monitor/systemstatus | ./json.sh | grep '\["result-code"\]'|awk '{print $2}'`
if [ "${result_code}" == "0" ]
then
exit 0
fi
exit 1

