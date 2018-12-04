#!/bin/sh


Usage="this script is to copy the log file to a temp date dir\n
common case is to copy log files to a ttemp date dir while thoese files are not consumed by the filebeat\n
usage: $0 [date fileter]\n
it will grep log files using the date filter\n"


if [ ! $# -eq 1 ];then
    echo -e "$Usage"
    echo "$(date): please set the source and destination file"
    echo ""
    exit 1
fi

DATE=`expr substr $TIME 1 8`

mkdir "$DATE"
if [ $? = 1 ]; then
    exit 1
fi 

for logFile in `ls -lh ./" | grep '.log$' | grep '$1$'`; do
    echo $logFile
done
   
