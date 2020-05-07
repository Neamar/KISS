images = 1.svg.png 2.svg.png 3.svg.png 4.svg.png

all: $(images)
pngcrush: $(images:.svg.png=.svg.crushed.png)
tinypng: $(images:.svg.png=.svg.tinypng.png)

%.svg.crushed.png: %.svg.png
	@echo building $@ in $(dir $@) from $^
	pngcrush -reduce -brute $^ $@

%.svg.tinypng.png: %.svg.png
	@echo building $@ in $(dir $@) from $^
	curl $$(curl --silent --user api:${TINYPNG_API_KEY} --data-binary @$^ https://api.tinify.com/shrink | jq -r '.output.url') -o $@

%.svg.png: %.svg
	@echo building $@ in $(dir $@) from $^
	inkscape --file=$^ --export-png=$@ --export-dpi=96

clean:
	rm -f $(images) $(images:.svg.png=.svg.tinypng.png)
