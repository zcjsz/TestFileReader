#!/bin/sh
# usage: tar utility


DATE=`date +%Y%m%d --date="-20 day"`

output_tar_log()
{
    echo ""
    echo "*************************************************************"
    echo "*                   tar utility                             *"
    echo "*************************************************************"
    echo ""
}


if [ $# = 0 ]; then
  echo "please specify the file which you want to tar"
  exit 3
else
  APP_LOG_PATH=$1

  if [ ! -d "$APP_LOG_PATH" ]; then
          echo "$APP_LOG_PATH doesn't exist"
          exit 3
  #else
    #echo "input file: $APP_LOG_PATH"
  fi
fi

# list all the _20180904.log, tar the file which was generated beyond 7 days only

fileNo=0

for file in `ls $APP_LOG_PATH | grep .log`
do
    logDate=${file##*_}
    #echo $logDate
    logDate=${logDate%.*}
    #echo $logDate
    
    DateLength=$(expr length $logDate)
    #echo $DateLength
    echo ""
    if [ $DateLength -ne 8 ]; then
        #echo "this is not my format date log file: $file"
        continue
    fi
    
    if [ $logDate -le $DATE ]; then
        if [ $fileNo -eq 0 ]; then
            output_tar_log
        fi
        fileNo=1
        
        echo "I'm going to tar this file $file"
        TAR_CMD="tar -zcvf $APP_LOG_PATH/$file.tar.gz $APP_LOG_PATH/$file"
        #echo "$TAR_CMD"
        $TAR_CMD
        if [ $? -eq 0 ]; then
            echo "successed to tar this file $file"
            rm -f $APP_LOG_PATH/$file
            if [ $? -eq 0 ]; then
                echo "successed to remove this file $file"
            else
                echo "failed to remove this file $file"
            fi
        else
            echo "failed to tar this file $file"
        fi
    #else
        #echo "skip this file $file since keeps for 7 days"
    fi
done
exit 0



