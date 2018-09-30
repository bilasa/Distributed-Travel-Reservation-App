
package Server.RMI;

import Server.Interface.*;

import java.net;

import Server.Common.*;

import java.util.*;
import java.io.*;

public class TCPMiddleware extends Middleware
{
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 1099;
    
    // Server name
    private static String s_serverName = "Middleware";
    
    // Server names for 4 different ResourceManagers
    private static String flightServerName = "flightServer";
    private static String carServerName = "carServer";
    private static String roomServerName = "roomServer";
    private static String customerServerName = "customerServer";
    
    private static String s_rmiPrefix = "group32";
    
    public static void main(String args[])
    {
        // Set the server host and server names based on the arguments
        if (args.length > 0)
        {
            s_serverHost = args[0];
        }
        if (args.length > 1)
        {
            flightServerName = args[1];
        }
        if (args.length > 2)
        {
            carServerName = args[2];
        }
        if (args.length > 3)
        {
            roomServerName = args[3];
        }
        if (args.length > 4)
        {
            customerServerName = args[4];
        }
        if (args.length > 5)
        {
            //System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }
        
        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }
        
        try {
            ServerSocket middlewareSocket = new ServerSocket(s_serverPort);
            
            // Connect to the ResourceManagers
            TCPServerConnection flightServer = new TCPServerConnection(flightServerName, s_serverPort);
            TCPServerConnection carServer = new TCPServerConnection(carServerName, s_serverPort);
            TCPServerConnection roomServer = new TCPServerConnection(roomServerName, s_serverPort);
            TCPServerConnection customerServer = new TCPServerConnection(customerServerName, s_serverPort);
            
            // ExecutorService to execute the client threads
            ExecutorService executor = Executors.newCachedThreadPool();
            
            // Connect to the clients
            while(true) {
                // Accept a connection from a client, create a thread for that client
                Socket clientSocket = middlewareSocket.accept();
                executors.execute(new ClientThread(clientSocket));
            }

        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    // Services the requests for each unique client (one thread per client)
    public class ClientThread implements Runnable {
        Socket clientSocket;
        PrintWriter out; // output buffer
        BufferedReader in; // input buffer
        
        public ClientThread(ServerSocket clientSocket) {
            this.clientSocket = clientSocket;
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }
        
        @Override
        public void run() {
            // Service the client's requests
            
            String inputLine, outputLine;
            
            // Optionally send initial message to client to confirm the connection
            out.println("Successfully connected to server.");
            
            while((inputLine = in.readLine()) != null) {
                // *** TO-DO ***
                // Get action class from the input buffer
                // Check the action type
                // Send the action to the appropriate ResourceManager
                // Send the response from the ResourceManager to the output
            }
        }
        
    }

    public TCPMiddleware(String name)
    {
        super(name);
    }
    
}

