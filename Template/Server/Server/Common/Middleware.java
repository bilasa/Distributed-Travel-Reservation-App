// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;

import java.util.*;
import java.rmi.RemoteException;
import java.rmi.ConnectException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import java.io.*;

/*
 * Middleware acts as intermediary server between the Client and the 3 different
 * ResourceManagers (flight, car, and room). It also communicates with an additional
 * ResourceManager server for customers.
 */
public abstract class Middleware implements IResourceManager
{
    protected String m_name = "";
    //protected RMHashMap m_data = new RMHashMap();
    
    // ResourceManager remote interfaces (made public to access outside of package)
    public IResourceManager flightResourceManager = null;
    public IResourceManager carResourceManager = null;
    public IResourceManager roomResourceManager = null;
    public IResourceManager customerResourceManager = null;
    
    public Middleware(String p_name)
    {
        m_name = p_name;
    }
    
    // Create a new flight, or add seats to existing flight
    // NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException
    {
        return flightResourceManager.addFlight(xid, flightNum, flightSeats, flightPrice);
    }
    
    // Create a new car location or add cars to an existing location
    // NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int xid, String location, int count, int price) throws RemoteException
    {
        return carResourceManager.addCars(xid, location, count, price);
    }
    
    // Create a new room location or add rooms to an existing location
    // NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int xid, String location, int count, int price) throws RemoteException
    {
        return roomResourceManager.addRooms(xid, location, count, price);
    }
    
    // Deletes flight
    public boolean deleteFlight(int xid, int flightNum) throws RemoteException
    {
        return flightResourceManager.deleteFlight(xid, flightNum);
    }
    
    // Delete cars at a location
    public boolean deleteCars(int xid, String location) throws RemoteException
    {
        return carResourceManager.deleteCars(xid, location);
    }
    
    // Delete rooms at a location
    public boolean deleteRooms(int xid, String location) throws RemoteException
    {
        return roomResourceManager.deleteRooms(xid, location);
    }
    
    // Returns the number of empty seats in this flight
    public int queryFlight(int xid, int flightNum) throws RemoteException
    {
        return flightResourceManager.queryFlight(xid, flightNum);
    }
    
    // Returns the number of cars available at a location
    public int queryCars(int xid, String location) throws RemoteException
    {
        return carResourceManager.queryCars(xid, location);
    }
    
    // Returns the amount of rooms available at a location
    public int queryRooms(int xid, String location) throws RemoteException
    {
        return roomResourceManager.queryRooms(xid, location);
    }
    
    // Returns price of a seat in this flight
    public int queryFlightPrice(int xid, int flightNum) throws RemoteException
    {
        return flightResourceManager.queryFlightPrice(xid, flightNum);
    }
    
    // Returns price of cars at this location
    public int queryCarsPrice(int xid, String location) throws RemoteException
    {
        return carResourceManager.queryCarsPrice(xid, location);
    }
    
    // Returns room price at this location
    public int queryRoomsPrice(int xid, String location) throws RemoteException
    {
        return roomResourceManager.queryRoomsPrice(xid, location);
    }
    
    public String queryCustomerInfo(int xid, int customerID) throws RemoteException
    {
        return customerResourceManager.queryCustomerInfo(xid, customerID);
    }
    
    public int newCustomer(int xid) throws RemoteException
    {
        return customerResourceManager.newCustomer(xid);
    }
    
    public boolean newCustomer(int xid, int customerID) throws RemoteException
    {
        return customerResourceManager.newCustomer(xid, customerID);
    }
    
    public boolean deleteCustomer(int xid, int customerID) throws RemoteException
    {
        return customerResourceManager.deleteCustomer(xid, customerID);
    }
    
    // Adds flight reservation to this customer
    public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException
    {
        return flightResourceManager.reserveFlight(xid, customerID, flightNum);
    }
    
    // Adds car reservation to this customer
    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException
    {
        return carResourceManager.reserveCar(xid, customerID, location);
    }
    
    // Adds room reservation to this customer
    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException
    {
        return roomResourceManager.reserveRoom(xid, customerID, location);
    }
    
    // Reserve bundle
    public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException
    {
        return false;
    }
    
    public String getName() throws RemoteException
    {
        return m_name;
    }
}

