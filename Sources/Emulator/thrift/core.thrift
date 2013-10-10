namespace java com.github.mistertea.webcomic.thrift

struct PlayerStatus
{
  1:string name,
}

struct Status
{
  1:list<PlayerStatus> playerStatus = []
}

