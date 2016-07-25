echo "Need admin password to install packages"
sudo apt-get install wget libfontconfig1-dev libgconf2-dev libgtk2.0-dev libsdl2-ttf-dev yasm libqt4-dev aria2 libSDL2-dev
MACHINE_TYPE=`uname -m`
if [ ${MACHINE_TYPE} == 'x86_64' ]; then
	sudo apt-get install libfontconfig1:i386 libpng12-0:i386 libasound2:i386 libqt4-gui:i386 libsdl1.2debian:i386 libSDL-ttf2.0-0:i386 libxinerama1:i386
fi

