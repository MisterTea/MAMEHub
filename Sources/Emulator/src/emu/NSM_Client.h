#include "NSM_Common.h"

#include "zlib.h"

Client *createGlobalClient(std::string _username);

void deleteGlobalClient();

#define MAX_COMPRESSED_OUTBUF_SIZE (1024*1024*64)

class running_machine;

class Client : public Common
{
 protected:

  std::vector<boost::shared_ptr<MemoryBlock> > syncCheckBlocks;
  std::vector<unsigned char> incomingMsg;

  bool initComplete;

  unsigned char *syncPtr;

  bool firstResync;

  std::vector<unsigned char> initialSyncBuffer;

  RakNet::TimeUS timeBeforeSync;

  int syncGeneration;
  int syncSeconds;
  long long syncAttoseconds;

 public:
  Client(std::string _username);

  void shutdown();

  std::vector<boost::shared_ptr<MemoryBlock> > createMemoryBlock(const std::string& name, unsigned char* ptr,int size);

  bool initializeConnection(unsigned short selfPort,const char *hostname,unsigned short port,running_machine *machine);

  void updateSyncCheck();

  bool sync(running_machine *machine);

  void revert(running_machine *machine);

  bool update(running_machine *machine);

  void loadInitialData(unsigned char *data,int size,running_machine *machine);
  void createInitialBlocks(running_machine *machine);

  bool resync(unsigned char *data,int size,running_machine *machine);

  inline bool isInitComplete()
  {
    return initComplete;
  }

  int getNumSessions();

};
