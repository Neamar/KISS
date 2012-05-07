
dia = $(wildcard *.dia)

all: png

png: $(patsubst %.dia,%.png,$(dia))

eps: $(patsubst %.jpg,%.eps,$(wildcard *.jpg)) $(patsubst %.png,%.eps,$(wildcard *.png)) $(patsubst %.dia,%.eps,$(dia))

define DIA_template
$(1).png: $(1).dia
	dia -t png -s 1024 $$<
endef

$(foreach d,$(patsubst %.dia,%,$(dia)),$(eval $(call DIA_template,$(d))))

.PHONY: png all
