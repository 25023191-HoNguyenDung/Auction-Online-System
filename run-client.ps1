$mvn = "C:\Users\phamt\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5\bin\mvn.cmd"
Write-Host "Building..." -ForegroundColor Yellow
& $mvn install -DskipTests -q
Write-Host "Running client..." -ForegroundColor Green
& $mvn javafx:run -pl client
