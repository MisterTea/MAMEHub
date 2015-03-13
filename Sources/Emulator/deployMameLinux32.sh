gzip --best -k -f csume
scp csume.gz www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Linux32/csume_new.gz
ssh www-data@10ghost.net mv /var/www/MAMEHubDownloads/Emulators/Linux32/csume_new.gz /var/www/MAMEHubDownloads/Emulators/Linux32/csume.gz
