#!/bin/sh

set -e

test -e publican.cfg || (echo "Run it from the publican top level dir please" >&2; exit 1)

TEMP=$(getopt -o l:so: -l lang:,skip,opts: -- "$@")

eval set -- "$TEMP"

lang="en-US"
skip=""
while true; do
    case "$1" in
	-l|--lang) lang="$2"; shift 2; ;;
	-o|--opts) DBLATEX_OPTS="$2"; shift 2; ;;
	-s|--skip) skip=1; shift; ;;
	--) shift; break; ;;
	*) echo "Invalid option $1"; exit 1; ;;
    esac
done

cd en-US/

echo "RUN: dblatex -c ../dblatex/mydblatex.conf $DBLATEX_OPTS debian-handbook.xml"
if dblatex -c ../dblatex/librement.conf $DBLATEX_OPTS debian-handbook.xml; then
    echo "SUCCESS: en-US/debian-handbook.pdf"
else
    echo "FAILURE"
fi

