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

cd en-US

dbtoepub -s ../epub/tdah-epub.xsl -c ../epub/tdah.css $OPTS debian-handbook.xml
if [ -x /usr/bin/ebook-convert ]; then
    dbtoepub -s ../epub/tdah-epub.xsl -c ../epub/tdah-plain.css \
	-o debian-handbook-plain.epub $OPTS debian-handbook.xml
    ebook-convert debian-handbook-plain.epub debian-handbook.mobi \
	--output-profile=kindle \
	--chapter="/" \
	--no-chapters-in-toc \
	--isbn=979-10-91414-01-2 \
	--tags=Debian,Linux,Computing,Administration \
	--cover=images/cover.png \
	--mobi-ignore-margins \
	--margin-left=2 \
	--margin-right=2
fi
echo "SUCCESS"
