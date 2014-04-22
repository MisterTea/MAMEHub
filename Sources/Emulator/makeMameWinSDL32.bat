set PATH=c:\mingw-mame\mingw64-w32\bin;c:\MinGW\msys\1.0\bin;%PATH%
make NOWERROR=1 OSD="sdl" PTR64=0 TARGET=mame
echo make -j4 NOWERROR=1 OSD="sdl" PTR64=0 TARGET=mame DEBUG=1 SYMBOLS=1

