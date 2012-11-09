<?xml version="1.0" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:m="http://www.w3.org/1998/Math/MathML"
                version="1.0">

<xsl:import href="config.xsl"/>

<!-- Add backmatter to the default layout -->
<xsl:param name="doc.layout">coverpage toc frontmatter mainmatter backmatter index </xsl:param>

<xsl:param name="latex.class.options">twoside,11pt</xsl:param>
<xsl:param name="latex.enddocument">
  <!-- Force empty page at the end, required by lulu for distribution -->
  <xsl:text>&#10;\clearpage&#10;</xsl:text>
  <xsl:text>\thispagestyle{empty}&#10;</xsl:text>
  <xsl:text>\hbox{}&#10;</xsl:text>
  <xsl:text>\end{document}&#10;</xsl:text>
</xsl:param>
<xsl:param name="geometry.options">headheight=0cm,headsep=0cm</xsl:param>
<xsl:param name="hyphenation.custom"/>

<xsl:template name="user.params.set">
  <xsl:apply-imports />
  <xsl:if test="$figure.emptypage">
    <xsl:text>\def\DBKEmptyPagePicture{</xsl:text>
    <xsl:value-of select="$figure.emptypage" />
    <xsl:text>}&#10;</xsl:text>
  </xsl:if>
  <xsl:if test="$hyphenation.custom">
    <xsl:text>\usepackage{hyphenat}&#10;</xsl:text>
    <xsl:text>\hyphenation{</xsl:text>
    <xsl:value-of select="$hyphenation.custom" />
    <xsl:text>}&#10;</xsl:text>
  </xsl:if>

  <xsl:text>\newcommand\minitocname{</xsl:text>
  <xsl:call-template name="gentext">
    <xsl:with-param name="key" select="'minitoc'"/>
  </xsl:call-template>
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<!-- Use custom symbol before URL -->
<xsl:param name="ulink.block.symbol">\textcolor{textgray}{\ding{232}}</xsl:param>

<xsl:template match="mediaobject" mode="formatchapter">
  <xsl:text>\setchapterimage{</xsl:text>
  <xsl:value-of select="imageobject[1]/imagedata/@fileref"/>
  <xsl:text>}</xsl:text>
</xsl:template>

<xsl:template match="chapterinfo/keywordset" mode="formatchapter">
  <xsl:text>&#10;\keywordsname{</xsl:text>
  <xsl:call-template name="gentext">
    <xsl:with-param name="key" select="'keywordset'"/>
  </xsl:call-template>
  <xsl:text>}&#10;</xsl:text>
  <xsl:text>&#10;\keywords</xsl:text>
  <xsl:apply-templates mode="formatchapter" />
  <xsl:text>\endkeywords&#10;</xsl:text>
</xsl:template>

<xsl:template match="chapterinfo//keyword" mode="formatchapter">
  <xsl:text>{</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>}</xsl:text>
</xsl:template>

<xsl:template match="highlights" mode="formatchapter">
  <xsl:text>&#10;\highlights{</xsl:text>
  <xsl:apply-templates />
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="highlights">
</xsl:template>

<xsl:template match="chapterinfo">
  <xsl:apply-templates select="*[not(self::mediaobject)][not(self::keywordset)]"/>
</xsl:template>

<xsl:template match="chapter">
  <xsl:apply-templates select="chapterinfo//mediaobject" mode="formatchapter"/>
  <xsl:apply-templates select="chapterinfo//keywordset" mode="formatchapter"/>
  <xsl:apply-templates select="highlights[1]" mode="formatchapter"/>
  <xsl:apply-imports/>
</xsl:template>

<xsl:template match="emphasis" mode="sidebar-category">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="sidebar/title/emphasis[count(preceding-sibling::node()) = 0]">
</xsl:template>

<xsl:template match="sidebar/title">
  <xsl:text>{</xsl:text>
  <xsl:apply-templates select="emphasis[count(preceding-sibling::node()) = 0]"
                       mode="sidebar-category" />
  <xsl:text>}{</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>}</xsl:text>
</xsl:template>

</xsl:stylesheet>
