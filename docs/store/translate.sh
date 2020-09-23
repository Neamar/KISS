#!/bin/bash

# exit when any command fails
set -euo pipefail

key=$(cat $2 | perl -n -e'/>t:([^<]+)</ && print $1 and last')

echo "key: $key"

if [ ! -z "$key" ]; then
  if [ "$key" == "featureGraphic.subtitle" ]; then
    translation=$(cat "../../fastlane/metadata/android/$1/short_description.txt" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')
  else
    translation=$(cat "$1.json" | jq -r ".$key")
  fi

  echo $translation
  sed "s/t:${key}/${translation}/g" $2 | inkscape --pipe --export-type=png --export-filename=$3 --export-dpi=96
else
  cat $2 | inkscape --pipe --export-type=png --export-filename=$3 --export-dpi=96
fi


