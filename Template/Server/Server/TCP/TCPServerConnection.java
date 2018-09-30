
/*
 * Class to represent the connection a specific Client has to
 * a specific ResourceManager
 */
public class TCPServerConnection {
    Socket serverSocket;
    ObjectInputStream inRM; // input stream from ResourceManager
    ObjectOutputStream outRM; // output stream to ResourceManager
    ObjectOutputStream outClient; // output stream used to relay responses from the ResourceManager to the Client

    public TCPServerConnection(ObjectOutputStream outClient, String serverName, int serverPort) {
        serverSocket = new Socket(serverName, serverPort);
        inRM = new ObjectInputStream(serverSocket.getInputStream());
        outRM = new ObjectOutputStream(serverSocket.getOutputStream());
        this.outClient = outClient;
    }
    
    /*
     * Send a travel action to the ResourceManager
     */
    public void sendAction(TravelAction travelAction) {
        outRM.writeObject(travelAction);
        outRM.flush();
        
        outClient.writeObject(inRM.readObject()); // relay the object to the Client
    }

}
