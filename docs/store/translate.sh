#!/bin/bash

# key=$(pcregrep --match-limit=1 --buffer-size=10M -o1 '>t:([^<]+)<' $2)
key=$(cat $2 | perl -n -e'/>t:([^<]+)</ && print $1 and last')
echo $key
translation=$(cat "$1/screenshots.json" | jq -r ".$key")

echo $translation

mkdir -p $(dirname $3)

tempfile=$(mktemp)
cp $2 "$tempfile"
sed -i "s/t:${key}/${translation}/g" "$tempfile"

inkscape "$tempfile" --export-type=png --export-filename=$3 --export-dpi=96
