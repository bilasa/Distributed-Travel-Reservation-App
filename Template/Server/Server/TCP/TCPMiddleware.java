package Server.TCP;

import Server.Interface.*;
import Server.Common.*;

import java.net;

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
            
            while(true) {
                Socket clientSocket = middlewareSocket.accept(); // accept a new request from a client
                Thread reqThread = new RequestThread(clientSocket); // create a thread to service that request
            }

        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /*
     * Services a request from a client
     */
    public class RequestThread implements Runnable {
        Socket clientSocket;
        ObjectInputStream in; // input buffer
        ObjectOutputStream out; // output buffer
        
        public RequestThread(ServerSocket clientSocket) {
            this.clientSocket = clientSocket;
            in = new ObjectInptStream(clientSocket.getInputStream()));
            out = new ObjectOutputStream(clientSocket.getOutputStream());
        }
        
        @Override
        public void run() {

            // Get action object from the input stream
            TravelAction travelAction = (TravelAction)in.readObject();
                
            // Send the action to the appropriate ResourceManager server
            try {
                
                switch (travelAction.getActionType()) {
                        
                    case ACTION_TYPE.RESERVE_ACTION:
                        int xid = (ReserveFlightAction)travelAction.getXid();
                        int customerID = (ReserveFlightAction)travelAction.getCustomerID();
                        int flightNumber = (ReserveFlightAction)travelAction.getFlightNumber();
                        
                        // *** TO-DO ***
                        UpdateFlightAction updateFlightAction = new UpdateFlightAction(...);
                        UpdateCustomerAction updateCustomerAction = new UpdateCustomerAction(...);
                        
                        TCPServerConnection flightServer = new TCPServerConnection(out, flightServerName, s_serverPort);
                        flightServer.send(updateFlightAction); // send the action to the flight server
                        
                        TCPServerConnection customerServer = new TCPServerConnection(out, customerServerName, s_serverPort);
                        customerServer.send(updateCustomerAction); // send the action to the customer server
                        break;
                        
                    case ACTION_TYPE.FLIGHT_ACTION:
                        TCPServerConnection flightServer = new TCPServerConnection(out, flightServerName, s_serverPort);
                        flightServer.send(travelAction); // send the action to the flight server
                        break;
                        
                    case ACTION_TYPE.CAR_ACTION:
                        TCPServerConnection carServer = new TCPServerConnection(out, carServerName, s_serverPort);
                        carServer.send(travelAction); // send the action to the car server
                        break;
                        
                    case ACTION_TYPE.ROOM_ACTION:
                        TCPServerConnection roomServer = new TCPServerConnection(out, roomServerName, s_serverPort);
                        roomServer.send(travelAction); // send the action to the room server
                        break;
                        
                    case ACTION_TYPE.CUSTOMER_ACTION:
                        TCPServerConnection customerServer = new TCPServerConnection(out, customerServerName, s_serverPort);
                        customerServer.send(travelAction); // send the action to the customer server
                        break;
                        
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            
            // Close the streams
            try {
                in.close();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        
    }

    public TCPMiddleware(String name)
    {
        super(name);
    }
    
}

