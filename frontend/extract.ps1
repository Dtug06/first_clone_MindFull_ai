$word = New-Object -ComObject Word.Application
$doc = $word.Documents.Open("C:\Users\ADMIN\OneDrive\Desktop\MINDBRIDGE\MINDBRIDGE_Final_V2.docx")
$doc.Content.Text | Out-File -FilePath "C:\Users\ADMIN\OneDrive\Desktop\MINDBRIDGE\content.txt" -Encoding UTF8
$doc.Close()
$word.Quit()
Write-Host "Done"
