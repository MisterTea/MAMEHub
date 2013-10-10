make -j4 TARGET=ume NOWERROR=1
make -j4 TARGET=ume DEBUG=1 SYMBOLS=1 NOWERROR=1
scp csume64 www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Linux/
