package Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;
import Server.Common.*;

/** 
 * Simplified version from CSE 593 Univ. of Washington
 *
 * Distributed  System in Java.
 * 
 * failure reporting is done using two pieces, exceptions and boolean 
 * return values.  Exceptions are used for systemy things. Return
 * values are used for operations that would affect the consistency
 * 
 * If there is a boolean return value and you're not sure how it 
 * would be used in your implementation, ignore it.  I used boolean
 * return values in the interface generously to allow flexibility in 
 * implementation.  But don't forget to return true when the operation
 * has succeeded.
 */

public interface IResourceManager extends Remote 
{
    /**
     * Add seats to a flight.
     *
     * In general this will be used to create a new
     * flight, but it should be possible to add seats to an existing flight.
     * Adding to an existing flight should overwrite the current price of the
     * available seats.
     *
     * @return Success
     */
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) 
	throws RemoteException; 
    
    /**
     * Add car at a location.
     *
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addCars(int id, String location, int numCars, int price) 
	throws RemoteException; 
   
    /**
     * Add room at a location.
     *
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addRooms(int id, String location, int numRooms, int price) 
	throws RemoteException; 			    
			    
    /**
     * Add customer.
     *
     * @return Unique customer identifier
     */
    public int newCustomer(int id) 
	throws RemoteException; 
    
    /**
     * Add customer with id.
     *
     * @return Success
     */
    public boolean newCustomer(int id, int cid)
        throws RemoteException;

    /**
     * Delete the flight.
     *
     * deleteFlight implies whole deletion of the flight. If there is a
     * reservation on the flight, then the flight cannot be deleted
     *
     * @return Success
     */   
    public boolean deleteFlight(int id, int flightNum) 
	throws RemoteException; 
    
    /**
     * Delete all cars at a location.
     *
     * It may not succeed if there are reservations for this location
     *
     * @return Success
     */		    
    public boolean deleteCars(int id, String location) 
	throws RemoteException; 

    /**
     * Delete all rooms at a location.
     *
     * It may not succeed if there are reservations for this location.
     *
     * @return Success
     */
    public boolean deleteRooms(int id, String location) 
	throws RemoteException; 
    
    /**
     * Delete a customer and associated reservations.
     *
     * @return Success
     */
    public boolean deleteCustomer(int id, int customerID) 
	throws RemoteException; 

    /**
     * Query the status of a flight.
     *
     * @return Number of empty seats
     */
    public int queryFlight(int id, int flightNumber) 
	throws RemoteException; 

    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public int queryCars(int id, String location) 
	throws RemoteException; 

    /**
     * Query the status of a room location.
     *
     * @return Number of available rooms at this location
     */
    public int queryRooms(int id, String location) 
	throws RemoteException; 

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public String queryCustomerInfo(int id, int customerID) 
	throws RemoteException; 
    
    /**
     * Query the status of a flight.
     *
     * @return Price of a seat in this flight
     */
    public int queryFlightPrice(int id, int flightNumber) 
	throws RemoteException; 

    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public int queryCarsPrice(int id, String location) 
	throws RemoteException; 

    /**
     * Query the status of a room location.
     *
     * @return Price of a room
     */
    public int queryRoomsPrice(int id, String location) 
	throws RemoteException; 

    /**
     * Reserve a seat on this flight.
     *
     * @return Success
     */
    public boolean reserveFlight(int id, int customerID, int flightNumber) 
	throws RemoteException; 

    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public boolean reserveCar(int id, int customerID, String location) 
	throws RemoteException; 

    /**
     * Reserve a room at this location.
     *
     * @return Success
     */
    public boolean reserveRoom(int id, int customerID, String location) 
	throws RemoteException; 

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
	throws RemoteException; 

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */

    public Integer reserveFlight_FlightRM(int xid, int flightNum, int toReserve) throws RemoteException;

	// Function to reserve flights (multiple) in FlightResourceManager
	public ArrayList<Integer> reserveFlights_FlightRM(int xid, ArrayList<Integer> flightNums, int toReserve);

	// Function to reserve car in CarResourceManager (this returns an integer value as updating in the customer resource manager requires latest reserved price of item)
	public Integer reserveCar_CarRM(int xid, String location, int toReserve);

	// Function to reserve room in RoomResourceManager (this returns an integer value as updating in the customer resource manager requires latest reserved price of item)
	public Integer reserveRoom_RoomRM(int xid, String location, int toReserve);

	// Function to reserve flight in CustomerResourceManager (this returns an integer value as updating in the customer resource manager requires latest reserved price of item)
	public boolean reserveFlight_CustomerRM(int xid, int customerID, int flightNum, int price) throws RemoteException;

	// Function to reserve flights (multiple) in CustomerResourceManager 
	public boolean reserveFlights_CustomerRM(int xid, int customerID, ArrayList<Integer> flightNums, ArrayList<Integer> prices);

	// Function to reserve car in CustomerResourceManager
	public boolean reserveCar_CustomerRM(int xid, int customerID, String location, int price) throws RemoteException;

	// Function to reserve room in CustomerResourceManager
	public boolean reserveRoom_CustomerRM(int xid, int customerID, String location, int price) throws RemoteException;

	// Function to reserve item in CustomerResourceManager
	public boolean reserveItem_CustomerRM(int xid, int customerID, String key, String location, int price);

	// Function to delete customer in customer database
	public ArrayList<ReservedItem> deleteCustomer_CustomerRM(int xid, int customerID) throws RemoteException;

	// Function to reserve bundle (TCP)
	public boolean bundle(
		int xid, 
		int customerID, 
		Vector<String> flightNumbers, 
		ArrayList<Integer> flightPrices, 
		String location, boolean car, 
		Integer carPrice, 
		boolean room, 
        Integer roomPrice) throws RemoteException;
    
    public String getName()
        throws RemoteException;
}
