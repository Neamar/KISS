<?xml version='1.0'?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'>

<xsl:param name="ignore.image.scaling">1</xsl:param>

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

<!-- Misc publican fixups -->

<!-- Reinstate the docbook-xsl implementation of anchor -->
<xsl:template name="anchor">
  <xsl:param name="node" select="."/>
  <xsl:param name="conditional" select="1"/>
  <xsl:variable name="id">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$node"/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:if test="not($node[parent::blockquote])">
    <xsl:if test="$conditional = 0 or $node/@id or $node/@xml:id">
      <a id="{$id}"/>
    </xsl:if>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>

