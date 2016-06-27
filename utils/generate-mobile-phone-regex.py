#!/usr/bin/env python3

"""Generate Mobile Phone Regex

Usage:
    generate-mobile-phone-regex.py -d [-i INPUT]
    generate-mobile-phone-regex.py [-i INPUT [-o OUTPUT]]

Options:
    -i --input INPUT    Input file path [default: ./libphonenumber/resources/PhoneNumberMetadata.xml]
    -o --output OUTPUT  Output file path [default: ../app/src/main/res/raw/phone_number_textable.re]
    -d --debug          Debug output (instead of concatenating the final regular expression)
    -h --help
"""

import os.path
import sys
import xml.sax
from docopt import docopt


class PhoneNumberContentHandler(xml.sax.handler.ContentHandler):
    def __init__(self):
        xml.sax.handler.ContentHandler.__init__(self)

        self._regexps = {}
        self._path    = []
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
        return self._regexps


def main(debug, input, output):
    base_path = os.path.dirname(__file__)

    filepath_input = input if os.path.isabs(input) else os.path.join(base_path, input)
    filepath_output = output if os.path.isabs(output) else os.path.join(base_path, output)

    handler = PhoneNumberContentHandler()

    parser = xml.sax.make_parser()
    parser.setContentHandler(handler)
    parser.parse(filepath_input)

    if debug:
        from pprint import pprint
        pprint(handler.get_regexps())
        return 0

    file = open(filepath_output, 'w')
    file.write('\\+(?:')
    for idx, (country, regexps) in enumerate(handler.get_regexps().items()):
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


if __name__ == '__main__':
    arguments = docopt(__doc__)
    sys.exit(main(arguments['--debug'],
                  arguments['--input'],
                  arguments['--output']))
