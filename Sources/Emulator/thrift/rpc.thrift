include 'core.thrift'

namespace java com.github.mistertea.webcomic.rpc

service MameHubRpc
{
  core.Status getStatus(),
}
