<<<<<<< HEAD
rm -Rf gen-java ~/Programming/mame/MAMEHubServer/gen-java ~/Programming/mame/MAMEHubServer/Cassandra/genpy
find thrift/ -type f | xargs -I repme thrift -gen java repme
find thrift/ -type f | xargs -I repme thrift -gen py:new_style repme
=======
rm -Rf gen-java ~/Programming/MAMEHubServer/gen-java ~/Programming/MAMEHubServer/Cassandra/genpy
find thrift/ -type f | xargs -I repme /Users/jgauci/apache/thrift-0.9.0-dev/compiler/cpp/thrift -gen java repme
find thrift/ -type f | xargs -I repme /Users/jgauci/apache/thrift-0.9.0-dev/compiler/cpp/thrift -gen py:new_style repme
>>>>>>> 321f16a3c44f12e5936c1e8ec2dc22931d574892
cp -Rf gen-java ~/Programming/mame/MAMEHubServer/
mv gen-py ~/Programming/mame/MAMEHubServer/Cassandra/genpy
