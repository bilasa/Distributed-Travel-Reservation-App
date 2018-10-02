// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import java.util.*;
import Server.Interface.*;
import Server.Common.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.NotBoundException;

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
			IResourceManager resourceManager = (IResourceManager) UnicastRemoteObject.exportObject(server, 0);

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

	/* NOTE: The following functions are to support the Client-Middleware-RMs design */

	// Function to reserve flight in FlightResourceManager
	public Integer reserveFlight_FlightRM(int xid, int flightNum, int toReserve) throws RemoteException
	{	
		Trace.info("RM::updateFlight(" + xid + ", " + flightNum + ") called");
		
		// Retrieve flight
		Flight curObj = (Flight) readData(xid, Flight.getKey(flightNum));

		if (curObj == null) return new Integer(-1);

		// Count and reservations
		int nCount = curObj.getCount() - toReserve;
		int nReserved = curObj.getReserved() + toReserve;

		if (nCount < 0 || nReserved < 0) return new Integer(-1);

		// Update 
		curObj.setCount(nCount);
		curObj.setReserved(nReserved);
		writeData(xid, curObj.getKey(), curObj);

		return new Integer(curObj.getPrice());
	}

	// Function to reserve car in CarResourceManager (this returns an integer value as updating in the customer resource manager requires latest reserved price of item)
	public Integer reserveCar_CarRM(int xid, String location, int toReserve)
	{	
		Trace.info("RM::updateCars(" + xid + ", " + location + ") called");

		// Retrieve car
		Car curObj = (Car) readData(xid, Car.getKey(location));

		if (curObj == null) return new Integer(-1);

		// Count and reservations
		int nCount = curObj.getCount() - toReserve;
		int nReserved = curObj.getReserved() + toReserve;

		if (nCount < 0 || nReserved < 0) return new Integer(-1);

		// Update 
		curObj.setCount(nCount);
		curObj.setReserved(nReserved);
		writeData(xid, curObj.getKey(), curObj);
		
		return new Integer(curObj.getPrice());
	}

	// Function to reserve room in RoomResourceManager (this returns an integer value as updating in the customer resource manager requires latest reserved price of item)
	public Integer reserveRoom_RoomRM(int xid, String location, int toReserve)
	{
		Trace.info("RM::updateRooms(" + xid + ", " + location + ") called");

		// Reserve room
		Room curObj = (Room) readData(xid, Room.getKey(location));

		if (curObj == null) return new Integer(-1);

		// Count and reservations
		int nCount = curObj.getCount() - toReserve;
		int nReserved = curObj.getReserved() + toReserve;

		if (nCount < 0 || nReserved < 0) return new Integer(-1);

		// Update 
		curObj.setCount(nCount);
		curObj.setReserved(nReserved);
		writeData(xid, curObj.getKey(), curObj);

		return new Integer(curObj.getPrice());
	}

	// Function to reserve flight in CustomerResourceManager (this returns an integer value as updating in the customer resource manager requires latest reserved price of item)
	public boolean reserveFlight_CustomerRM(int xid, int customerID, int flightNum, int price) throws RemoteException
	{
		return reserveItem_CustomerRM(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum), price);
	}

	// Function to reserve car in CustomerResourceManager
	public boolean reserveCar_CustomerRM(int xid, int customerID, String location, int price) throws RemoteException
	{
		return reserveItem_CustomerRM(xid, customerID, Car.getKey(location), location, price);
	}

	// Function to reserve room in CustomerResourceManager
	public boolean reserveRoom_CustomerRM(int xid, int customerID, String location, int price) throws RemoteException
	{
		return reserveItem_CustomerRM(xid, customerID, Room.getKey(location), location, price);
	}

	// Function to reserve item in CustomerResourceManager
	protected boolean reserveItem_CustomerRM(int xid, int customerID, String key, String location, int price)
	{
		Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );   
		
		// Retrieve customer
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));

		if (customer == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		} 

		// Update customer
		customer.reserve(key, location, price);        
		writeData(xid, customer.getKey(), customer);

		Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
		return true;
	}

	public ArrayList<ReservedItem> deleteCustomer_(int xid, int customerID) throws RemoteException 
	{	
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");

		// Retrieve customer 
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));

		if (customer == null)
		{
			Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			return new ArrayList<ReservedItem>();
		}

		ArrayList<ReservedItem> res = new ArrayList<ReservedItem>();
		RMHashMap reservations = customer.getReservations();
		
		for (String reservedKey : reservations.keySet()) {
			ReservedItem item = customer.getReservedItem(reservedKey);
			res.add(item);
		}

		// Remove customer from storage
		removeData(xid, customer.getKey());
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");

		return res;
	}
    
	public RMIResourceManager(String name)
	{
		super(name);
	}
}
