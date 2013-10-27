"C:\Program Files (x86)\Git\bin\scp.exe" csume64.exe www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/
"C:\Program Files (x86)\Git\bin\scp.exe" csume64.sym www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/
"C:\Program Files (x86)\Git\bin\ssh.exe" www-data@10ghost.net gzip -f /var/www/MAMEHubDownloads/Emulators/Windows/csume64.exe
"C:\Program Files (x86)\Git\bin\ssh.exe" www-data@10ghost.net gzip -f /var/www/MAMEHubDownloads/Emulators/Windows/csume64.sym
