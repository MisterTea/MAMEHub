./gradlew solid
scp solid/MAMEHubClient-0.0.1-SNAPSHOT.jar pawn@10ghost.net:/var/www/html/MAMEHubDownloads/Frontend/MAMEHubClient.jar
ssh pawn@10ghost.net "gzip -c --best /var/www/html/MAMEHubDownloads/Frontend/MAMEHubClient.jar > /var/www/html/MAMEHubDownloads/Frontend/MAMEHubClient.jar.gz"
