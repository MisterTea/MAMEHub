#ifndef __NSM_COMMON__
#define __NSM_COMMON__

//RAKNET MUST COME FIRST, OTHER LIBS TRY TO REPLACE new/delete/malloc/free WITH THEIR OWN SHIT
//for ID_USER_PACKET_ENUM
#include "RakNet/MessageIdentifiers.h"

//for guid, systemaddress, etc.
#include "RakNet/RakNetTypes.h"

//For RakNet::GetTimeUS
#include "RakNet/GetTime.h"

#include <algorithm>
#include <cstdlib>
#include <deque>
#include <iostream>
#include <list>
#include <set>
#include <vector>
#include <string>
#include <map>
#include <cstring>

#include "emu.h"
#include "attotime.h"

#include "nsm.pb.h"

#include "zlib.h"

#include "boost/circular_buffer.hpp"

#include <boost/thread.hpp>

int zlibGetMaxCompressedSize(int origSize);
int lzmaGetMaxCompressedSize(int origSize);

void lzmaCompress(
                  unsigned char* destBuf,
                  int &destSize,
                  unsigned char *srcBuf,
                  int srcSize,
                  int compressionLevel
                  );

void lzmaUncompress(
                    unsigned char* destBuf,
                    int destSize,
                    unsigned char *srcBuf,
                    int srcSize
                    );

enum OrderingChannelType
  {
    ORDERING_CHANNEL_INPUTS,
    ORDERING_CHANNEL_BASE_DELAY,
    ORDERING_CHANNEL_SYNC,
    ORDERING_CHANNEL_CONST_DATA,
    ORDERING_CHANNEL_END
  };

enum CustomPacketType
  {
    ID_INPUTS=ID_USER_PACKET_ENUM,
    ID_BASE_DELAY,
    ID_INITIAL_SYNC_PARTIAL,
    ID_INITIAL_SYNC_COMPLETE,
    ID_RESYNC_PARTIAL,
    ID_RESYNC_COMPLETE,
    ID_SETTINGS,
    ID_REJECT_NEW_HOST,
    ID_ACCEPT_NEW_HOST,
    ID_HOST_ACCEPTED,
    ID_SEND_PEER_ID,
    ID_CLIENT_INFO,
    ID_END
  };

class Client;
class Server;
class Common;

extern Client *netClient;
extern Server *netServer;
extern Common *netCommon;

class MemoryBlock
{
 public:
  unsigned char *data;
  int size;

 MemoryBlock()
   :
  data(NULL),
    size(0)
      {
      }

 MemoryBlock(int _size)
   :
  size(_size)
  {
    data = (unsigned char*)malloc(_size);
    memset(data,0,_size);
  }

 MemoryBlock(unsigned char *_data,int _size)
   :
  data(_data),
    size(_size)
    {
    }

  int getBitCount()
  {
    int bitCount=0;
    for(int a=0;a<size;a++)
      {
        for(int bitNum=0;bitNum<7;bitNum++)
          {
            if( (data[a]&(1<<bitNum)) != 0)
              {
                bitCount++;
              }
          }
      }
    return bitCount;
  }
};

class BlockValueLocation
{
 public:
  unsigned char ramRegion;
  int blockIndex,memoryStart,memorySize;
  unsigned char memoryMask;

 BlockValueLocation(unsigned char _ramRegion,int _blockIndex,int _memoryStart,int _memorySize,unsigned char _memoryMask)
   :
  ramRegion(_ramRegion),
    blockIndex(_blockIndex),
    memoryStart(_memoryStart),
    memorySize(_memorySize),
    memoryMask(_memoryMask)
    {
    }

  bool operator <(const BlockValueLocation &other) const {
    if(ramRegion<other.ramRegion) return true;
    else if(ramRegion>other.ramRegion) return false;

    if(blockIndex<other.blockIndex) return true;
    else if(blockIndex>other.blockIndex) return false;

    if(memoryStart<other.memoryStart) return true;
    else if(memoryStart>other.memoryStart) return false;

    if(memorySize<other.memorySize) return true;
    else if(memorySize>other.memorySize) return false;

    if(memoryMask<other.memoryMask) return true;
    else if(memoryMask>other.memoryMask) return false;

    return false;
  }
};

class PeerData
{
 public:
  std::string name;
  std::list<nsm::PeerInputData> availableInputs;
  std::map<int, nsm::PeerInputData> delayedInputs;

  boost::circular_buffer<nsm::PeerInputData> oldInputs;
  attotime startTime;
  attotime lastInputTime;
  int nextGC;

  PeerData() {}

 PeerData(std::string _name, attotime _startTime)
   :
  name(_name),
    oldInputs(15000),
    startTime(_startTime),
    lastInputTime(startTime),
    nextGC(0)
      {
      }
};

class Common
{
 protected:
  RakNet::RakPeerInterface *rakInterface;

  int secondsBetweenSync;
  int globalInputCounter;

  std::vector<MemoryBlock> blocks,staleBlocks;

  z_stream inputStream;
  z_stream outputStream;

  int selfPeerID;

  std::map<RakNet::RakNetGUID,int> peerIDs;

  std::string username;
  std::map<int,PeerData> peerData;

  std::vector<std::pair<BlockValueLocation,int> > forcedLocations;

 public:

  Common(std::string _username);

  virtual ~Common();

  void upsertPeer(RakNet::RakNetGUID guid,int peerID,std::string name,attotime startTime);

  int getLargestPing();

  RakNet::SystemAddress ConnectBlocking(const char *defaultAddress, unsigned short defaultPort, bool newClient);

  int getSecondsBetweenSync()
  {
    return secondsBetweenSync;
  }

  void setSecondsBetweenSync(int _secondsBetweenSync);

  virtual MemoryBlock createMemoryBlock(int size) = 0;

  virtual std::vector<MemoryBlock> createMemoryBlock(unsigned char* ptr,int size) = 0;

  virtual bool update(running_machine *machine) = 0;

  int getNumBlocks()
  {
    return int(blocks.size());
  }

  MemoryBlock getMemoryBlock(int i)
  {
    return blocks[i];
  }

  bool hasPeerWithID(int peerID);

  std::string getLatencyString(int peerID);

  std::string getStatisticsString();

  void getPeerIDs(std::vector<int> &retval);

  int getNumPeers();

  int getPeerID(int a);

  virtual nsm::PeerInputData popInput(int peerID);

  attotime getStartTime(int peerID);

  inline int getSelfPeerID()
  {
    return selfPeerID;
  }

  inline const std::string &getPeerNameFromID(int id)
  {
    return peerData[id].name;
  }

  std::vector<BlockValueLocation> getLocationsWithValue(unsigned int value,
                                                        const std::vector<BlockValueLocation> &locationsToIntersect,
                                                        const std::vector<std::pair<unsigned char *,int> > &ramBlocks);

  void forceLocation(BlockValueLocation location,unsigned int value) {
    forcedLocations.push_back(std::pair<BlockValueLocation,int>(location,value));
  }

  void updateForces(const std::vector<std::pair<unsigned char *,int> > &ramBlocks);

  void sendInputs(const attotime &inputTime, nsm::PeerInputData::PeerInputType inputType, const nsm::InputState &inputState);
  void sendInputs(const attotime &inputTime, nsm::PeerInputData::PeerInputType inputType, const std::string &inputString);

  void receiveInputs(const nsm::PeerInputDataList *inputDataList);

  std::pair<int,attotime> getOldestPeerInputTime();

  int getPlayer() { return player; }

  void setPlayer(int newPlayer) { player = newPlayer; }

 protected:
  void sendInputs(const nsm::PeerInputData &peerInputData);

  std::string doInflate(const unsigned char *inputString, int length);

  int player;
};

#endif
