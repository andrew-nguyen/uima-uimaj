<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output 
    method="xml" 
    encoding="UTF-8"
    omit-xml-declaration="no"

    indent="yes"
/>
<xsl:template match="/"> 
  <xsl:copy-of select="."/>
</xsl:template>

</xsl:stylesheet>