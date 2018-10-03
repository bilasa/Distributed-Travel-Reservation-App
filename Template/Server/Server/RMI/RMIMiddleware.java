package Server.RMI;

import Server.Interface.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import Server.Common.*;

import java.util.*;
import java.io.*;

public class RMIMiddleware extends Middleware
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
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }
        
        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }
        
        // Get a reference to the RMIRegister
        try {
            RMIMiddleware middleware = new RMIMiddleware(s_serverName);
            middleware.connectServers();
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public RMIMiddleware(String name)
    {
        super(name);
    }
    
    // Connects the Middleware to all 4 ResourceManager servers
    public void connectServers()
    {
        connectServer(s_serverHost, s_serverPort, flightServerName);
        connectServer(s_serverHost, s_serverPort, carServerName);
        connectServer(s_serverHost, s_serverPort, roomServerName);
        connectServer(s_serverHost, s_serverPort, customerServerName);
    }
    
    public void connectServer(String server, int port, String name)
    {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);
                    
                    // Assign the remote interface corresponding to the server name
                    if (name == flightServerName) 
                    {
                        flightResourceManager = (RMIResourceManager) registry.lookup(s_rmiPrefix + name);
                    } 
                    else if (name == carServerName) 
                    {
                        carResourceManager = (RMIResourceManager) registry.lookup(s_rmiPrefix + name);
                    } 
                    else if (name == roomServerName) 
                    {
                        roomResourceManager = (RMIResourceManager) registry.lookup(s_rmiPrefix + name);
                    } 
                    else if (name == customerServerName) 
                    {
                            customerResourceManager = (RMIResourceManager) registry.lookup(s_rmiPrefix + name);
                    }
                    
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                    break;
                }
                catch (NotBoundException|RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

