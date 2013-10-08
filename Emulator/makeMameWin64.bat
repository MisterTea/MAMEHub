set PATH=G:\mingw-mame\mingw64-w64\bin;c:\MinGW\msys\1.0\bin;%PATH%
make -j8 NOWERROR=1 DIRECT3D=9 DIRECTINPUT=8 PTR64=1 MAXOPT=1 TARGET=ume SYMBOLS=1 OPTIMIZE=3 PROFILER=1 SYMLEVEL=1
make -j8 NOWERROR=1 DIRECT3D=9 DIRECTINPUT=8 PTR64=1 TARGET=ume DEBUG=1
scp csume64.exe www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/

