#!/bin/sh

# script to decrypt the gpg files in the given folder
# usage: DecryptionBatch <Folder>


AppName="FileDecrypt"
RemoveFile="rm -f "
UnzipFile="tar -zxvf "
ChDir="cd "
Usage="$0 SourceFile DestinationFile"
Move="mv "
RMDIR="rmdir --ignore-fail-on-non-empty "
REMOVEFile="rm -f "


TIME=$(date "+%Y%m%d%H%M%S")
echo "start task on $TIME"
DATE=`expr substr $TIME 1 8`
#echo $DATE

if [ ! $# -eq 2 ];then
  echo $Usage
  echo "please set the input file directory"
  exit 1
fi



$ChDir$1
if [ $? = 0 ]; then
  echo "change dir to $1"
  APPPATH=`pwd`
  APPPATH="$APPPATH/"
  echo "pwd: $APPPATH"
else
  exit 1
fi

#for i in `ls ./ | sed "s:^:$1/:" | grep .gpg`
for i in `ls ./ | grep .gpg`
do
    KeepGoing=0
    fullFile=`echo $i | sed 's/.gpg$//'`    # generate fileName without ".gpg"
    fullFile="./$fullFile"
    # delete this file if exist
    if [ -f "$fullFile" ]; then
      $RemoveFile$fullFile
      if [ $? = 0 ]; then
        KeepGoing=1
        echo "successed to remove the existing tar.gz file $fullFile"
      fi
    else
      KeepGoing=1
    fi

    if [ $KeepGoing = 0 ]; then
      continue;
    fi
    
    KeepGoing=0
    gpg --passphrase QazWsx123 -o $fullFile -d "./$i"    # decryption
    if [ $? = 0 ]; then
      KeepGoing=1
      echo "successed to proceed the gpg file: $i"
      $RemoveFile$i
      if [ $? = 0 ]; then
        echo "successed to remove the gpg file: $i"
      else
        echo "failed to remove this file: $i"
      fi
    else
      echo "failed to proceed the gpg file: $i"
    fi
    echo ""
done

for file in `ls ./ | grep .tar.gz`
do
  zipFile="./$file"
  $UnzipFile$zipFile
  if [ $? = 0 ]; then
    echo "successed to extract this file: $file"
    $RemoveFile$zipFile
    if [ $? = 0 ]; then
      echo "successed to remove the tar.gz file: $file"
    else
      echo "failed to remove this file: $file"
    fi
  else
    echo "failed to execute this command $UnzipFile$zipFile"
    echo "failed to extract this file: $file"
  fi
  echo ""
done




for file in `ls ./`
do
  if [ -d $file ]; then
    for kdfFile in `ls ./$file | grep .kdf`
      do
        fullKDF="./$file/$kdfFile"
        # move kdf file to destination
        mvCmd="$Move$fullKDF $2$kdfFile.$TIME"
        $mvCmd
        if [ $? = 0 ]; then
          echo "successed to move kdf file $kdfFile to $2"
        else
          echo "failed to execute command $mvCmd"
          echo "failed to move $kdfFile to $2"
        fi
        echo ""
      done
  fi
done

for file in `ls ./`
do
  if [ -d $file ]; then
    for kdfFile in `ls ./$file | grep .dis`
      do
        fullKDF="./$file/$kdfFile"
        # move kdf file to destination
        mvCmd="$Move$fullKDF $2$kdfFile.$TIME"
        $mvCmd
        if [ $? = 0 ]; then
          echo "successed to move dis file $kdfFile to $2"
        else
          echo "failed to execute command $mvCmd"
          echo "failed to move $kdfFile to $2"
        fi
        echo""
      done
  fi
done

for file in `ls ./`
do
  if [ -d $file ]; then
    for rmapFile in `ls ./$file | grep .rmap`
    do
      # remove all rmaps
      fullFile="./$file/$rmapFile"
      $REMOVEFile$fullFile
      if [ $? = 0 ]; then
        echo "successed to remove rmap file: $rmapFile"
      else
        echo "failed to remove rmap file: $rmapFile"
      fi
      echo ""
    done

  fi
done

for file in `ls ./`
do
  if [ -d $file ]; then
    count=`ls ./$file |wc -w`
    if [ $count -ne 0 ]; then
      echo "$file size $count"
    else
      fullPath="./$file"
      $RMDIR$fullPath
      echo "rm cmd: $RMDIR$fullPath"
      if [ $? = 0 ]; then
        echo "successed to remove this empty dir: $file"
      else
        echo "failed to execute this cmd: $RMDIR$fullPath"
        echo "failed to remove this empty dir: $file"
      fi
      echo ""
    fi
  fi
done
