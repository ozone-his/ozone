#!/usr/bin/env bash

exclusionFileName=$1
targetDir=$2

echo "Parsing '$exclusionFileName' to identify exclusions..."
cat $exclusionFileName | while read line || [ -n "$line" ]
do
  if [ "${line:0:1}" != "#" ]; then
    echo "Evaluating RegEx \"$line\" for possible matching exclusions..."
    count=$(find ${targetDir} -regex ${line} | wc -l)
    if [ "$count" -ne "0" ]; then 
      echo "Found the following match(es): "
      find ${targetDir} -regex ${line}
      find ${targetDir} -regex ${line} | xargs rm -r
      echo "$count file(s) excluded."
    else
      echo "No file matches the exclusion pattern."
    fi
  fi
done
