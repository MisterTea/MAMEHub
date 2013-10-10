rm -Rf src/emu/gen-cpp
find thrift/ -type f | xargs -I repme /Users/jgauci/apache/thrift-0.9.0-dev/compiler/cpp/thrift -gen cpp repme
mv gen-cpp src/emu/
pushd src/emu/gen-cpp/; for f in *.cpp; do mv $f `basename $f .cpp`.c; done; popd;

