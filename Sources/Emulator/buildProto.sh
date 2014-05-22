protoc -I=src/emu --cpp_out=src/emu src/emu/nsm.proto
mv src/emu/nsm.pb.cc src/emu/nsm.pb.c
