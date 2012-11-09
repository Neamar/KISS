<?xml version="1.0"?>
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml"
  version="1.0">

<xsl:import href="/usr/share/xml/docbook/stylesheet/docbook-xsl/epub/docbook.xsl" />

<!-- Parameters -->
<xsl:param name="section.autolabel">1</xsl:param>
<xsl:param name="section.label.includes.component.label">1</xsl:param>
<xsl:param name="section.autolabel.max.depth">4</xsl:param>
<xsl:param name="ignore.image.scaling">1</xsl:param>
<xsl:param name="toc.section.depth">1</xsl:param>

<xsl:param name="make.clean.html">1</xsl:param>
<xsl:param name="docbook.css.link">0</xsl:param>

<!--
Drop the first paragraph in a listitem because when converted to .mobi and
read on a Kindle, it introduces an unwanted empty line after the bullet.
-->
<xsl:template match="itemizedlist/listitem/para[1]|orderedlist/listitem/para[1]">
  <xsl:apply-templates />
</xsl:template>

<!-- Add support for <ulink type="block" … /> -->
<xsl:template match="ulink">
  <xsl:choose>
    <xsl:when test="@type='block'">
      <div class="url">
        <xsl:text>→ </xsl:text>
        <xsl:apply-imports />
      </div>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-imports />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="sidebar">
  <hr class="delimit-sidebar" />
  <xsl:apply-imports />
  <hr class="delimit-sidebar" />
</xsl:template>

<!-- Avoid passing @width -->
<xsl:template match="programlisting|screen|synopsis">
  <xsl:param name="suppress-numbers" select="'0'"/>
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:call-template name="anchor"/>

  <xsl:variable name="div.element">
    <xsl:choose>
      <xsl:when test="$make.clean.html != 0">div</xsl:when>
      <xsl:otherwise>pre</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$suppress-numbers = '0' and @linenumbering = 'numbered' and $use.extensions != '0' and $linenumbering.extension != '0'">
      <xsl:variable name="rtf">
        <xsl:choose>
          <xsl:when test="$highlight.source != 0">
            <xsl:call-template name="apply-highlighting"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:element name="{$div.element}" namespace="http://www.w3.org/1999/xhtml">
        <xsl:apply-templates select="." mode="common.html.attributes"/>
<!--
        <xsl:if test="@width != ''">
          <xsl:attribute name="width">
            <xsl:value-of select="@width"/>
          </xsl:attribute>
        </xsl:if>
-->
        <xsl:call-template name="number.rtf.lines">
          <xsl:with-param name="rtf" select="$rtf"/>
        </xsl:call-template>
      </xsl:element>
    </xsl:when>
    <xsl:otherwise>
      <xsl:element name="{$div.element}" namespace="http://www.w3.org/1999/xhtml">
        <xsl:apply-templates select="." mode="common.html.attributes"/>
<!--
        <xsl:if test="@width != ''">
          <xsl:attribute name="width">
            <xsl:value-of select="@width"/>
          </xsl:attribute>
        </xsl:if>
-->
        <xsl:choose>
          <xsl:when test="$highlight.source != 0">
            <xsl:call-template name="apply-highlighting"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:element>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- Drop subsections from preface -->

<xsl:template match="preface" mode="toc">
  <xsl:param name="toc-context" select="."/>
  <xsl:call-template name="subtoc">
    <xsl:with-param name="toc-context" select="$toc-context"/>
    <xsl:with-param name="nodes" select="foo"/>
  </xsl:call-template>
</xsl:template>

</xsl:stylesheet>
