#include "NSM_Common.h"

#include "zlib.h"

Server *createGlobalServer(std::string _username,unsigned short _port);

void deleteGlobalServer();

class running_machine;

namespace apache { namespace thrift { namespace server {
      class TSimpleServer;
    } } }

class MameHubServerProcessor
{
 public:
 MameHubServerProcessor(int port) :
  port_(port) {
  }

  void operator()();

  void stop();

 protected:
  boost::shared_ptr<apache::thrift::server::TSimpleServer> server_;
  int port_;
};

class Server : public Common
{
 public:
  bool syncOverride;

 protected:
  std::vector<MemoryBlock> initialBlocks;
  nsm::Attotime staleTime;
  int staleGeneration;

  int port;

  int syncCount;

  std::list<std::pair<unsigned char *,int> > syncPacketQueue;

  int syncTransferSeconds;

  std::vector<RakNet::RakNetGUID> acceptedPeers;
  std::map<RakNet::RakNetGUID,std::vector<RakNet::RakNetGUID> > waitingForAcceptFrom;
  int maxPeerID;
  std::map<RakNet::RakNetGUID,int> deadPeerIDs;
  std::map<RakNet::RakNetGUID,std::string> candidateNames;

  bool blockNewClients;

  boost::thread syncThread;
  bool syncReady;
  nsm::Sync syncProto;

  MameHubServerProcessor mameHubServerProcessor;
  boost::thread serverThread;

 public:
  Server(std::string _username,int _port);

  virtual ~Server();

  void shutdown();

  void acceptPeer(RakNet::RakNetGUID guidToAccept,running_machine *machine);

  void removePeer(RakNet::RakNetGUID guid,running_machine *machine);

  bool initializeConnection();

  MemoryBlock createMemoryBlock(int size);

  std::vector<MemoryBlock> createMemoryBlock(unsigned char* ptr,int size);

  void initialSync(const RakNet::RakNetGUID &sa,running_machine *machine);

  virtual nsm::PeerInputData popInput(int peerID);

  bool update(running_machine *machine);

  void sync(running_machine *machine);

  void popSyncQueue();

  void setSyncTransferTime(int _syncTransferSeconds)
  {
    syncTransferSeconds = _syncTransferSeconds;
  }

  void sendBaseDelay(int baseDelay);

  inline void setBlockNewClients(bool blockNewClients) {
    this->blockNewClients = blockNewClients;
  }

  inline bool isBlockNewClients() { return blockNewClients; }
};
