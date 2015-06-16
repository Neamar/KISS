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

<!-- Add <link rel="canonical"> to the headers -->
<xsl:template name="user.head.content">
  <xsl:variable name="filename">
    <xsl:apply-templates mode="chunk-filename" select="."/>
  </xsl:variable>
  <xsl:variable name="lang-scope"
		select="ancestor-or-self::*
		        [@lang or @xml:lang][1]"/>
  <xsl:variable name="lang" select="string(($lang-scope/@lang | $lang-scope/@xml:lang)[1])"/>
  <link rel="canonical">
    <xsl:attribute name="href">
      <xsl:value-of select="'https://debian-handbook.info/browse/'"/>
      <xsl:choose>
        <xsl:when test="$lang = 'en-US'">
	  <xsl:value-of select="'stable/'"/>
        </xsl:when>
        <xsl:when test="$lang = 'en'">
	  <xsl:value-of select="'stable/'"/>
        </xsl:when>
        <xsl:when test="contains($lang, '-')">
	  <xsl:value-of select="concat($lang, '/stable/')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:message terminate="yes">
            The language code ({$lang}) is not fully qualified as we expect it.
          </xsl:message>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:if test="$filename != 'index.html'">
        <xsl:value-of select="$filename"/>
      </xsl:if>
    </xsl:attribute>
  </link>
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

