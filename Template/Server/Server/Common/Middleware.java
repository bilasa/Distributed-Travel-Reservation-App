 // -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;
import Server.RMI.*;

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
    public RMIResourceManager flightResourceManager = null;
    public RMIResourceManager carResourceManager = null;
    public RMIResourceManager roomResourceManager = null;
    public RMIResourceManager customerResourceManager = null;
    
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
        ArrayList<ReservedItem> items = customerResourceManager.deleteCustomer_CustomerRM(xid, customerID);

        for (ReservedItem item : items) 
        {
            String key = item.getKey();
            String[] parts = key.split("-");
            int count = item.getCount();

            if (parts[0].equals("flight"))
            {
                flightResourceManager.reserveFlight_FlightRM(xid, Integer.parseInt(parts[1]), -count);
            }

            if (parts[0].equals("car"))
            {
                carResourceManager.reserveCar_CarRM(xid, parts[1], -count);
            }

            if (parts[0].equals("room"))
            {
                roomResourceManager.reserveRoom_RoomRM(xid, parts[1], -count);
            }
        }

        return true;
    }
    
    // Adds flight reservation to this customer
    public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException
    {
        // Reserve a seat in the flight and get the price for the flight
        Integer flightPrice = flightResourceManager.reserveFlight_FlightRM(xid, flightNum, 1).intValue();
    
        if ((int) flightPrice == -1) 
        {
            return false; // flight reservation failed
        } 
        else {
            return customerResourceManager.reserveFlight_CustomerRM(xid, customerID, flightNum, flightPrice);
        }
    }
    
    // Adds car reservation to this customer
    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException
    {
        // Reserve a car and get its price
        Integer carPrice = carResourceManager.reserveCar_CarRM(xid, location, 1).intValue();
        
        if ((int) carPrice == -1) 
        {
            return false; // car reservation failed
        } 
        else {
            return customerResourceManager.reserveCar_CustomerRM(xid, customerID, location, carPrice);
        }
    }
    
    // Adds room reservation to this customer
    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException
    {
        // Reserve a room and get its price
        Integer roomPrice = roomResourceManager.reserveRoom_RoomRM(xid, location, 1).intValue();
        
        if ((int) roomPrice == -1) 
        {
            return false; // room reservation failed
        } 
        else {
            return customerResourceManager.reserveRoom_CustomerRM(xid, customerID, location, roomPrice);
        }
    }
    
    // Reserve bundle
    public boolean bundle(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException
    {   
        ArrayList<Integer> prices = new ArrayList<Integer>();
        int carPrice = -1;
        int roomPrice = -1;
        boolean customer = true;

        // Convert flight numbers from string format to integer format
        ArrayList<Integer> flights  = new ArrayList<Integer>();
        for (String f : flightNumbers) flights.add(Integer.parseInt(f));

        // Validate 
        prices = flightResourceManager.reserveFlights_FlightRM(xid, flights, 1);
        if (car) carPrice = carResourceManager.reserveCar_CarRM(xid, location, 1);
        if (room) roomPrice = roomResourceManager.reserveRoom_RoomRM(xid, location, 1);
        customer = !(customerResourceManager.queryCustomerInfo(xid, customerID).isEmpty());

        // Invalid cases
        if (
            (prices.size() != flightNumbers.size()) ||
            (car && carPrice == -1) || 
            (room && roomPrice == -1) || 
            (customer == false)
        ) { 
            if (prices.size() == flightNumbers.size()) flightResourceManager.reserveFlights_FlightRM(xid, flights, -1);
            if (car && carPrice != -1) carResourceManager.reserveCar_CarRM(xid, location, -1);
            if (room && roomPrice != -1) roomResourceManager.reserveRoom_RoomRM(xid, location, -1);

            return false;
        }
        
        // Reserve items for customer
        customerResourceManager.reserveFlights_CustomerRM(xid, customerID, flights, prices);
        if (car) customerResourceManager.reserveCar_CustomerRM(xid, customerID, location, carPrice);
        if (room) customerResourceManager.reserveRoom_CustomerRM(xid, customerID, location, roomPrice);

        return true; 
    }
    
    public String getName() throws RemoteException
    {
        return m_name;
    }
}

