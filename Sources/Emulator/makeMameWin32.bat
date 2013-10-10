set PATH=G:\mingw-mame\mingw64-w32\bin;c:\MinGW\msys\1.0\bin;%PATH%
make -j8 NOWERROR=1 DIRECT3D=9 DIRECTINPUT=8 PTR64=0 MAXOPT=1 TARGET=ume SYMBOLS=1 OPTIMIZE=3 PROFILER=1 SYMLEVEL=1 ARCHOPTS=-march=pentiumpro
make -j8 NOWERROR=1 DIRECT3D=9 DIRECTINPUT=8 PTR64=0 TARGET=ume DEBUG=1
scp csume.exe www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Windows/

