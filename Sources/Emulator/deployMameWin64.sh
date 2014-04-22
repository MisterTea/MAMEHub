set -e
scp csume64.exe www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/
scp csume64.sym www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/
ssh www-data@10ghost.net gzip -f /var/www/MAMEHubDownloads/Emulators/Windows/csume64.exe
ssh www-data@10ghost.net gzip -f /var/www/MAMEHubDownloads/Emulators/Windows/csume64.sym
