#!/bin/sh
# usage: shell wrapper to schedule the hygon data file parse task

TIME=$(date "+%Y%m%d%H%M%S")
DATE=`expr substr $TIME 1 8`
echo "$TIME: start the task"

JAVA="/usr/bin/jdk1.8.0_151/bin/java"
APP_PATH="/home/tdni/hygon_apps/engFileReader"

ConfigFile="$APP_PATH/config/dataformat.xml"
APP_NAME="engFileReader.jar"
TaskApp="$APP_PATH/target/$APP_NAME"


if [ ! -d $APP_PATH ]; then
    echo "$APP_PATH does not exist"
    exit 1
fi

if [ ! -f $TaskApp ]; then
    echo "$TaskApp does not exist"
    exit 1
fi
if [ ! -f $ConfigFile ]; then
    echo "$ConfigFile does not exist"
    exit 1
fi




# please use the full path here to avoid any path mistakes
LOG_PATH="$APP_PATH/log"
LogFile="$LOG_PATH/log_$DATE.log"

TAR_TOOL="$APP_PATH/TarTask.sh"

pid=`ps -ef | grep $APP_NAME |grep -v grep | grep -v $0 | awk '{print $2}'`
if [ -z "$pid" ]; then
    echo "$TaskApp is not running, and now start it"
    $TAR_TOOL $LOG_PATH

else
    echo "$TaskApp is already running, bye"
    exit 0
fi

$JAVA -Xms6G -Xmx6G -jar $TaskApp $ConfigFile  >> $LogFile 2>&1

TIME=$(date "+%Y%m%d%H%M%S")
echo "$TIME: end task"




