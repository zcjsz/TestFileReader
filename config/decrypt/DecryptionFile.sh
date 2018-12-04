#!/bin/sh

# script to decrypt the gpg files in the given folder
# usage: DecryptionBatch <source> <destinaltion>


AppName="FileDecrypt"
RemoveFile="rm -f "
UnzipFile="tar -zxvf "
ChDir="cd "
Usage="this script is to transfer the hygon kdf/wat/smap files to a destination file\n
a typical source file contains SORT/WAT/SMAP etc\n
the steps to proceed files are:\n 
1)this script will first try decrypt the gpg file and then extract the tar.gz file and then remove the gpg and tar.gz files\n
2)move all the kdf files to the cooresponding folder while a timestamp will be append to the file name\n
  eg. SORT/Lot/abc.xxx.kdf will move to destination/SORT/abc.xxx.kdf.20180101010101, skip the lot dir\n
  and then clean up all the empty kdf lot folder\n
3)move all the dis files to the cooresponding folder while a timestamp will be append to the file name\n
  e.g WAT/Lot/abc.xxx.dis will move to destination/WAT/Lot/abc.xxx.dis.20180101010101\n
  and then remove the empty lot folder\n
4)move all the smap files to the cooresponding folder while a timestamp will be append to the file name\n
  e.g SMAP/Lot/abc.smap will move to destination/SMAP/Lot/abc.smap.20180101010101\n
  and then remove the empty lot folder\n
Usage: $0 [SourceFile] [DestinationFile] [GPGBackupFile]"

enableSort=1
enableEngSort=1
enableFt=1
enableSlt=1
enableWat=1
enableSmap=1
enableYms=1

Move="mv "
RMDIR="rmdir --ignore-fail-on-non-empty "
REMOVEFile="rm -f "

SORT_PATH="SORT"
WAT_PATH="WAT"
SMAP_PATH="SMAP"
ENG_PATH="EngSORT"

FT_PATH="FT"
SLT_PATH="SLT"
YMS_PATH_ROOT="TransYieldRep";
YMS_PATH_shift="$YMS_PATH_ROOT/shift"
YMS_PATH_week="$YMS_PATH_ROOT/weekly"

if [ ! $# -eq 3 ];then
    echo -e "$Usage"
    echo "$(date): please set the source and destination file"
    echo ""
    exit 1
fi

TIME=$(date "+%Y%m%d%H%M%S")
echo ""
echo "$(date): start the task"


DATE=`expr substr $TIME 1 8`
DATE_PATH="$DATE/"
AppPath=`pwd $0`


cd $1
if [ $? = 0 ]; then
    SourcePath=`pwd`
    cd $AppPath
    if [ $? = 0 ]; then
        cd $2
        if [ $? = 0 ]; then
            DestPath=`pwd`
            cd $3
            if [ $? = 0 ]; then
                GPGBackupPath=`pwd`
            else
                echo "$(date): path $3  does not exist"
                exit 1
            fi
        else
            echo "$(date): path $2 doesn't exist"
            exit 1
        fi
    fi
else
    echo "$(date): path $1 doesn't exist"
    exit 1
fi

echo "$(date): full app path is $AppPath"
echo "$(date): full source path is $SourcePath"
echo "$(date): full destination path is $DestPath"
echo "$(date): full gpg backup path is $GPGBackupPath"




sort_gpg_path="$GPGBackupPath/$SORT_PATH"
ft_gpg_path="$GPGBackupPath/$FT_PATH"
slt_gpg_path="$GPGBackupPath/$SLT_PATH"
eng_sort_gpg_path="$GPGBackupPath/$ENG_PATH"
smap_gpg_path="$GPGBackupPath/$SMAP_PATH"
wat_gpg_path="$GPGBackupPath/$WAT_PATH"
yms_gpg_path_shift="$GPGBackupPath/$YMS_PATH_shift"
yms_gpg_path_week="$GPGBackupPath/$YMS_PATH_week"

tempFile="$DestPath/temp"

fileLimit=300


# $1 is the full path to make
make_path()
{
    if [ ! $# -eq 1 ];then
        echo "$(date): $0 has 1 arg"
        echo "usage: $0 [full file path to make]"
        exit 1
    fi
    
    if [ -d $1 ]; then
        echo "$(date): file $1 already exist"
        return 0
    fi

    mkdir -p $1
    if [ $? = 0 ]; then
        echo "$(date): successed to mkdir for $1"
        return 0
    else
        echo "$(date): failed to mkdir for $1"
        return 1
    fi
}
change_dir_to_yms_shift()
{
    if [ $enableYms = 0 ]; then
        echo "$(date): yms is disabled"
	return 1
    fi
	
    path=$SourcePath/$YMS_PATH_shift
    $ChDir$path
    if [ $? = 0 ]; then
      echo ""
      echo `pwd`
    else
      make_path $path
      if [ $? = 0 ]; then
        return 0
      else
        return 1
      fi
    fi
    return 0
}
change_dir_to_yms_week()
{
    if [ $enableYms = 0 ]; then
        echo "$(date): yms is disabled"
	return 1
    fi
	
    path=$SourcePath/$YMS_PATH_week
    $ChDir$path
    if [ $? = 0 ]; then
      echo ""
      echo `pwd`
    else
      make_path $path
      if [ $? = 0 ]; then
        return 0
      else
        return 1
      fi
    fi
    return 0
}


change_dir_to_sort()
{
    if [ $enableSort = 0 ]; then
        echo "$(date): sort is disabled"
	return 1
    fi
	
    path=$SourcePath/$SORT_PATH
    $ChDir$path
    if [ $? = 0 ]; then
      echo ""
      echo `pwd`
    else
      make_path $path
      if [ $? = 0 ]; then
        return 0
      else
        return 1
      fi
    fi
    return 0
}

change_dir_to_eng()
{
    if [ $enableEngSort = 0 ]; then
        echo "$(date): eng sort is disabled"
        return 1
    fi

    path=$SourcePath/$ENG_PATH
    $ChDir$path
    if [ $? = 0 ]; then
        echo ""
        echo `pwd`
    else
        make_path $path
        if [ $? = 0 ]; then
          return 0
        else
          return 1
        fi
    fi
    return 0
}

change_dir_to_wat()
{
    if [ $enableWat = 0 ]; then
        echo "$(date): wat is disabled"
        return 1
    fi

    path='pwd'
    path=$SourcePath/$WAT_PATH
    $ChDir$path
    if [ $? = 0 ]; then
      echo ""
      echo `pwd`
    else
        make_path $path
        if [ $? = 0 ]; then
          return 0
        else
          return 1
        fi
    fi
    return 0
}
change_dir_to_smap()
{
    if [ $enableSmap = 0 ]; then
        echo "$(date): smap is disabled"
        return 1
    fi
    
    path='pwd'
    path=$SourcePath/$SMAP_PATH
    $ChDir$path
    if [ $? = 0 ]; then
      echo ""
      echo `pwd`
    else
        make_path $path
        if [ $? = 0 ]; then
          return 0
        else
          return 1
        fi
    fi
    return 0
}


change_dir_to_slt()
{
    if [ $enableSlt = 0 ]; then
        echo "$(date): slt is disabled"
        return 1
    fi


    path='pwd'
    path=$SourcePath/$SLT_PATH
    $ChDir$path
    if [ $? = 0 ]; then
      echo ""
      echo `pwd`
    else
        make_path $path
        if [ $? = 0 ]; then
          return 0
        else
          return 1
        fi
    fi
    return 0
}

change_dir_to_ft()
{

    if [ $enableFt = 0 ]; then
        echo "$(date): ft is disabled"
        return 1
    fi

    path='pwd'
    path=$SourcePath/$FT_PATH
    $ChDir$path
    if [ $? = 0 ]; then
      echo ""
      echo `pwd`
    else
        make_path $path
        if [ $? = 0 ]; then
          return 0
        else
          return 1
        fi
    fi
    return 0
}

clear_temp_file()
{
    rm -rf $tempFile/*
    if [ $? = 0 ]; then
        echo "$(date): successed to clean the temp dir $tempFile"
        return 0
    else
        echo "$(date): failed to clean the temp dir $tempFile"
        return 1
    fi
}


# $1 is the source file
# $2 is the destination file
move_file()
{
    if [ ! $# -eq 2 ];then
        echo "$(date): $0 has 2 args"
        echo "usage: $0 [source file] [destintaion file]"
        exit 1
    fi

    mv $1 $2
    if [ $? = 0 ]; then
        echo "$(date): successed to move $1 to $2"
        return 0
    else
        echo "$(date): failed to move $1 to $2"
        return 1
    fi
}

# check if the file name is ends with the type
# $1 is the file name
is_tar_file()
{
    if [ ! $# -eq 1 ];then
        echo "$0 has 1 args"
        echo "usage: $0 [file name]"
        exit 1
    fi
    echo "$1" | grep 'tar.gz$'
    if [ $? = 0 ]; then
        echo "$(date): $1 is a tar.gz file"
        return 0
    else
        echo "$(date): $1 is not a tar.gz file"
        return 1
    fi    
}


# check if the file name is ends with the type kdf
# $1 is the file name
is_kdf_file()
{
    if [ ! $# -eq 1 ];then
        echo "$0 has 1 args"
        echo "usage: $0 [file name]"
        exit 1
    fi
    echo "$1" | grep '.kdf$'
    if [ $? = 0 ]; then
        echo "$(date): $1 is a kdf file"
        return 0
    else
        echo "$(date): $1 is not a kdf file"
        return 1
    fi    
}

# extract file to the tempFile
# $1 is the tar.gz file

extract_file()
{
    if [ ! $# -eq 1 ];then
        echo "$(date): $0 has 1 args"
        echo "usage: $0 [tar.gz file]"
        exit 1
    fi

    $UnzipFile$1 -C $tempFile
    if [ $? = 0 ]; then
      echo "$(date): successed to extract this file: $1 to $tempFile"
      return 0
    else
        echo "$(date): failed to extract this file: $1 to $tempFile"
        return 1
    fi
}

# $1 is the gpg file
# $2 is the target file
deceypt_file()
{
    if [ ! $# -eq 2 ];then
        echo "$(date): $0 has 2 args"
        echo "usage: $0 [gpg file] [output file]"
        exit 1
    fi
    gpg --passphrase QazWsx123 --batch -o $2 -d $1    # decryption
    if [ $? = 0 ]; then
      echo "$(date): successed to decrypt the gpg file: $1 to $2"
      return 0
    else
      echo "$(date): failed to decrypt the gpg file: $1 to $2"
      return 1
    fi
}

# move all the kdf file to destination hour files
# $1 is the destination hourFile
# $2 is the second timestamp
move_kdf_file()
{
    if [ ! $# -eq 2 ];then
        echo "$(date): $0 has 2 args"
        echo "usage: $0 [destination kdf hour file] [timsestamp in second]"
        exit 1
    fi

    for kdfFile in `ls $tempFile | grep '.kdf$'`; do
        move_file "$tempFile/$kdfFile" "$1/$kdfFile.$2"
    done
    
    
    for lotFile in `ls $tempFile`; do
        if [ -d $tempFile/$lotFile ]; then
            for kdfFile in `ls "$tempFile/$lotFile" | grep '.kdf$'`; do
                move_file "$tempFile/$lotFile/$kdfFile" "$1/$kdfFile.$2"
            done
        fi
    done    
}




# move all the dis file to destination hour files
# $1 is the destination hourFile
# $2 is the second timestamp
move_wat_file()
{
    if [ ! $# -eq 2 ];then
        echo "$(date): $0 has 2 args"
        echo "usage: $0 [destination dis hour file] [timsestamp in second]"
        exit 1
    fi

    for lotFile in `ls $tempFile`
    do
      if [ -d $tempFile/$lotFile ]; then
        for disFile in `ls "$tempFile/$lotFile" | grep '.dis$'`; do
            make_path "$1/$lotFile"
            if [ $? = 1 ]; then
                continue
            fi
            move_file "$tempFile/$lotFile/$disFile" "$1/$lotFile/$disFile.$2"
        done
      fi
    done
}

# move all the dis file to destination hour files
# $1 is the destination hourFile
# $2 is the second timestamp
move_smap_file()
{
    if [ ! $# -eq 2 ];then
        echo "$(date): $0 has 2 args"
        echo "usage: $0 [destination smap hour file] [timsestamp in second]"
        exit 1
    fi

    for lotFile in `ls $tempFile`; do
      if [ -d $tempFile/$lotFile ]; then
        removeXls="rm -f "$tempFile/$lotFile/*xlsx""
        $removeXls
        for smapFile in `ls "$tempFile/$lotFile" | grep '.smap$'`; do
            make_path "$1/$lotFile"
            if [ $? = 1 ]; then
                continue
            fi
            move_file "$tempFile/$lotFile/$smapFile" "$1/$lotFile/$smapFile.$2"
        done
      fi
    done
}

# move all the kdf file to destination hour files
# $1 is the destination hourFile
# $2 is the second timestamp
move_yms_file()
{
    if [ ! $# -eq 2 ];then
        echo "$(date): $0 has 2 args"
        echo "usage: $0 [destination yms hour file] [timsestamp in second]"
        exit 1
    fi

    for kdfFile in `ls $tempFile | grep '.xls$'`; do
        move_file "$tempFile/$kdfFile" "$1/$kdfFile.$2"
    done
    
    
    for lotFile in `ls $tempFile`; do
        if [ -d $tempFile/$lotFile ]; then
            for kdfFile in `ls "$tempFile/$lotFile" | grep '.xls$'`; do
                move_file "$tempFile/$lotFile/$kdfFile" "$1/$kdfFile.$2"
            done
        fi
    done    
}


#Decrypt and extract
# add the arg for path to store the raw data
# $1 is the gpg destination file
# $2 the kdf source file
# $3 means if this is the kdf source, value 1 means kdf, value 2 means smap, value 3 means wat
decrypt_extract()
{
    if [ ! $# -eq 3 ];then
        echo "$(date): $0 has 3 ars"
        echo "usage: $0 [gpg destination file] [source data file] [source type]"
        exit 1
    fi
    echo "$(date): gpg backup file is : $1"
    echo "$(date): source data file is: $2"
    echo "$(date): source is kdf: $3"
    isKdf=$3
    fileCnt=0
    #for i in `ls ./ | sed "s:^:$1/:" | grep .gpg`
    for gpgFile in `ls ./ | grep '.gpg$'`; do
        echo ""
        fileCnt=$(($fileCnt + 1))
        if [ $fileCnt = $fileLimit ]; then
            echo "$(date): have a break since $fileCnt, byebye"
            break
        fi

        # get the hourDir
        second=$(date "+%Y%m%d%H%M%S")
        hour=`expr substr $second 1 8`

        gpg_backup_hourFile="$1/$hour"
        kdfHourFile="$2/$hour";
        
        # make the gpg_backup_hourFile
        make_path $gpg_backup_hourFile
        if [ $? = 1 ]; then
            continue
        fi
        
        # make the kdfHourFile
        make_path $kdfHourFile
        if [ $? = 1 ]; then
            continue
        fi
        
        # clean the temp file
        clear_temp_file
        if [ $? = 1 ]; then
            continue
        fi

        # move gpg file to temp file
        move_file "./$gpgFile" "$tempFile/$gpgFile"
        if [ $? = 1 ]; then
            continue
        fi

        # decrypt the gpg file
        tarFile=`echo $gpgFile | sed 's/.gpg$//'`    # generate fileName without ".gpg"
        
        deceypt_file "$tempFile/$gpgFile" "$tempFile/$tarFile"
        
        # move the gpg file to gpg_backup_hourFile and rename is with timestamp
        if [ $? = 1 ]; then
            move_file "$tempFile/$gpgFile" "$gpg_backup_hourFile/$gpgFile.$second"
            continue
        else
            move_file "$tempFile/$gpgFile" "$gpg_backup_hourFile/$gpgFile.$second"
        fi


        is_tar_file "$tarFile"
        if [ $? = 0 ]; then
            #unzip the tar.gz file
            extract_file "$tempFile/$tarFile"
            if [ $? = 1 ]; then
                continue
            fi
        fi
        # 
        # move the kdf file to kdfHourFile if this is a kdf type 
        # 
        if [ $3 -eq 1 ]; then
            move_kdf_file "$kdfHourFile" "$second"
        elif [ $3 -eq 2 ]; then
            move_smap_file "$kdfHourFile" "$second"
        elif [ $3 -eq 3 ]; then
            move_wat_file "$kdfHourFile" "$second"
        elif [ $3 -eq 4 ]; then
            # place all the camstar report file in the same $hour file
            camAllPath="$DestPath/$YMS_PATH_ROOT/$hour"
            make_path $camAllPath
            if [ $? = 0 ]; then
                move_yms_file "$camAllPath" "$second"
            fi
            
        else
            echo "$(date): unknow source type number $3 found"
        fi
        
    done   
}


make_path $tempFile
if [ $? = 1 ]; then
    echo "$(date): byebye"
    exit 1
fi

# make sort gpg backup path
make_path $sort_gpg_path
if [ $? = 1 ]; then
    echo "$(date): byebye"
    exit 1
fi

# make ft gpg backup path
make_path $ft_gpg_path
if [ $? = 1 ]; then
    echo "$(date): byebye"
    exit 1
fi

# make slt gpg backup path
make_path $slt_gpg_path
if [ $? = 1 ]; then
    echo "$(date): byebye"
    exit 1
fi

# make eng sort gpg backup path
make_path $eng_sort_gpg_path
if [ $? = 1 ]; then
    echo "$(date): byebye"
    exit 1
fi


# make ft smap backup path
make_path $smap_gpg_path
if [ $? = 1 ]; then
    echo "$(date): byebye"
    exit 1
fi


# make wat gpg backup path
make_path $wat_gpg_path
if [ $? = 1 ]; then
    echo "$(date): byebye"
    exit 1
fi

# make yms week gpg backup path
make_path $yms_gpg_path_week
if [ $? = 1 ]; then
    echo "$(date): byebye"
    exit 1
fi

# make yms shift gpg backup path
make_path $yms_gpg_path_shift
if [ $? = 1 ]; then
    echo "$(date): byebye"
    exit 1
fi

# $1 is the gpg destination file
# $2 the kdf source file
# $3 means if this is the kdf source, value 1 means kdf, value 2 means smap, value 3 means wat




change_dir_to_slt
if [ $? = 0 ]; then
    echo "$(date): slt task start"
    decrypt_extract "$slt_gpg_path" "$DestPath/$SLT_PATH" "1"
    echo "$(date): slt task done"
fi

change_dir_to_sort
if [ $? = 0 ]; then
    echo "$(date): sort task start"
    decrypt_extract "$sort_gpg_path" "$DestPath/$SORT_PATH" "1"
    echo "$(date): sort task done"
fi


change_dir_to_eng
if [ $? = 0 ]; then
    echo "$(date): eng sort task start"
    decrypt_extract "$eng_sort_gpg_path" "$DestPath/$ENG_PATH" "1"
    echo "$(date): eng sort task done"
fi

change_dir_to_ft
if [ $? = 0 ]; then
    echo "$(date): ft task start"
    decrypt_extract "$ft_gpg_path" "$DestPath/$FT_PATH" "1"
    echo "$(date): ft task done"
fi


change_dir_to_wat
if [ $? = 0 ]; then
    echo "$(date): wat task start"
    decrypt_extract "$wat_gpg_path" "$DestPath/$WAT_PATH" "3"
    echo "$(date): wat task done"
fi

change_dir_to_smap
if [ $? = 0 ]; then
    echo "$(date): smap task start"
    decrypt_extract "$smap_gpg_path" "$DestPath/$SMAP_PATH" "2"
    echo "$(date): smap task done"
fi


change_dir_to_yms_shift
if [ $? = 0 ]; then
    echo "$(date): yms shift task start"
    decrypt_extract "$yms_gpg_path_shift" "$DestPath/$YMS_PATH_shift" "4"
    echo "$(date): yms shift task done"
fi

change_dir_to_yms_week
if [ $? = 0 ]; then
    echo "$(date): yms week task start"
    decrypt_extract "$yms_gpg_path_week" "$DestPath/$YMS_PATH_week" "4"
    echo "$(date): yms week task done"
fi



exit 0
#change_dir_to_eng

#change_dir_to_ft


#change_dir_to_slt



#change_dir_to_wat



#change_dir_to_smap
