
/*
 * Class to represent a connection to a server, and send/receive
 * to/from that server. Used by the Middleware to connect to the
 * 4 different ResourceManager servers.
 */
public class TCPServerConnection {
    Socket serverSocket;
    PrintWriter out;
    BufferedReader in;
    
    public TCPServerConnection(String serverName, int serverPort) {
        serverSocket = new Socket(serverName, serverPort);
        out = new PrintWriter(serverSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
    }

}
