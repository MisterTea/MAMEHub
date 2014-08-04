include 'core.thrift'

namespace * com.mamehub.rpc

exception NotAuthorizedException {
    1: string errorMessage,
}

service MameHubRpc
{
    i32 ping(),

  core.Player getMyself(1:string token),

  core.MessageQueue getMessages (1:string token, 2:i32 startIndex) throws ( 1:NotAuthorizedException e ),
  
  void sendMessage (1:string token, 2:string targetUserId, 3:core.Message message) throws ( 1:NotAuthorizedException e),

  void broadcastMessage (1:string token, 2:core.Message message) throws ( 1:NotAuthorizedException e),
  
  core.Player getPlayer(1:string token, 2:string targetId) throws ( 1:NotAuthorizedException e),
  
  core.ServerState getServerState(1:string token) throws ( 1:NotAuthorizedException e),
  
  void logout(1:string token) throws (1:NotAuthorizedException e),
  
  core.Game hostGame(1:string token, 2:string system, 3:string rom, 4:string cart) throws (1:NotAuthorizedException e),

  string joinGame(1:string token, 2:string gameId) throws (1:NotAuthorizedException e),
  
  void leaveGame(1:string token) throws (1:NotAuthorizedException e),
  
  void sendError(1:string error),
  
  set<core.RomPointer> getFavorites() throws (1:NotAuthorizedException e),

    bool changePassword(1:string token, 2:string oldPassword, 3:string newPassword) throws (1:NotAuthorizedException e),

    bool changeUsername(1:string token, 2:string newUsername) throws (1:NotAuthorizedException e),

    bool emailPassword(1:string emailAddress),

    core.PlayerProfile getPlayerProfile(1:string token, 2:string playerId) throws (1:NotAuthorizedException e),

    void updateProfile(1:string token, 2:core.PlayerProfile newProfile) throws (1:NotAuthorizedException e),
    
    void updateStatus(1:string token, 2:core.PlayerStatus status) throws (1:NotAuthorizedException e),
    
    void postUserFeedback(1:string token, 2:string comment, 3:string log) throws (1:NotAuthorizedException e),
    
    void setPorts(1:string token, 2:i32 basePort, 3:i32 secondaryPort) throws (1:NotAuthorizedException e),
    
    string getMOTD(),
}


service MameHubClientRpc
{
  core.DownloadableRomState getDownloadableRoms(1:i64 lastCheckTime),
  
  set<string> requestRoms(1:string system, 2:set<string> romNames),

  i32 getFileCount(1:string system, 2:string romName),
  
  core.PeerFileInfo getFileInfo(1:string system, 2:string romName, 3:i32 index),
  
    core.FileResponse getFileChunk(1:core.FileRequest request),

    bool ping(),
}