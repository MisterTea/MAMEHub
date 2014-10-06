#ifndef __NSM_COMMON__
#define __NSM_COMMON__

#include "thrift_config.h"

#include "boost/circular_buffer.hpp"
#include <boost/thread.hpp>

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

#include "nsm.pb.h"

#include "zlib.h"

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

class running_machine;

class MemoryBlock
{
public:
std::string name;
unsigned char *data;
int size;
bool ownsMemory;

MemoryBlock(const std::string& _name, int _size);
MemoryBlock(const std::string& _name, unsigned char *_data,int _size);
~MemoryBlock();

private:
MemoryBlock(MemoryBlock const&);
    MemoryBlock& operator=(MemoryBlock const&);
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
  nsm::Attotime startTime;
  nsm::Attotime lastInputTime;
  int nextGC;

  PeerData() {}

 PeerData(std::string _name, nsm::Attotime _startTime)
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

std::vector<boost::shared_ptr<MemoryBlock> > blocks,staleBlocks;

  z_stream inputStream;
  z_stream outputStream;

  int selfPeerID;
  int generation;
  int unmeasuredNoise;

  std::map<RakNet::RakNetGUID,int> peerIDs;

  std::string username;
  std::map<int,PeerData> peerData;

  std::vector<std::pair<BlockValueLocation,int> > forcedLocations;

 public:

  Common(std::string _username, int _unmeasuredNoise);

  virtual ~Common();

  void upsertPeer(RakNet::RakNetGUID guid,int peerID,std::string name,nsm::Attotime startTime);

  int getLargestPing(int machineSeconds);

  RakNet::SystemAddress ConnectBlocking(const char *defaultAddress, unsigned short defaultPort, bool newClient);

  int getSecondsBetweenSync()
  {
    return secondsBetweenSync;
  }

  void setSecondsBetweenSync(int _secondsBetweenSync);

  virtual std::vector<boost::shared_ptr<MemoryBlock> > createMemoryBlock(const std::string& name, unsigned char* ptr,int size) = 0;

  virtual bool update(running_machine *machine) = 0;

  int getNumBlocks()
  {
    return int(blocks.size());
  }

  boost::shared_ptr<MemoryBlock> getMemoryBlock(int i)
  {
    return blocks[i];
  }

  nsm::Attotime newAttotime(int seconds, long long attoseconds) {
    nsm::Attotime at;
    at.set_seconds(seconds);
    at.set_attoseconds(attoseconds);
    return at;
  }

  bool hasPeerWithID(int peerID);

  std::string getLatencyString(int peerID);

  std::string getStatisticsString();

  void getPeerIDs(std::vector<int> &retval);

  int getNumPeers();

  int getPeerID(int a);

  virtual nsm::PeerInputData popInput(int peerID);

  nsm::Attotime getStartTime(int peerID);

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

  void sendInputs(const nsm::Attotime &inputTime, nsm::PeerInputData::PeerInputType inputType, const nsm::InputState &inputState);
  void sendInputs(const nsm::Attotime &inputTime, nsm::PeerInputData::PeerInputType inputType, const std::string &inputString);

  void receiveInputs(const nsm::PeerInputDataList *inputDataList);

  std::pair<int,nsm::Attotime> getOldestPeerInputTime();

  int getPlayer() { return player; }

  void setPlayer(int newPlayer) { player = newPlayer; }

  inline int getInputCounter() { return globalInputCounter; }

 protected:
  void sendInputs(const nsm::PeerInputData &peerInputData);

  std::string doInflate(const unsigned char *inputString, int length);

  int player;
};

#endif
