#!/bin/bash

log_file="./run.log"
temp_file="./temp_.csv"
msc_file="./msc.txt"

if [ -e $1 ]
then
  log_file=$1
fi

Rscript mscconverter.R $log_file $msc_file
mscgen -T png -i $msc_file
rm -f $temp_file
rm -f $msc_file
