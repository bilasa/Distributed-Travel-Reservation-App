package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;

public class RMIMiddleware extends Middleware
{
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 2133;
    private static int s_serverPort2 = 2176;
    
    // Server name
    private static String s_serverName = "RMIMiddleware";
    
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
            flightServerName = args[0];
        }
        if (args.length > 1)
        {
            carServerName = args[1];
        }
        if (args.length > 2)
        {
            roomServerName = args[2];
        }
        if (args.length > 3)
        {
            customerServerName = args[3];
        }

        if (args.length > 4)
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
           
            IResourceManager stub = (IResourceManager) UnicastRemoteObject.exportObject(middleware, s_serverPort2);

            // Bind the remote object's stub in the registry
			Registry l_registry;

			try {
				l_registry = LocateRegistry.createRegistry(s_serverPort2);
			} 
			catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(s_serverPort2);
            }
            
            final Registry registry = l_registry;
			registry.rebind(s_rmiPrefix + s_serverName, stub);

            Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_serverName);
						System.out.println("'" + s_serverName + "' resource manager unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
            });
            
            System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");

            middleware.connectServers();
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

        // Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
    }
    
    public RMIMiddleware(String name)
    {
        super(name);
    }
    
    // Connects the Middleware to all 4 ResourceManager servers
    public void connectServers()
    {   
        connectServer("Flights", 2138, flightServerName);
        connectServer("Cars", 2134, carServerName);
        connectServer("Rooms", 2135, roomServerName);
        connectServer("Customers", 2136, customerServerName);
    }
    
    public void connectServer(String server, int port, String host) // middleware name, port, RM hostname
    {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(host, port);
                    
                    // Assign the remote interface corresponding to the server name
                    if (server.equals("Flights")) 
                    {
                        flightResourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + server);
                    } 
                    else if (server.equals("Cars")) 
                    {
                        carResourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + server);
                    } 
                    else if (server.equals("Rooms")) 
                    {
                        roomResourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + server);
                    } 
                    else if (server.equals("Customers")) 
                    {
                            customerResourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + server);
                    }
                    
                    System.out.println("Connected to '" + server + "' server [" + host + ":" + port + "/" + s_rmiPrefix + server + "]");
                    break;
                }
                catch (NotBoundException|RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + server + "' server [" + server + ":" + port + "/" + s_rmiPrefix + server + "]");
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

