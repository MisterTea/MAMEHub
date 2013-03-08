rm -Rf gen-java
find thrift/ -type f | xargs -I repme thrift -gen java repme
