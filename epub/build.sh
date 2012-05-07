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

#cp epub/tdah-epub.xsl epub/tdah.css tmp/$lang/xml/
#cd tmp/$lang/xml/
cd en-US

echo "RUN: dbtoepub -s ../epub/tdah-epub.xsl -c ../epub/tdah-plain.css -o debian-handbook-plain.epub $OPTS debian-handbook.xml"
dbtoepub -s ../epub/tdah-epub.xsl -c ../epub/tdah-plain.css -o debian-handbook-plain.epub $OPTS debian-handbook.xml
echo "RUN: dbtoepub -s ../epub/tdah-epub.xsl -c ../epub/tdah.css $OPTS debian-handbook.xml"
if dbtoepub -s ../epub/tdah-epub.xsl -c ../epub/tdah.css $OPTS debian-handbook.xml; then
    echo "SUCCESS: en-US/debian-handbook.epub"
else
    echo "FAILURE"
fi

