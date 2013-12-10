rm -Rf gen-java ~/Programming/mame/MAMEHubServer/gen-java ~/Programming/mame/MAMEHubServer/Cassandra/genpy
find thrift/ -type f | xargs -I repme thrift -gen java repme
find thrift/ -type f | xargs -I repme thrift -gen py:new_style repme
cp -Rf gen-java ~/Programming/mame/MAMEHubServer/
mv gen-py ~/Programming/mame/MAMEHubServer/Cassandra/genpy
