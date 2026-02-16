c:\windows\microsoft.net\framework\v2.0.50727\installutil c:\pumpestyring\dotnet\pushservice\pushservice.exe
sc config Pushservice depend= MSSQL$SQLEXPRESS
net start pushservice