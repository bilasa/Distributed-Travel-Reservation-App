// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIResourceManager extends ResourceManager 
{
	private static String s_serverName = "Server";

	private static String s_rmiPrefix = "group32";

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverName = args[0];
		}
			
		// Create the RMI server entry
		try {
			// Create a new Server object
			RMIResourceManager server = new RMIResourceManager(s_serverName);

			// Dynamically generate the stub (client proxy)
			IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(server, 0);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(1099);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(1099);
			}
			final Registry registry = l_registry;
			registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

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
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
	}
    
    /*
     * Check if a flight exists. If it does and seats are available decrement the seats
     */
    /*public boolean updateFlight(int flightNum) {
        Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
        if (curObj == null) {
            return false;
        } else {
            int numSeats = curObj.getCount();
            if (numSeats <= 0) {
                return false
            }
        }
        
        curObj.setCount(numSeats - 1);
        
        return true;
    }
    
    /*
     * Check if a flight exists. If it does and seats are available decrement the seats
     */
    /*public boolean updateCar(int flightNum) {
        Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
        if (curObj == null) {
            return false;
        } else {
            int numSeats = curObj.getCount();
            if (numSeats <= 0) {
                return false
            }
        }
        
        curObj.setCount(numSeats - 1);
        
        return true;
    }*/
    
    /*
     * Check if a flight exists. If it does and seats are available decrement the seats
     */
    /*public boolean updateRoom(int flightNum) {
        Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
        if (curObj == null) {
            return false;
        } else {
            int numSeats = curObj.getCount();
            if (numSeats <= 0) {
                return false
            }
        }
        
        curObj.setCount(numSeats - 1);
        
        return true;
    }*/

	public RMIResourceManager(String name)
	{
		super(name);
	}
}
