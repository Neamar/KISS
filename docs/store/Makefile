# Inspired from https://stackoverflow.com/a/32535737/2367848
locales     := fr-FR en-US
templates   := phoneScreenshots/1.svg phoneScreenshots/2.svg phoneScreenshots/3.svg phoneScreenshots/4.svg featureGraphic.svg

localized_screenshots_raw = $(foreach locale, $(locales), $(addprefix ${locale}/, $(templates:.svg=.inkscape.png)))
localized_screenshots = $(foreach locale, $(locales), $(addprefix ${locale}/, $(templates:.svg=.png)))

all: $(localized_screenshots_raw)
tinypng: $(localized_screenshots)

# eval argument is expanded twice; first by the eval function, then the results of that expansion are expanded again
# when they are parsed as makefile syntax. This means you may need to provide extra levels of escaping for “$” characters when using eval.
define LOCALE_rule
$(1)/%.png: $(1)/%.inkscape.png
	curl $$$$(curl --silent --user api:${TINYPNG_API_KEY} --data-binary @$$^ https://api.tinify.com/shrink | jq -r '.output.url') -o $$@

$(1)/%.inkscape.png: %.svg $(1)/screenshots.json
	@echo The locale is $(1) and the target is $$@ in $$(dir $@) from 1: $$^ 2: $$<
	./translate.sh $1 $$< $$@
endef

# Replace eval with info to see what eval evals to!
$(foreach locale,$(locales),$(eval $(call LOCALE_rule,$(locale))))

.PHONY: all clean

clean:
	rm -f $(localized_screenshots) $(localized_screenshots_raw)
