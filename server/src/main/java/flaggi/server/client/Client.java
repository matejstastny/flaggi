package flaggi.server.client;

import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.server.Server;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ProtoUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/** Client data structure class. */
public class Client {

  private final String uuid;
  private final String name;
  private final int udpPort;
  private final Socket socket;
  private final OutputStream out;
  private final InetAddress address;

  // Constructor --------------------------------------------------------------

  public Client(String uuid, String name, Socket socket, OutputStream out, int udpPort) {
    this.address = socket.getInetAddress();
    this.udpPort = udpPort;
    this.socket = socket;
    this.uuid = uuid;
    this.name = name;
    this.out = out;
  }

  // Accesors -----------------------------------------------------------------

  public String uuid() {
    return uuid;
  }

  public String username() {
    return name;
  }

  public Socket socket() {
    return socket;
  }

  public OutputStream outputStream() {
    return out;
  }

  public InetAddress address() {
    return address;
  }

  public int udpPort() {
    return udpPort;
  }

  // Util ---------------------------------------------------------------------

  public void sendMessage(ServerMessage message) {
    try {
      ProtoUtil.sendServerMessage(out, message);
    } catch (IOException e) {
      Logger.log(LogLevel.ERR, "Failed to send message to client " + uuid, e);
      Server.handleFatalError();
    }
  }
}
