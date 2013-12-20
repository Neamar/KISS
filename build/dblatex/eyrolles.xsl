<?xml version="1.0" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:m="http://www.w3.org/1998/Math/MathML"
                version="1.0">

<!-- Disable babel for a try
<xsl:param name="latex.babel.use">0</xsl:param>
-->

<!-- Paper size wanted by Eyrolles -->
<xsl:param name="full.bleed" select="1"/>
<xsl:param name="page.width">19.00cm</xsl:param>
<xsl:param name="page.height">23.00cm</xsl:param>
<xsl:param name="page.margin.inner">2cm</xsl:param> <!-- left -->
<xsl:param name="page.margin.outer">2cm</xsl:param> <!-- right -->
<xsl:param name="page.margin.top">1.3cm</xsl:param>
<xsl:param name="page.margin.bottom">1.3cm</xsl:param>

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
<xsl:param name="crop.mode">cam</xsl:param>

<xsl:param name="crop.page.width">
  <xsl:if test="$full.bleed != 0">
    <xsl:variable name="paper.width"
                  select="number(translate($page.width,'cm','
'))+1"/>
    <xsl:value-of select="concat(string($paper.width), 'cm')"/>
  </xsl:if>
</xsl:param>

<xsl:param name="crop.page.height">
  <xsl:if test="$full.bleed != 0">
    <xsl:variable name="paper.height"
                  select="number(translate($page.height,'cm','
'))+1"/>
    <xsl:value-of select="concat(string($paper.height), 'cm')"/>
  </xsl:if>
</xsl:param>

</xsl:stylesheet>
