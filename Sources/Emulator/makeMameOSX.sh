export PATH=~/Libraries/sdl/out/bin:$PATH
export MACOSX_DEPLOYMENT_TARGET=10.5
nice nice make -j8 MACOSX_USE_LIBSDL=1 PTR64=1 NOWERROR=1 TARGET=ume PROFILER=1 SYMBOLS=1 OPTIMIZE=3 CC=/usr/bin/clang LD=/usr/bin/clang++ AR=/usr/bin/ar
echo nice nice make -j8 MACOSX_USE_LIBSDL=1 PTR64=1 NOWERROR=1 TARGET=ume DEBUG=1 SYMBOLS=1 PROFILER=1 SYMBOLS=1 CC=/usr/bin/clang LD=/usr/bin/clang++ AR=/usr/bin/ar
cp csume64 ~/Programming/mame/Hub/src/java/
scp csume64 www-data@10ghost.net:/var/www/MAMEHubDownloads/Emulators/Mac/
