gzip -k -f --best csume64
scp csume64.gz pawn@10ghost.net:/var/www/html/MAMEHubDownloads/Emulators/Mac/csume64_new.gz
ssh pawn@10ghost.net mv /var/www/html/MAMEHubDownloads/Emulators/Mac/csume64_new.gz /var/www/html/MAMEHubDownloads/Emulators/Mac/csume64.gz
#cp csume64d ~/Programming/mame/Hub/src/java/
#scp csume64d www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Mac/
#ssh www-data@10ghost.net gzip -f /var/www/MAMEHubDownloads/Emulators/Mac/csume64d
