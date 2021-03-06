#!/bin/sh
# usage: to delete the eof log files

TIME=$(date "+%Y%m%d%H%M%S")
echo ""
echo "$TIME: start task"

LOGPATH="/var/log/filebeat"
LOG=".log"

for file in `ls $LOGPATH | grep filebeat`
do
   
    LOGFILE="$LOGPATH/$file"
    echo "$LOGFILE"
    a=`grep "End of file reached:" $LOGFILE`

    for line in $a; do
        logFile=${line%kdf*}
        if [ $logFile != $line ]; then
            logFile=${line%.*}
            echo $logFile
            if [ -f $logFile ]; then
		FileTime=`stat -c %Y $logFile`
            	CurrTime=`date +%s`
	
		if [ $[ $CurrTime - $FileTime ] -gt 14400 ]; then	
			rm -f $logFile
			if [ $? = 0 ]; then
			    echo "successed to remove kdf log file: $logFile"
			else
			    echo "failed to remove kdf log file: $logFile"
			fi
		else
			echo "this file only existing for less than 4 hours"
		fi
            fi
        fi

        logFile=${line%smap*}
        if [ $logFile != $line ]; then
            logFile=${line%.*}
            echo $logFile
            echo $logFile
            if [ -f $logFile ]; then
		FileTime=`stat -c %Y $logFile`
                CurrTime=`date +%s`

		if [ $[ $CurrTime - $FileTime ] -gt 14400 ]; then
			rm -f $logFile
			if [ $? = 0 ]; then
			    echo "successed to remove kdf log file: $logFile"
			else
			    echo "failed to remove kdf log file: $logFile"
			fi
		else
                        echo "this file only existing for less than 4 hours"
                fi

            fi
        fi
    done
    	


done

TIME=$(date "+%Y%m%d%H%M%S")
echo ""
echo "$TIME: end task"
exit 0



