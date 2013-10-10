rm -Rf gen-java ~/Programming/MAMEHubServer/gen-java ~/Programming/MAMEHubServer/Cassandra/genpy
find thrift/ -type f | xargs -I repme /Users/jgauci/apache/thrift-0.9.0-dev/compiler/cpp/thrift -gen java repme
find thrift/ -type f | xargs -I repme /Users/jgauci/apache/thrift-0.9.0-dev/compiler/cpp/thrift -gen py:new_style repme
cp -Rf gen-java ~/Programming/MAMEHubServer/
mv gen-py ~/Programming/MAMEHubServer/Cassandra/genpy
