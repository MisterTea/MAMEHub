pushd Binaries
wget -PTools -N http://10ghost.net/MAMEHubDownloads/Tools/Updater.jar
java -jar Tools/Updater.jar
pushd dist
java -Xmx1g -jar MAMEHubClient.jar
popd
popd
