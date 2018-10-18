#!/bin/sh
# usage: shell wrapper to schedule the hygon data file parse task

TIME=$(date "+%Y%m%d%H%M%S")
DATE=`expr substr $TIME 1 8`

echo "$TIME: start the task"
TaskApp="FileReader.java"

JAVA='which java'
TARAPP="./TarTask.sh ./log"

ConfigFile="./config/dataformat.xml"
LogFile="./log/log_$DATE.log"

pid=`ps -ef | grep $TaskApp |grep -v grep | grep -v $0 | awk '{print $2}'`
if [ -z "$pid" ]; then
    echo "$TaskApp is not running"
    $TARAPP
else
    echo "$TaskApp is already running, bye"
    exit 0
fi
echo "Now start run $TaskApp"

$JAVA -jar $TaskApp -Xms6G -Xmx6G $ConfigFile >> $LogFile 2>&1

TIME=$(date "+%Y%m%d%H%M%S")
echo "$TIME: all the taskd done"

