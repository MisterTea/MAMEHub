gzip -k -f --best csume64
scp csume64.gz www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Mac/csume64_new.gz
ssh www-data@10ghost.net mv /var/www/MAMEHubDownloads/Emulators/Mac/csume64_new.gz /var/www/MAMEHubDownloads/Emulators/Mac/csume64.gz
#cp csume64d ~/Programming/mame/Hub/src/java/
#scp csume64d www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Mac/
#ssh www-data@10ghost.net gzip -f /var/www/MAMEHubDownloads/Emulators/Mac/csume64d
