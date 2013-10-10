#include <cstdio>
#include <cstring>
#include <iostream>
#include <fstream>
#include <vector>
using namespace std;

// libwebm parser includes
#include "libwebm/mkvreader.hpp"
#include "libwebm/mkvparser.hpp"

// libwebm muxer includes
#include "libwebm/mkvmuxer.hpp"
#include "libwebm/mkvwriter.hpp"
#include "libwebm/mkvmuxerutil.hpp"

#include "vpx/vpx_encoder.h"

extern "C" {
#include "vpx/vp8cx.h"
#include "vpx_ports/mem_ops.h"
#include "vpx_ports/vpx_timer.h"
#include "tools_common.h"
#include "y4minput.h"
#include "libmkv/EbmlWriter.h"
#include "libmkv/EbmlIDs.h"
#include <vorbis/vorbisenc.h>
}

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <math.h>

#include <boost/thread.hpp>

class WebMEncoder;

class WebMVideoProcessor {
 public:
  WebMVideoProcessor() {};

  WebMVideoProcessor(WebMEncoder *encoder);

  void operator()();

 protected:
  WebMEncoder *encoder;
};

class WebMEncoder {
 public:
  enum InputVideoFormat {
    IVF_RGB=0,
    IVF_RGBA=1,
  };

  WebMEncoder(const char *filename,int width,int height,InputVideoFormat videoFormat,int frameRateNum,int frameRateDen,int audioSampleRate,int audioChannels);
    
  ~WebMEncoder();
  
  void encodeAudioInterleaved(const short* samples,int samplesToEncode);
  void encodeVideo(unsigned char *inputImage,int inputStride);
  int encode();

  inline vpx_image_t &getRaw() { return raw; }
  
  void initializeVideoEncoder();
  void initializeAudioEncoder();
  void initializeContainer(const char *filename);
  void convertFrame(unsigned char *inputImage,int inputStride);

  int width,height,frameRateNum,frameRateDen,audioSampleRate,audioChannels;
  InputVideoFormat inputVideoFormat;
  vector<long long> fracFrame;

  vorbis_info      vi; /* struct that stores all the static vorbis bitstream
			  settings */
  vorbis_comment   vc; /* struct that stores all the user comments */
  
  vorbis_dsp_state vd; /* central working state for the packet->PCM decoder */
  vorbis_block     vb; /* local working space for packet->PCM decode */

  vpx_codec_ctx_t        encoder;
  vpx_codec_enc_cfg_t    cfg;
  vpx_codec_err_t        res;
  vpx_image_t            raw;

  mkvmuxer::MkvWriter* writer;
  mkvmuxer::Segment* muxer_segment;

  long long lastAudioTime;

  WebMVideoProcessor videoProcessor;
  boost::thread videoProcessorThread;
};
