#include "NSM_Common.h"

#include "zlib.h"

Client *createGlobalClient(std::string _username);

void deleteGlobalClient();

#define MAX_COMPRESSED_OUTBUF_SIZE (1024*1024*64)

class running_machine;

class Client : public Common
{
protected:

	std::vector<MemoryBlock> syncCheckBlocks;
    std::vector<unsigned char> incomingMsg;

    bool initComplete;

	unsigned char *syncPtr;

	bool firstResync;

	std::vector<unsigned char> initialSyncBuffer;

    RakNet::TimeUS timeBeforeSync;

public:
    Client() {}

	Client(std::string _username);

    void shutdown();

	MemoryBlock createMemoryBlock(int size);

	std::vector<MemoryBlock> createMemoryBlock(unsigned char* ptr,int size);

	bool initializeConnection(unsigned short selfPort,const char *hostname,unsigned short port,running_machine *machine);

	void updateSyncCheck();

    bool sync(running_machine *machine);

    void revert(running_machine *machine);

    bool update(running_machine *machine);

    void loadInitialData(unsigned char *data,int size,running_machine *machine);

    bool resync(unsigned char *data,int size,running_machine *machine);

	void checkMatch(Server *server);

    inline bool isInitComplete()
    {
	    return initComplete;
    }

    int getNumSessions();

};
