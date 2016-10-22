#!/usr/bin/env python3

"""Generate Mobile Phone Regex

Usage:
    generate-mobile-phone-regex.py -d [-i INPUT]
    generate-mobile-phone-regex.py [-i INPUT [-o OUTPUT] [-O OUTPUT2]]

Options:
    -i --input INPUT             Input XML file path with phone information for all regions (from `libphonenumber`)
                                 [default: ./libphonenumber/resources/PhoneNumberMetadata.xml]
    -o --output-textable OUTPUT  Output file path for regular expression that matches textable phone numbers
                                 [default: ../app/src/main/res/raw/phone_number_textable.re]
    -O --output-prefixes OUTPUT2 Output file path for file mapping country ISO codes to their national and
                                 international phone number prefixes (for normalizing numbers)
                                 [default: ../app/src/main/res/raw/phone_number_prefixes.csv]
    -d --debug                   Debug output (instead of concatenating the final regular expression)
    -h --help                    Show this screen

Remarks:
    This script uses data from googles libphonenumber. Please download
    PhoneNumberMetadata.xml and pass its path to the --input parameter.
    See: https://github.com/googlei18n/libphonenumber
"""

import csv
import os.path
import sys
import xml.sax
from docopt import docopt


class PhoneNumberContentHandler(xml.sax.handler.ContentHandler):
    def __init__(self):
        xml.sax.handler.ContentHandler.__init__(self)

        self._regexps  = {}
        self._normdata = {}
        self._path     = []
        self._country  = None

        self._next_regexp = ""

    def startElement(self, name, attrs):
        self._path.append(name)

        # Only process per-country information
        if len(self._path) < 3            \
        or self._path[1] != 'territories' \
        or self._path[2] != 'territory':
            return

        # Country definition
        if len(self._path) == 3:
            # Remember current phone number code (might be ambigous)
            self._country = attrs['countryCode']

            # Create RegExps storage for country code
            if self._country not in self._regexps:
                self._regexps[self._country] = set()

            # Remember country parameters for normalization
            if len(attrs['countryCode']) == 2:
                self._normdata[attrs['id']] = (
                    attrs['countryCode'],
                    attrs.get('internationalPrefix', ""),
                    attrs.get('nationalPrefix',      "")
                )

    def characters(self, content):
        # Store number pattern content for mobile phone numbers
        if  len(self._path) == 5      \
        and self._path[3] == 'mobile' \
        and self._path[4] == 'nationalNumberPattern':
            self._next_regexp += content.strip(' \t\n')

    def endElement(self, name):
        self._path.pop()

        # Add complete number pattern content to regexp list
        if len(self._path) == 4 and self._next_regexp:
            self._regexps[self._country].add(self._next_regexp)

            self._next_regexp = ""

    def get_regexps(self):
        return sorted(self._regexps.items())

    def get_normdata(self):
        return sorted(self._normdata.items())


def main(debug, input, textable, normdata):
    base_path = os.path.dirname(__file__)

    filepath_input    = input    if os.path.isabs(input)    else os.path.join(base_path, input)
    filepath_textable = textable if os.path.isabs(textable) else os.path.join(base_path, textable)
    filepath_normdata = normdata if os.path.isabs(normdata) else os.path.join(base_path, normdata)

    handler = PhoneNumberContentHandler()

    parser = xml.sax.make_parser()
    parser.setContentHandler(handler)
    parser.parse(filepath_input)

    if debug:
        from pprint import pprint

        print(" • Textable phone number regular expression:")
        pprint(handler.get_regexps())
        print()

        print(" • Country number prefixes:")
        pprint(handler.get_normdata())
        print()

        return 0

    with open(filepath_textable, 'w') as file:
        file.write('\\+(?:')
        for idx, (country, regexps) in enumerate(handler.get_regexps()):
            if idx > 0:
                file.write('|')

            # Group regexp patterns by country dial code
            file.write(country)

            file.write('(?:')
            for idx, regexp in enumerate(regexps):
                if idx > 0:
                    file.write('|')

                file.write(regexp)
            file.write(')')
        file.write(')')

    with open(filepath_normdata, 'w') as file:
        writer = csv.writer(file)
        for country, data in handler.get_normdata():
            writer.writerow((country,) + data)


if __name__ == '__main__':
    arguments = docopt(__doc__)
    sys.exit(main(arguments['--debug'],
                  arguments['--input'],
                  arguments['--output-textable'],
                  arguments['--output-prefixes']))
