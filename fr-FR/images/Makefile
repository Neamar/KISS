
dia = $(wildcard *.dia)

all: png
eps: $(patsubst %.jpg,%.eps,$(wildcard *.jpg)) $(patsubst %.png,%.eps,$(wildcard *.png)) $(patsubst %.dia,%.eps,$(dia))
png: $(patsubst %.dia,%.png,$(dia))

clean::
	rm -f $(wildcard *.eps)

define DIA_template
$(1).eps: $(1).dia
	dia -t eps $$<

$(1).png: $(1).dia
	dia -t png -s 1024 $$<
endef

$(foreach d,$(patsubst %.dia,%,$(dia)),$(eval $(call DIA_template,$(d))))

%.eps: %.png
	convert $< $@

%.eps: %.jpg
	convert $< $@

.PHONY: eps png clean all
