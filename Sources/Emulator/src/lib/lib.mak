###########################################################################
#
#   lib.mak
#
#   MAME dependent library makefile
#
#   Copyright Nicola Salmoria and the MAME Team.
#   Visit http://mamedev.org for licensing and usage restrictions.
#
###########################################################################


LIBSRC = $(SRC)/lib
LIBOBJ = $(OBJ)/lib

OBJDIRS += \
	$(LIBOBJ)/util \
	$(LIBOBJ)/expat \
	$(LIBOBJ)/formats \
	$(LIBOBJ)/zlib \
	$(LIBOBJ)/softfloat \
	$(LIBOBJ)/libjpeg \
	$(LIBOBJ)/libflac \
	$(LIBOBJ)/lib7z \
	$(LIBOBJ)/portmidi \
	$(LIBOBJ)/lua \
    $(LIBOBJ)/thrift \
    $(LIBOBJ)/thrift/transport \
    $(LIBOBJ)/thrift/protocol \
    $(LIBOBJ)/thrift/server \
    $(LIBOBJ)/thrift/windows \
    $(LIBOBJ)/protobuf \
    $(LIBOBJ)/protobuf/google \
    $(LIBOBJ)/protobuf/google/protobuf \
    $(LIBOBJ)/protobuf/google/protobuf/io \
    $(LIBOBJ)/protobuf/google/protobuf/stubs \
	$(LIBOBJ)/RakNet \
	$(LIBOBJ)/boost_thread \
	$(LIBOBJ)/boost_thread/libs \
	$(LIBOBJ)/boost_thread/libs/thread \
	$(LIBOBJ)/boost_thread/libs/thread/pthread \
	$(LIBOBJ)/boost_thread/libs/thread/win32 \
	$(LIBOBJ)/webm \
	$(LIBOBJ)/webm/libmkv \
	$(LIBOBJ)/webm/ogg \
	$(LIBOBJ)/webm/vorbis \
	$(LIBOBJ)/webm/vp8 \
	$(LIBOBJ)/webm/vp8/common \
	$(LIBOBJ)/webm/vp8/common/arm \
	$(LIBOBJ)/webm/vp8/common/arm/neon \
	$(LIBOBJ)/webm/vp8/common/generic \
	$(LIBOBJ)/webm/vp8/common/x86 \
	$(LIBOBJ)/webm/vp8/decoder \
	$(LIBOBJ)/webm/vp8/decoder/arm \
	$(LIBOBJ)/webm/vp8/decoder/arm/armv6 \
	$(LIBOBJ)/webm/vp8/decoder/arm/neon \
	$(LIBOBJ)/webm/vp8/decoder/generic \
	$(LIBOBJ)/webm/vp8/decoder/x86 \
	$(LIBOBJ)/webm/vp8/encoder \
	$(LIBOBJ)/webm/vp8/encoder/arm \
	$(LIBOBJ)/webm/vp8/encoder/arm/neon \
	$(LIBOBJ)/webm/vp8/encoder/generic \
	$(LIBOBJ)/webm/vp8/encoder/ppc \
	$(LIBOBJ)/webm/vp8/encoder/x86 \
	$(LIBOBJ)/webm/vpx \
	$(LIBOBJ)/webm/vpx/src \
	$(LIBOBJ)/webm/vpx_mem \
	$(LIBOBJ)/webm/vpx_mem/memory_manager \
	$(LIBOBJ)/webm/vpx_ports \
	$(LIBOBJ)/webm/vpx_scale \
	$(LIBOBJ)/webm/vpx_scale/generic \
	$(LIBOBJ)/webm/libwebm \
	$(LIBOBJ)/web \
	$(LIBOBJ)/web/json \


#-------------------------------------------------
# utility library objects
#-------------------------------------------------

UTILOBJS = \
	$(LIBOBJ)/util/astring.o \
	$(LIBOBJ)/util/avhuff.o \
	$(LIBOBJ)/util/aviio.o \
	$(LIBOBJ)/util/bitmap.o \
	$(LIBOBJ)/util/cdrom.o \
	$(LIBOBJ)/util/chd.o \
	$(LIBOBJ)/util/chdcd.o \
	$(LIBOBJ)/util/chdcodec.o \
	$(LIBOBJ)/util/corefile.o \
	$(LIBOBJ)/util/corestr.o \
	$(LIBOBJ)/util/coreutil.o \
	$(LIBOBJ)/util/flac.o \
	$(LIBOBJ)/util/harddisk.o \
	$(LIBOBJ)/util/hashing.o \
	$(LIBOBJ)/util/huffman.o \
	$(LIBOBJ)/util/jedparse.o \
	$(LIBOBJ)/util/md5.o \
	$(LIBOBJ)/util/opresolv.o \
	$(LIBOBJ)/util/options.o \
	$(LIBOBJ)/util/palette.o \
	$(LIBOBJ)/util/plaparse.o \
	$(LIBOBJ)/util/png.o \
	$(LIBOBJ)/util/pool.o \
	$(LIBOBJ)/util/sha1.o \
	$(LIBOBJ)/util/unicode.o \
	$(LIBOBJ)/util/unzip.o \
	$(LIBOBJ)/util/un7z.o \
	$(LIBOBJ)/util/vbiparse.o \
	$(LIBOBJ)/util/xmlfile.o \
	$(LIBOBJ)/util/zippath.o \

$(OBJ)/libutil.a: $(UTILOBJS)

PROTOBUFOBJS = \
	$(LIBOBJ)/protobuf/google/protobuf/descriptor.o\
	$(LIBOBJ)/protobuf/google/protobuf/descriptor.pb.o\
	$(LIBOBJ)/protobuf/google/protobuf/descriptor_database.o\
	$(LIBOBJ)/protobuf/google/protobuf/dynamic_message.o\
	$(LIBOBJ)/protobuf/google/protobuf/extension_set.o\
	$(LIBOBJ)/protobuf/google/protobuf/extension_set_heavy.o\
	$(LIBOBJ)/protobuf/google/protobuf/generated_message_reflection.o\
	$(LIBOBJ)/protobuf/google/protobuf/generated_message_util.o\
	$(LIBOBJ)/protobuf/google/protobuf/message.o\
	$(LIBOBJ)/protobuf/google/protobuf/message_lite.o\
	$(LIBOBJ)/protobuf/google/protobuf/reflection_ops.o\
	$(LIBOBJ)/protobuf/google/protobuf/repeated_field.o\
	$(LIBOBJ)/protobuf/google/protobuf/service.o\
	$(LIBOBJ)/protobuf/google/protobuf/text_format.o\
	$(LIBOBJ)/protobuf/google/protobuf/unknown_field_set.o\
	$(LIBOBJ)/protobuf/google/protobuf/wire_format.o\
	$(LIBOBJ)/protobuf/google/protobuf/wire_format_lite.o\
\
	$(LIBOBJ)/protobuf/google/protobuf/io/coded_stream.o\
	$(LIBOBJ)/protobuf/google/protobuf/io/lzma_protobuf_stream.o\
	$(LIBOBJ)/protobuf/google/protobuf/io/gzip_stream.o\
	$(LIBOBJ)/protobuf/google/protobuf/io/printer.o\
	$(LIBOBJ)/protobuf/google/protobuf/io/tokenizer.o\
	$(LIBOBJ)/protobuf/google/protobuf/io/zero_copy_stream.o\
	$(LIBOBJ)/protobuf/google/protobuf/io/zero_copy_stream_impl.o\
	$(LIBOBJ)/protobuf/google/protobuf/io/zero_copy_stream_impl_lite.o\
\
	$(LIBOBJ)/protobuf/google/protobuf/stubs/common.o\
	$(LIBOBJ)/protobuf/google/protobuf/stubs/once.o\
	$(LIBOBJ)/protobuf/google/protobuf/stubs/structurally_valid.o\
	$(LIBOBJ)/protobuf/google/protobuf/stubs/strutil.o\
	$(LIBOBJ)/protobuf/google/protobuf/stubs/substitute.o\


$(OBJ)/libprotobuf.a: $(PROTOBUFOBJS)

$(LIBOBJ)/protobuf/google/protobuf/%.o: $(LIBSRC)/protobuf/google/protobuf/%.cc | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) -I$(LIBSRC)/protobuf $(CDEFS) $(CCOMFLAGS) -c $< -o $@
$(LIBOBJ)/protobuf/google/protobuf/io/%.o: $(LIBSRC)/protobuf/google/protobuf/io/%.cc | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) -I$(LIBSRC)/protobuf $(CDEFS) $(CCOMFLAGS) -c $< -o $@
$(LIBOBJ)/protobuf/google/protobuf/stubs/%.o: $(LIBSRC)/protobuf/google/protobuf/stubs/%.cc | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) -I$(LIBSRC)/protobuf $(CDEFS) $(CCOMFLAGS) -c $< -o $@

THRIFTOBJS = \
	$(LIBOBJ)/thrift/TApplicationException.o\
	$(LIBOBJ)/thrift/Thrift.o\
	$(LIBOBJ)/thrift/VirtualProfiling.o\
\
	$(LIBOBJ)/thrift/transport/TBufferTransports.o\
	$(LIBOBJ)/thrift/transport/TFDTransport.o\
	$(LIBOBJ)/thrift/transport/TFileTransport.o\
	$(LIBOBJ)/thrift/transport/THttpClient.o\
	$(LIBOBJ)/thrift/transport/THttpServer.o\
	$(LIBOBJ)/thrift/transport/THttpTransport.o\
	$(LIBOBJ)/thrift/transport/TServerSocket.o\
	$(LIBOBJ)/thrift/transport/TSimpleFileTransport.o\
	$(LIBOBJ)/thrift/transport/TSocket.o\
	$(LIBOBJ)/thrift/transport/TSocketPool.o\
	$(LIBOBJ)/thrift/transport/TTransportException.o\
	$(LIBOBJ)/thrift/transport/TTransportUtils.o\
	$(LIBOBJ)/thrift/transport/TZlibTransport.o\
\
	$(LIBOBJ)/thrift/protocol/TBase64Utils.o\
	$(LIBOBJ)/thrift/protocol/TJSONProtocol.o\
\
	$(LIBOBJ)/thrift/server/TServer.o\
	$(LIBOBJ)/thrift/server/TSimpleServer.o\
\

ifeq ($(TARGETOS),win32)
THRIFTOBJS += \
	$(LIBOBJ)/thrift/windows/WinFcntl.o\
	$(LIBOBJ)/thrift/windows/TWinsockSingleton.o\
	$(LIBOBJ)/thrift/windows/SocketPair.o\

endif

$(OBJ)/libthrift.a: $(THRIFTOBJS)

$(LIBOBJ)/thrift/%.o: $(LIBSRC)/thrift/%.cpp | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) -c $< -o $@

$(LIBOBJ)/thrift/transport/%.o: $(LIBSRC)/thrift/transport/%.cpp | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) -c $< -o $@

$(LIBOBJ)/thrift/protocol/%.o: $(LIBSRC)/thrift/protocol/%.cpp | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) -c $< -o $@

$(LIBOBJ)/thrift/server/%.o: $(LIBSRC)/thrift/server/%.cpp | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) -c $< -o $@

$(LIBOBJ)/thrift/windows/%.o: $(LIBSRC)/thrift/windows/%.cpp | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) -c $< -o $@


WEBMARMOBJS = \
$(LIBOBJ)/webm/vp8/common/arm/arm_systemdependent.o \
$(LIBOBJ)/webm/vp8/common/arm/bilinearfilter_arm.o \
$(LIBOBJ)/webm/vp8/common/arm/filter_arm.o \
$(LIBOBJ)/webm/vp8/common/arm/loopfilter_arm.o \
$(LIBOBJ)/webm/vp8/common/arm/neon/recon_neon.o \
$(LIBOBJ)/webm/vp8/common/arm/reconintra_arm.o \
$(LIBOBJ)/webm/vpx_ports/arm_cpudetect.o \
$(LIBOBJ)/webm/vp8/decoder/arm/arm_dsystemdependent.o \
$(LIBOBJ)/webm/vp8/decoder/arm/armv6/idct_blk_v6.o \
$(LIBOBJ)/webm/vp8/decoder/arm/dequantize_arm.o \
$(LIBOBJ)/webm/vp8/decoder/arm/neon/idct_blk_neon.o \
$(LIBOBJ)/webm/vp8/encoder/arm/arm_csystemdependent.o \
$(LIBOBJ)/webm/vp8/encoder/arm/boolhuff_arm.o \
$(LIBOBJ)/webm/vp8/encoder/arm/dct_arm.o \
$(LIBOBJ)/webm/vp8/encoder/arm/neon/picklpf_arm.o \
$(LIBOBJ)/webm/vp8/encoder/arm/quantize_arm.o \
$(LIBOBJ)/webm/vp8/encoder/arm/variance_arm.o \
$(LIBOBJ)/webm/vpx_scale/arm/neon/yv12extend_arm.o \
$(LIBOBJ)/webm/vpx_scale/arm/scalesystemdependent.o \

WEBMPPCOBJS = \
$(LIBOBJ)/webm/vp8/common/ppc/loopfilter_altivec.o \
$(LIBOBJ)/webm/vp8/common/ppc/systemdependent.o \
$(LIBOBJ)/webm/vp8/encoder/ppc/csystemdependent.o \

WEBMWIN32OBJS = \
$(LIBOBJ)/webm/vpx_scale/win32/scaleopt.o \
$(LIBOBJ)/webm/vpx_scale/win32/scalesystemdependent.o \

WEBMX86ASMOBJS = \
$(LIBOBJ)/webm/vp8/common/x86/idctllm_mmx.o \
$(LIBOBJ)/webm/vp8/common/x86/iwalsh_mmx.o \
$(LIBOBJ)/webm/vp8/common/x86/recon_mmx.o \
$(LIBOBJ)/webm/vp8/common/x86/subpixel_mmx.o \
$(LIBOBJ)/webm/vp8/common/x86/loopfilter_mmx.o \
$(LIBOBJ)/webm/vp8/common/x86/idctllm_sse2.o \
$(LIBOBJ)/webm/vp8/common/x86/recon_sse2.o \
$(LIBOBJ)/webm/vp8/common/x86/subpixel_sse2.o \
$(LIBOBJ)/webm/vp8/common/x86/loopfilter_sse2.o \
$(LIBOBJ)/webm/vp8/common/x86/iwalsh_sse2.o \
$(LIBOBJ)/webm/vp8/common/x86/subpixel_ssse3.o \
$(LIBOBJ)/webm/vp8/common/x86/postproc_mmx.o \
$(LIBOBJ)/webm/vp8/common/x86/postproc_sse2.o \
$(LIBOBJ)/webm/vp8/encoder/x86/variance_impl_mmx.o \
$(LIBOBJ)/webm/vp8/encoder/x86/sad_mmx.o \
$(LIBOBJ)/webm/vp8/encoder/x86/dct_mmx.o \
$(LIBOBJ)/webm/vp8/encoder/x86/subtract_mmx.o \
$(LIBOBJ)/webm/vp8/encoder/x86/dct_sse2.o \
$(LIBOBJ)/webm/vp8/encoder/x86/variance_impl_sse2.o \
$(LIBOBJ)/webm/vp8/encoder/x86/sad_sse2.o \
$(LIBOBJ)/webm/vp8/encoder/x86/fwalsh_sse2.o \
$(LIBOBJ)/webm/vp8/encoder/x86/quantize_sse2.o \
$(LIBOBJ)/webm/vp8/encoder/x86/subtract_sse2.o \
$(LIBOBJ)/webm/vp8/encoder/x86/temporal_filter_apply_sse2.o \
$(LIBOBJ)/webm/vp8/encoder/x86/sad_sse3.o \
$(LIBOBJ)/webm/vp8/encoder/x86/sad_ssse3.o \
$(LIBOBJ)/webm/vp8/encoder/x86/variance_impl_ssse3.o \
$(LIBOBJ)/webm/vp8/encoder/x86/quantize_ssse3.o \
$(LIBOBJ)/webm/vp8/encoder/x86/sad_sse4.o \
$(LIBOBJ)/webm/vp8/encoder/x86/quantize_sse4.o \
$(LIBOBJ)/webm/vp8/encoder/x86/quantize_mmx.o \
$(LIBOBJ)/webm/vp8/encoder/x86/encodeopt.o \
$(LIBOBJ)/webm/vp8/decoder/x86/dequantize_mmx.o \
$(LIBOBJ)/webm/vpx_ports/emms.o \
$(LIBOBJ)/webm/vpx_ports/x86_abi_support.o \

WEBMOBJS = \
$(LIBOBJ)/webm/libmkv/EbmlWriter.o \
$(LIBOBJ)/webm/ogg/bitwise.o \
$(LIBOBJ)/webm/ogg/framing.o \
$(LIBOBJ)/webm/RGB2YV12.o \
$(LIBOBJ)/webm/tools_common.o \
$(LIBOBJ)/webm/vorbis/analysis.o \
$(LIBOBJ)/webm/vorbis/barkmel.o \
$(LIBOBJ)/webm/vorbis/bitrate.o \
$(LIBOBJ)/webm/vorbis/block.o \
$(LIBOBJ)/webm/vorbis/codebook.o \
$(LIBOBJ)/webm/vorbis/envelope.o \
$(LIBOBJ)/webm/vorbis/floor0.o \
$(LIBOBJ)/webm/vorbis/floor1.o \
$(LIBOBJ)/webm/vorbis/info.o \
$(LIBOBJ)/webm/vorbis/lookup.o \
$(LIBOBJ)/webm/vorbis/lpc.o \
$(LIBOBJ)/webm/vorbis/lsp.o \
$(LIBOBJ)/webm/vorbis/mapping0.o \
$(LIBOBJ)/webm/vorbis/mdct.o \
$(LIBOBJ)/webm/vorbis/psy.o \
$(LIBOBJ)/webm/vorbis/registry.o \
$(LIBOBJ)/webm/vorbis/res0.o \
$(LIBOBJ)/webm/vorbis/sharedbook.o \
$(LIBOBJ)/webm/vorbis/smallft.o \
$(LIBOBJ)/webm/vorbis/synthesis.o \
$(LIBOBJ)/webm/vorbis/tone.o \
$(LIBOBJ)/webm/vorbis/vorbisenc.o \
$(LIBOBJ)/webm/vorbis/vorbisfile.o \
$(LIBOBJ)/webm/vorbis/window.o \
$(LIBOBJ)/webm/vp8/common/alloccommon.o \
$(LIBOBJ)/webm/vp8/common/asm_com_offsets.o \
$(LIBOBJ)/webm/vp8/common/blockd.o \
$(LIBOBJ)/webm/vp8/common/context.o \
$(LIBOBJ)/webm/vp8/common/debugmodes.o \
$(LIBOBJ)/webm/vp8/common/entropy.o \
$(LIBOBJ)/webm/vp8/common/entropymode.o \
$(LIBOBJ)/webm/vp8/common/entropymv.o \
$(LIBOBJ)/webm/vp8/common/extend.o \
$(LIBOBJ)/webm/vp8/common/filter.o \
$(LIBOBJ)/webm/vp8/common/findnearmv.o \
$(LIBOBJ)/webm/vp8/common/generic/systemdependent.o \
$(LIBOBJ)/webm/vp8/common/idctllm.o \
$(LIBOBJ)/webm/vp8/common/invtrans.o \
$(LIBOBJ)/webm/vp8/common/loopfilter.o \
$(LIBOBJ)/webm/vp8/common/loopfilter_filters.o \
$(LIBOBJ)/webm/vp8/common/mbpitch.o \
$(LIBOBJ)/webm/vp8/common/modecont.o \
$(LIBOBJ)/webm/vp8/common/modecontext.o \
$(LIBOBJ)/webm/vp8/common/postproc.o \
$(LIBOBJ)/webm/vp8/common/quant_common.o \
$(LIBOBJ)/webm/vp8/common/recon.o \
$(LIBOBJ)/webm/vp8/common/reconinter.o \
$(LIBOBJ)/webm/vp8/common/reconintra.o \
$(LIBOBJ)/webm/vp8/common/reconintra4x4.o \
$(LIBOBJ)/webm/vp8/common/setupintrarecon.o \
$(LIBOBJ)/webm/vp8/common/swapyv12buffer.o \
$(LIBOBJ)/webm/vp8/common/textblit.o \
$(LIBOBJ)/webm/vp8/common/treecoder.o \
$(LIBOBJ)/webm/vp8/common/x86/loopfilter_x86.o \
$(LIBOBJ)/webm/vp8/common/x86/recon_wrapper_sse2.o \
$(LIBOBJ)/webm/vp8/common/x86/vp8_asm_stubs.o \
$(LIBOBJ)/webm/vp8/common/x86/x86_systemdependent.o \
$(LIBOBJ)/webm/vp8/decoder/asm_dec_offsets.o \
$(LIBOBJ)/webm/vp8/decoder/dboolhuff.o \
$(LIBOBJ)/webm/vp8/decoder/decodemv.o \
$(LIBOBJ)/webm/vp8/decoder/decodframe.o \
$(LIBOBJ)/webm/vp8/decoder/dequantize.o \
$(LIBOBJ)/webm/vp8/decoder/detokenize.o \
$(LIBOBJ)/webm/vp8/decoder/generic/dsystemdependent.o \
$(LIBOBJ)/webm/vp8/decoder/idct_blk.o \
$(LIBOBJ)/webm/vp8/decoder/onyxd_if.o \
$(LIBOBJ)/webm/vp8/decoder/reconintra_mt.o \
$(LIBOBJ)/webm/vp8/decoder/threading.o \
$(LIBOBJ)/webm/vp8/decoder/x86/idct_blk_mmx.o \
$(LIBOBJ)/webm/vp8/decoder/x86/idct_blk_sse2.o \
$(LIBOBJ)/webm/vp8/decoder/x86/x86_dsystemdependent.o \
$(LIBOBJ)/webm/vp8/encoder/asm_enc_offsets.o \
$(LIBOBJ)/webm/vp8/encoder/bitstream.o \
$(LIBOBJ)/webm/vp8/encoder/boolhuff.o \
$(LIBOBJ)/webm/vp8/encoder/dct.o \
$(LIBOBJ)/webm/vp8/encoder/encodeframe.o \
$(LIBOBJ)/webm/vp8/encoder/encodeintra.o \
$(LIBOBJ)/webm/vp8/encoder/encodemb.o \
$(LIBOBJ)/webm/vp8/encoder/encodemv.o \
$(LIBOBJ)/webm/vp8/encoder/ethreading.o \
$(LIBOBJ)/webm/vp8/encoder/firstpass.o \
$(LIBOBJ)/webm/vp8/encoder/generic/csystemdependent.o \
$(LIBOBJ)/webm/vp8/encoder/lookahead.o \
$(LIBOBJ)/webm/vp8/encoder/mcomp.o \
$(LIBOBJ)/webm/vp8/encoder/modecosts.o \
$(LIBOBJ)/webm/vp8/encoder/onyx_if.o \
$(LIBOBJ)/webm/vp8/encoder/pickinter.o \
$(LIBOBJ)/webm/vp8/encoder/picklpf.o \
$(LIBOBJ)/webm/vp8/encoder/psnr.o \
$(LIBOBJ)/webm/vp8/encoder/quantize.o \
$(LIBOBJ)/webm/vp8/encoder/ratectrl.o \
$(LIBOBJ)/webm/vp8/encoder/rdopt.o \
$(LIBOBJ)/webm/vp8/encoder/sad_c.o \
$(LIBOBJ)/webm/vp8/encoder/segmentation.o \
$(LIBOBJ)/webm/vp8/encoder/temporal_filter.o \
$(LIBOBJ)/webm/vp8/encoder/tokenize.o \
$(LIBOBJ)/webm/vp8/encoder/treewriter.o \
$(LIBOBJ)/webm/vp8/encoder/variance_c.o \
$(LIBOBJ)/webm/vp8/encoder/x86/variance_mmx.o \
$(LIBOBJ)/webm/vp8/encoder/x86/variance_sse2.o \
$(LIBOBJ)/webm/vp8/encoder/x86/variance_ssse3.o \
$(LIBOBJ)/webm/vp8/encoder/x86/x86_csystemdependent.o \
$(LIBOBJ)/webm/vp8/vp8_cx_iface.o \
$(LIBOBJ)/webm/vp8/vp8_dx_iface.o \
$(LIBOBJ)/webm/vpx/src/vpx_codec.o \
$(LIBOBJ)/webm/vpx/src/vpx_decoder.o \
$(LIBOBJ)/webm/vpx/src/vpx_decoder_compat.o \
$(LIBOBJ)/webm/vpx/src/vpx_encoder.o \
$(LIBOBJ)/webm/vpx/src/vpx_image.o \
$(LIBOBJ)/webm/vpx_mem/memory_manager/hmm_alloc.o \
$(LIBOBJ)/webm/vpx_mem/memory_manager/hmm_base.o \
$(LIBOBJ)/webm/vpx_mem/memory_manager/hmm_dflt_abort.o \
$(LIBOBJ)/webm/vpx_mem/memory_manager/hmm_grow.o \
$(LIBOBJ)/webm/vpx_mem/memory_manager/hmm_largest.o \
$(LIBOBJ)/webm/vpx_mem/memory_manager/hmm_resize.o \
$(LIBOBJ)/webm/vpx_mem/memory_manager/hmm_shrink.o \
$(LIBOBJ)/webm/vpx_mem/memory_manager/hmm_true.o \
$(LIBOBJ)/webm/vpx_mem/vpx_mem.o \
$(LIBOBJ)/webm/vpx_mem/vpx_mem_tracker.o \
$(LIBOBJ)/webm/vpx_ports/x86_cpuid.o \
$(LIBOBJ)/webm/vpx_scale/generic/gen_scalers.o \
$(LIBOBJ)/webm/vpx_scale/generic/scalesystemdependent.o \
$(LIBOBJ)/webm/vpx_scale/generic/vpxscale.o \
$(LIBOBJ)/webm/vpx_scale/generic/yv12config.o \
$(LIBOBJ)/webm/vpx_scale/generic/yv12extend.o \
$(LIBOBJ)/webm/WebMEncoder.o \
$(LIBOBJ)/webm/libwebm/mkvmuxer.o \
$(LIBOBJ)/webm/libwebm/mkvmuxerutil.o \
$(LIBOBJ)/webm/libwebm/mkvparser.o \
$(LIBOBJ)/webm/libwebm/mkvreader.o \
$(LIBOBJ)/webm/libwebm/mkvwriter.o \

$(OBJ)/libwebm.a: $(WEBMOBJS) $(WEBMX86ASMOBJS)

ifeq ($(TARGETOS),macosx)
$(LIBOBJ)/webm/%.o: $(LIBSRC)/webm/%.asm | $(OSPREBUILD)
	@echo Compiling ASM FILE $<...
	yasm -f macho64 $(INCPATH) -o $@ $<
endif
ifeq ($(TARGETOS),linux)
ifeq ($(PTR64),1)
$(LIBOBJ)/webm/%.o: $(LIBSRC)/webm/%.asm | $(OSPREBUILD)
	@echo Compiling ASM FILE $<...
	yasm -f elf64 $(INCPATH) -o $@ $<
else
$(LIBOBJ)/webm/%.o: $(LIBSRC)/webm/%.asm | $(OSPREBUILD)
	@echo Compiling ASM FILE $<...
	yasm -f elf32 $(INCPATH) -o $@ $<
endif
endif
ifeq ($(TARGETOS),win32)
ifeq ($(PTR64),1)
$(LIBOBJ)/webm/%.o: $(LIBSRC)/webm/%.asm | $(OSPREBUILD)
	@echo Compiling ASM FILE $<...
	yasm -f x64 $(INCPATH) -o $@ $<
else
$(LIBOBJ)/webm/%.o: $(LIBSRC)/webm/%.asm | $(OSPREBUILD)
	@echo Compiling ASM FILE $<...
	yasm -f win32 $(INCPATH) -o $@ $<
endif
endif

$(LIBOBJ)/webm/%.o: $(LIBSRC)/webm/%.c | $(OSPREBUILD)
	@echo Compiling C FILE $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) $(CONLYFLAGS) -c $< -o $@

$(LIBOBJ)/webm/%.o: $(LIBSRC)/webm/%.cpp | $(OSPREBUILD)
	@echo Compiling CPLUSPLUS FILE $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) -c $< -o $@

RAKNETOBJS = \
$(LIBOBJ)/RakNet/_FindFirst.o \
$(LIBOBJ)/RakNet/Base64Encoder.o \
$(LIBOBJ)/RakNet/BitStream.o \
$(LIBOBJ)/RakNet/CCRakNetSlidingWindow.o \
$(LIBOBJ)/RakNet/CCRakNetUDT.o \
$(LIBOBJ)/RakNet/CheckSum.o \
$(LIBOBJ)/RakNet/CloudClient.o \
$(LIBOBJ)/RakNet/CloudCommon.o \
$(LIBOBJ)/RakNet/CloudServer.o \
$(LIBOBJ)/RakNet/CommandParserInterface.o \
$(LIBOBJ)/RakNet/ConnectionGraph2.o \
$(LIBOBJ)/RakNet/ConsoleServer.o \
$(LIBOBJ)/RakNet/DataCompressor.o \
$(LIBOBJ)/RakNet/DirectoryDeltaTransfer.o \
$(LIBOBJ)/RakNet/DR_SHA1.o \
$(LIBOBJ)/RakNet/DS_BytePool.o \
$(LIBOBJ)/RakNet/DS_ByteQueue.o \
$(LIBOBJ)/RakNet/DS_HuffmanEncodingTree.o \
$(LIBOBJ)/RakNet/DS_Table.o \
$(LIBOBJ)/RakNet/DynDNS.o \
$(LIBOBJ)/RakNet/EmailSender.o \
$(LIBOBJ)/RakNet/EpochTimeToString.o \
$(LIBOBJ)/RakNet/FileList.o \
$(LIBOBJ)/RakNet/FileListTransfer.o \
$(LIBOBJ)/RakNet/FileOperations.o \
$(LIBOBJ)/RakNet/FormatString.o \
$(LIBOBJ)/RakNet/FullyConnectedMesh2.o \
$(LIBOBJ)/RakNet/Getche.o \
$(LIBOBJ)/RakNet/Gets.o \
$(LIBOBJ)/RakNet/GetTime.o \
$(LIBOBJ)/RakNet/gettimeofday.o \
$(LIBOBJ)/RakNet/GridSectorizer.o \
$(LIBOBJ)/RakNet/HTTPConnection.o \
$(LIBOBJ)/RakNet/HTTPConnection2.o \
$(LIBOBJ)/RakNet/IncrementalReadInterface.o \
$(LIBOBJ)/RakNet/Itoa.o \
$(LIBOBJ)/RakNet/LinuxStrings.o \
$(LIBOBJ)/RakNet/LocklessTypes.o \
$(LIBOBJ)/RakNet/LogCommandParser.o \
$(LIBOBJ)/RakNet/MessageFilter.o \
$(LIBOBJ)/RakNet/NatPunchthroughClient.o \
$(LIBOBJ)/RakNet/NatPunchthroughServer.o \
$(LIBOBJ)/RakNet/NatTypeDetectionClient.o \
$(LIBOBJ)/RakNet/NatTypeDetectionCommon.o \
$(LIBOBJ)/RakNet/NatTypeDetectionServer.o \
$(LIBOBJ)/RakNet/NetworkIDManager.o \
$(LIBOBJ)/RakNet/NetworkIDObject.o \
$(LIBOBJ)/RakNet/PacketConsoleLogger.o \
$(LIBOBJ)/RakNet/PacketFileLogger.o \
$(LIBOBJ)/RakNet/PacketizedTCP.o \
$(LIBOBJ)/RakNet/PacketLogger.o \
$(LIBOBJ)/RakNet/PacketOutputWindowLogger.o \
$(LIBOBJ)/RakNet/PluginInterface2.o \
$(LIBOBJ)/RakNet/PS4Includes.o \
$(LIBOBJ)/RakNet/Rackspace.o \
$(LIBOBJ)/RakNet/RakMemoryOverride.o \
$(LIBOBJ)/RakNet/RakNetCommandParser.o \
$(LIBOBJ)/RakNet/RakNetSocket.o \
$(LIBOBJ)/RakNet/RakNetSocket2.o \
$(LIBOBJ)/RakNet/RakNetSocket2_360_720.o \
$(LIBOBJ)/RakNet/RakNetSocket2_Berkley.o \
$(LIBOBJ)/RakNet/RakNetSocket2_Berkley_NativeClient.o \
$(LIBOBJ)/RakNet/RakNetSocket2_NativeClient.o \
$(LIBOBJ)/RakNet/RakNetSocket2_PS3_PS4.o \
$(LIBOBJ)/RakNet/RakNetSocket2_PS4.o \
$(LIBOBJ)/RakNet/RakNetSocket2_Vita.o \
$(LIBOBJ)/RakNet/RakNetSocket2_Windows_Linux.o \
$(LIBOBJ)/RakNet/RakNetSocket2_Windows_Linux_360.o \
$(LIBOBJ)/RakNet/RakNetSocket2_WindowsStore8.o \
$(LIBOBJ)/RakNet/RakNetStatistics.o \
$(LIBOBJ)/RakNet/RakNetTransport2.o \
$(LIBOBJ)/RakNet/RakNetTypes.o \
$(LIBOBJ)/RakNet/RakPeer.o \
$(LIBOBJ)/RakNet/RakSleep.o \
$(LIBOBJ)/RakNet/RakString.o \
$(LIBOBJ)/RakNet/RakThread.o \
$(LIBOBJ)/RakNet/RakWString.o \
$(LIBOBJ)/RakNet/Rand.o \
$(LIBOBJ)/RakNet/RandSync.o \
$(LIBOBJ)/RakNet/ReadyEvent.o \
$(LIBOBJ)/RakNet/RelayPlugin.o \
$(LIBOBJ)/RakNet/ReliabilityLayer.o \
$(LIBOBJ)/RakNet/ReplicaManager3.o \
$(LIBOBJ)/RakNet/Router2.o \
$(LIBOBJ)/RakNet/RPC4Plugin.o \
$(LIBOBJ)/RakNet/SecureHandshake.o \
$(LIBOBJ)/RakNet/SendToThread.o \
$(LIBOBJ)/RakNet/SignaledEvent.o \
$(LIBOBJ)/RakNet/SimpleMutex.o \
$(LIBOBJ)/RakNet/SocketLayer.o \
$(LIBOBJ)/RakNet/StatisticsHistory.o \
$(LIBOBJ)/RakNet/StringCompressor.o \
$(LIBOBJ)/RakNet/StringTable.o \
$(LIBOBJ)/RakNet/SuperFastHash.o \
$(LIBOBJ)/RakNet/TableSerializer.o \
$(LIBOBJ)/RakNet/TCPInterface.o \
$(LIBOBJ)/RakNet/TeamBalancer.o \
$(LIBOBJ)/RakNet/TeamManager.o \
$(LIBOBJ)/RakNet/TelnetTransport.o \
$(LIBOBJ)/RakNet/ThreadsafePacketLogger.o \
$(LIBOBJ)/RakNet/TwoWayAuthentication.o \
$(LIBOBJ)/RakNet/UDPForwarder.o \
$(LIBOBJ)/RakNet/UDPProxyClient.o \
$(LIBOBJ)/RakNet/UDPProxyCoordinator.o \
$(LIBOBJ)/RakNet/UDPProxyServer.o \
$(LIBOBJ)/RakNet/VariableDeltaSerializer.o \
$(LIBOBJ)/RakNet/VariableListDeltaTracker.o \
$(LIBOBJ)/RakNet/VariadicSQLParser.o \
$(LIBOBJ)/RakNet/VitaIncludes.o \
$(LIBOBJ)/RakNet/WSAStartupSingleton.o

$(OBJ)/libraknet.a: $(RAKNETOBJS)

$(LIBOBJ)/RakNet/%.o: $(LIBSRC)/RakNet/%.cpp | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) -c $< -o $@

BOOSTTHREADWINDOWSOBJS = \
$(LIBOBJ)/boost_thread/libs/thread/win32/tss_dll.o \
$(LIBOBJ)/boost_thread/libs/thread/win32/tss_pe.o \
$(LIBOBJ)/boost_thread/libs/thread/win32/thread.o

BOOSTTHREADPOSIXOBJS = \
$(LIBOBJ)/boost_thread/libs/thread/pthread/once.o \
$(LIBOBJ)/boost_thread/libs/thread/pthread/thread.o

$(OBJ)/libboostthreadwindows.a: $(BOOSTTHREADWINDOWSOBJS)

$(OBJ)/libboostthreadposix.a: $(BOOSTTHREADPOSIXOBJS)

$(LIBOBJ)/boost_thread/libs/thread/pthread/%.o: $(LIBSRC)/boost_thread/libs/thread/pthread/%.cpp | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) -c $< -o $@

$(LIBOBJ)/boost_thread/libs/thread/win32/%.o: $(LIBSRC)/boost_thread/libs/thread/win32/%.cpp | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) -c $< -o $@

#-------------------------------------------------
# expat library objects
#-------------------------------------------------

EXPATOBJS = \
	$(LIBOBJ)/expat/xmlparse.o \
	$(LIBOBJ)/expat/xmlrole.o \
	$(LIBOBJ)/expat/xmltok.o

$(OBJ)/libexpat.a: $(EXPATOBJS)

$(LIBOBJ)/expat/%.o: $(LIBSRC)/expat/%.c | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) $(CONLYFLAGS) -c $< -o $@



#-------------------------------------------------
# formats library objects
#-------------------------------------------------

FORMATSOBJS = \
	$(LIBOBJ)/formats/cassimg.o     \
	$(LIBOBJ)/formats/flopimg.o     \
	$(LIBOBJ)/formats/imageutl.o    \
	$(LIBOBJ)/formats/ioprocs.o     \
	$(LIBOBJ)/formats/basicdsk.o    \
	$(LIBOBJ)/formats/a26_cas.o     \
	$(LIBOBJ)/formats/a5105_dsk.o   \
	$(LIBOBJ)/formats/ace_tap.o     \
	$(LIBOBJ)/formats/adam_cas.o    \
	$(LIBOBJ)/formats/adam_dsk.o    \
	$(LIBOBJ)/formats/ami_dsk.o     \
	$(LIBOBJ)/formats/ap2_dsk.o     \
	$(LIBOBJ)/formats/apf_apt.o     \
	$(LIBOBJ)/formats/apridisk.o    \
	$(LIBOBJ)/formats/apollo_dsk.o  \
	$(LIBOBJ)/formats/ap_dsk35.o    \
	$(LIBOBJ)/formats/applix_dsk.o  \
	$(LIBOBJ)/formats/asst128_dsk.o \
	$(LIBOBJ)/formats/atari_dsk.o   \
	$(LIBOBJ)/formats/atarist_dsk.o \
	$(LIBOBJ)/formats/atom_tap.o    \
	$(LIBOBJ)/formats/bw2_dsk.o     \
	$(LIBOBJ)/formats/bw12_dsk.o    \
	$(LIBOBJ)/formats/cbm_tap.o     \
	$(LIBOBJ)/formats/cgen_cas.o    \
	$(LIBOBJ)/formats/coco_cas.o    \
	$(LIBOBJ)/formats/coco_dsk.o    \
	$(LIBOBJ)/formats/comx35_dsk.o  \
	$(LIBOBJ)/formats/coupedsk.o    \
	$(LIBOBJ)/formats/cpis_dsk.o    \
	$(LIBOBJ)/formats/cqm_dsk.o     \
	$(LIBOBJ)/formats/csw_cas.o     \
	$(LIBOBJ)/formats/d64_dsk.o     \
	$(LIBOBJ)/formats/d67_dsk.o     \
	$(LIBOBJ)/formats/d80_dsk.o     \
	$(LIBOBJ)/formats/d81_dsk.o     \
	$(LIBOBJ)/formats/d88_dsk.o     \
	$(LIBOBJ)/formats/dfi_dsk.o     \
	$(LIBOBJ)/formats/dim_dsk.o     \
	$(LIBOBJ)/formats/dsk_dsk.o     \
	$(LIBOBJ)/formats/ep64_dsk.o    \
	$(LIBOBJ)/formats/esq8_dsk.o    \
	$(LIBOBJ)/formats/esq16_dsk.o   \
	$(LIBOBJ)/formats/fdi_dsk.o     \
	$(LIBOBJ)/formats/fm7_cas.o     \
	$(LIBOBJ)/formats/fmsx_cas.o    \
	$(LIBOBJ)/formats/g64_dsk.o     \
	$(LIBOBJ)/formats/gtp_cas.o     \
	$(LIBOBJ)/formats/hect_dsk.o    \
	$(LIBOBJ)/formats/hect_tap.o    \
	$(LIBOBJ)/formats/iq151_dsk.o   \
	$(LIBOBJ)/formats/imd_dsk.o     \
	$(LIBOBJ)/formats/ipf_dsk.o     \
	$(LIBOBJ)/formats/kaypro_dsk.o  \
	$(LIBOBJ)/formats/kc_cas.o      \
	$(LIBOBJ)/formats/kc85_dsk.o    \
	$(LIBOBJ)/formats/kim1_cas.o    \
	$(LIBOBJ)/formats/lviv_lvt.o    \
	$(LIBOBJ)/formats/m20_dsk.o     \
	$(LIBOBJ)/formats/m5_dsk.o      \
	$(LIBOBJ)/formats/mbee_dsk.o    \
	$(LIBOBJ)/formats/mm_dsk.o      \
	$(LIBOBJ)/formats/msx_dsk.o     \
	$(LIBOBJ)/formats/mfi_dsk.o     \
	$(LIBOBJ)/formats/mz_cas.o      \
	$(LIBOBJ)/formats/nanos_dsk.o   \
	$(LIBOBJ)/formats/nes_dsk.o     \
	$(LIBOBJ)/formats/orao_cas.o    \
	$(LIBOBJ)/formats/oric_dsk.o    \
	$(LIBOBJ)/formats/oric_tap.o    \
	$(LIBOBJ)/formats/p6001_cas.o   \
	$(LIBOBJ)/formats/pasti_dsk.o   \
	$(LIBOBJ)/formats/pc_dsk.o      \
	$(LIBOBJ)/formats/pc98_dsk.o    \
	$(LIBOBJ)/formats/pc98fdi_dsk.o \
	$(LIBOBJ)/formats/pmd_cas.o     \
	$(LIBOBJ)/formats/primoptp.o    \
	$(LIBOBJ)/formats/pyldin_dsk.o  \
	$(LIBOBJ)/formats/rk_cas.o      \
	$(LIBOBJ)/formats/sc3000_bit.o  \
	$(LIBOBJ)/formats/sf7000_dsk.o  \
	$(LIBOBJ)/formats/smx_dsk.o     \
	$(LIBOBJ)/formats/sorc_dsk.o    \
	$(LIBOBJ)/formats/sord_cas.o    \
	$(LIBOBJ)/formats/st_dsk.o      \
	$(LIBOBJ)/formats/svi_cas.o     \
	$(LIBOBJ)/formats/svi_dsk.o     \
	$(LIBOBJ)/formats/td0_dsk.o     \
	$(LIBOBJ)/formats/thom_cas.o    \
	$(LIBOBJ)/formats/thom_dsk.o    \
	$(LIBOBJ)/formats/ti99_dsk.o    \
	$(LIBOBJ)/formats/tiki100_dsk.o \
	$(LIBOBJ)/formats/trd_dsk.o     \
	$(LIBOBJ)/formats/trs_cas.o     \
	$(LIBOBJ)/formats/trs_dsk.o     \
	$(LIBOBJ)/formats/tvc_cas.o     \
	$(LIBOBJ)/formats/tvc_dsk.o     \
	$(LIBOBJ)/formats/tzx_cas.o     \
	$(LIBOBJ)/formats/uef_cas.o     \
	$(LIBOBJ)/formats/upd765_dsk.o  \
	$(LIBOBJ)/formats/vg5k_cas.o    \
	$(LIBOBJ)/formats/vt_cas.o      \
	$(LIBOBJ)/formats/vt_dsk.o      \
	$(LIBOBJ)/formats/vtech1_dsk.o  \
	$(LIBOBJ)/formats/wavfile.o     \
	$(LIBOBJ)/formats/wd177x_dsk.o  \
	$(LIBOBJ)/formats/x07_cas.o     \
	$(LIBOBJ)/formats/x1_tap.o      \
	$(LIBOBJ)/formats/xdf_dsk.o     \
	$(LIBOBJ)/formats/z80ne_dsk.o   \
	$(LIBOBJ)/formats/zx81_p.o      \
	$(LIBOBJ)/formats/hxcmfm_dsk.o  \

$(OBJ)/libformats.a: $(FORMATSOBJS)



#-------------------------------------------------
# zlib library objects
#-------------------------------------------------

ifdef DEBUG
ZLIBOPTS=-Dverbose=-1
endif

ZLIBOPTS += -DZLIB_CONST -Wno-strict-prototypes

ZLIBOBJS = \
	$(LIBOBJ)/zlib/adler32.o \
	$(LIBOBJ)/zlib/compress.o \
	$(LIBOBJ)/zlib/crc32.o \
	$(LIBOBJ)/zlib/deflate.o \
	$(LIBOBJ)/zlib/inffast.o \
	$(LIBOBJ)/zlib/inflate.o \
	$(LIBOBJ)/zlib/infback.o \
	$(LIBOBJ)/zlib/inftrees.o \
	$(LIBOBJ)/zlib/trees.o \
	$(LIBOBJ)/zlib/uncompr.o \
	$(LIBOBJ)/zlib/zutil.o



$(OBJ)/libz.a: $(ZLIBOBJS)

$(LIBOBJ)/zlib/%.o: $(LIBSRC)/zlib/%.c | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) $(CONLYFLAGS) $(ZLIBOPTS) -c $< -o $@



#-------------------------------------------------
# SoftFloat library objects
#-------------------------------------------------

PROCESSOR_H = $(LIBSRC)/softfloat/processors/mamesf.h
SOFTFLOAT_MACROS = $(LIBSRC)/softfloat/softfloat/bits64/softfloat-macros

SOFTFLOATOBJS = \
	$(LIBOBJ)/softfloat/softfloat.o \
	$(LIBOBJ)/softfloat/fsincos.o \
	$(LIBOBJ)/softfloat/fyl2x.o

$(OBJ)/libsoftfloat.a: $(SOFTFLOATOBJS)

$(LIBOBJ)/softfloat/softfloat.o: $(LIBSRC)/softfloat/softfloat.c $(LIBSRC)/softfloat/softfloat.h $(LIBSRC)/softfloat/softfloat-macros $(LIBSRC)/softfloat/softfloat-specialize
$(LIBOBJ)/softfloat/fsincos.o: $(LIBSRC)/softfloat/fsincos.c $(LIBSRC)/softfloat/fpu_constant.h $(LIBSRC)/softfloat/softfloat.h $(LIBSRC)/softfloat/softfloat-macros $(LIBSRC)/softfloat/softfloat-specialize



#-------------------------------------------------
# libJPEG library objects
#-------------------------------------------------

LIBJPEGOBJS= \
	$(LIBOBJ)/libjpeg/jaricom.o \
	$(LIBOBJ)/libjpeg/jcapimin.o \
	$(LIBOBJ)/libjpeg/jcapistd.o \
	$(LIBOBJ)/libjpeg/jcarith.o \
	$(LIBOBJ)/libjpeg/jccoefct.o \
	$(LIBOBJ)/libjpeg/jccolor.o \
	$(LIBOBJ)/libjpeg/jcdctmgr.o \
	$(LIBOBJ)/libjpeg/jchuff.o \
	$(LIBOBJ)/libjpeg/jcinit.o \
	$(LIBOBJ)/libjpeg/jcmainct.o \
	$(LIBOBJ)/libjpeg/jcmarker.o \
	$(LIBOBJ)/libjpeg/jcmaster.o \
	$(LIBOBJ)/libjpeg/jcomapi.o \
	$(LIBOBJ)/libjpeg/jcparam.o \
	$(LIBOBJ)/libjpeg/jcprepct.o \
	$(LIBOBJ)/libjpeg/jcsample.o \
	$(LIBOBJ)/libjpeg/jctrans.o \
	$(LIBOBJ)/libjpeg/jdapimin.o \
	$(LIBOBJ)/libjpeg/jdapistd.o \
	$(LIBOBJ)/libjpeg/jdarith.o \
	$(LIBOBJ)/libjpeg/jdatadst.o \
	$(LIBOBJ)/libjpeg/jdatasrc.o \
	$(LIBOBJ)/libjpeg/jdcoefct.o \
	$(LIBOBJ)/libjpeg/jdcolor.o \
	$(LIBOBJ)/libjpeg/jddctmgr.o \
	$(LIBOBJ)/libjpeg/jdhuff.o \
	$(LIBOBJ)/libjpeg/jdinput.o \
	$(LIBOBJ)/libjpeg/jdmainct.o \
	$(LIBOBJ)/libjpeg/jdmarker.o \
	$(LIBOBJ)/libjpeg/jdmaster.o \
	$(LIBOBJ)/libjpeg/jdmerge.o \
	$(LIBOBJ)/libjpeg/jdpostct.o \
	$(LIBOBJ)/libjpeg/jdsample.o \
	$(LIBOBJ)/libjpeg/jdtrans.o \
	$(LIBOBJ)/libjpeg/jerror.o \
	$(LIBOBJ)/libjpeg/jfdctflt.o \
	$(LIBOBJ)/libjpeg/jfdctfst.o \
	$(LIBOBJ)/libjpeg/jfdctint.o \
	$(LIBOBJ)/libjpeg/jidctflt.o \
	$(LIBOBJ)/libjpeg/jidctfst.o \
	$(LIBOBJ)/libjpeg/jidctint.o \
	$(LIBOBJ)/libjpeg/jquant1.o \
	$(LIBOBJ)/libjpeg/jquant2.o \
	$(LIBOBJ)/libjpeg/jutils.o \
	$(LIBOBJ)/libjpeg/jmemmgr.o \
	$(LIBOBJ)/libjpeg/jmemansi.o \

$(OBJ)/libjpeg.a: $(LIBJPEGOBJS)

$(LIBOBJ)/libjpeg/%.o: $(LIBSRC)/libjpeg/%.c | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) $(CONLYFLAGS) -I$(LIBSRC)/libjpeg -c $< -o $@



#-------------------------------------------------
# libflac library objects
#-------------------------------------------------

ifeq ($(TARGETOS),macosx)
ifdef BIGENDIAN
ARCHFLAGS = -DWORDS_BIGENDIAN=1
else
ARCHFLAGS = -DWORDS_BIGENDIAN=0
endif
else
ARCHFLAGS = -DWORDS_BIGENDIAN=0
endif

FLACOPTS=-DFLAC__NO_ASM -D_LARGEFILE_SOURCE -D_FILE_OFFSET_BITS=64 -DHAVE_CONFIG_H=0 -DFLAC__HAS_OGG=0 -Wno-unused-function $(ARCHFLAGS) -O0

LIBFLACOBJS = \
	$(LIBOBJ)/libflac/bitmath.o \
	$(LIBOBJ)/libflac/bitreader.o \
	$(LIBOBJ)/libflac/bitwriter.o \
	$(LIBOBJ)/libflac/cpu.o \
	$(LIBOBJ)/libflac/crc.o \
	$(LIBOBJ)/libflac/fixed.o \
	$(LIBOBJ)/libflac/float.o \
	$(LIBOBJ)/libflac/format.o \
	$(LIBOBJ)/libflac/lpc.o \
	$(LIBOBJ)/libflac/md5.o \
	$(LIBOBJ)/libflac/memory.o \
	$(LIBOBJ)/libflac/stream_decoder.o \
	$(LIBOBJ)/libflac/stream_encoder.o \
	$(LIBOBJ)/libflac/stream_encoder_framing.o \
	$(LIBOBJ)/libflac/window.o

$(OBJ)/libflac.a: $(LIBFLACOBJS)

$(LIBOBJ)/libflac/%.o: $(LIBSRC)/libflac/libflac/%.c | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CONLYFLAGS) $(CCOMFLAGS) $(FLACOPTS) -I$(LIBSRC)/libflac/include -c $< -o $@



#-------------------------------------------------
# lib7z library objects
#-------------------------------------------------

7ZOPTS=-D_7ZIP_PPMD_SUPPPORT -D_7ZIP_ST

LIB7ZOBJS = \
	$(LIBOBJ)/lib7z/7zBuf.o \
	$(LIBOBJ)/lib7z/7zBuf2.o \
	$(LIBOBJ)/lib7z/7zCrc.o \
	$(LIBOBJ)/lib7z/7zCrcOpt.o \
	$(LIBOBJ)/lib7z/7zDec.o \
	$(LIBOBJ)/lib7z/7zIn.o \
	$(LIBOBJ)/lib7z/CpuArch.o \
	$(LIBOBJ)/lib7z/LzmaDec.o \
	$(LIBOBJ)/lib7z/Lzma2Dec.o \
	$(LIBOBJ)/lib7z/LzmaEnc.o \
	$(LIBOBJ)/lib7z/Lzma2Enc.o \
	$(LIBOBJ)/lib7z/LzFind.o \
	$(LIBOBJ)/lib7z/Bra.o \
	$(LIBOBJ)/lib7z/Bra86.o \
	$(LIBOBJ)/lib7z/Bcj2.o \
	$(LIBOBJ)/lib7z/Ppmd7.o \
	$(LIBOBJ)/lib7z/Ppmd7Dec.o \
	$(LIBOBJ)/lib7z/7zStream.o \
	$(LIBOBJ)/lib7z/Sha256.o \

$(OBJ)/lib7z.a: $(LIB7ZOBJS)

$(LIBOBJ)/lib7z/%.o: $(LIBSRC)/lib7z/%.c | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(7ZOPTS) $(CCOMFLAGS) $(CONLYFLAGS) -I$(LIBSRC)/lib7z/ -c $< -o $@

#-------------------------------------------------
# portmidi library objects
#-------------------------------------------------

PMOPTS =

# common objects
LIBPMOBJS = \
	$(LIBOBJ)/portmidi/portmidi.o \
	$(LIBOBJ)/portmidi/porttime.o \
	$(LIBOBJ)/portmidi/pmutil.o

ifeq ($(TARGETOS),linux)
PMOPTS = -DPMALSA=1

LIBPMOBJS += \
	$(LIBOBJ)/portmidi/pmlinux.o \
	$(LIBOBJ)/portmidi/pmlinuxalsa.o \
	$(LIBOBJ)/portmidi/finddefaultlinux.o \
	$(LIBOBJ)/portmidi/ptlinux.o

endif

ifeq ($(TARGETOS),macosx)
LIBPMOBJS += \
	$(LIBOBJ)/portmidi/pmmac.o \
	$(LIBOBJ)/portmidi/pmmacosxcm.o \
	$(LIBOBJ)/portmidi/finddefault.o \
	$(LIBOBJ)/portmidi/readbinaryplist.o \
	$(LIBOBJ)/portmidi/ptmacosx_mach.o \
	$(LIBOBJ)/portmidi/osxsupport.o
endif

ifeq ($(TARGETOS),win32)
LIBPMOBJS += \
	$(LIBOBJ)/portmidi/pmwin.o \
	$(LIBOBJ)/portmidi/pmwinmm.o \
	$(LIBOBJ)/portmidi/ptwinmm.o
endif

$(OBJ)/libportmidi.a: $(LIBPMOBJS)

$(LIBOBJ)/portmidi/%.o: $(LIBSRC)/portmidi/%.c | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(PMOPTS) $(CCOMFLAGS) $(CONLYFLAGS) -I$(LIBSRC)/portmidi/ -c $< -o $@

#-------------------------------------------------
# LUA library objects
#-------------------------------------------------

LUAOBJS = \
	$(LIBOBJ)/lua/lapi.o \
	$(LIBOBJ)/lua/lcode.o \
	$(LIBOBJ)/lua/lctype.o \
	$(LIBOBJ)/lua/ldebug.o \
	$(LIBOBJ)/lua/ldo.o \
	$(LIBOBJ)/lua/ldump.o \
	$(LIBOBJ)/lua/lfunc.o \
	$(LIBOBJ)/lua/lgc.o \
	$(LIBOBJ)/lua/llex.o \
	$(LIBOBJ)/lua/lmem.o \
	$(LIBOBJ)/lua/lobject.o \
	$(LIBOBJ)/lua/lopcodes.o \
	$(LIBOBJ)/lua/lparser.o \
	$(LIBOBJ)/lua/lstate.o \
	$(LIBOBJ)/lua/lstring.o \
	$(LIBOBJ)/lua/ltable.o \
	$(LIBOBJ)/lua/ltm.o \
	$(LIBOBJ)/lua/lundump.o \
	$(LIBOBJ)/lua/lvm.o \
	$(LIBOBJ)/lua/lzio.o \
	$(LIBOBJ)/lua/lauxlib.o \
	$(LIBOBJ)/lua/lbaselib.o \
	$(LIBOBJ)/lua/lbitlib.o \
	$(LIBOBJ)/lua/lcorolib.o \
	$(LIBOBJ)/lua/ldblib.o \
	$(LIBOBJ)/lua/liolib.o \
	$(LIBOBJ)/lua/lmathlib.o \
	$(LIBOBJ)/lua/loslib.o \
	$(LIBOBJ)/lua/lstrlib.o \
	$(LIBOBJ)/lua/ltablib.o \
	$(LIBOBJ)/lua/loadlib.o \
	$(LIBOBJ)/lua/linit.o \

$(OBJ)/liblua.a: $(LUAOBJS)

LUA_FLAGS =
ifeq ($(TARGETOS),linux)
LUA_FLAGS += -DLUA_USE_POSIX
endif

ifeq ($(TARGETOS),macosx)
LUA_FLAGS += -DLUA_USE_POSIX
endif

$(LIBOBJ)/lua/%.o: $(LIBSRC)/lua/%.c | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CCOMFLAGS) $(CONLYFLAGS) -DLUA_COMPAT_ALL $(LUA_FLAGS) -c $< -o $@

#-------------------------------------------------
# web library objects
#-------------------------------------------------

WEBOBJS = \
	$(LIBOBJ)/web/mongoose.o \
	$(LIBOBJ)/web/json/json_reader.o \
	$(LIBOBJ)/web/json/json_value.o \
	$(LIBOBJ)/web/json/json_writer.o \

$(OBJ)/libweb.a: $(WEBOBJS)

$(LIBOBJ)/web/%.o: $(LIBSRC)/web/%.cpp | $(OSPREBUILD)
	@echo Compiling $<...
	$(CC) $(CDEFS) $(CFLAGS) -I$(LIBSRC)/web -c $< -o $@
