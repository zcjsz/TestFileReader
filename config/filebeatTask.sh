#!/bin/sh
# usage: shell wrapper to schedule the remove of old kdf log files


echo ""
TIME=$(date "+%Y%m%d%H%M%S")
DATE=`expr substr $TIME 1 8`
echo "$TIME: start the task"

APP_PATH="/home/tdni/hygon_apps/beat"

APP_NAME="logDeleter.sh"
TaskApp="$APP_PATH/$APP_NAME"

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

$TaskApp >> $LogFile 2>&1

TIME=$(date "+%Y%m%d%H%M%S")
echo "$TIME: end task"