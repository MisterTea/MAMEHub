set -e

gzip --best < csume.exe > csume.exe.gz

scp csume.exe.gz www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/csume_new.exe.gz
#scp csume.sym www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/

ssh www-data@10ghost.net cp /var/www/MAMEHubDownloads/Emulators/Windows/csume_new.exe.gz /var/www/MAMEHubDownloads/Emulators/Windows/csume.exe.gz
ssh www-data@10ghost.net mv /var/www/MAMEHubDownloads/Emulators/Windows/csume_new.exe.gz /var/www/MAMEHubDownloads/Emulators/Windows/csume64.exe.gz

#ssh www-data@10ghost.net gzip -f /var/www/MAMEHubDownloads/Emulators/Windows/csume.sym

#scp csume.exe www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/csume64.exe
#scp csume.sym www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/csume64.sym
#ssh www-data@10ghost.net gzip -f /var/www/MAMEHubDownloads/Emulators/Windows/csume64.exe
#ssh www-data@10ghost.net gzip -f /var/www/MAMEHubDownloads/Emulators/Windows/csume64.sym

scp csumed.exe www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/
ssh www-data@10ghost.net gzip -f /var/www/MAMEHubDownloads/Emulators/Windows/csumed.exe

scp csumed.exe www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/csume64d.exe
ssh www-data@10ghost.net gzip -f /var/www/MAMEHubDownloads/Emulators/Windows/csume64d.exe
