#!/usr/bin/env python3
import os.path
import sys
import xml.sax


FILEPATH_INPUT  = "libphonenumber/resources/PhoneNumberMetadata.xml"
FILEPATH_OUTPUT = "../app/src/main/res/raw/phone_number_textable.re"

class PhoneNumberContentHandler(xml.sax.handler.ContentHandler):
	def __init__(self):
		xml.sax.handler.ContentHandler.__init__(self)
		
		self._regexps = {}
		self._path    = []
		self._contry  = None
		
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


def main(argv=sys.argv[1:], program=sys.argv[0]):
	# Debug output (instead of concatenating the final regular expression)
	if len(argv) > 0 and argv[0] == '-d':
		debugging = True
		argv = argv[1:]
	else:
		debugging = False
	
	# Input file path
	if len(argv) > 0:
		filepath_input = argv[1]
		argv = argv[1:]
	else:
		filepath_input = os.path.join(os.path.dirname(__file__), FILEPATH_INPUT)
	
	# Output file path (ignored in debugging mode)
	if len(argv) > 0:
		filepath_output = argv[1]
		argv = argv[1:]
	else:
		filepath_output = os.path.join(os.path.dirname(__file__), FILEPATH_OUTPUT)
	
	handler = PhoneNumberContentHandler()
	
	parser = xml.sax.make_parser()
	parser.setContentHandler(handler)
	parser.parse(filepath_input)
	
	if debugging:
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
	sys.exit(main())
