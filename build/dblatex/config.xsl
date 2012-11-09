<?xml version="1.0" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:m="http://www.w3.org/1998/Math/MathML"
                version="1.0">

<!--
////// Update settings to customize the XSLT output to my taste //////
-->

<!-- Disable annoying warnings -->
<xsl:param name="output.quietly">1</xsl:param>
<xsl:param name="keep.relative.image.uris" select="1"/>

<!-- Ensure usage of UTF-8 & unicode -->
<xsl:param name="latex.encoding">utf8</xsl:param>
<xsl:param name="latex.unicode.use">1</xsl:param>

<!-- Control the links and internal references -->
<xsl:param name="ulink.show">1</xsl:param>
<xsl:param name="ulink.footnotes">1</xsl:param>
<xsl:param name="xref.with.number.and.title">1</xsl:param>
<xsl:param name="insert.xref.page.number">yes</xsl:param>
<xsl:param name="insert.link.page.number">yes</xsl:param>

<!-- Misc rendering options -->
<!-- Avoid line overflows when hyphenations are missing -->
<xsl:param name="hyphenation.setup">sloppy</xsl:param>

<!-- Support scaling of listings -->
<xsl:param name="literal.extensions">scale.by.width</xsl:param>

<!-- Don't include any list of tables/figures/etc. -->
<xsl:param name="doc.lot.show"></xsl:param>
<!-- But include a table of contents -->
<xsl:param name="doc.toc.show">1</xsl:param>

<!-- The number of levels that are numbered -->
<xsl:param name="doc.section.depth">3</xsl:param>

<!-- The number of levels that are included in the TOC -->
<xsl:param name="toc.section.depth">3</xsl:param>
<xsl:param name="preface.tocdepth">0</xsl:param>

<!-- Paper size 'Crown Quarto' -->
<xsl:param name="full.bleed" select="0"/>
<xsl:param name="page.width">18.90cm</xsl:param>
<xsl:param name="page.height">24.58cm</xsl:param>
<xsl:param name="page.margin.inner">2cm</xsl:param> <!-- left -->
<xsl:param name="page.margin.outer">2cm</xsl:param> <!-- right -->
<xsl:param name="page.margin.top">1.3cm</xsl:param>
<xsl:param name="page.margin.bottom">1.3cm</xsl:param>

<!-- Fonts used -->
<xsl:param name="xetex.font">
  <xsl:text>\setmainfont{Gentium Basic}&#10;</xsl:text>
  <xsl:text>\setsansfont[Scale=MatchLowercase]{Linux Biolinum O}&#10;</xsl:text>
  <xsl:text>\setmonofont[Scale=MatchLowercase]{DejaVu Sans Mono}&#10;</xsl:text>
</xsl:param>

<!-- In  Full-bleed mode, add crop margins of 0.25" (0.63cm) around
actual page. By default the page is centered as required in this mode.
-->
<xsl:param name="crop.marks">
  <xsl:if test="$full.bleed != 0">
    <xsl:text>1</xsl:text>
  </xsl:if>
  <xsl:if test="not($full.bleed != 0)">
    <xsl:text>0</xsl:text>
  </xsl:if>
</xsl:param>
<xsl:param name="crop.mode">off</xsl:param>

<xsl:param name="crop.page.width">
  <xsl:if test="$full.bleed != 0">
    <xsl:variable name="paper.width"
                  select="number(translate($page.width,'cm','
'))+0.635"/>
    <xsl:value-of select="concat(string($paper.width), 'cm')"/>
  </xsl:if>
</xsl:param>

<xsl:param name="crop.page.height">
  <xsl:if test="$full.bleed != 0">
    <xsl:variable name="paper.height"
                  select="number(translate($page.height,'cm','
'))+0.635"/>
    <xsl:value-of select="concat(string($paper.height), 'cm')"/>
  </xsl:if>
</xsl:param>

<!--
////// Extend the stylesheets to better suit my needs //////
-->
<xsl:param name="figure.emptypage">images/debian.png</xsl:param>
<xsl:param name="ulink.block.symbol">\ding{232}</xsl:param>

<xsl:template match="command|parameter|option">
  <xsl:call-template name="inline.monoseq"/>
</xsl:template>

<xsl:template match="literal">
  <xsl:call-template name="inline.sansseq"/>
</xsl:template>

<xsl:template match="replaceable">
  <xsl:call-template name="inline.italicseq"/>
</xsl:template>

<!-- Disable abstract page -->
<xsl:template match="abstract">
</xsl:template>

<!-- Extend/override generated texts:
     - override page citation text to "(page %p)"
     - auto-add quotes for xref to sidebar
-->
<xsl:param name="local.l10n.xml" select="document('')"/>
<l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">

  <l:l10n language="en">
    <l:gentext key="minitoc" text="Contents"/>
    <l:gentext key="keywordset" text="Keywords"/>
    <l:context name="xref">
      <l:template name="page.citation" text=" (page %p)"/>
    </l:context>
    <l:context name="xref">
      <l:template name="sidebar" text="“%t”"/>
    </l:context>
  </l:l10n>

  <l:l10n language="fr">
    <l:gentext key="minitoc" text="Sommaire"/>
    <l:gentext key="keywordset" text="Mots-cl&#233;s"/>
    <l:context name="xref">
      <l:template name="page.citation" text=" (page %p)"/>
    </l:context>
    <l:context name="xref">
      <l:template name="sidebar" text="« %t »"/>
    </l:context>
  </l:l10n>

</l:i18n>

</xsl:stylesheet>
