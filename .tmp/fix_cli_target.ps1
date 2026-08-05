$f = 'c:\\Users\\ADMIN\\OneDrive\\Desktop\\first_clone_MindFull_ai\\backend\\src\\main\\java\\com\\mindbridge\\behavior\\feature\\job\\cli\\DailyFeatureAggregationCliTargetParser.java'
$c = [System.IO.File]::ReadAllText($f)
$c = $c.Replace('public final static class','public final class')
$c = $c.Replace('USER requires UUID:DADE:','USER requires UUID:DATE:DATE')
[System.IO.File]::WriteAllText($f,$c)