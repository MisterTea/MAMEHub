#!/bin/bash
set -e

export PATH=~/Libraries/SDL2-2.0.0/out/bin:$PATH
export MACOSX_DEPLOYMENT_TARGET=10.5
nice nice make -j8 MACOSX_USE_LIBSDL=1 PTR64=1 NOWERROR=1 TARGET=ume PROFILER=1 SYMBOLS=1 SYMLEVEL=line-tables-only OPTIMIZE=3 CC=/usr/bin/clang LD=/usr/bin/clang++ AR=/usr/bin/ar
nice nice make -j8 MACOSX_USE_LIBSDL=1 PTR64=1 NOWERROR=1 TARGET=ume DEBUG=1 SYMBOLS=1 SYMLEVEL=line-tables-only PROFILER=1 SYMBOLS=1 CC=/usr/bin/clang LD=/usr/bin/clang++ AR=/usr/bin/ar
