#include "NSM_Common.h"

#include <stdexcept>

#include "RakNet/RakPeerInterface.h"
#include "RakNet/RakNetStatistics.h"
#include "RakNet/RakNetTypes.h"
#include "RakNet/BitStream.h"
#include "RakNet/PacketLogger.h"
#include "RakNet/RakNetTypes.h"

#include "lib7z/LzmaEnc.h"
#include "lib7z/LzmaDec.h"

#define NO_MEM_TRACKING
#include "emu.h"
#include "attotime.h"
#include "osdcore.h"

using namespace std;
using namespace nsm;

Common *netCommon = NULL;

SRes OnProgress(void *p, UInt64 inSize, UInt64 outSize)
{
  // Update progress bar.
  return SZ_OK;
}
ICompressProgress g_ProgressCallback = { &OnProgress };

void * AllocForLzma(void *p, size_t size)
{
  void *ptr = malloc(size);
  if(!ptr)
  {
    cout << "CANNOT ALLOCATE BLOCK OF SIZE: " << (size/1024.0/1024.0) << " MB!\n";
    throw std::runtime_error("FAILED TO ALLOCATE BLOCK");
  }
  return ptr;
}
void FreeForLzma(void *p, void *address)
{
  free(address);
}
ISzAlloc SzAllocForLzma = { &AllocForLzma, &FreeForLzma };

int zlibGetMaxCompressedSize(int origSize)
{
  return origSize*1.01 + 256;
}
int lzmaGetMaxCompressedSize(int origSize)
{
  return origSize + origSize/3 + 256 + LZMA_PROPS_SIZE;
}

void lzmaCompress(
  unsigned char* destBuf,
  int &destSize,
  unsigned char *srcBuf,
  int srcSize,
  int compressionLevel
  )
{
  SizeT propsSize = LZMA_PROPS_SIZE;

  SizeT lzmaDestSize = (SizeT)destSize;

  CLzmaEncProps props;
  LzmaEncProps_Init(&props);
  props.level = compressionLevel; //compression level
  //props.dictSize = 1 << 16;
  props.dictSize = 1 << 24;
  props.writeEndMark = 1; // 0 or 1
  LzmaEncProps_Normalize(&props);

  int res = LzmaEncode(
    destBuf+LZMA_PROPS_SIZE, &lzmaDestSize,
    srcBuf, srcSize,
    &props, destBuf, &propsSize, props.writeEndMark,
    &g_ProgressCallback, &SzAllocForLzma, &SzAllocForLzma);

  destSize = (int)lzmaDestSize + LZMA_PROPS_SIZE;

  cout << "COMPRESSED " << srcSize << " BYTES DOWN TO " << destSize << endl;

  if(res != SZ_OK || propsSize != LZMA_PROPS_SIZE)
  {
    cout << "ERROR COMPRESSING DATA\n";
    cout << res << ',' << propsSize << ',' << LZMA_PROPS_SIZE << endl;
    exit(1);
  }
}

void lzmaUncompress(
  unsigned char* destBuf,
  int destSize,
  unsigned char *srcBuf,
  int srcSize
  )
{
  SizeT lzmaDestSize = (SizeT)destSize;
  SizeT lzmaSrcSize = (SizeT)srcSize - LZMA_PROPS_SIZE;

  cout << "DECOMPRESSING " << srcSize << endl;

  ELzmaStatus finishStatus;
  int res = LzmaDecode(
    destBuf, &lzmaDestSize,
    srcBuf+LZMA_PROPS_SIZE, &lzmaSrcSize,
    srcBuf, LZMA_PROPS_SIZE, LZMA_FINISH_END,
    &finishStatus, &SzAllocForLzma);

  cout << "DECOMPRESSED " << srcSize << " BYTES DOWN TO " << lzmaDestSize << endl;

  if(res != SZ_OK || finishStatus != LZMA_STATUS_FINISHED_WITH_MARK)
  {
    cout << "ERROR DECOMPRESSING DATA\n";
    cout << res << ',' << finishStatus << endl;
    exit(1);
  }
}

extern volatile bool memoryBlocksLocked;

// Copied from Multiplayer.cpp
// If the first byte is ID_TIMESTAMP, then we want the 5th byte
// Otherwise we want the 1st byte
extern unsigned char GetPacketIdentifier(RakNet::Packet *p);
extern unsigned char *GetPacketData(RakNet::Packet *p);
extern int GetPacketSize(RakNet::Packet *p);

Common::Common(string _username) :
  secondsBetweenSync(0),
  globalInputCounter(0),
  selfPeerID(0),
  generation(1),
  username(_username),
  player(0)
{
  if(username.length()>16)
  {
    username = username.substr(0,16);
  }

  /* allocate deflate state */
  outputStream.zalloc = Z_NULL;
  outputStream.zfree = Z_NULL;
  outputStream.opaque = Z_NULL;
  int retval = deflateInit(&outputStream,9);
  if(retval != Z_OK)
  {
    cout << "ERROR INITIALIZING ZLIB\n";
    exit(1);
  }

  /* allocate inflate state */
  inputStream.zalloc = Z_NULL;
  inputStream.zfree = Z_NULL;
  inputStream.opaque = Z_NULL;
  retval = inflateInit(&inputStream);
  if(retval != Z_OK)
  {
    cout << "ERROR INITIALIZING ZLIB\n";
    exit(1);
  }
}

Common::~Common() {
}

extern int baseDelayFromPing;

RakNet::SystemAddress Common::ConnectBlocking(const char *defaultAddress, unsigned short defaultPort,bool newClient)
{
  char ipAddr[64];
  {
    {
      strcpy(ipAddr, defaultAddress);
    }
  }
  if (rakInterface->Connect(ipAddr, defaultPort, "MAME", (int)strlen("MAME"))!=RakNet::CONNECTION_ATTEMPT_STARTED)
  {
    printf("Failed connect call for %s : %d.\n", defaultAddress, int(defaultPort) );
    return RakNet::UNASSIGNED_SYSTEM_ADDRESS;
  }
  printf("Connecting to %s:%d...",ipAddr,defaultPort);
  RakNet::Packet *packet;
  while (1)
  {
    for (packet=rakInterface->Receive(); packet; rakInterface->DeallocatePacket(packet), packet=rakInterface->Receive())
    {
      cout << "GOT PACKET: " << int(packet->data[0] - ID_USER_PACKET_ENUM) << endl;

      if (packet->data[0]==ID_CONNECTION_REQUEST_ACCEPTED)
      {
        printf("Connected!\n");
        return packet->systemAddress;
      }
      else if(packet->data[0]==ID_INPUTS)
      {
        string s = doInflate(GetPacketData(packet), GetPacketSize(packet));
        PeerInputDataList inputDataList;
        inputDataList.ParseFromString(s);
        receiveInputs(&inputDataList);
      }
      else if(packet->data[0]== ID_BASE_DELAY)
      {
        cout << "Changing base delay from " << baseDelayFromPing;
        memcpy(&baseDelayFromPing,GetPacketData(packet),sizeof(int));
        cout << " to " << baseDelayFromPing << endl;
      }
      else
      {
        printf("Failed: %d\n",int(packet->data[0]));
        return RakNet::UNASSIGNED_SYSTEM_ADDRESS;
      }
      //JJG: Need to sleep for 1/10th a sec here osd_sleep(100);
    }
  }
}

std::string Common::doInflate(const unsigned char *inputString, int length) {
  int chunkSize = 128*1024;
  vector<char> v(chunkSize, '\0');
  int writeIndex=0;
  inflateReset(&inputStream);
  inputStream.avail_out = chunkSize;
  inputStream.next_out = (Bytef*)(&(v[writeIndex]));
  inputStream.avail_in = length;
  inputStream.next_in = (Bytef*)inputString;
  while(true) {
    int ret = inflate(&inputStream, Z_NO_FLUSH);
    if(ret == Z_STREAM_END) {
      writeIndex += (chunkSize - inputStream.avail_out);
      break;
    } else if(ret != Z_OK) {
      printf("OOPS: GOT ERROR INFLATING: %d\n",ret);
      exit(1);
    } else {
      printf("GOT OK\n");
      if(inputStream.avail_out==0) {
        printf("NEED TO RESIZE BUFFER %d %ld %d %d\n", writeIndex, v.size()+chunkSize, inputStream.avail_in, inputStream.avail_out);
        v.resize(v.size()+chunkSize, '\0');
        writeIndex += chunkSize;
        inputStream.next_out = (Bytef*)(&(v[writeIndex]));
        inputStream.avail_out = chunkSize;
      }
    }
  }
  string s(&(v[0]), writeIndex);
  return s;
}

void Common::upsertPeer(RakNet::RakNetGUID guid,int peerID,string name,nsm::Attotime startTime)
{
  if(startTime.seconds()<1) {
    startTime = newAttotime(1,0);
  }
  cout << "UPSERTING PEER WITH ID: " << peerID << " AND NAME: " << name << endl;
  peerIDs[guid] = peerID;
  cout << "UPSERTING PEER WITH ID: " << peerID << " AND NAME: " << name << endl;
  cout << "PEER DATA SIZE: " << peerData.size() << endl;
  if(peerData.find(peerID)==peerData.end()) {
    cout << "PEER DOES NOT EXIST, INSERTING" << endl;
    peerData[peerID] = PeerData(name,startTime);
  }
  else {
    cout << "PEER ALREADY EXISTS, UPDATING" << endl;
    peerData[peerID].name = name;
    peerData[peerID].startTime = startTime;
  }
}

void Common::setSecondsBetweenSync(int _secondsBetweenSync)
{
  secondsBetweenSync = _secondsBetweenSync;
}

time_t lastSecondChecked=0;
double predictedPingMean=100.0;
double predictedPingVariance=10.0;
int numPingSamples=0;

int Common::getLargestPing(int currentSecond)
{
  time_t curSec = time(NULL);
  if (curSec > lastSecondChecked) {
    lastSecondChecked = curSec;
    int lastPing=-1;
    for(int a=0; a<rakInterface->NumberOfConnections(); a++)
    {
      lastPing = max(rakInterface->GetLastPing(rakInterface->GetSystemAddressFromIndex(a)),lastPing);
      //printf("PING: %d\n",rakInterface->GetAveragePing(rakInterface->GetSystemAddressFromIndex(a)));
    }
    if (lastPing == -1) {
      // No connections, don't update stats
      return predictedPingMean;
    }
    if (numPingSamples==0) {
      predictedPingMean = lastPing;
    } else {
      const int PRIOR_SAMPLE_ESTIMATE = 600;
      double oldMean = predictedPingMean;
      predictedPingMean = predictedPingMean + ((lastPing - predictedPingMean) / PRIOR_SAMPLE_ESTIMATE);
      predictedPingVariance = (predictedPingVariance*(PRIOR_SAMPLE_ESTIMATE-1) + ((lastPing - oldMean)*(lastPing - predictedPingMean))) / PRIOR_SAMPLE_ESTIMATE;
    }
    numPingSamples++;
  }
  if (numPingSamples<60) {
    // Guess what the variance will be, add for unmeasured noise
    return int(predictedPingMean + 100 + 100);
  } else {
    // Use the computed variance, add for unmeasured noise
    return int(predictedPingMean + sqrt(predictedPingVariance+100)*3);
  }

/*
  int largestPing=1;
  for(int a=0; a<rakInterface->NumberOfConnections(); a++)
  {
    largestPing = max(rakInterface->GetAveragePing(rakInterface->GetSystemAddressFromIndex(a)),largestPing);
  }
  return largestPing;
*/
}

bool Common::hasPeerWithID(int peerID)
{
  if(selfPeerID==peerID) return true;
  for(
    std::map<RakNet::RakNetGUID,int>::iterator it = peerIDs.begin();
    it != peerIDs.end();
    it++
    )
  {
    if(it->second==peerID)
    {
      return true;
    }
  }
  return false;
}

string Common::getLatencyString(int peerID)
{
  for(
    std::map<RakNet::RakNetGUID,int>::iterator it = peerIDs.begin();
    it != peerIDs.end();
    it++
    )
  {
    if(it->second==peerID)
    {
      char buf[4096];
      sprintf(buf,"Peer %d: %d ms", peerID, rakInterface->GetAveragePing(it->first));
      return string(buf);
    }
  }
  printf("ERROR GETTING LATENCY STRING\n");
  return "";
}

string Common::getStatisticsString()
{
  RakNet::RakNetStatistics *rss;
  string retval;
  for(int a=0; a<rakInterface->NumberOfConnections(); a++)
  {
    char message[4096];
    rss=rakInterface->GetStatistics(rakInterface->GetSystemAddressFromIndex(a));
    sprintf(
      message,
      "Sent: %d\n"
      "Recv: %d\n"
      "Loss: %.0f%%\n"
      "Latency: %dms\n",
      (int)rss->valueOverLastSecond[RakNet::ACTUAL_BYTES_SENT],
      (int)rss->valueOverLastSecond[RakNet::ACTUAL_BYTES_RECEIVED],
      rss->packetlossLastSecond,
      int((predictedPingMean + sqrt(predictedPingVariance)*3)/2)
      );
    retval += string(message) + string("\n");
  }
  return retval;
}

void Common::getPeerIDs(vector<int> &retval)
{
  retval.clear();
  for(
    std::map<RakNet::RakNetGUID,int>::iterator it = peerIDs.begin();
    it != peerIDs.end();
    it++
    )
  {
    retval.push_back(it->second);
  }
}

int Common::getNumPeers()
{
  return int(peerIDs.size());
}

int Common::getPeerID(int index)
{
  for(
    std::map<RakNet::RakNetGUID,int>::iterator it = peerIDs.begin();
    it != peerIDs.end();
    it++
    )
  {
    if(index==0)
    {
      return it->second;
    }
    index--;
  }
  throw std::runtime_error("TRIED TO GET UNKNOWN PEER ID_");
}

PeerInputData Common::popInput(int peerID)
{
  if(peerData.find(peerID)==peerData.end())
    throw std::runtime_error("TRIED TO POP INPUT FROM UNKNOWN PEER");

  while (true) {
    if(peerData[peerID].availableInputs.empty()) {
      return PeerInputData();
    }

    list<PeerInputData>::iterator it = peerData[peerID].availableInputs.begin();
    //cout << "POPPING INPUT " << it->first << " FROM PEER " << peerID << endl;
    PeerInputData retval = *it;

    if (retval.generation() > generation) {
      // This input is too new, do not process it (yet)
      cout << "GOT INPUT FROM FUTURE GENERATION (THIS COULD BE BAD)\n";
      return PeerInputData();
    }

    peerData[peerID].oldInputs.push_back(retval);
    peerData[peerID].availableInputs.erase(it);
    if (retval.generation() == generation) {
      // If the input is from an older generation, do not return it.
      return retval;
    } else {
      cout << "GOT INPUT FROM OLDER GENERATION (DISCARDING)\n";
    }
  }
}

nsm::Attotime Common::getStartTime(int peerID)
{
  if(peerData.find(peerID)==peerData.end())
    throw std::runtime_error("TRIED TO GET STARTTIME FROM UNKNOWN PEER");
  return peerData[peerID].startTime;
}

/*
  void addSubByteLocations(const vector<MemoryBlock> &blocks,unsigned char value,const set<BlockValueLocation> &locationsToIntersect,vector<BlockValueLocation> &newLocations) {
	//cout << "ON SIZE: " << sizeof(T) << endl;
	for(int a=0;a<(int)blocks.size();a++)
	{
  //cout << "ON BLOCK: " << a << " WITH SIZE " << blocks[a].size << endl;
  for(int b=0;b<int(blocks[a].size);b++)
  {
  //cout << "BLOCK INDEX: " << b << endl;
  //if(b<=(int(blocks[a].size)-sizeof(T)))
  //cout << "IN RANGE\n";
  //cout << "VALUE: " << *((T*)(blocks[a].data+b)) << endl;
  for(int c=3;c<8;c++) {
  unsigned char mask = 0;
  for(int d=0;d<c;d++) mask |= (1<<d);

  if((value&mask)!=value) continue; //Mask is not big enough

  int maxShift = 8-c;

  for(int d=0;d<=maxShift;d++) {
  if((value<<d) == ((blocks[a].data[b])&(mask<<d))) {
  bool doNotAdd=false;
  if(!doNotAdd)
  {
  BlockValueLocation bvl(a,b,1,(mask<<d));
  if(locationsToIntersect.empty()==false)
  {
  doNotAdd=false;
  if(locationsToIntersect.find(bvl)==locationsToIntersect.end())
  doNotAdd=true;
  if(!doNotAdd)
  {
  newLocations.push_back(bvl);
  }
  }
  else
  {
  newLocations.push_back(bvl);
  }
  }
  }
  }
  }
  }
	}
  }
*/

template<class T>
void addLocations(const vector<MemoryBlock> &blocks,unsigned int value,const set<BlockValueLocation> &locationsToIntersect,vector<BlockValueLocation> &newLocations,const vector<pair<unsigned char *,int> > &ramBlocks)
{
	//cout << "ON SIZE: " << sizeof(T) << endl;
	for(int a=0;a<(int)blocks.size();a++)
	{
		//cout << "ON BLOCK: " << a << " WITH SIZE " << blocks[a].size << endl;
		for(int b=0;b<int(blocks[a].size);b++)
		{
			//cout << "BLOCK INDEX: " << b << endl;
			//if(b<=(int(blocks[a].size)-sizeof(T)))
      //cout << "IN RANGE\n";
			//cout << "VALUE: " << *((T*)(blocks[a].data+b)) << endl;
			if(b>(int(blocks[a].size)-sizeof(T))) continue;
			if( (((T)value) == *((T*)(blocks[a].data+b))) )
			{
				bool doNotAdd=false;
				if(!doNotAdd)
				{
					BlockValueLocation bvl(0,a,b,sizeof(T),0);
					if(locationsToIntersect.empty()==false)
					{
						doNotAdd=false;
						if(locationsToIntersect.find(bvl)==locationsToIntersect.end())
              doNotAdd=true;
						if(!doNotAdd)
						{
							newLocations.push_back(bvl);
						}
					}
					else
          {
						newLocations.push_back(bvl);
					}
				}
			}
    }
  }
	//cout << "ON SIZE: " << sizeof(T) << endl;
	for(int a=0;a<(int)ramBlocks.size();a++)
	{
		//cout << "ON BLOCK: " << a << " WITH SIZE " << blocks[a].size << endl;
		for(int b=0;b<int(ramBlocks[a].second);b++)
		{
			//cout << "BLOCK INDEX: " << b << endl;
			//if(b<=(int(blocks[a].size)-sizeof(T)))
      //cout << "IN RANGE\n";
			//cout << "VALUE: " << *((T*)(blocks[a].data+b)) << endl;
			if(b>(int(ramBlocks[a].second)-sizeof(T))) continue;
			if( (((T)value) == *((T*)(ramBlocks[a].first+b))) )
			{
				bool doNotAdd=false;
				if(!doNotAdd)
				{
					BlockValueLocation bvl(1,a,b,sizeof(T),0);
					if(locationsToIntersect.empty()==false)
					{
						doNotAdd=false;
						if(locationsToIntersect.find(bvl)==locationsToIntersect.end())
							doNotAdd=true;
						if(!doNotAdd)
						{
							newLocations.push_back(bvl);
						}
					}
					else
					{
						newLocations.push_back(bvl);
					}
				}
			}
		}
	}
}

vector<BlockValueLocation> Common::getLocationsWithValue(unsigned int value, const vector<BlockValueLocation> &locationsToIntersect, const vector<pair<unsigned char *,int> > &ramBlocks)
{
	set<BlockValueLocation> locationsSet(locationsToIntersect.begin(),locationsToIntersect.end());
	//cout << "CHECKING FOR " << value << endl;
	vector<BlockValueLocation> newLocations;
	addLocations<unsigned int>(blocks,value,locationsSet,newLocations,ramBlocks);
	//addLocations<signed int>(blocks,value,locationsSet,newLocations);
	addLocations<unsigned short>(blocks,value,locationsSet,newLocations,ramBlocks);
	//addLocations<signed short>(blocks,value,locationsSet,newLocations);
	addLocations<unsigned char>(blocks,value,locationsSet,newLocations,ramBlocks);
	//addLocations<signed char>(blocks,value,locationsSet,newLocations);
	//addSubByteLocations(blocks,value,locationsSet,newLocations);
	return newLocations;
}

void Common::updateForces(const vector<pair<unsigned char *,int> > &ramBlocks) {
  for(int a=0;a<forcedLocations.size();a++) {
    BlockValueLocation &bvl = forcedLocations[a].first;
    if(bvl.ramRegion==0) {
      if(bvl.memorySize==1) {
        if(bvl.memoryMask>0) {
          unsigned char curValue = ((*((unsigned char*)(blocks[bvl.blockIndex].data+bvl.memoryStart))) & (~(bvl.memoryMask)));
          // Calculate the shift from the mask
          int shift=0;
          unsigned char tmpMask = bvl.memoryMask;
          while((tmpMask&1)==0) {
            shift++;
            tmpMask>>=1;
          }
          *((unsigned char*)(blocks[bvl.blockIndex].data+bvl.memoryStart)) = ((((unsigned char)forcedLocations[a].second)<<shift) | curValue);
        } else {
          *((unsigned char*)(blocks[bvl.blockIndex].data+bvl.memoryStart)) = (unsigned char)forcedLocations[a].second;
        }
      }
      if(bvl.memorySize==2) {
        *((unsigned short*)(blocks[bvl.blockIndex].data+bvl.memoryStart)) = (unsigned short)forcedLocations[a].second;
      }
      if(bvl.memorySize==4) {
        *((unsigned int*)(blocks[bvl.blockIndex].data+bvl.memoryStart)) = (unsigned int)forcedLocations[a].second;
      }
    } else {
      if(bvl.memorySize==1) {
        if(bvl.memoryMask>0) {
          unsigned char curValue = ((*((unsigned char*)(ramBlocks[bvl.blockIndex].first+bvl.memoryStart))) & (~(bvl.memoryMask)));
          // Calculate the shift from the mask
          int shift=0;
          unsigned char tmpMask = bvl.memoryMask;
          while((tmpMask&1)==0) {
            shift++;
            tmpMask>>=1;
          }
          *((unsigned char*)(ramBlocks[bvl.blockIndex].first+bvl.memoryStart)) = ((((unsigned char)forcedLocations[a].second)<<shift) | curValue);
        } else {
          *((unsigned char*)(ramBlocks[bvl.blockIndex].first+bvl.memoryStart)) = (unsigned char)forcedLocations[a].second;
        }
      }
      if(bvl.memorySize==2) {
        *((unsigned short*)(ramBlocks[bvl.blockIndex].first+bvl.memoryStart)) = (unsigned short)forcedLocations[a].second;
      }
      if(bvl.memorySize==4) {
        *((unsigned int*)(ramBlocks[bvl.blockIndex].first+bvl.memoryStart)) = (unsigned int)forcedLocations[a].second;
      }
    }
  }
}

void Common::sendInputs(const nsm::Attotime &inputTime, PeerInputData::PeerInputType inputType, const InputState &inputState)
{
  PeerInputData peerInputData;
  peerInputData.set_counter(globalInputCounter);
  peerInputData.set_inputtype(inputType);
  peerInputData.set_generation(generation);
  Attotime *inputDataTime = peerInputData.mutable_time();
  inputDataTime->set_seconds(inputTime.seconds());
  inputDataTime->set_attoseconds(inputTime.attoseconds());
  InputState *inputStateDest = peerInputData.mutable_inputstate();
  inputStateDest->MergeFrom(inputState);

  sendInputs(peerInputData);
}

void Common::sendInputs(const nsm::Attotime &inputTime, PeerInputData::PeerInputType inputType, const string &inputString)
{
  PeerInputData peerInputData;
  peerInputData.set_counter(globalInputCounter);
  peerInputData.set_inputtype(inputType);
  peerInputData.set_generation(generation);
  Attotime *inputDataTime = peerInputData.mutable_time();
  inputDataTime->set_seconds(inputTime.seconds());
  inputDataTime->set_attoseconds(inputTime.attoseconds());
  peerInputData.set_inputbuffer(inputString);

  sendInputs(peerInputData);
}

void Common::sendInputs(const PeerInputData& peerInputData) {
  //cout << "SENDING INPUTS AT TIME " << peerInputData.time().seconds() << "." << peerInputData.time().attoseconds() << endl;
  //cout << "SELF PEER ID: " << selfPeerID << endl;
	if(peerData.find(selfPeerID)==peerData.end()) {
		throw std::runtime_error("DO NOT KNOW MY OWN PEER ID YET");
	}
	//cout << "INPUT STRING: " << inputString << endl;

  PeerInputDataList peerInputDataList;
  peerInputDataList.set_peer_id(selfPeerID);
  peerInputDataList.add_input_data()->CopyFrom(peerInputData);

  //cout << "SENDING INPUT WITH GC " << globalInputCounter << endl;

  // Broadcast the input and the last EXTRA_INPUTS_TO_PACK to everyone else
  int inputsAdded = 0;
  const int EXTRA_INPUTS_TO_PACK = 2;
  for(list<PeerInputData>::reverse_iterator it = peerData[selfPeerID].availableInputs.rbegin();
      it !=  peerData[selfPeerID].availableInputs.rend() && inputsAdded < EXTRA_INPUTS_TO_PACK; it++) {
    peerInputDataList.add_input_data()->CopyFrom(*it);
    inputsAdded++;
  }
  for(boost::circular_buffer<PeerInputData>::reverse_iterator it = peerData[selfPeerID].oldInputs.rbegin();
      it != peerData[selfPeerID].oldInputs.rend() && inputsAdded < EXTRA_INPUTS_TO_PACK; it++) {
    peerInputDataList.add_input_data()->CopyFrom(*it);
    inputsAdded++;
  }

  string sNoHeader;
  peerInputDataList.AppendToString(&sNoHeader);

  string sCompress(sNoHeader.length()*2, 0);
  sCompress[0] = ID_INPUTS;
  deflateReset(&outputStream);

  outputStream.avail_in = sNoHeader.length();
  outputStream.next_in = (Bytef*)sNoHeader.c_str();
  outputStream.avail_out = sCompress.length() - 1;
  outputStream.next_out = (Bytef*)&(sCompress[1]);
  while(outputStream.avail_in>0) {
    if (deflate(&outputStream, Z_FINISH) == Z_STREAM_ERROR) {
      printf("ZLIB ERROR\n");
      exit(1);
    }
    if(outputStream.avail_out==0 && outputStream.avail_in>0) {
      printf("ZLIB OUTPUT BUFFER WAS TOO SMALL\n");
      exit(1);
    }
  }
  int bytesUsed = sCompress.length() - outputStream.avail_out;

  //cout << "SENDING INPUT PACKET OF SIZE: " << sNoHeader.length() << " (compresses to " << bytesUsed << ")" << endl;

  rakInterface->Send(
    sCompress.c_str(),
    bytesUsed,
    HIGH_PRIORITY,
    RELIABLE,
    ORDERING_CHANNEL_INPUTS,
    RakNet::UNASSIGNED_SYSTEM_ADDRESS,
    true
    );

  // "Send" the inputs to yourself
  receiveInputs(&peerInputDataList);

  // Increment the global input counter
  globalInputCounter++;
}

attotime protoToAttotime(const Attotime &at) {
  attotime t(at.seconds(), at.attoseconds());
  return t;
}

void Common::receiveInputs(const PeerInputDataList *inputDataList) {
  int peerID = inputDataList->peer_id();
  //cout << "GOT INPUTS FROM " << peerID << endl;
  if(peerData.find(peerID)==peerData.end()) {
    peerData[peerID] = PeerData("unknown", newAttotime(0,0));
  }
    
  int nextGC = peerData[peerID].nextGC;

  list<PeerInputData> &availableInputs = peerData[peerID].availableInputs;
  map<int, PeerInputData> &delayedInputs = peerData[peerID].delayedInputs;

  for(int a=0;a<inputDataList->input_data_size();a++) {
    const PeerInputData &inputData = inputDataList->input_data(a);

    //cout << "GOT INPUT " << inputData.counter() << " (looking for " << nextGC << ") " << endl;
    //cout << inputData.time().seconds() << "." << inputData.time().attoseconds() << endl;
    if(inputData.counter() == nextGC) {
      if(inputData.has_inputstate()) {
        attotime t = protoToAttotime(inputData.time());
        if(t > protoToAttotime(peerData[peerID].lastInputTime)) {
          peerData[peerID].lastInputTime = inputData.time();
        }
      }
      availableInputs.push_back(inputData);
      nextGC++;
    } else if(inputData.counter() > nextGC) {
      peerData[peerID].delayedInputs[inputData.counter()] = inputData;
    }
  }

  // Check if other data can now be made available
  while(true) {
    map<int,PeerInputData>::iterator it = delayedInputs.find(nextGC);

    if(it == delayedInputs.end()) {
      break;
    } else {
      availableInputs.push_back(it->second);
      attotime t = protoToAttotime(it->second.time());
      if(t > protoToAttotime(peerData[peerID].lastInputTime)) {
        peerData[peerID].lastInputTime = it->second.time();
      }
      delayedInputs.erase(it);
      nextGC++;
    }
  }

  peerData[peerID].nextGC = nextGC;
}

pair<int,nsm::Attotime> Common::getOldestPeerInputTime() {
  int i = -1;
  nsm::Attotime t = newAttotime(0,0);
  for(
    std::map<int,PeerData>::iterator it = peerData.begin();
    it != peerData.end();
    it++
    )
  {
    if(i==-1) {
      t = it->second.lastInputTime;
      i = it->first;
    } else {
      if(protoToAttotime(t) > protoToAttotime(it->second.lastInputTime)) {
        t = it->second.lastInputTime;
        i = it->first;
      }
    }
  }
  return pair<int,nsm::Attotime>(i,t);
}

