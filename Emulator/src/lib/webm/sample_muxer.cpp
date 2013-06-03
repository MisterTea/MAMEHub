// Copyright (c) 2011 The WebM project authors. All Rights Reserved.
//
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file in the root of the source
// tree. An additional intellectual property rights grant can be found
// in the file PATENTS.  All contributing project authors may
// be found in the AUTHORS file in the root of the source tree.

#include <cstdio>
#include <cstring>
#include <iostream>
#include <fstream>
using namespace std;

// libwebm parser includes
#include "mkvreader.hpp"
#include "mkvparser.hpp"

// libwebm muxer includes
#include "mkvmuxer.hpp"
#include "mkvwriter.hpp"
#include "mkvmuxerutil.hpp"

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

namespace {

void Usage() {
  printf("Usage: sample_muxer -i input -o output [options]\n");
  printf("\n");
  printf("Main options:\n");
  printf("  -h | -?                     show help\n");
  printf("  -video <int>                >0 outputs video\n");
  printf("  -audio <int>                >0 outputs audio\n");
  printf("  -live <int>                 >0 puts the muxer into live mode\n");
  printf("                              0 puts the muxer into file mode\n");
  printf("  -output_cues <int>          >0 outputs cues element\n");
  printf("  -cues_on_video_track <int>  >0 outputs cues on video track\n");
  printf("  -cues_on_audio_track <int>  >0 outputs cues on audio track\n");
  printf("                              0 outputs cues on audio track\n");
  printf("  -max_cluster_duration <double> in seconds\n");
  printf("  -max_cluster_size <int>     in bytes\n");
  printf("  -switch_tracks <int>        >0 switches tracks in output\n");
  printf("  -audio_track_number <int>   >0 Changes the audio track number\n");
  printf("  -video_track_number <int>   >0 Changes the video track number\n");
  printf("\n");
  printf("Video options:\n");
  printf("  -display_width <int>        Display width in pixels\n");
  printf("  -display_height <int>       Display height in pixels\n");
  printf("  -stereo_mode <int>          3D video mode\n");
  printf("\n");
  printf("Cues options:\n");
  printf("  -output_cues_block_number <int> >0 outputs cue block number\n");
}

} //end namespace

struct codec_item
{
    char const              *name;
    const vpx_codec_iface_t *iface;
    unsigned int             fourcc;
};

extern vpx_codec_iface_t vpx_enc_vp8_algo;

codec_item codecs[] =
{
    {"vp8",  &vpx_enc_vp8_algo, 0x30385056},
    //{"vp8",  &vpx_codec_vp8_cx_algo, 0x30385056},
    //{"vorbis",  &vpx_codec_vp8_cx_algo, 0x76726273},
};

int main(int argc, char* argv[]) {
  ogg_stream_state os; /* take physical pages, weld into a logical
                          stream of packets */
  ogg_page         og; /* one Ogg bitstream page.  Vorbis packets are inside */
  ogg_packet       op; /* one raw packet of data for decode */

  vorbis_info      vi; /* struct that stores all the static vorbis bitstream
                          settings */
  vorbis_comment   vc; /* struct that stores all the user comments */

  vorbis_dsp_state vd; /* central working state for the packet->PCM decoder */
  vorbis_block     vb; /* local working space for packet->PCM decode */

  using mkvmuxer::uint64;

  char* input = NULL;
  char* output = NULL;

  // Segment variables
  bool output_video = true;
  bool output_audio = true;
  bool live_mode = false;
  bool output_cues = true;
  bool cues_on_video_track = true;
  bool cues_on_audio_track = true;
  uint64 max_cluster_duration = 0;
  uint64 max_cluster_size = 0;
  bool switch_tracks = false;
  int audio_track_number = 0; // 0 tells muxer to decide.
  int video_track_number = 0; // 0 tells muxer to decide.

  bool output_cues_block_number = true;

  uint64 display_width = 0;
  uint64 display_height = 0;
  uint64 stereo_mode = 0;

  vpx_codec_ctx_t        encoder;
  vpx_codec_enc_cfg_t    cfg;
  vpx_codec_err_t        res;
  vpx_image_t            raw;

  /* Populate encoder configuration */
  res = vpx_codec_enc_config_default(&vpx_enc_vp8_algo, &cfg, 0);
  
  if (res) {
    fprintf(stderr, "Failed to get config: %s\n",
	    vpx_codec_err_to_string(res));
    return EXIT_FAILURE;
  }

  /* Change the default timebase to a high enough value so that the encoder
   * will always create strictly increasing timestamps.
   */
  cfg.g_timebase.num = 1;
  cfg.g_timebase.den = 1000;

  /* Never use the library's default resolution, require it be parsed
   * from the file or set on the command line.
   */
  cfg.g_w = 540;
  cfg.g_h = 360;

  for (int i = 1; i < argc; ++i) {
    char* end;

    if (!strcmp("-h", argv[i]) || !strcmp("-?", argv[i])) {
      Usage();
      return 0;
    } else if (!strcmp("-i", argv[i])) {
      input = argv[++i];
    } else if (!strcmp("-o", argv[i])) {
      output = argv[++i];
    } else if (!strcmp("-video", argv[i])) {
      output_video = strtol(argv[++i], &end, 10) == 0 ? false : true;
    } else if (!strcmp("-audio", argv[i])) {
      output_audio = strtol(argv[++i], &end, 10) == 0 ? false : true;
    } else if (!strcmp("-live", argv[i])) {
      live_mode = strtol(argv[++i], &end, 10) == 0 ? false : true;
    } else if (!strcmp("-output_cues", argv[i])) {
      output_cues = strtol(argv[++i], &end, 10) == 0 ? false : true;
    } else if (!strcmp("-cues_on_video_track", argv[i])) {
      cues_on_video_track = strtol(argv[++i], &end, 10) == 0 ? false : true;
    } else if (!strcmp("-cues_on_audio_track", argv[i])) {
      cues_on_audio_track = strtol(argv[++i], &end, 10) == 0 ? false : true;
    } else if (!strcmp("-max_cluster_duration", argv[i])) {
      const double seconds = strtod(argv[++i], &end);
      max_cluster_duration =
          static_cast<uint64>(seconds * 1000000000.0);
    } else if (!strcmp("-max_cluster_size", argv[i])) {
      max_cluster_size = strtol(argv[++i], &end, 10);
    } else if (!strcmp("-switch_tracks", argv[i])) {
      switch_tracks = strtol(argv[++i], &end, 10) == 0 ? false : true;
    } else if (!strcmp("-audio_track_number", argv[i])) {
      audio_track_number = strtol(argv[++i], &end, 10);
    } else if (!strcmp("-video_track_number", argv[i])) {
      video_track_number = strtol(argv[++i], &end, 10);
    } else if (!strcmp("-display_width", argv[i])) {
      display_width = strtol(argv[++i], &end, 10);
    } else if (!strcmp("-display_height", argv[i])) {
      display_height = strtol(argv[++i], &end, 10);
    } else if (!strcmp("-stereo_mode", argv[i])) {
      stereo_mode = strtol(argv[++i], &end, 10);
    } else if (!strcmp("-output_cues_block_number", argv[i])) {
      output_cues_block_number =
          strtol(argv[++i], &end, 10) == 0 ? false : true;
    }
  }

  if (output == NULL) {
    Usage();
    return 0;
  }

  vpx_img_alloc(&raw, VPX_IMG_FMT_YV12,
		cfg.g_w, cfg.g_h, 1);

#define RGB_TO_YUV(R,G,B,Y,U,V) Y = (R)/3 + (G)/3 + (B)/3; U = (( (3*((int)(B))) - (3*(((int)(Y)))) + 512 )>>2); V = (( (3*((int)(R))) - (3*(((int)(Y)))) + 512 )>>2);
  int ii,j;
  printf("IMAGE INFO: %d %d %d %d %d %d\n",raw.stride[0],raw.stride[1],raw.stride[2],raw.bps,raw.w,raw.h);
  {
    unsigned char *chr = raw.planes[0];
    for(ii=0;ii<raw.h;ii++) {
      for(j=0;j<raw.w;j++) {
	unsigned char dummy1,dummy2;
	RGB_TO_YUV(0,0,(j%256),chr[ii*raw.w+j],dummy1,dummy2);
	//printf("Y = %d U = %d V = %d\n",chr[ii*raw.w+j],dummy1,dummy2);
	//chr[ii*raw.w+j]= 127;
      }
    }
  }
  {
    unsigned char *chr = raw.planes[1];
    for(ii=0;ii<raw.h/2;ii++) {
      for(j=0;j<raw.w/2;j++) {
	unsigned char dummy1,dummy2;
	RGB_TO_YUV(0,0,(j*2)%256,dummy1,chr[ii*raw.w/2+j],dummy2);
	//chr[ii*raw.w/2+j] = 255;
	//printf("R,G,B = %d Y = %d U = %d V = %d\n",j*2,dummy1,chr[ii*raw.w/2+j],dummy2);
      }
    }
  }
  {
    unsigned char *chr = raw.planes[2];
    for(ii=0;ii<raw.h/2;ii++) {
      for(j=0;j<raw.w/2;j++) {
	unsigned char dummy1,dummy2;
	RGB_TO_YUV(0,0,(j*2)%256,dummy1,dummy2,chr[ii*raw.w/2+j]);
	//chr[ii*raw.w/2+j] = 128;
	//printf("R,G,B = %d Y = %d U = %d V = %d\n",j*2,dummy1,dummy2,chr[ii*raw.w/2+j]);
      }
    }
  }

  cfg.g_pass = VPX_RC_ONE_PASS;

  /* Initialize Vorbis */
  vorbis_info_init(&vi);
  const int CHANNELS = 2;
  const int AUDIO_BITRATE = 44100;
  const float QUALITY = 0.1f;
  int ret2=vorbis_encode_init_vbr(&vi,CHANNELS,AUDIO_BITRATE,QUALITY);
  if(ret2) {
    printf("ERROR: BAD VORBIS INIT\n");
    exit(1);
  }
  /* add a comment */
  vorbis_comment_init(&vc);
  vorbis_comment_add_tag(&vc,"SOURCE","MAME Game Audio");
  /* set up the analysis state and auxiliary encoding storage */
  vorbis_analysis_init(&vd,&vi);
  vorbis_block_init(&vd,&vb);

  /* Construct Encoder Context */
  vpx_codec_enc_init(&encoder, &vpx_enc_vp8_algo, &cfg, 0);
  if(encoder.err) {
    printf("Failed to initialize encoder\n");
  }

  // Get parser header info
  mkvparser::MkvReader reader;

  if (reader.Open(input)) {
    printf("\n Filename is invalid or error while opening.\n");
    return -1;
  }

  long long pos = 0;
  mkvparser::EBMLHeader ebml_header;
  ebml_header.Parse(&reader, pos);

  mkvparser::Segment* parser_segment;
  long long ret = mkvparser::Segment::CreateInstance(&reader,
                                                     pos,
                                                     parser_segment);
  if (ret) {
    printf("\n Segment::CreateInstance() failed.");
    return -1;
  }

  ret = parser_segment->Load();
  if (ret < 0) {
    printf("\n Segment::Load() failed.");
    return -1;
  }

  const mkvparser::SegmentInfo* const segment_info = parser_segment->GetInfo();
  const long long timeCodeScale = segment_info->GetTimeCodeScale();

  // Set muxer header info
  mkvmuxer::MkvWriter writer;

  if (!writer.Open(output)) {
    printf("\n Filename is invalid or error while opening.\n");
    return -1;
  }

  // Set Segment element attributes
  mkvmuxer::Segment muxer_segment(&writer);
  muxer_segment.set_mode(mkvmuxer::Segment::kFile);

  if (max_cluster_duration > 0)
    muxer_segment.set_max_cluster_duration(max_cluster_duration);
  if (max_cluster_size > 0)
    muxer_segment.set_max_cluster_size(max_cluster_size);
  muxer_segment.OutputCues(output_cues);

  // Set SegmentInfo element attributes
  mkvmuxer::SegmentInfo* const info = muxer_segment.GetSegmentInfo();
  info->set_timecode_scale(timeCodeScale);
  info->set_writing_app("sample_muxer");

  // Set Tracks element attributes
  enum { kVideoTrack = 1, kAudioTrack = 2 };
  const mkvparser::Tracks* const parser_tracks = parser_segment->GetTracks();
  unsigned long i = 0;
  uint64 vid_track = 0; // no track added
  uint64 aud_track = 0; // no track added

  while (i != parser_tracks->GetTracksCount()) {
    int track_num = i++;
    if (switch_tracks)
      track_num = i % parser_tracks->GetTracksCount();

    const mkvparser::Track* const parser_track =
        parser_tracks->GetTrackByIndex(track_num);

    if (parser_track == NULL)
      continue;

    // TODO(fgalligan): Add support for language to parser.
    const char* const track_name = parser_track->GetNameAsUTF8();

    const long long track_type = parser_track->GetType();

    if (track_type == kVideoTrack && output_video) {
      // Get the video track from the parser
      const mkvparser::VideoTrack* const pVideoTrack =
          static_cast<const mkvparser::VideoTrack*>(parser_track);
      const long long width =  pVideoTrack->GetWidth();
      const long long height = pVideoTrack->GetHeight();
      cout << "DIMENSIONS: "<<width<<"x"<<height<<endl;

      // Add the video track to the muxer
      int VIDEO_TRACK_NUMBER = 1;
      vid_track = muxer_segment.AddVideoTrack(static_cast<int>(width),
                                              static_cast<int>(height),
                                              VIDEO_TRACK_NUMBER);
      if (!vid_track) {
        printf("\n Could not add video track.\n");
        return -1;
      }

      mkvmuxer::VideoTrack* const video =
          static_cast<mkvmuxer::VideoTrack*>(
              muxer_segment.GetTrackByNumber(vid_track));
      if (!video) {
        printf("\n Could not get video track.\n");
        return -1;
      }

      if (track_name)
        video->set_name(track_name);

      if (display_width > 0)
        video->set_display_width(display_width);
      if (display_height > 0)
        video->set_display_height(display_height);
      if (stereo_mode > 0)
        video->SetStereoMode(stereo_mode);

      const double rate = pVideoTrack->GetFrameRate();
      cout << "FRAME RATE: " << rate << endl;
      if (rate > 0.0) {
        video->set_frame_rate(rate);
      }
    } else if (track_type == kAudioTrack && output_audio) {
      // Get the audio track from the parser
      const mkvparser::AudioTrack* const pAudioTrack =
          static_cast<const mkvparser::AudioTrack*>(parser_track);
      const long long channels =  pAudioTrack->GetChannels();
      const double sample_rate = pAudioTrack->GetSamplingRate();

      printf("CHANNELS %lld SAMPLE RATE %lf\n",channels,sample_rate);

      // Add the audio track to the muxer
      int AUDIO_TRACK_NUMBER = 2;
      aud_track = muxer_segment.AddAudioTrack(AUDIO_BITRATE, CHANNELS, AUDIO_TRACK_NUMBER);
      if (!aud_track) {
        printf("\n Could not add audio track.\n");
        return -1;
      }

      mkvmuxer::AudioTrack* const audio =
          static_cast<mkvmuxer::AudioTrack*>(
              muxer_segment.GetTrackByNumber(aud_track));
      if (!audio) {
        printf("\n Could not get audio track.\n");
        return -1;
      }

      if (track_name)
        audio->set_name(track_name);

      size_t private_size;
      const unsigned char* const private_data =
          pAudioTrack->GetCodecPrivate(private_size);
#if 0
      if (private_size > 0) {
        if (!audio->SetCodecPrivate(private_data, private_size)) {
          printf("\n Could not add audio private data.\n");
          return -1;
        }
      }
#endif

      ogg_packet opIden,opComm,opSetup;
      vorbis_analysis_headerout(&vd,&vc,&opIden,&opComm,&opSetup);
      
      int privateSize=0;
      unsigned char privateData[1024*1024*1];
      privateData[privateSize++] = 2; //# of packets minus 1
      {
	int curSize = opIden.bytes;
	for (int i = 0; i < curSize / 255; i++)
	  privateData[privateSize++] = 255;
	privateData[privateSize++] = curSize%255;
      }      
      {
	int curSize = opComm.bytes;
	for (int i = 0; i < curSize / 255; i++)
	  privateData[privateSize++] = 255;
	privateData[privateSize++] = curSize%255;
      }
      memcpy(privateData+privateSize,opIden.packet,opIden.bytes);
      privateSize += opIden.bytes;
      memcpy(privateData+privateSize,opComm.packet,opComm.bytes);
      privateSize += opComm.bytes;
      memcpy(privateData+privateSize,opSetup.packet,opSetup.bytes);
      privateSize += opSetup.bytes;

      if(privateSize>1024*1024) {
	cout << "PRIVATE SIZE IS TOO BIG\n";
      }
      for(int a=0;a<privateSize && a<private_size;a++) {
	//cout << int(private_data[a]) << ' ' << int(privateData[a]) << endl;
      }
      //exit(1);
      if (!audio->SetCodecPrivate(privateData, privateSize)) {
	printf("\n Could not add audio private data.\n");
	return -1;
      }

      const long long bit_depth = pAudioTrack->GetBitDepth();
      cout << "BIT DEPTH: " << bit_depth << endl;
      audio->set_bit_depth(bit_depth);
    }
  }

  // Set Cues element attributes
  mkvmuxer::Cues* const cues = muxer_segment.GetCues();
  cues->set_output_block_number(output_cues_block_number);
  if (cues_on_video_track && vid_track)
    muxer_segment.CuesTrack(vid_track);
  if (cues_on_audio_track && aud_track)
    muxer_segment.CuesTrack(aud_track);

  // Write clusters
  unsigned char* data = NULL;
  int data_len = 0;

  const mkvparser::Cluster* cluster = parser_segment->GetFirst();

  while ((cluster != NULL) && !cluster->EOS()) {
    const mkvparser::BlockEntry* block_entry = cluster->GetFirst();

    while ((block_entry != NULL) && !block_entry->EOS()) {
      const mkvparser::Block* const block = block_entry->GetBlock();
      const long long trackNum = block->GetTrackNumber();
      const mkvparser::Track* const parser_track =
          parser_tracks->GetTrackByNumber(
              static_cast<unsigned long>(trackNum));
      const long long track_type = parser_track->GetType();

      if ((track_type == kAudioTrack && output_audio) ||
          (track_type == kVideoTrack && output_video)) {
        const int frame_count = block->GetFrameCount();
        const long long time_ns = block->GetTime(cluster);
        const bool is_key = block->IsKey();

        for (int i = 0; i < frame_count; ++i) {
          const mkvparser::Block::Frame& frame = block->GetFrame(i);

          if (frame.len > data_len) {
            delete [] data;
            data = new unsigned char[frame.len];
            if (!data)
              return -1;
            data_len = frame.len;
          }

          if (frame.Read(&reader, data))
            return -1;

          uint64 track_num = vid_track;
          if (track_type == kAudioTrack) {
	    track_num = aud_track;

            static ogg_int64_t curtime = 0;
	    cout << "TIMES: " << ((long long)(double(curtime)*1000000000.0/44100.0)) << " " << time_ns << endl;
	    if(((long long)(double(curtime)*1000000000.0/44100.0))>time_ns) {
	      cout << "Skipping frame\n";
	      continue;
	    }
#if 1
	    float **buffer = vorbis_analysis_buffer(&vd,44100/30);
	    //float samples[2][44100/30];
	    for(int a=0;a<44100/30;a++) {
	      static float curamp = 0;
	      buffer[0][a] = buffer[1][a] = sinf(curamp);
	      curamp += 0.15f;
	    }
	    //memcpy(buffer[0],samples[0],sizeof(float)*44100/30);
	    //memcpy(buffer[1],samples[1],sizeof(float)*44100/30);
	    //memset(buffer[0],0,sizeof(float)*44100/30);
	    //memset(buffer[1],0,sizeof(float)*44100/30);

	    vorbis_analysis_wrote(&vd,44100/30);

	    /* vorbis does some data preanalysis, then divvies up blocks for
	       more involved (potentially parallel) processing.  Get a single
	       block for encoding now */
	    int numBlocks=0;
	    int numPackets=0;
	    cout << "NEW FRAME\n";
	    while(vorbis_analysis_blockout(&vd,&vb)==1){
	      
	      /* analysis, assume we want to use bitrate management */
	      vorbis_analysis(&vb,NULL);
	      vorbis_bitrate_addblock(&vb);

	      while(vorbis_bitrate_flushpacket(&vd,&op)){

              curtime = op.granulepos;
	      cout << "PACKET BYTES: " << op.bytes << endl;
              if (!muxer_segment.AddFrame(op.packet,
                                          op.bytes,
                                          track_num,
                                          ((long long)(double(curtime)*1000000000.0/44100.0)),
                                          is_key)) {
                  printf("\n Could not add frame.\n");
                  return -1;
              }
	      cout << "CURRENT TIME IN SAMPLES: " << curtime << " IN SECONDS: " << (double(curtime)/44100.0) << endl;

#if 0
		if (!muxer_segment.AddFrame(op.packet,
					    op.bytes,
					    track_num,
					    time_ns,
					    is_key)) {
		  printf("\n Could not add frame.\n");
		  return -1;
		}
#endif
	
	      }
	    }
#endif

#if 0
	    if (!muxer_segment.AddFrame(data,
					frame.len,
					track_num,
					time_ns,
					is_key)) {
	      printf("\n Could not add frame.\n");
	      return -1;
	    }
#endif
          } else { //video track

#if 0
	    if (!muxer_segment.AddFrame(data,
					frame.len,
					track_num,
					time_ns,
					is_key)) {
	      printf("\n Could not add frame.\n");
	      return -1;
	    }
#endif

#if 1
	    static int frame_start = -33;
	    static int next_frame_start = 0;

	    frame_start += 33;
	    next_frame_start += 33;

            vpx_codec_encode(&encoder, &raw, frame_start,
			     33, 0, 0);

            vpx_codec_iter_t iter = NULL;
            const vpx_codec_cx_pkt_t *pkt;

	    int numPacket=0;
            while ((pkt = vpx_codec_get_cx_data(&encoder, &iter))) {
	      switch (pkt->kind)
                {
                case VPX_CODEC_CX_FRAME_PKT:
		  numPacket++;
		  if(numPacket>1) {
		    printf("OOPS\n");
		  }
		  printf("PACKET SIZE: %lu\n",pkt->data.frame.sz);
		  printf("FRAME LENGTH: %lu\n",frame.len);
		  if (!muxer_segment.AddFrame((mkvmuxer::uint8*)pkt->data.frame.buf,
					      pkt->data.frame.sz,
					      track_num,
					      time_ns,
					      is_key)) {
		    printf("\n Could not add frame.\n");
		    return -1;
		  }	    
		  break;
		default:
		  break;
		}
	      
	    }
#endif
	  }
        }
      }

      block_entry = cluster->GetNext(block_entry);
    }

    cluster = parser_segment->GetNext(cluster);
  }

  muxer_segment.Finalize();

  delete [] data;
  delete parser_segment;

  writer.Close();
  reader.Close();

  return 0;
}



