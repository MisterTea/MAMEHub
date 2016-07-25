namespace * com.mamehub.thrift

const i32 MAMEHUB_VERSION = 12

enum MR
{
  MISSING_FILES,
  BAD_FILES,
  MISSING_CLONE,
  MISSING_PARENT,
  MISSING_SYSTEM,
  MISSING_CHD,
}

struct SoftwareList
{
  1:string name,
  2:string filter,
}

struct RomInfo
{
  1:string _id, // id is the same as the shortname
  2:string cloneRom,
  3:string parentRom,
  4:list<string> filenames = [],
  5:string description,
  6:MR missingReason,
  8:list<SoftwareList> softwareLists = [];
  9:string system,

  // The following are for software ROMs
  10:map<string,string> interfaceFileMap = {},
}

struct FileInfo {
  1:string _id, //Also the path
  2:bool bad,
  3:string crc32,
  4:i64 length,
  5:map<string,string> contentsCrc32 = {},
  6:string chdName,
  7:string systemName,
}

struct MediaType
{
  1:string mediaName,
  2:list<string> extensions,
}

enum ChatStatus {
  ONLINE,
  QUIET,
  AWAY,
}

enum OperatingSystem {
  WINDOWS,
  LINUX,
  MAC,
}

struct PlayerStatus {
  1:ChatStatus chatStatus,
  2:OperatingSystem operatingSystem,
}

struct Player
{
  1:string _id,
  2:string name,
  3:string ipAddress,
  //4:i32 port = 6805,
  5:bool loggedIn = true,
  6:bool moderator = false,
  7:string inGame = "",
  8:bool portsOpen = false,
  //9:string emailAddress = "",
  //10:string status,
  11:PlayerStatus status,
  12:i32 basePort = 6805,
  13:i32 secondaryPort = 6806,
}

struct PlayerRomProfile
{
  1:string romId,
  2:i32 stars = 0,
  3:string note
}

struct PlayerProfile
{
  1:string _id,
  2:map<string, PlayerRomProfile> romProfiles = {};
}

struct Game
{
  1:string _id,
  10:string system,
  2:string rom,
  4:bool locked = false,
  5:i64 startTime,
  6:i64 endTime = 0,
  7:string hostPlayerId,
  8:string hostPlayerIpAddress,
  9:i32 hostPlayerPort,
}

struct ArchivedGame
{
  1:string _id,
  2:Game game
}


struct Message
{
  1:i64 timestamp,
  2:string sourceId,
  3:string chat,
  7:Player playerChanged,
  8:Game gameChanged,
}

struct RomPointer {
  1:string system = "",
  2:string rom
}

struct FileRequest
{
  1:string requestRom,
  2:string requestSystem,
  3:i64 byteOffset = 0,
  4:i32 chunkSize = 16384,
  5:i32 fileIndex = 0,
}

enum FileResponseCode
{
  OK,
  EOF,
  ERROR,
}

struct PeerFileInfo
{
  1:string filename = "",
  2:i64 length = 0,
}

struct FileResponse
{
  1:string dataHex,
  2:FileResponseCode code,
}

struct ServerState
{
  1:map<string,Player> loggedInPlayers,
  2:map<string,Game> games,
}

struct PeerState
{
  1:bool canConnect = false,
  2:i32 ping = -1,
  3:map<string, set<string>> downloadableRoms = {},
  4:i64 lastCheckTime = 0,
}

struct SystemRomPair
{
  1:string system,
  2:string rom,
}

struct RomHashEntryValue
{
  1:string filename,
  2:string location,
  3:string system,
}

struct PlayerFeedback
{
  1:string _id,
  2:string comment,
  3:string log,
  4:string playerId,
}

struct DownloadableRomState
{
  1:map<string, set<string>> roms = {},
  2:bool stale = false
}

struct IpRangeData
{
  1:i64 ipStart,
  2:i64 ipEnd,
  3:string countryCode2,
  4:string countryCode3,
  5:string countryName,
}

struct CommitAuthor {
  1:string name,
  2:string email,
  3:string date,
}

struct Commit
{
  1:string message,
  2:CommitAuthor author,
}

struct CommitData
{
  1:Commit commit,
}

