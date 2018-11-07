 // -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;
import Server.LockManager.DeadlockException;
import Server.RMI.*;
import Server.Common.*;
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
    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.FLIGHT);
            updateTransaction(xid, rms);
            return flightResourceManager.addFlight(xid, flightNum, flightSeats, flightPrice);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            //e.printStackTrace();
            System.out.println("Exception caught: Middleware addflight catches deadlock Exception");
        }

        return false;
    }
    
    // Create a new car location or add cars to an existing location
    // NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int xid, String location, int count, int price) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CAR);
            updateTransaction(xid, rms);
            return carResourceManager.addCars(xid, location, count, price);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware addcars catches deadlock Exception"); //e.printStackTrace();
        }
        
        return false;
    }
    
    // Create a new room location or add rooms to an existing location
    // NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int xid, String location, int count, int price) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.ROOM);
            updateTransaction(xid, rms);
            return roomResourceManager.addRooms(xid, location, count, price);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware addrooms catches deadlock Exception"); //e.printStackTrace();
        }

        return false;
    }
    
    // Deletes flight
    public boolean deleteFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.FLIGHT);
            updateTransaction(xid, rms);
            return flightResourceManager.deleteFlight(xid, flightNum);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware deleteflight catches deadlock Exception"); //e.printStackTrace();
        }

        return false;
    }
    
    // Delete cars at a location
    public boolean deleteCars(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {      
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CAR);
            updateTransaction(xid, rms);
            return carResourceManager.deleteCars(xid, location);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware deletecars catches deadlock Exception"); //e.printStackTrace();
        }

        return false;
    }
    
    // Delete rooms at a location
    public boolean deleteRooms(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.ROOM);
            updateTransaction(xid, rms);
            return roomResourceManager.deleteRooms(xid, location);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware deleterooms catches deadlock Exception"); //e.printStackTrace();
        }

        return false;
    }
    
    // Returns the number of empty seats in this flight
    public int queryFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.FLIGHT);
            updateTransaction(xid, rms);
            return flightResourceManager.queryFlight(xid, flightNum);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware queryflight catches deadlock Exception"); //e.printStackTrace();
        }

        return -1;
    }
    
    // Returns the number of cars available at a location
    public int queryCars(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CAR);
            updateTransaction(xid, rms);
            return carResourceManager.queryCars(xid, location);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware querycars catches deadlock Exception"); //e.printStackTrace();
        }

        return -1;
    }
    
    // Returns the amount of rooms available at a location
    public int queryRooms(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {      
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.ROOM);
            updateTransaction(xid, rms);
            return roomResourceManager.queryRooms(xid, location);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware queryrooms catches deadlock Exception"); //e.printStackTrace();
        }

        return -1;
    }
    
    // Returns price of a seat in this flight
    public int queryFlightPrice(int xid, int flightNum) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.FLIGHT);
            updateTransaction(xid, rms);
            return flightResourceManager.queryFlightPrice(xid, flightNum);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware queryflightprice catches deadlock Exception"); //e.printStackTrace();
        }

        return -1;
    }
    
    // Returns price of cars at this location
    public int queryCarsPrice(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CAR);
            updateTransaction(xid, rms);
            return carResourceManager.queryCarsPrice(xid, location);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware querycarsprice catches deadlock Exception"); //e.printStackTrace();
        }

        return -1;
    }
    
    // Returns room price at this location
    public int queryRoomsPrice(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.ROOM);
            updateTransaction(xid, rms);
            return roomResourceManager.queryRoomsPrice(xid, location);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware queryroomsprice catches deadlock Exception"); //e.printStackTrace();
        }

        return -1;
    }
    
    public String queryCustomerInfo(int xid, int customerID) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CUSTOMER);
            updateTransaction(xid, rms);
            return customerResourceManager.queryCustomerInfo(xid, customerID);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware querycustomerinfo catches deadlock Exception"); //e.printStackTrace();
        }

        return null;
    }
    
    public int newCustomer(int xid) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CUSTOMER);
            updateTransaction(xid, rms);
            return customerResourceManager.newCustomer(xid);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware newcustomer catches deadlock Exception"); //e.printStackTrace();
        }

        return -1;
    }
    
    public boolean newCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CUSTOMER);
            updateTransaction(xid, rms);
            return customerResourceManager.newCustomer(xid, customerID);
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware newcustomer with id catches deadlock Exception"); //e.printStackTrace();
        }

        return false;
    }
    
    public boolean deleteCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CUSTOMER);

            ArrayList<ReservedItem> items = customerResourceManager.deleteCustomer_CustomerRM(xid, customerID);

            for (ReservedItem item : items) 
            {
                String key = item.getKey();
                String[] parts = key.split("-");
                int count = item.getCount();

                if (parts[0].equals("flight"))
                {   
                    rms.add(RESOURCE_MANAGER_TYPE.FLIGHT);
                    flightResourceManager.reserveFlight_FlightRM(xid, Integer.parseInt(parts[1]), -count);
                }

                if (parts[0].equals("car"))
                {   
                    rms.add(RESOURCE_MANAGER_TYPE.CAR);
                    carResourceManager.reserveCar_CarRM(xid, parts[1], -count);
                }

                if (parts[0].equals("room"))
                {   
                    rms.add(RESOURCE_MANAGER_TYPE.ROOM);
                    roomResourceManager.reserveRoom_RoomRM(xid, parts[1], -count);
                }
            }

            updateTransaction(xid, rms);
            return true;
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware deletecustomer catches deadlock Exception"); //e.printStackTrace();
        }

         return false;
    }
    
    // Adds flight reservation to this customer
    public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try { 
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CUSTOMER);
            
            // Reserve a seat in the flight and get the price for the flight
            Integer flightPrice = flightResourceManager.reserveFlight_FlightRM(xid, flightNum, 1).intValue();
        
            if ((int) flightPrice == -1) 
            {
                return false; // flight reservation failed
            } 
            else {
                rms.add(RESOURCE_MANAGER_TYPE.FLIGHT);
                updateTransaction(xid, rms);
                return customerResourceManager.reserveFlight_CustomerRM(xid, customerID, flightNum, flightPrice);
            }
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware reserveflight catches deadlock Exception"); //e.printStackTrace();
        }

         return false;
    }
    
    // Adds car reservation to this customer
    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CUSTOMER);

            // Reserve a car and get its price
            Integer carPrice = carResourceManager.reserveCar_CarRM(xid, location, 1).intValue();
            
            if ((int) carPrice == -1) 
            {
                return false; // car reservation failed
            } 
            else {
                rms.add(RESOURCE_MANAGER_TYPE.CAR);
                updateTransaction(xid, rms);
                return customerResourceManager.reserveCar_CustomerRM(xid, customerID, location, carPrice);
            }
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware reservecar catches deadlock Exception");//e.printStackTrace();
        }

         return false;
    }
    
    // Adds room reservation to this customer
    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CUSTOMER);

            // Reserve a room and get its price
            Integer roomPrice = roomResourceManager.reserveRoom_RoomRM(xid, location, 1).intValue();
            
            if ((int) roomPrice == -1) 
            {
                return false; // room reservation failed
            } 
            else {
                rms.add(RESOURCE_MANAGER_TYPE.ROOM);
                updateTransaction(xid, rms);
                return customerResourceManager.reserveRoom_CustomerRM(xid, customerID, location, roomPrice);
            }
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware reserveroom catches deadlock Exception"); //e.printStackTrace();
        }

         return false;
    }

    // Reserve bundle
    public boolean bundle(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        try {
            ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();
            rms.add(RESOURCE_MANAGER_TYPE.CUSTOMER);
            
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
            rms.add(RESOURCE_MANAGER_TYPE.FLIGHT);
            customerResourceManager.reserveFlights_CustomerRM(xid, customerID, flights, prices);
            if (car) 
            {   
                rms.add(RESOURCE_MANAGER_TYPE.CAR);
                customerResourceManager.reserveCar_CustomerRM(xid, customerID, location, carPrice);
            }
            if (room) 
            {
                rms.add(RESOURCE_MANAGER_TYPE.ROOM);
                customerResourceManager.reserveRoom_CustomerRM(xid, customerID, location, roomPrice);
            }
            
            updateTransaction(xid, rms);
            return true; 
        }
        catch (DeadlockException e)
        {   
            this.transactions.remove(e.getXId());
            this.timers.remove(e.getXId());
            System.out.println("Exception caught: Middleware bundle catches deadlock Exception"); //e.printStackTrace();
        }

         return false;
    }

    //====================================================================================================
    //====================================================================================================

    /**
     * THE FOLLOWING INCORPORATE THE IMPLEMENTATION OF TRANSACTION MANAGEMENT
     * - START
     * - COMMIT
     * - ABORT
     * - SHUTDOWN
     */

    private HashMap<Integer,Transaction> transactions = new HashMap<Integer,Transaction>();
    private HashMap<Integer,Timer> timers = new HashMap<Integer,Timer>();
    private long TRANSACTION_TIME_LIMIT = 120000;
    private int count = 0;

    // Function to start transaction
    public int startTransaction(String client_id) throws RemoteException
    {   
        synchronized(this.transactions)
        {
            int id = this.count++; //(int) new Date().getTime();
            int xid = id < 0? -id : id;
            this.transactions.put(xid, new Transaction(xid,client_id));

            Timer t = new Timer();
            this.timers.put(xid, t);
            /*t.schedule(new TimerTask(){
            
                @Override
                public void run() {
                    try {
                        initiateAbort(xid);
                    }
                    catch (InvalidTransactionException e) 
                    {
                        System.out.println("Exception caught: Middleware-InvalidTransacton"); //e.printStackTrace();
                    }
                    catch (TransactionAbortedException e)
                    {
                        System.out.println("Exception caught: Middleware-TransactionAbortedTransacton"); //e.printStackTrace();
                    }
                    catch (RemoteException e)
                    {
                        System.out.println("Exception caught: Middleware-Remote"); //e.printStackTrace();
                    }
                    
                }
            }, this.TRANSACTION_TIME_LIMIT);*/

            flightResourceManager.start(xid);
            carResourceManager.start(xid);
            roomResourceManager.start(xid);
            customerResourceManager.start(xid);

            return xid;
        }
    }

    // Function to commit transaction
    public boolean commitTransaction(int xid) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        synchronized(this.transactions)
        {   
            synchronized (this.timers) 
            {
                if (!this.transactions.containsKey(xid)) 
                {
                    throw new InvalidTransactionException(xid,"Cannot commit to a non-existent transaction xid from middleware)");
                }

                Transaction ts = this.transactions.get(xid);
                ArrayList<Operation> ops = ts.getOperations();
                HashSet<RESOURCE_MANAGER_TYPE> set = new HashSet<RESOURCE_MANAGER_TYPE>();

                for (Operation op : ops)
                {
                    ArrayList<RESOURCE_MANAGER_TYPE> rms = op.getResourceManagers();

                    for (RESOURCE_MANAGER_TYPE rm : rms)
                    {
                        if (!set.contains(rm)) set.add(rm);
                    }
                }

                for (RESOURCE_MANAGER_TYPE rm : set) 
                {
                    switch (rm)
                    {
                        case FLIGHT:
                            flightResourceManager.commit(xid);
                            break;
                        case CAR:
                            carResourceManager.commit(xid);
                            break;
                        case ROOM:
                            roomResourceManager.commit(xid);
                            break;
                        case CUSTOMER:
                            customerResourceManager.commit(xid);
                            break;
                        default:
                            break;
                    }
                }

                this.transactions.remove(xid);
                this.timers.remove(xid);
            }
        }   

        return true;
    }

    // Function to abort transaction
    public boolean abortTransaction(int xid) throws RemoteException,InvalidTransactionException,TransactionAbortedException
    {   
        synchronized(this.transactions)
        {   
            if (!this.transactions.containsKey(xid)) {
                throw new InvalidTransactionException(xid,"Cannot abort to a non-existent transaction xid (from middleware)");
            }
            
            return initiateAbort(xid);
        }
    }

    // Function to shutdown
    public boolean shutdownClient(String client_id) throws RemoteException
    {   
        synchronized(this.transactions)
        {   
            for (Integer xid : transactions.keySet())
            {
                if (this.transactions.get(xid).getClientId().equals(client_id)) 
                {
                    return false;
                }
            }
        }

        return true;
    }

    // Function to initiate abort
    public boolean initiateAbort(int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException
    {
        synchronized (this.transactions)
        {
            synchronized (this.timers)
            {
                Transaction ts = this.transactions.get(xid);
                ArrayList<Operation> ops = ts.getOperations();
                HashSet<RESOURCE_MANAGER_TYPE> set = new HashSet<RESOURCE_MANAGER_TYPE>();

                for (Operation op : ops)
                {
                    ArrayList<RESOURCE_MANAGER_TYPE> rms = op.getResourceManagers();

                    for (RESOURCE_MANAGER_TYPE rm : rms)
                    {
                        if (!set.contains(rm)) set.add(rm);
                    }
                }

                for (RESOURCE_MANAGER_TYPE rm : set)
                {
                    switch (rm)
                        {
                            case FLIGHT:
                                flightResourceManager.abort(xid);
                                break;
                            case CAR:
                                carResourceManager.abort(xid);
                                break;
                            case ROOM:
                                roomResourceManager.abort(xid);
                                break;
                            case CUSTOMER:
                                customerResourceManager.abort(xid);
                                break;
                            default:
                                break;
                        }
                }

                this.transactions.remove(xid);
                this.timers.remove(xid);

                System.out.println(this.transactions.containsKey(xid)? "Transaction-" + xid + " removed" : "Transaction-" + xid + " not removed");
                System.out.println(this.timers.containsKey(xid)? "Timer-" + xid + " removed" : "Timer-" + xid + " not removed");
            }
        }
        return true;
    }

    // Function to add operation and to update timer for a transaction
    public void updateTransaction(int xid, ArrayList<RESOURCE_MANAGER_TYPE> rms) throws RemoteException, InvalidTransactionException, TransactionAbortedException
    {   
        synchronized (this.transactions) 
        {
            synchronized (this.timers) 
            {
                if (!this.transactions.containsKey(xid) || !this.timers.containsKey(xid)) 
                {
                    throw new InvalidTransactionException(xid,"Cannot identify the transaction xid by transaction manager");
                }

                Transaction ts = this.transactions.get(xid);
                Timer t = this.timers.get(xid);

                ts.addOperation(new Operation(rms));
                /*t.schedule(new TimerTask(){
                
                    @Override
                    public void run() {
                        try {
                            initiateAbort(xid);
                        }
                        catch (InvalidTransactionException e) 
                        {
                            System.out.println("Exception caught: Middleware-updateTransaction-InvalidTransacton");//e.printStackTrace();
                        }
                        catch (TransactionAbortedException e)
                        {
                            System.out.println("Exception caught: Middleware-updateTransaction-TransactonAborted"); //e.printStackTrace();
                        }
                        catch (RemoteException e)
                        {
                            System.out.println("Exception caught: Middleware-updateTransaction-Remote"); //e.printStackTrace();
                        }
                    }
                }, this.TRANSACTION_TIME_LIMIT);*/
            }
        }
    }

    //====================================================================================================
    //====================================================================================================

    /**
     * THE FOLLOWING ARE NOT USED IN RMI ARCHITECTURE, BUT ARE IMPLEMENTED DUE TO INHERITANCE
     */

    public boolean bundle(
		int xid, 
		int customerID, 
		Vector<String> flightNumbers, 
		ArrayList<Integer> flightPrices, 
		String location, boolean car, 
		Integer carPrice, 
		boolean room, 
        Integer roomPrice) throws RemoteException 
    {
        return false;
    }

    public Integer reserveFlight_FlightRM(int xid, int flightNum, int toReserve) throws RemoteException
    {
        return new Integer(-1);
    }

	// Function to reserve flights (multiple) in FlightResourceManager
    public ArrayList<Integer> reserveFlights_FlightRM(int xid, ArrayList<Integer> flightNums, int toReserve) throws RemoteException
    {
        return new ArrayList<Integer>();
    }

	// Function to reserve car in CarResourceManager (this returns an integer value as updating in the customer resource manager requires latest reserved price of item)
    public Integer reserveCar_CarRM(int xid, String location, int toReserve) throws RemoteException
    {
        return new Integer(-1);
    }

	// Function to reserve room in RoomResourceManager (this returns an integer value as updating in the customer resource manager requires latest reserved price of item)
    public Integer reserveRoom_RoomRM(int xid, String location, int toReserve) throws RemoteException
    {
        return new Integer(-1);
    }

	// Function to reserve flight in CustomerResourceManager (this returns an integer value as updating in the customer resource manager requires latest reserved price of item)
    public boolean reserveFlight_CustomerRM(int xid, int customerID, int flightNum, int price) throws RemoteException
    {
        return false;
    }

	// Function to reserve flights (multiple) in CustomerResourceManager 
    public boolean reserveFlights_CustomerRM(int xid, int customerID, ArrayList<Integer> flightNums, ArrayList<Integer> prices) throws RemoteException
    {
        return false;
    }

	// Function to reserve car in CustomerResourceManager
    public boolean reserveCar_CustomerRM(int xid, int customerID, String location, int price) throws RemoteException
    {
        return false;
    }

	// Function to reserve room in CustomerResourceManager
    public boolean reserveRoom_CustomerRM(int xid, int customerID, String location, int price) throws RemoteException
    {
        return false;
    }

	// Function to reserve item in CustomerResourceManager
    public boolean reserveItem_CustomerRM(int xid, int customerID, String key, String location, int price) throws RemoteException
    {
        return false;
    }

	// Function to delete customer in customer database
    public ArrayList<ReservedItem> deleteCustomer_CustomerRM(int xid, int customerID) throws RemoteException
    {
        return new ArrayList<ReservedItem>();
    }
    
    public String getName() throws RemoteException
    {
        return m_name;
    }

    // Commits a transaction
    public boolean commit(int xid) throws RemoteException, InvalidTransactionException
    {
        return false;
    }

    // Aborts a transaction
    public void abort(int xid) throws RemoteException, InvalidTransactionException
    {
        return;
    }

    // Exits the server
    public void shutdown() throws RemoteException
    {
        return;
    }

    // Start a transaction, add the a local history for the transaction in the hashmap of local histories
    public boolean start(int xid) throws RemoteException
    {
        return false;
    }
}

