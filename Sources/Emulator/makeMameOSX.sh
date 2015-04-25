#!/bin/bash
set -e

export PATH=~/Libraries/sdl2/out/bin:$PATH
export MACOSX_DEPLOYMENT_TARGET=10.5
nice nice make -j8 MACOSX_USE_LIBSDL=1 PTR64=1 NOWERROR=1 TARGET=ume PROFILER=1 SYMBOLS=1 SYMLEVEL=line-tables-only OPTIMIZE=3 SANITIZE=address CC=~/programs/clang/bin/clang LD=~/programs/clang/bin/clang++ AR=/usr/bin/ar
nice nice make -j8 MACOSX_USE_LIBSDL=1 PTR64=1 NOWERROR=1 TARGET=ume DEBUG=1 SYMBOLS=1 SYMLEVEL=line-tables-only PROFILER=1 SYMBOLS=1 SANITIZE=address CC=~/programs/clang/bin/clang LD=~/programs/clang/bin/clang++ AR=/usr/bin/ar
