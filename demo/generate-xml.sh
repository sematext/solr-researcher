echo "<add>"
while read line; do
  echo "  <doc><field name=\"id\"><![CDATA[$line]]></field><field name=\"foo\"><![CDATA[$line]]></field></doc>"
done;
echo "</add>"

