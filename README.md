MAMEHub
=======

MAMEHub: A free gaming platform that brings people from different ends of the earth together to experience both arcade and console games online

Imagine walking into the biggest arcade you've ever seen in your life.  There are thousands of arcade cabinets with all of your favorite past-time games, and even rooms full of consoles, including super nintendo, genesis, and even playstation.  Just like a real arcade, you can call your friends and invite them to come and play games, but in this arcade your friends don't have to meet you in the arcade, they don't even have to be on the same continent.

MAMEHub is the world's biggest arcade & living room in one: a massive library of games sitting on a scalable, peer-to-peer platform that can bring people from different ends of the earth together to experience both arcade and console games online.

When it comes to code, MAMEHub is two things:  A fork of MAME & MESS that supports Netplay (coming to github soon), and a client/server interface that allows players from around the word to meet and play games (server coming soon).

Building the MAMEHub Emulator
-----------------------------
The MAMEHub Emulator is a fork of MAME that adds a fully-connected peer-to-peer networking layer.  Building MAMEHub should be the same as building MAME, but for completeness, here are some instructions on a per-OS basis.

### Mac OS/X

1. Download SDL from http://libsdl.org/
2. Open terminal, go to the SDL directory
3. run ./configure --prefix=SDL_INSTALL_DIRECTORY  (replace with an empty directory name)
4. run make install
5. Download and install yasm from http://yasm.tortall.net/
6. In the same terminal, go to the MAMEHub/Emulator directory
7. run export PATH=SDL_INSTALL_DIRECTORY/bin:$PATH
8. run export MACOSX_DEPLOYMENT_TARGET=10.5
9. run make -j8 MACOSX_USE_LIBSDL=1 PTR64=1 NOWERROR=1 TARGET=ume PROFILER=1 SYMBOLS=1 OPTIMIZE=3 CC=/usr/bin/clang LD=/usr/bin/clang++ AR=/usr/bin/ar
10. You should now have a csume64 file which is the emulator binary.

### Linux

1. Install sdl and yasm using a package manager
2. Open a terminal and go to the MAMEHub/Emulator directory
3. run make -j8 NOWERROR=1 TARGET=ume
4. You should now have a csume or csume64 file which is the emulator binary.

### Windows

1. Download the mamedev tools from http://www.mamedev.org/tools/
2. Download yasm from http://yasm.tortall.net/ .  Rename yasm to "yasm.exe" and put in your path.
2. Follow the instructions on that page up to the make command
3. Replace make command with make -j8 NOWERROR=1 TARGET=ume
4. You should now have a csume.exe which is the emulator binary

Building the MAMEHub Client
---------------------------
The MAMEHub Client uses [Maven](http://maven.apache.org/).  To build the client, go to the Client/ directory and run this command in the client directory:

    mvn package

Maven will automatically download the dependencies and build MAMEHub.  Then to run your client, run this command:

    java -jar target/MAMEHubClient-0.0.1-SNAPSHOT.jar
