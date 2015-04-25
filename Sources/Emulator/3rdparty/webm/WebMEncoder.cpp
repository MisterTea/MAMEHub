#include "WebMEncoder.h"

extern "C" {
    extern vpx_codec_iface_t vpx_enc_vp8_algo;
}

map<long long, ogg_packet*> packetsToWrite;
long long curFrame;
boost::mutex muxerMutex;

WebMVideoProcessor::WebMVideoProcessor(WebMEncoder *encoder) {
    this->encoder = encoder;
}

void WebMVideoProcessor::operator()() {
    //cout << "Started encoding video frame\n";
    long long videoTime = (encoder->cfg.g_timebase.den*curFrame)/encoder->frameRateDen;
    long long frameDuration = ((encoder->cfg.g_timebase.den*(curFrame+1))/encoder->frameRateDen) - videoTime;
    
    //cout << "Encoding video frame at time " << double(videoTime)/30.0 << endl;
    int res = vpx_codec_encode(&(encoder->encoder), &(encoder->raw), videoTime,
                               frameDuration, 0, 15000);
    if (res != VPX_CODEC_OK) {
        cout << "ERROR IN ENCODING: " << res << endl;
    }
    //cout << "Finished encoding video frame\n";
  
    vpx_codec_iter_t iter = NULL;
    const vpx_codec_cx_pkt_t *pkt;
  
    while ((pkt = vpx_codec_get_cx_data(&(encoder->encoder), &iter))) {
        switch (pkt->kind)
        {
        case VPX_CODEC_CX_FRAME_PKT:
        {
            long long videoTimeNS = ((1000000000ULL*videoTime)*encoder->cfg.g_timebase.num)/encoder->cfg.g_timebase.den;

            {
                boost::mutex::scoped_lock scoped_lock(muxerMutex);

                while(packetsToWrite.size()>0 && packetsToWrite.begin()->first < videoTimeNS) {
                    if (!encoder->muxer_segment->AddFrame(packetsToWrite.begin()->second->packet,
                                                          packetsToWrite.begin()->second->bytes,
                                                          2, //Audio track
                                                          packetsToWrite.begin()->first,
                                                          false // No audio keyframes
                            )) {
                        printf("\n Could not add audio frame.\n");
                    }
                    free(packetsToWrite.begin()->second->packet);
                    delete packetsToWrite.begin()->second;
                    packetsToWrite.erase(packetsToWrite.begin());
                }
            }

            //printf("curframe: %d %lld %d\n",curFrame, videoTimeNS/1000000, (curFrame % encoder->frameRateDen == 0));

            //printf("PACKET SIZE: %lu\n",pkt->data.frame.sz);
            if (!encoder->muxer_segment->AddFrame((mkvmuxer::uint8*)pkt->data.frame.buf,
                                                  pkt->data.frame.sz,
                                                  1, //Video track
                                                  videoTimeNS, //timestamp
                                                  curFrame % encoder->frameRateDen == 0 //1 keyframe per sec
                    )) {
                printf("\n Could not add frame.\n");
            }

            curFrame++;
            break;
        }
        default:
            break;
        }
    
    }
    //cout << "Finished muxing video frame\n";
}

WebMEncoder::WebMEncoder(const char *filename,int width,int height,InputVideoFormat inputVideoFormat,int frameRateNum,int frameRateDen,int audioSampleRate,int audioChannels) {
    this->width = width;
    this->height = height;
    curFrame=0;
    this->inputVideoFormat = inputVideoFormat;
    this->frameRateNum = frameRateNum;
    this->frameRateDen = frameRateDen;
    this->audioSampleRate = audioSampleRate;
    this->audioChannels = audioChannels;
    this->lastAudioTime = 0;
    initializeVideoEncoder();
    initializeAudioEncoder();
    initializeContainer(filename);
    videoProcessor = WebMVideoProcessor(this);
}

void WebMEncoder::encodeAudioInterleaved(const short* samples,int samplesToEncode) {

	//cout << "AUDIO TIME: " << (lastAudioTime+samplesToEncode-audioSampleRate/2)/double(audioSampleRate) << " VIDEO TIME " << curFrame/30.0 << endl;
    if( (lastAudioTime+samplesToEncode)>audioSampleRate/2 && (lastAudioTime+samplesToEncode-24000)/double(audioSampleRate) > curFrame/30.0)
	    return; // Skip this frame

    float **buffer = vorbis_analysis_buffer(&vd,samplesToEncode);
    
    const short* curSample = samples;
    for(int a=0;a<samplesToEncode;a++) {
        for(int b=0;b<2;b++) {
            buffer[b][a] = (*curSample)/32768.0f;
            curSample++;
        }
    }
    
    vorbis_analysis_wrote(&vd,samplesToEncode);

    /* vorbis does some data preanalysis, then divvies up blocks for
       more involved (potentially parallel) processing.  Get a single
       block for encoding now */
    while(vorbis_analysis_blockout(&vd,&vb)==1){
        
        /* analysis, assume we want to use bitrate management */
        vorbis_analysis(&vb,NULL);
        vorbis_bitrate_addblock(&vb);
        
        while(true){
            ogg_packet opNotMine;
            if(!vorbis_bitrate_flushpacket(&vd,&opNotMine)) {
                break;
            }
            ogg_packet* op = new ogg_packet();
            op->packet = (unsigned char*)malloc(opNotMine.bytes);
            memcpy(op->packet, opNotMine.packet, opNotMine.bytes);
            op->bytes = opNotMine.bytes;
            op->granulepos = opNotMine.granulepos;
            
            lastAudioTime = op->granulepos;
            boost::mutex::scoped_lock scoped_lock(muxerMutex);
            long long audioTimeNS = ((long long)(double(op->granulepos)*1000000000.0/audioSampleRate));
            //printf("GOT AUDIO FRAME: %lld\n", audioTimeNS/1000000);
            packetsToWrite[audioTimeNS] = op;
            //cout << "CURRENT AUDIO TIME IN SAMPLES: " << lastAudioTime << " IN SECONDS: " << (double(lastAudioTime)/audioSampleRate) << endl;
            
            
        }
    }

}

void WebMEncoder::encodeVideo(unsigned char *inputImage,int inputStride) {
    //cout << "Blown timeline encoding video\n";
    //cout << "Waiting to encode video\n";
    videoProcessorThread.join();
    //cout << "Starting to encode video\n";

    convertFrame(inputImage,inputStride);

    videoProcessorThread = boost::thread(videoProcessor);
}

#define RGB2Y(Y,R,G,B) Y = ( (  66*int(R) + 129*int(G) +  25*int(B) + 128) >> 8) +  16;

#define RGB2U(U,R1,G1,B1,R2,G2,B2,R3,G3,B3,R4,G4,B4) U = ( ( -38 * (int(R1)+R2+R3+R4) - 74 * (int(G1)+G2+G3+G4) + 112 * (int(B1)+B2+B3+B4) + 512) >> 10) + 128;

#define RGB2V(V,R1,G1,B1,R2,G2,B2,R3,G3,B3,R4,G4,B4) V = ( ( 112 * (int(R1)+R2+R3+R4) - 94 * (int(G1)+G2+G3+G4) - 18 * (int(B1)+B2+B3+B4) + 512) >> 10) + 128;

#define RGB_TO_YUV(R,G,B,Y,U,V) Y = (int(R)+G+B)/3; \
    U = ((3*( int(B) - Y) + 512 )>>4);              \
    V = ((3*( int(R) - Y) + 512 )>>4);

void WebMEncoder::convertFrame(unsigned char *inputImage,int inputStride) {
    int ii,j;
    //printf("IMAGE INFO: %d %d %d %d %d %d %d %d\n",raw.stride[0],raw.stride[1],raw.stride[2],raw.bps,raw.w,raw.h,raw.fmt,VPX_IMG_FMT_YV12);
    {
        unsigned char *yData = raw.planes[0];
        unsigned char *uData = raw.planes[1];
        unsigned char *vData = raw.planes[2];
        
        memset(uData,0,(width*height)>>2);
        memset(vData,0,(width*height)>>2);
        
        int stride=3;
        switch(inputVideoFormat) {
        case IVF_RGB:
            stride = 3;
            break;
        case IVF_RGBA:
            stride = 4;
            break;
        }
        int fullYIndex=0;

        int fullRGBTLIndex=0;
        int fullRGBTRIndex=stride;
        int fullRGBBLIndex=stride*inputStride;
        int fullRGBBRIndex=stride*(inputStride+1);

        int halfIndex=0;
        int halfHeight = height>>1;
        int halfWidth = width>>1;
        for(ii=0;ii<halfHeight;ii++) {
            for(j=0;j<halfWidth;j++) {
                /*
                  image[fullRGBIndex]=255;
                  image[fullRGBIndex+1]=0;
                  image[fullRGBIndex+2]=0;
                  image[fullRGBIndex+stride]=255;
                  image[fullRGBIndex+stride+1]=0;
                  image[fullRGBIndex+stride+2]=0;
                  image[fullRGBIndex+stride*width]=255;
                  image[fullRGBIndex+stride*width+1]=0;
                  image[fullRGBIndex+stride*width+2]=0;
                  image[fullRGBIndex+stride*width+stride]=255;
                  image[fullRGBIndex+stride*width+stride+1]=0;
                  image[fullRGBIndex+stride*width+stride+2]=0;
                */

                RGB2Y(yData[fullYIndex],inputImage[fullRGBTLIndex+2],inputImage[fullRGBTLIndex+1],inputImage[fullRGBTLIndex])
                    RGB2Y(yData[fullYIndex+1],inputImage[fullRGBTRIndex+2],inputImage[fullRGBTRIndex+1],inputImage[fullRGBTRIndex])
                    RGB2Y(yData[fullYIndex+width],inputImage[fullRGBBLIndex+2],inputImage[fullRGBBLIndex+1],inputImage[fullRGBBLIndex])
                    RGB2Y(yData[fullYIndex+width+1],inputImage[fullRGBBRIndex+2],inputImage[fullRGBBRIndex+1],inputImage[fullRGBBRIndex])

                    RGB2U(uData[halfIndex],\
                          inputImage[fullRGBTLIndex+2],inputImage[fullRGBTLIndex+1],inputImage[fullRGBTLIndex],\
                          inputImage[fullRGBTRIndex+2],inputImage[fullRGBTRIndex+1],inputImage[fullRGBTRIndex],\
                          inputImage[fullRGBBLIndex+2],inputImage[fullRGBBLIndex+1],inputImage[fullRGBBLIndex],\
                          inputImage[fullRGBBRIndex+2],inputImage[fullRGBBRIndex+1],inputImage[fullRGBBRIndex]\
                        );

                RGB2V(vData[halfIndex],\
                      inputImage[fullRGBTLIndex+2],inputImage[fullRGBTLIndex+1],inputImage[fullRGBTLIndex],\
                      inputImage[fullRGBTRIndex+2],inputImage[fullRGBTRIndex+1],inputImage[fullRGBTRIndex],\
                      inputImage[fullRGBBLIndex+2],inputImage[fullRGBBLIndex+1],inputImage[fullRGBBLIndex],\
                      inputImage[fullRGBBRIndex+2],inputImage[fullRGBBRIndex+1],inputImage[fullRGBBRIndex]\
                    );

                //yData[fullYIndex] = yData[fullYIndex+1] = yData[fullYIndex+width] = yData[fullYIndex+width+1] = 255;
                //uData[halfIndex] = 0;
                //vData[halfIndex] = 127;

                fullYIndex+=2;

                fullRGBTLIndex += stride*2;
                fullRGBTRIndex += stride*2;
                fullRGBBLIndex += stride*2;
                fullRGBBRIndex += stride*2;

                halfIndex++;
            }
            fullYIndex += width;

            fullRGBTLIndex += stride*((inputStride-width)+inputStride);
            fullRGBTRIndex += stride*((inputStride-width)+inputStride);
            fullRGBBLIndex += stride*((inputStride-width)+inputStride);
            fullRGBBRIndex += stride*((inputStride-width)+inputStride);
        }
    }
}

WebMEncoder::~WebMEncoder() {
    videoProcessorThread.join();

    while(packetsToWrite.size()>0) {
        if (!muxer_segment->AddFrame(packetsToWrite.begin()->second->packet,
                                     packetsToWrite.begin()->second->bytes,
                                     2, //Audio track
                                     packetsToWrite.begin()->first,
                                     curFrame%frameRateDen==0 //keyframe every second
                )) {
            printf("\n Could not add audio frame.\n");
        }
        delete packetsToWrite.begin()->second;
        packetsToWrite.erase(packetsToWrite.begin());
    }

    muxer_segment->Finalize();
    
    writer->Close();
}

void WebMEncoder::initializeVideoEncoder() {
    /* Populate encoder configuration */
    vpx_codec_err_t res = vpx_codec_enc_config_default(&vpx_enc_vp8_algo, &cfg, 0);
    
    if (res) {
        fprintf(stderr, "Failed to get config: %s\n",
                vpx_codec_err_to_string(res));
	    exit(1);
    }

    if(boost::thread::hardware_concurrency()) {
        cfg.g_threads = boost::thread::hardware_concurrency() - 1;
    }

    /* Change the default timebase to a high enough value so that the encoder
     * will always create strictly increasing timestamps.
     */
    cfg.g_timebase.num = 1;
    cfg.g_timebase.den = 30;

    // lag_in_frames allows for extra optimization at the expense of not writing frames in realtime
    cfg.g_lag_in_frames = 5;

    // Variable bit rate
    cfg.rc_end_usage = VPX_VBR;

    // Target data rate in Kbps (lowercase b)
    cfg.rc_target_bitrate = 1024; //1 Mbit/sec
    
    /* Never use the library's default resolution, require it be parsed
     * from the file or set on the command line.
     */
    cfg.g_w = width;
    cfg.g_h = height;
    
    vpx_img_alloc(&raw, VPX_IMG_FMT_YV12,
                  cfg.g_w, cfg.g_h, 1);
    
    cfg.g_pass = VPX_RC_ONE_PASS;
    
    /* Construct Encoder Context */
    vpx_codec_enc_init(&encoder, &vpx_enc_vp8_algo, &cfg, 0);
    if(encoder.err) {
        printf("Failed to initialize encoder\n");
    }
}

void WebMEncoder::initializeAudioEncoder() {
    /* Initialize Vorbis */
    vorbis_info_init(&vi);
    const float QUALITY = 0.4f;
    int ret2=vorbis_encode_init_vbr(&vi,audioChannels,audioSampleRate,QUALITY);
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
}

void WebMEncoder::initializeContainer(const char *filename) {
    writer = new mkvmuxer::MkvWriter();
    
    if (!writer->Open(filename)) {
        printf("\n Filename is invalid or error while opening.\n");
    }
    
    // Set Segment element attributes
    muxer_segment = new mkvmuxer::Segment(writer);
    muxer_segment->set_mode(mkvmuxer::Segment::kFile);
    
    muxer_segment->OutputCues(true);
    
    // Set SegmentInfo element attributes
    mkvmuxer::SegmentInfo* const info = muxer_segment->GetSegmentInfo();
    info->set_timecode_scale(1000000);
    info->set_writing_app("sample_muxer");
    
    // Set Tracks element attributes
    enum { kVideoTrack = 1, kAudioTrack = 2 };
    int vid_track = 0; // no track added
    int aud_track = 0; // no track added
    
    {
        // Add the video track to the muxer
        int VIDEO_TRACK_NUMBER = 1;
        int WIDTH = width;
        int HEIGHT= height;
        vid_track = muxer_segment->AddVideoTrack(WIDTH,
                                                 HEIGHT,
                                                 VIDEO_TRACK_NUMBER);
        if (!vid_track) {
            printf("\n Could not add video track.\n");
        }
        
        mkvmuxer::VideoTrack* const video =
            static_cast<mkvmuxer::VideoTrack*>(
                muxer_segment->GetTrackByNumber(vid_track));
        if (!video) {
            printf("\n Could not get video track.\n");
        }
        
        video->set_frame_rate(frameRateDen);
    }

    {
        // Add the audio track to the muxer
        int AUDIO_TRACK_NUMBER = 2;
        aud_track = muxer_segment->AddAudioTrack(audioSampleRate, audioChannels, AUDIO_TRACK_NUMBER);
        if (!aud_track) {
            printf("\n Could not add audio track.\n");
        }
        
        mkvmuxer::AudioTrack* const audio =
            static_cast<mkvmuxer::AudioTrack*>(
                muxer_segment->GetTrackByNumber(aud_track));
        if (!audio) {
            printf("\n Could not get audio track.\n");
        }
        
        ogg_packet opIden,opComm,opSetup;
        vorbis_analysis_headerout(&vd,&vc,&opIden,&opComm,&opSetup);
        
        int privateSize=0;
        unsigned char *privateData = (unsigned char*)malloc(1024*1024*1);
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
        
        if(privateSize>=1024*1024*1) {
            printf("PRIVATE DATA OVERFLOW IN WEBMENCODER\n");
            exit(1);
        }
        if (!audio->SetCodecPrivate(privateData, privateSize)) {
            printf("\n Could not add audio private data.\n");
        }
        free(privateData);
        
        audio->set_bit_depth(16);
    }
    
    // Set Cues element attributes
    mkvmuxer::Cues* const cues = muxer_segment->GetCues();
    cues->set_output_block_number(true);
    if (vid_track)
        muxer_segment->CuesTrack(vid_track);
    if (aud_track)
        muxer_segment->CuesTrack(aud_track);
}
