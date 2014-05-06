#include <stdexcept>

#include "RakNet/RakPeerInterface.h"
#include "RakNet/RakNetStatistics.h"
#include "RakNet/RakNetTypes.h"
#include "RakNet/BitStream.h"
#include "RakNet/PacketLogger.h"
#include "RakNet/RakNetTypes.h"
#include "RakNet/GetTime.h"
#include "RakNet/RakSleep.h"

#include "NSM_Server.h"

#include <assert.h>
#include <cstdio>
#include <cstring>
#include <algorithm>
#include <stdlib.h>

#include "emu.h"

#include "unicode.h"
#include "ui.h"
#include "osdcore.h"
#include "emuopts.h"

#include "google/protobuf/io/lzma_protobuf_stream.h"
#include "google/protobuf/io/zero_copy_stream_impl_lite.h"

#include "gen-cpp/MameHubRpc.h"
#include <protocol/TJSONProtocol.h>
#include <server/TSimpleServer.h>
#include <transport/TServerSocket.h>
#include <transport/TTransportUtils.h>

using boost::shared_ptr;

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::server;

using namespace std;
using namespace nsm;
using namespace google::protobuf::io;

Server *netServer=NULL;

Server *createGlobalServer(string _username,unsigned short _port)
{
  cout << "Creating server on port " << _port << endl;
  netServer = new Server(_username,_port);
  netCommon = netServer;
  return netServer;
}

void deleteGlobalServer()
{
  if(netServer)
  {
    netServer->shutdown();
  }
  netServer = NULL;
}

// Copied from Multiplayer.cpp
// If the first byte is ID_TIMESTAMP, then we want the 5th byte
// Otherwise we want the 1st byte
extern unsigned char GetPacketIdentifier(RakNet::Packet *p);
extern unsigned char *GetPacketData(RakNet::Packet *p);
extern int GetPacketSize(RakNet::Packet *p);

#define INITIAL_BUFFER_SIZE (1024*1024*32)
unsigned char *compressedBuffer = (unsigned char*)malloc(INITIAL_BUFFER_SIZE);
int compressedBufferSize = INITIAL_BUFFER_SIZE;
unsigned char *syncBuffer = (unsigned char*)malloc(INITIAL_BUFFER_SIZE);
int syncBufferSize = INITIAL_BUFFER_SIZE;
unsigned char *uncompressedBuffer = (unsigned char*)malloc(INITIAL_BUFFER_SIZE);
int uncompressedBufferSize = INITIAL_BUFFER_SIZE;

class MameHubServerHandler : public MameHubRpcIf {
public:
  MameHubServerHandler() {
  }

  void getStatus(Status& status) {
    PlayerStatus playerStatus;
    playerStatus.name = "Digitalghost";
    status.playerStatus.push_back(playerStatus);
  }
};

void MameHubServerProcessor::operator()() {
  shared_ptr<MameHubServerHandler> handler(new MameHubServerHandler());
  shared_ptr<TProcessor> processor(new MameHubRpcProcessor(handler));
  shared_ptr<TServerTransport> serverTransport(new TServerSocket(port_));
  shared_ptr<TTransportFactory> transportFactory(new TBufferedTransportFactory());
  shared_ptr<TProtocolFactory> protocolFactory(new TJSONProtocolFactory());

  server_.reset(new TSimpleServer(processor,
                                  serverTransport,
                                  transportFactory,
                                  protocolFactory));
  server_->serve();
}

void MameHubServerProcessor::stop() {
  server_->stop();
}

Server::Server(string username,int _port)
  :
  Common(username),
  syncOverride(false),
  port(_port),
  maxPeerID(10),
  blockNewClients(false),
  mameHubServerProcessor(_port + 1) {
  rakInterface = RakNet::RakPeerInterface::GetInstance();

  syncCount=0;

  upsertPeer(rakInterface->GetMyGUID(),1,username,attotime(1,0));
  selfPeerID = 1;

  serverThread = boost::thread(mameHubServerProcessor);
}

Server::~Server() {
  mameHubServerProcessor.stop();
  serverThread.join();
}

void Server::shutdown()
{
  syncThread.join();

  // Be nice and let the server know we quit.
  rakInterface->Shutdown(300);

  // We're done with the network
  RakNet::RakPeerInterface::DestroyInstance(rakInterface);
}

void Server::acceptPeer(RakNet::RakNetGUID guidToAccept,running_machine *machine)
{
  cout << "ACCEPTED PEER " << guidToAccept.ToString() << endl;
  if(acceptedPeers.size()>=maxPeerID-1)
  {
    //Whoops! Someone took the last spot
    rakInterface->CloseConnection(guidToAccept,true);
    return;
  }

  //Accept this host
  acceptedPeers.push_back(guidToAccept);
  char buf[4096];
  buf[0] = ID_HOST_ACCEPTED;
  int assignID=-1;
  for(
    std::map<RakNet::RakNetGUID,int>::iterator it = deadPeerIDs.begin();
    it != deadPeerIDs.end();
    it++
    )
  {
    if(it->first==guidToAccept)
    {
      assignID = it->second;
    }
  }
  int lastUsedPeerID=1;
  if(assignID==-1)
  {
    bool usingNextPeerID=true;
    while(usingNextPeerID)
    {
      //Peer ID's are 1-based between 1 and maxPeerID inclusive, with 1 being reserved for the server
      lastUsedPeerID = (lastUsedPeerID+1)%(maxPeerID+1);
      if(!lastUsedPeerID) lastUsedPeerID=2;
      usingNextPeerID=false;

      for(
        std::map<RakNet::RakNetGUID,int>::iterator it = peerIDs.begin();
        it != peerIDs.end();
        it++
        )
      {
        if(it->second==lastUsedPeerID)
        {
          usingNextPeerID=true;
          break;
        }
      }
      if(!usingNextPeerID)
      {
        //We took a dead person's ID, delete their history
        for(
          std::map<RakNet::RakNetGUID,int>::iterator it = deadPeerIDs.begin();
          it != deadPeerIDs.end();
          )
        {
          if(it->second==lastUsedPeerID)
          {
            std::map<RakNet::RakNetGUID,int>::iterator itold = it;
            it++;
            deadPeerIDs.erase(itold);
          }
          else
          {
            it++;
          }
        }
      }
    }
    assignID = lastUsedPeerID;
  }
  upsertPeer(guidToAccept,assignID,candidateNames[guidToAccept],machine->time());
  candidateNames.erase(candidateNames.find(guidToAccept));

  printf("ASSIGNING ID %d TO NEW CLIENT\n",assignID);
  char *tmpbuf = buf+1;
  memcpy(tmpbuf,&assignID,sizeof(int));
  tmpbuf += sizeof(int);
  memcpy(tmpbuf,&(guidToAccept.g),sizeof(uint64_t));
  tmpbuf += sizeof(uint64_t);
  memcpy(tmpbuf,&(peerData[assignID].startTime.seconds),sizeof(peerData[assignID].startTime.seconds));
  tmpbuf += sizeof(peerData[assignID].startTime.seconds);
  memcpy(tmpbuf,&(peerData[assignID].startTime.attoseconds),sizeof(peerData[assignID].startTime.attoseconds));
  tmpbuf += sizeof(peerData[assignID].startTime.attoseconds);
  strcpy(
    (char*)tmpbuf,
    peerData[assignID].name.c_str()
    );
  tmpbuf += peerData[assignID].name.length()+1;  //add 1 so we get the \0 at the end
  rakInterface->Send(
    buf,
    int(tmpbuf-buf),
    HIGH_PRIORITY,
    RELIABLE_ORDERED,
    ORDERING_CHANNEL_SYNC,
    RakNet::UNASSIGNED_SYSTEM_ADDRESS,
    true
    );

  //Perform initial sync with player
  initialSync(guidToAccept,machine);
}

void Server::removePeer(RakNet::RakNetGUID guid,running_machine *machine)
{
  if(peerIDs.find(guid)!=peerIDs.end())
    if(peerData.find(peerIDs[guid]) != peerData.end())
      peerData.erase(peerIDs[guid]);
  cout << "REMOVING PEER\n";
  if(waitingForAcceptFrom.find(guid)!=waitingForAcceptFrom.end())
  {
    waitingForAcceptFrom.erase(waitingForAcceptFrom.find(guid));
  }
  //else
  {
    for(int a=0; a<(int)acceptedPeers.size(); a++)
    {
      if(acceptedPeers[a]==guid)
      {
        acceptedPeers.erase(acceptedPeers.begin()+a);
        //Add peer to the dead peer list
        for(
          std::map<RakNet::RakNetGUID,int>::iterator it = peerIDs.begin();
          it != peerIDs.end();
          )
        {
          if(it->first==guid)
          {
            deadPeerIDs[guid] = it->second;
            std::map<RakNet::RakNetGUID,int>::iterator itold = it;
            it++;
            peerIDs.erase(itold);
            break;
          }
          else
          {
            it++;
          }
        }
        break;
      }
    }
    for(std::map<RakNet::RakNetGUID,std::vector<RakNet::RakNetGUID> >::iterator it = waitingForAcceptFrom.begin();
        it != waitingForAcceptFrom.end();
      )
    {
      for(int a=0; a<(int)it->second.size(); a++)
      {
        if(it->second[a]==guid)
        {
          it->second.erase(it->second.begin()+a);
          a--;
        }
      }
      if(it->second.empty())
      {
        //A peer is now accepted because the person judging them disconnected
        RakNet::RakNetGUID accpetedPeer = it->first;
        std::map<RakNet::RakNetGUID,std::vector<RakNet::RakNetGUID> >::iterator oldit = it;
        it++;
        waitingForAcceptFrom.erase(oldit);
        acceptPeer(accpetedPeer,machine);
      }
      else
      {
        it++;
      }
    }
  }
}

bool Server::initializeConnection()
{
  RakNet::SocketDescriptor sd(0,0);
  printf("PORT: %d\n",port);
  sd.port = port;
  RakNet::StartupResult retval = rakInterface->Startup(512,&sd,1);
  rakInterface->SetMaximumIncomingConnections(512);
  rakInterface->SetIncomingPassword("MAME",(int)strlen("MAME"));
  rakInterface->SetTimeoutTime(5000,RakNet::UNASSIGNED_SYSTEM_ADDRESS);
  rakInterface->SetOccasionalPing(true);
  rakInterface->SetUnreliableTimeout(1000);

  if(retval != RakNet::RAKNET_STARTED)
  {
    printf("Server failed to start. Terminating\n");
    return false;
  }

  DataStructures::List<RakNet::RakNetSocket2*> sockets;
  rakInterface->GetSockets(sockets);
  printf("Ports used by RakNet:\n");
  for (unsigned int i=0; i < sockets.Size(); i++)
  {
    printf("%i. %i\n", i+1, sockets[i]->GetBoundAddress().GetPort());
  }
  return true;
}

MemoryBlock Server::createMemoryBlock(int size)
{
  blocks.push_back(MemoryBlock(size));
  staleBlocks.push_back(MemoryBlock(size));
  initialBlocks.push_back(MemoryBlock(size));
  return blocks.back();
}

vector<MemoryBlock> Server::createMemoryBlock(unsigned char *ptr,int size)
{
  if(blocks.size()==39)
  {
    //throw ("OOPS");
  }
  vector<MemoryBlock> retval;
  const int BYTES_IN_MB=1024*1024;
  if(size>BYTES_IN_MB)
  {
    for(int a=0;;a+=BYTES_IN_MB)
    {
      if(a+BYTES_IN_MB>=size)
      {
        vector<MemoryBlock> tmp = createMemoryBlock(ptr+a,size-a);
        retval.insert(retval.end(),tmp.begin(),tmp.end());
        break;
      }
      else
      {
        vector<MemoryBlock> tmp = createMemoryBlock(ptr+a,BYTES_IN_MB);
        retval.insert(retval.end(),tmp.begin(),tmp.end());
      }
    }
    return retval;
  }
  //printf("Creating memory block at %X with size %d\n",ptr,size);
  blocks.push_back(MemoryBlock(ptr,size));
  staleBlocks.push_back(MemoryBlock(size));
  initialBlocks.push_back(MemoryBlock(size));
  retval.push_back(blocks.back());
  return retval;
}

extern bool waitingForClientCatchup;
attotime oldInputTime;
extern astring &nvram_filename(astring &result, device_t &device);
extern int nvram_size(running_machine &machine);

void Server::initialSync(const RakNet::RakNetGUID &guid,running_machine *machine)
{
  cout << "INITIAL SYNC WITH GUID: " << guid.ToString() << endl;
  unsigned char checksum = 0;

  waitingForClientCatchup=true;
  machine->osd().pauseAudio(true);

  nsm::InitialSync initial_sync;

  if(getSecondsBetweenSync())
  {
    cout << "IN CRITICAL SECTION\n";
    cout << "SERVER: Sending initial snapshot\n";

    // NOTE: The server must send stale data to the client for the first time
    // So that future syncs will be accurate
    for(int blockIndex=0; blockIndex<int(initialBlocks.size()); blockIndex++)
    {
      vector<unsigned char> deltaBlock;
      //cout << "BLOCK " << blockIndex << ":\n";
            
      for(int a=0; a<staleBlocks[blockIndex].size; a++)
      {
        checksum = checksum ^ staleBlocks[blockIndex].data[a];
        //cout << int(staleBlocks[blockIndex].data[a]) << '\n';
        unsigned char value = initialBlocks[blockIndex].data[a] ^ staleBlocks[blockIndex].data[a];
        deltaBlock.push_back(value);
      }
      //cout << int(checksum) << endl;
      initial_sync.add_initial_block(&deltaBlock[0], deltaBlock.size());
    }
    cout << "CHECKSUM: " << int(checksum) << endl;
    initial_sync.set_checksum(checksum);
  }

  for(
    map<int,PeerData >::iterator it = peerData.begin();
    it != peerData.end();
    it++
    )
  {
    nsm::PeerInputDataList* peer_data = initial_sync.add_peer_data();
    peer_data->set_peer_id(it->first);
        
    for(int a=0; a<int(it->second.oldInputs.size()); a++)
    {
      nsm::PeerInputData* input_data = peer_data->add_input_data();
      input_data->CopyFrom(it->second.oldInputs[a]);
    }
    for(list<PeerInputData>::iterator it2 = it->second.availableInputs.begin(); 
        it2 != it->second.availableInputs.end(); it2++)
    {
      nsm::PeerInputData* input_data = peer_data->add_input_data();
      input_data->CopyFrom(*it2);
    }
    for(map<int,PeerInputData>::iterator it2 = it->second.delayedInputs.begin(); 
        it2 != it->second.delayedInputs.end(); it2++)
    {
      nsm::PeerInputData* input_data = peer_data->add_input_data();
      input_data->CopyFrom(it2->second);
    }
  }

  bool writenvram=(nvram_size(*machine)<1024*1024*32);
  if(writenvram) 
  {
    if (machine->config().m_nvram_handler != NULL)
    {
      astring filename;
      emu_file file(machine->options().nvram_directory(), OPEN_FLAG_READ);
      if (file.open(nvram_filename(filename,machine->root_device()), ".nv") == FILERR_NONE)
      {
        vector<unsigned char> fileContents(file.size());
        file.read(&fileContents[0],file.size());
        initial_sync.add_nvram(&fileContents[0],file.size());
        cout << "ADDING NVRAM OF SIZE: " << file.size() << " " << file.filename() << endl;
        file.close();
      }
    }

    nvram_interface_iterator iter(machine->root_device());
    for (device_nvram_interface *nvram = iter.first(); nvram != NULL; nvram = iter.next())
    {
      astring filename;
      emu_file file(machine->options().nvram_directory(), OPEN_FLAG_READ);
      if (file.open(nvram_filename(filename,nvram->device())) == FILERR_NONE)
      {
        vector<unsigned char> fileContents(file.size());
        file.read(&fileContents[0],file.size());
        initial_sync.add_nvram(&fileContents[0],file.size());
        cout << "ADDING NVRAM OF SIZE: " << file.size() << " " << file.filename() << endl;
        file.close();
      }
    }
  }

  string s;
  {
    StringOutputStream sos(&s);
    {
      LzmaOutputStream los(&sos);
      los.ChangeEncodingOptions(4);
      initial_sync.SerializeToZeroCopyStream(&los);
      los.Flush();
    }
  }

  int sizeRemaining = s.length()+sizeof(int)+sizeof(int);
  int packetSize = max(256,min(1024,sizeRemaining/100));
  int offset = 0;

  oldInputTime.seconds = oldInputTime.attoseconds = 0;

  while(sizeRemaining>packetSize)
  {
    RakNet::BitStream bitStreamPart(packetSize+32);
    unsigned char header = ID_INITIAL_SYNC_PARTIAL;
    bitStreamPart.WriteBits((const unsigned char*)&header,8*sizeof(unsigned char));
    bitStreamPart.WriteBits((const unsigned char*)(s.c_str()+offset),8*packetSize);
    sizeRemaining -= packetSize;
    offset += packetSize;
    rakInterface->Send(
      &bitStreamPart,
      MEDIUM_PRIORITY,
      RELIABLE_ORDERED,
      ORDERING_CHANNEL_SYNC,
      guid,
      false
      );
    ui_update_and_render(*machine, &machine->render().ui_container());
    machine->osd().update(false);
    RakSleep(10);
  }
  {
    RakNet::BitStream bitStreamPart(packetSize+32);
    unsigned char header = ID_INITIAL_SYNC_COMPLETE;
    bitStreamPart.WriteBits((const unsigned char*)&header,8*sizeof(unsigned char));
    bitStreamPart.WriteBits((const unsigned char*)(s.c_str()+offset),8*sizeRemaining);
    rakInterface->Send(
      &bitStreamPart,
      MEDIUM_PRIORITY,
      RELIABLE_ORDERED,
      ORDERING_CHANNEL_SYNC,
      guid,
      false
      );
    ui_update_and_render(*machine, &machine->render().ui_container());
    machine->osd().update(false);
    RakSleep(10);
  }

  cout << "FINISHED SENDING BLOCKS TO CLIENT\n";
  cout << "SERVER: Done with initial snapshot\n";
  cout << "OUT OF CRITICAL AREA\n";
  cout.flush();
}

nsm::PeerInputData Server::popInput(int peerID) {
  nsm::PeerInputData inputToPop = Common::popInput(peerID);
  if(peerData[peerID].oldInputs.size() > 10000) {
    if(!blockNewClients) {
      //TODO: put a warning here.
    }
    blockNewClients = true;
  }
  return inputToPop;
}

bool Server::update(running_machine *machine)
{
  //cout << "SERVER TIME: " << RakNet::GetTimeMS()/1000.0f/60.0f << endl;
  //printf("Updating server\n");
  RakNet::Packet *p;
  for (p=rakInterface->Receive(); p; rakInterface->DeallocatePacket(p), p=rakInterface->Receive())
  {
    // We got a packet, get the identifier with our handy function
    unsigned char packetIdentifier = GetPacketIdentifier(p);

    //printf("GOT PACKET %d\n",int(packetIdentifier));

    // Check if this is a network message packet
    switch (packetIdentifier)
    {
    case ID_CONNECTION_LOST:
      // Couldn't deliver a reliable packet - i.e. the other system was abnormally
      // terminated
    case ID_DISCONNECTION_NOTIFICATION:
      // Connection lost normally
      printf("ID_DISCONNECTION_NOTIFICATION from %s\n", p->systemAddress.ToString(true));
      removePeer(p->guid,machine);
      break;


    case ID_NEW_INCOMING_CONNECTION:
      // Somebody connected.  We have their IP now
      printf("ID_NEW_INCOMING_CONNECTION from %s with GUID %s\n", p->systemAddress.ToString(true), p->guid.ToString());
      break;

    case ID_CLIENT_INFO:
      cout << "GOT ID_CLIENT_INFO\n";
      if(blockNewClients || (syncCount<1 && secondsBetweenSync>0) || syncPacketQueue.size())
      {
        // We aren't allowing new clients
        rakInterface->CloseConnection(p->guid,true);
        break;
      }

      //This client is requesting candidacy, set their info
      {
        char buf[4096];
        strcpy(buf,(char*)(p->data+1));
        candidateNames[p->guid] = buf;
      }

      //Find a session index for the player
      {
        char buf[4096];
        buf[0] = ID_SETTINGS;
        buf[1] = ((syncCount <= 1) ? 0 : 1); //Should the client catch up?
        memcpy(buf+2,&secondsBetweenSync,sizeof(int));
        strcpy(buf+2+sizeof(int),username.c_str());
        rakInterface->Send(
          buf,
          2+sizeof(int)+username.length()+1,
          HIGH_PRIORITY,
          RELIABLE_ORDERED,
          ORDERING_CHANNEL_SYNC,
          p->guid,
          false
          );
      }
      if(acceptedPeers.size()>=maxPeerID-1)
      {
        //Sorry, no room
        rakInterface->CloseConnection(p->guid,true);
      }
      else if(acceptedPeers.size())
      {
        printf("Asking other peers to accept %s %s\n",p->systemAddress.ToString(),p->guid.ToString());
        waitingForAcceptFrom[p->guid] = std::vector<RakNet::RakNetGUID>();
        for(int a=0; a<acceptedPeers.size(); a++)
        {
          RakNet::RakNetGUID guid = acceptedPeers[a];
          waitingForAcceptFrom[p->guid].push_back(guid);
          cout << "SENDING ADVERTIZE TO " << guid.ToString() << endl;
          char buf[4096];
          buf[0] = ID_ADVERTISE_SYSTEM;
          strcpy(buf+1,p->systemAddress.ToString(true));
          rakInterface->Send(buf,1+strlen(p->systemAddress.ToString(true))+1,HIGH_PRIORITY,RELIABLE_ORDERED,ORDERING_CHANNEL_SYNC,guid,false);
        }
        printf("Asking other peers to accept\n");
      }
      else
      {
        //First client, automatically accept
        acceptPeer(p->guid,machine);
      }
      break;

    case ID_INCOMPATIBLE_PROTOCOL_VERSION:
      printf("ID_INCOMPATIBLE_PROTOCOL_VERSION\n");
      break;

    case ID_ACCEPT_NEW_HOST:
    {
      printf("Accepting new host\n");
      RakNet::RakNetGUID guidToAccept;
      RakNet::SystemAddress saToAccept;
      saToAccept.SetBinaryAddress(((char*)p->data)+1);
      guidToAccept = rakInterface->GetGuidFromSystemAddress(saToAccept);
      if(waitingForAcceptFrom.find(guidToAccept)==waitingForAcceptFrom.end())
      {
        throw std::runtime_error("OOPS");
      }
      for(int a=0; a<waitingForAcceptFrom[guidToAccept].size(); a++)
      {
        if(waitingForAcceptFrom[guidToAccept][a]==p->guid)
        {
          waitingForAcceptFrom[guidToAccept].erase(waitingForAcceptFrom[guidToAccept].begin()+a);
          break;
        }
      }
      if(waitingForAcceptFrom[guidToAccept].empty())
      {
        cout << "Accepting: " << guidToAccept.ToString() << endl;
        waitingForAcceptFrom.erase(waitingForAcceptFrom.find(guidToAccept));
        acceptPeer(guidToAccept,machine);
      }
    }
    break;

    case ID_REJECT_NEW_HOST:
    {
      RakNet::RakNetGUID guidToReject;
      RakNet::SystemAddress saToReject;
      saToReject.SetBinaryAddress(((char*)p->data)+1);
      guidToReject = rakInterface->GetGuidFromSystemAddress(saToReject);
      printf("Rejecting new client\n");
      cout << p->guid.ToString() << " REJECTS " << guidToReject.ToString() << endl;
      if(waitingForAcceptFrom.find(guidToReject)==waitingForAcceptFrom.end())
        printf("Could not find waitingForAcceptFrom for this GUID, weird\n");
      else
        waitingForAcceptFrom.erase(waitingForAcceptFrom.find(guidToReject));
      rakInterface->CloseConnection(guidToReject,true);
    }
    break;

    case ID_INPUTS:
    {
      string s = doInflate(GetPacketData(p), GetPacketSize(p));
      PeerInputDataList inputDataList;
      inputDataList.ParseFromString(s);
      receiveInputs(&inputDataList);
      break;
    }
    default:
      printf("UNEXPECTED PACKET ID: %d\n",int(packetIdentifier));
      break;
    }

  }

  return true;
}

class SyncProcessor
{
public:
  int uncompressedSize;
  std::list<std::pair<unsigned char *,int> >* syncPacketQueue;
  int syncTransferSeconds;
  bool* syncReadyPtr;

  SyncProcessor(int _uncompressedSize,
                std::list<std::pair<unsigned char *,int> >* _syncPacketQueue,
                int _syncTransferSeconds,
                bool* _syncReadyPtr) :
    uncompressedSize(_uncompressedSize),
    syncPacketQueue(_syncPacketQueue),
    syncTransferSeconds(_syncTransferSeconds),
    syncReadyPtr(_syncReadyPtr) {
    *syncReadyPtr = false;
  }

  void operator()() {
    if(compressedBufferSize <= zlibGetMaxCompressedSize(uncompressedSize) + 128)
    {
      compressedBufferSize = zlibGetMaxCompressedSize(uncompressedSize)+128;
      compressedBuffer = (unsigned char*)realloc(compressedBuffer,compressedBufferSize);
      if(!compressedBuffer)
      {
        cout << __FILE__ << ":" << __LINE__ << " OUT OF MEMORY\n";
        exit(1);
      }
    }
    uLongf compressedSizeLong = compressedBufferSize;

    printf("COMPRESSING...\n");
    if(compress2(
         compressedBuffer,
         &compressedSizeLong,
         uncompressedBuffer,
         uncompressedSize,9
         )!=Z_OK)
    {
      cout << "ERROR COMPRESSING ZLIB STREAM\n";
      exit(1);
    }
    int compressedSize = (int)compressedSizeLong;
    printf("SYNC SIZE: %d (compressed: %d)\n",uncompressedSize,compressedSize);
    if(compressedSize > 16*1024*1024) { // If bigger than 16MB, don't even bother.
      netServer->syncOverride = true;
      return;
    }

    int SYNC_PACKET_SIZE=1024*1024*64;
    if(syncTransferSeconds)
    {
      int actualSyncTransferSeconds=max(1,syncTransferSeconds);
      while(true)
      {
        SYNC_PACKET_SIZE = compressedSize/60/actualSyncTransferSeconds;

        if(actualSyncTransferSeconds==1) {
          // Make sure that we send SOMETHING each frame
          if(SYNC_PACKET_SIZE==0) {
            SYNC_PACKET_SIZE = compressedSize;
          }
        }

        // This sends the data at 20 KB/sec minimum
        if(SYNC_PACKET_SIZE>=350 || actualSyncTransferSeconds==1) break;

        actualSyncTransferSeconds--;
      }
    }

    int sendMessageSize = 1+sizeof(int)+sizeof(int)+min(SYNC_PACKET_SIZE,compressedSize);
    int totalSendSizeEstimate = sendMessageSize*(compressedSize/SYNC_PACKET_SIZE + 2);
    if(syncBufferSize <= totalSendSizeEstimate)
    {
      syncBufferSize = totalSendSizeEstimate*1.5;
      syncBuffer = (unsigned char*)realloc(syncBuffer,totalSendSizeEstimate);
      if(!syncBuffer)
      {
        cout << __FILE__ << ":" << __LINE__ << " OUT OF MEMORY\n";
        exit(1);
      }
    }
    unsigned char *sendMessage = syncBuffer;
    sendMessage[0] = ID_RESYNC_PARTIAL;
    if(compressedSize<=SYNC_PACKET_SIZE)
      sendMessage[0] = ID_RESYNC_COMPLETE;
    memcpy(sendMessage+1,&uncompressedSize,sizeof(int));
    memcpy(sendMessage+1+sizeof(int),&compressedSize,sizeof(int));
    memcpy(sendMessage+1+sizeof(int)+sizeof(int),compressedBuffer,min(SYNC_PACKET_SIZE,compressedSize) );

    syncPacketQueue->push_back(make_pair(sendMessage,sendMessageSize));
    sendMessage += sendMessageSize;
    compressedSize -= SYNC_PACKET_SIZE;
    int atIndex = SYNC_PACKET_SIZE;

    while(compressedSize>0)
    {
      sendMessageSize = 1+min(SYNC_PACKET_SIZE,compressedSize);
      sendMessage[0] = ID_RESYNC_PARTIAL;
      if(compressedSize<=SYNC_PACKET_SIZE)
        sendMessage[0] = ID_RESYNC_COMPLETE;
      memcpy(sendMessage+1,compressedBuffer+atIndex,min(SYNC_PACKET_SIZE,compressedSize) );
      compressedSize -= SYNC_PACKET_SIZE;
      atIndex += SYNC_PACKET_SIZE;

      syncPacketQueue->push_back(make_pair(sendMessage,sendMessageSize));
      sendMessage += sendMessageSize;
    }

    if(int(sendMessage-syncBuffer) >= totalSendSizeEstimate)
    {
      cout << "INVALID SEND SIZE ESTIMATE!\n";
      exit(1);
    }
    cout << "FINISHED QUEUEING SYNC\n";
    *syncReadyPtr = true;
  }
};

void Server::sync()
{
  if(syncOverride)
    return;

  syncCount++;

  nsm::InitialSync initial_sync;

  cout << "IN CRITICAL SECTION\n";
  cout << "SERVER: Sending initial snapshot\n";
    
  int bytesSynched=0;
  //cout << "IN CRITICAL SECTION\n";
  //cout << "SERVER: Syncing with clients\n";
  bool anyDirty=false;
  unsigned char blockChecksum=0;
  unsigned char xorChecksum=0;
  unsigned char staleChecksum=0;
  unsigned char allStaleChecksum=0;
  unsigned char *uncompressedPtr = uncompressedBuffer;
  for(int blockIndex=0; blockIndex<int(blocks.size()); blockIndex++)
  {
    MemoryBlock &block = blocks[blockIndex];
    MemoryBlock &staleBlock = staleBlocks[blockIndex];
    MemoryBlock &initialBlock = initialBlocks[blockIndex];

    if(block.size != staleBlock.size)
    {
      cout << "BLOCK SIZE MISMATCH\n";
    }

    bool dirty=false;
    if(memcmp(block.data, staleBlock.data, block.size)) {
      dirty = true;
    }
    if(dirty)
    {
      for(int a=0; a<block.size; a++)
      {
        blockChecksum = blockChecksum ^ block.data[a];
        staleChecksum = staleChecksum ^ staleBlock.data[a];
      }
    }
    //dirty=true;
    if(syncCount==1)
    {
      memcpy(initialBlock.data,block.data,block.size);
    }
    if(dirty && !anyDirty)
    {
      //First dirty block
      anyDirty=true;
    }

    if(dirty)
    {
      bytesSynched += block.size;
      int bytesUsed = uncompressedPtr-uncompressedBuffer;
      while(bytesUsed+sizeof(int)+block.size >= uncompressedBufferSize)
      {
        cout << "REALLOCATING BUFFER FROM " << uncompressedBufferSize
             << " TO " << uncompressedBufferSize*3/2 << endl;
        uncompressedBufferSize *= 1.5;
        uncompressedBuffer = (unsigned char*)realloc(uncompressedBuffer,uncompressedBufferSize);
        uncompressedPtr = uncompressedBuffer + bytesUsed;
        if(!uncompressedBuffer)
        {
          cout << __FILE__ << ":" << __LINE__ << " OUT OF MEMORY\n";
          exit(1);
        }
      }
      memcpy(
        uncompressedPtr,
        &blockIndex,
        sizeof(int)
        );
      uncompressedPtr += sizeof(int);
      for(int a=0;a<block.size;a++)
      {
        *uncompressedPtr = block.data[a] ^ staleBlock.data[a];
        uncompressedPtr++;
      }
      memcpy(staleBlock.data,block.data,block.size);
    }
    //cout << "BLOCK " << blockIndex << ": ";
    for(int a=0; a<block.size; a++)
    {
      allStaleChecksum = allStaleChecksum ^ staleBlock.data[a];
    }
    //cout << int(allStaleChecksum) << endl;
  }
  if(anyDirty && syncCount>1) // The first sync is not sent to clients
  {
    printf("BLOCK CHECKSUM: %d\n",int(blockChecksum));
    printf("XOR CHECKSUM: %d\n",int(xorChecksum));
    printf("STALE CHECKSUM (dirty): %d\n",int(staleChecksum));
    printf("STALE CHECKSUM (all): %d\n",int(allStaleChecksum));
    int finishIndex = -1;
    memcpy(
      uncompressedPtr,
      &finishIndex,
      sizeof(int)
      );
    uncompressedPtr += sizeof(int);
    int uncompressedSize = uncompressedPtr-uncompressedBuffer;
    SyncProcessor syncProcessor(uncompressedSize, &syncPacketQueue,
                                syncTransferSeconds, &syncReady);
    syncThread.join();
    syncThread = boost::thread(syncProcessor);


  }
  else
  {
    printf("No dirty blocks found\n");
  }
  //if(runTimes%1==0) cout << "BYTES SYNCED: " << bytesSynched << endl;
  //cout << "OUT OF CRITICAL AREA\n";
  //cout.flush();
}

void Server::popSyncQueue()
{
  if(!syncReady)
    return;
  if(syncPacketQueue.size())
  {
    pair<unsigned char *,int> syncPacket = syncPacketQueue.front();
    printf("Sending sync message of size %d (%lu packets left)\n",syncPacket.second,syncPacketQueue.size());
    syncPacketQueue.pop_front();

    rakInterface->Send(
      (const char*)syncPacket.first,
      syncPacket.second,
      HIGH_PRIORITY,
      RELIABLE_ORDERED,
      ORDERING_CHANNEL_SYNC,
      RakNet::UNASSIGNED_SYSTEM_ADDRESS,
      true
      );
  }
}

void Server::sendBaseDelay(int baseDelay)
{
  char* dataToSend = (char*)malloc(5);
  dataToSend[0] = ID_BASE_DELAY;
  memcpy(dataToSend+1,&baseDelay,sizeof(int));
  //cout << "SENDING MESSAGE WITH LENGTH: " << intSize << endl;
  rakInterface->Send(
    dataToSend,
    5,
    MEDIUM_PRIORITY,
    RELIABLE_ORDERED,
    ORDERING_CHANNEL_BASE_DELAY,
    RakNet::UNASSIGNED_SYSTEM_ADDRESS,
    true
    );
  free(dataToSend);
}

