###########################################################################
#
#   windows.mak
#
#   UME Windows-specific makefile
#
###########################################################################

UME_WINSRC = src/ume/osd/windows
UME_WINOBJ = $(OBJ)/ume/osd/windows

OBJDIRS += \
	$(UMEOBJ)/osd \
	$(UMEOBJ)/osd/windows

RESFILE = $(UME_WINOBJ)/ume.res

$(LIBOSD): $(OSDOBJS)

$(LIBOCORE): $(OSDCOREOBJS)

$(LIBOCORE_NOMAIN): $(OSDCOREOBJS:$(WINOBJ)/main.o=)

#-------------------------------------------------
# generic rules for the resource compiler
#-------------------------------------------------

$(UME_WINOBJ)/%.res: $(UME_WINSRC)/%.rc
	@echo Compiling resources $<...
ifdef MXE
	/Users/jgauci/github/mxe//usr/bin/i686-pc-mingw32-windres $(RCDEFS) $(RCFLAGS) --include-dir $(UME_WINOBJ) -o $@ -i $<
else
	$(RC) $(RCDEFS) $(RCFLAGS) --include-dir $(UME_WINOBJ) -o $@ -i $<
endif


#-------------------------------------------------
# rules for resource file
#-------------------------------------------------

$(RESFILE): $(UME_WINSRC)/ume.rc $(UME_WINOBJ)/umevers.rc

$(UME_WINOBJ)/umevers.rc: $(SRC)/build/verinfo.py $(SRC)/version.c
	@echo Emitting $@...
	$(PYTHON) $(SRC)/build/verinfo.py -b ume -o $@ $(SRC)/version.c
