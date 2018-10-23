#!/bin/sh
# usage: shell wrapper to schedule the hygon data transfer task

TIME=$(date "+%Y%m%d%H%M%S")
DATE=`expr substr $TIME 1 8`

echo ""
echo "$TIME: start task"
APP_NAME="DecryptionFile.sh"
APP_PATH="/home/tdni/hygon_apps/decrypt"

TaskApp="$APP_PATH/$APP_NAME"

SourcePath="/data/Hygon_incoming"
DestinationPath="/extend/hygon_source_data"

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

$TaskApp $SourcePath $DestinationPath >> $LogFile 2>&1

TIME=$(date "+%Y%m%d%H%M%S")
echo "$TIME: end task"

