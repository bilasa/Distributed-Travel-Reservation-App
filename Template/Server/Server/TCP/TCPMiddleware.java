package Server.TCP;

import java.util.*;
import java.io.*;
import java.net.*;
import Server.Interface.*;
import Server.Common.*;
import Server.Actions.*;

// TODO

/*
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
                RequestThread reqThread = new RequestThread(clientSocket); // create a thread to service that request
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
  
    public static class RequestThread implements Runnable {
        Socket clientSocket;
        ObjectInputStream in; // input stream for client request
        ObjectOutputStream out; // output stream to client
        
        public RequestThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
        }
        
        @Override
        public void run() {

            TravelAction travelAction = (TravelAction)in.readObject(); // Get action object from the input stream
            TravelAction reserveAction = null, reserveAction_ = null;// extra actions for reserve actions
            
            Socket socket = null, socket_ = null;
            
            ObjectInputStream inRM, inRM_; // input stream from ResourceManager (extra one for reserve actions)
            ObjectOutputStream outRM, outRM_; // output stream to ResourceManager (extra one for reserve actions)
            
            boolean flag = false; // is the action a reserve action
                
            // Send the action to the appropriate ResourceManager server
            try {
                
                switch (travelAction.getType()) {
                        
                    case RESERVE_ACTION:
                        
                        /*switch (travelAction.getActionSubtype()) {
                            int xid = travelAction.getXid();
                            
                            case RESERVE_FLIGHT:
                                int customerID = (ReserveFlightAction)travelAction.getCustomerID();
                                int flightNumber = (ReserveFlightAction)travelAction.getFlightNumber();
                                
                                // *** TO-DO ***
                                reserveAction = new UpdateFlightAction(...);
                                serverSocket = new Socket(flightServerName, s_serverPort); // create a socket to the flight server
                            
                                break;
                                
                            case RESERVE_CAR:
                                int customerID = (ReserveCarAction)travelAction.getCustomerID();
                                String location = (ReserveCarAction)travelAction.getLocation();
                                
                                // *** TO-DO ***
                                reserveAction = new UpdateCarAction(...);
                                serverSocket = new Socket(carServerName, s_serverPort); // create a socket to the car server
                                
                                break;
                                
                            case RESERVE_ROOM:
                                int customerID = (ReserveRoomAction)travelAction.getCustomerID();
                                String location = (ReserveRoomAction)travelAction.getLocation();
                                
                                // *** TO-DO ***
                                reserveAction = new UpdateRoomAction(...);
                                serverSocket = new Socket(carServerName, s_serverPort); // create a socket to the room server
                                
                                break;
                                
                            case RESERVE_BUNDLE:
                                //int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room
                                int customerID = (ReserveBundleAction)travelAction.getCustomerID();
                                Vector<String> flightNumbers = (ReserveBundleAction)travelAction.getFlightNumbers();
                                String location = (ReserveBundleAction)travelAction.getLocation();
                                boolean car = (ReserveBundleAction)travelAction.getCar();
                                boolean room = (ReserveBundleAction)travelAction.getRoom();
                                
                                // *** TO-DO ***
                                
                                break;
                                
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                
                        // Customer action/socket for all reserve actions
                        reserveAction_ = new UpdateCustomerAction(...));
                        serverSocket_ = new Socket(customerServerName, s_serverPort);
                        
                        break;
                        
                    case FLIGHT_ACTION:
                        socket = new Socket(flightServerName, s_serverPort); // create a socket to the flight server
                        break;
                        
                    case CAR_ACTION:
                        socket = new Socket(carServerName, s_serverPort); // create a socket to the car server
                        break;
                        
                    case ROOM_ACTION:
                        socket = new Socket(roomServerName, s_serverPort); // create a socket to the room server
                        break;
                        
                    case CUSTOMER_ACTION:
                        socket = new Socket(customerServerName, s_serverPort); // create a socket to the customer server
                        break;
                        
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            
                // Create input stream and output stream from the server socket
                inRM = new ObjectInputStream(socket.getInputStream());
                outRM = new ObjectOutputStream(socket.getOutputStream());
                
                // Send the action to the server
                outRM.writeObject(travelAction);
                outRM.flush();
                
                // Send the response to the client
                Object response = inRM.readObject();
                if (response != null) {
                    out.writeObject(response);
                } else {
                    out.writeObject(new String("NULL"));
                }
                
                // Close the RM streams
                try {
                    inRM.close();
                    outRM.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                
                // Close the client streams
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

*/

