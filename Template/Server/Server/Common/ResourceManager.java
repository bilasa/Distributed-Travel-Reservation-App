// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;
import Server.LockManager.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public class ResourceManager extends LockManager implements IResourceManager
{
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();

    // For shadowing (master record)
    protected File a;
    protected File b;
    protected File master;

    // Global lock for commit
    Lock commit_lock = new ReentrantLock();
    
    // Hashmap indexed by xid to store the local histories of each transaction
    protected Map<Integer, RMHashMap> local = new HashMap<Integer, RMHashMap>();

	public ResourceManager(String p_name)
	{
		m_name = p_name;

        // To distinguish the files of different RMs
        a = new File("a_" + m_name + ".csv");
        b = new File("b_" + m_name + ".csv");
        master = new File("master_" + m_name + ".csv");

        // Handles crash case, if we create a new ResourceManager
        restoreMainMemory();
	}

	// Reads a data item
	protected RMItem readData(int xid, String key) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
        try {
            Lock(xid, key, TransactionLockObject.LockType.LOCK_READ);
        }
        catch (DeadlockException deadlock) {
            throw deadlock;
        }
        
        
        // Get the local history for the transaction
        synchronized(local) {
            RMHashMap local_data = local.get(xid);
            if (local_data == null)
            {
                 throw new InvalidTransactionException(xid,"Cannot read data for a non-existent transaction xid");
            }
            else
            {
                // Check if local history already contains the item
                if (local_data.containsKey(key))
                {
                    RMItem local_item = local_data.get(key);
                    if (local_item == null) // Item was removed by transaction
                    {
                        return null;
                    }
                    else // Item in local history, not removed
                    {
                        return (RMItem)local_item.clone();
                    }
                }
                
                else
                {
                    // otherwise, check the main memory
                    RMItem item;
                    synchronized(m_data) {
                        item = m_data.get(key);
                    }
                    if (item != null)
                    {
                        // add item to local history
                        local_data.put(key, item);
                        
                        // update the hashmap of local histories
                        local.put(xid, local_data);
                        
                        return (RMItem)item.clone();
                    }
                    return null;
                }
            }
        }
	}

	// Writes a data item
	protected void writeData(int xid, String key, RMItem value) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
        try {
            Lock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
        }
        catch (DeadlockException deadlock) {
            throw deadlock;
        }
        
        // Get the local history for the transaction
        synchronized(local) {
            RMHashMap local_data = local.get(xid);
            if (local_data == null)
            {
                throw new InvalidTransactionException(xid,"Cannot write data for a non-existent transaction xid");
            }
            
            local_data.put(key, value);
            
            // update the hashmap of local histories
            local.put(xid, local_data);
        }
	}
    
    // Commits a transaction
    public  boolean commit(int xid) throws RemoteException, InvalidTransactionException
    {
        commit_lock.lock();

        synchronized(local) {
            RMHashMap local_data = local.get(xid);
            if (local_data == null)
            {
                throw new InvalidTransactionException(xid,"Cannot commit to a non-existent transaction xid");
            }
            else
            {
                synchronized(m_data) {

                    // Put all items in local history into main memory
                    for (String key : local_data.keySet())
                    {
                        RMItem item = local_data.get(key);

                        if (item == null) // for removing an item
                        {
                            m_data.remove(key);
                        }
                        else // for writing an item
                        {
                            m_data.put(key, item);
                        }
     
                    }

                    // Get the name of the last committed copy
                    String last;
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(master)); 
                        last = br.readLine();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Write main memory copy to non-latest committed copy, and switch master record pointer
                    if(last == a.getName()) 
                    {
                        writeFile(b);
                        updateMaster(b.getName());
                    } 
                    else 
                    {
                        writeFile(a);
                        updateMaster(a.getName());
                    }

                }
                
                // Unlock all locks owned by transaction
                UnlockAll(xid);
                System.out.println("Commited transaction " + xid);
                
                // Remove the local history
                local.remove(xid);

                Trace.info("RM::commit(" + xid + ") succeeded");

                commit_lock.unlock();

                return true;
            }
        }
    }
    
    // Aborts a transaction
    public  void abort(int xid) throws RemoteException, InvalidTransactionException
    {
        if (local.get(xid) != null)
        {
            // Discard main memory copy, put latest committed copy into main memory (this step is unecessary for our implementation)
            synchronized(m_data) {
                // No need to write contents of master file to m_data, as m_data not modified until commit
            }

            // Remove the local history
            System.out.println("Aborted transaction " + xid);
            local.remove(xid);

            // Unlock all locks owned by transaction
            UnlockAll(xid);
        }
        else
        {
            throw new InvalidTransactionException(xid,"Cannot abort to a non-existent transaction xid");
        }
    }
    
    // Exits the server
    public void shutdown() throws RemoteException
    {   
        System.out.println("Server is shutdown");
        System.exit(0);
    }
    
    // Start a transaction, add the a local history for the transaction in the hashmap of local histories
    public boolean start(int xid) throws RemoteException 
    {
        synchronized(local) {
            RMHashMap local_data = new RMHashMap();
            
            // update the hashmap of local histories
            local.put(xid, local_data);
        }

        return true;
    }

	// Remove the item out of storage
	protected void removeData(int xid, String key) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
	{

        try {
            Lock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
        }
        catch (DeadlockException deadlock) {
            throw deadlock;
        }
        
        // Get the local history for the transaction
        synchronized(local) {
            RMHashMap local_data = local.get(xid);
            if (local_data == null) // Transaction doesn't exist
            {
                throw new InvalidTransactionException(xid,"Cannot write data for a non-existent transaction xid");
            }

            local_data.put(key, null);
            
            // update the hashmap of local histories
            local.put(xid, local_data);
        }
	}

	// Deletes the encar item
	protected boolean deleteItem(int xid, String key) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
            ReservableItem curObj = (ReservableItem)readData(xid, key);
            // Check if there is such an item in the storage
            if (curObj == null)
            {
                Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
                return false;
            }
            else
            {
                if (curObj.getReserved() == 0)
                {
                    removeData(xid, curObj.getKey());
                    Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
                    return true;
                }
                else
                {
                    Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
                    return false;
                }
            }
    
	}

	// Query the number of available seats/rooms/cars
	protected int queryNum(int xid, String key) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
            ReservableItem curObj = (ReservableItem)readData(xid, key);
            int value = 0;
            if (curObj != null)
            {
                value = curObj.getCount();
            }
            Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
            return value;
        
	}    

	// Query the price of an item
	protected int queryPrice(int xid, String key) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
            ReservableItem curObj = (ReservableItem)readData(xid, key);
            int value = 0;
            if (curObj != null)
            {
                value = curObj.getPrice();
            }
            Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
            return value;
        
	}

	// Reserve an item
	protected boolean reserveItem(int xid, int customerID, String key, String location) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );
            // Read customer object if it exists (and read lock it)
            Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
            if (customer == null)
            {
                Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
                return false;
            }

            // Check if the item is available
            ReservableItem item = (ReservableItem)readData(xid, key);
            if (item == null)
            {
                Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
                return false;
            }
            else if (item.getCount() == 0)
            {
                Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
                return false;
            }
            else
            {
                customer.reserve(key, location, item.getPrice());
                writeData(xid, customer.getKey(), customer);

                // Decrease the number of available items in the storage
                item.setCount(item.getCount() - 1);
                item.setReserved(item.getReserved() + 1);
                writeData(xid, item.getKey(), item);

                Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
                return true;
            }
        
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
            Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
           
            if (curObj == null)
            {
                // Doesn't exist yet, add it
                Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
    
                writeData(xid, newObj.getKey(), newObj);
                Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
            }
            else
            {
                // Add seats to existing flight and update the price if greater than zero
                curObj.setCount(curObj.getCount() + flightSeats);
                if (flightPrice > 0)
                {
                    curObj.setPrice(flightPrice);
                }
                writeData(xid, curObj.getKey(), curObj);
                Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
            }
            return true;
        
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
            Car curObj = (Car)readData(xid, Car.getKey(location));
            if (curObj == null)
            {
                // Car location doesn't exist yet, add it
                Car newObj = new Car(location, count, price);
                writeData(xid, newObj.getKey(), newObj);
                Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
            }
            else
            {
                // Add count to existing car location and update price if greater than zero
                curObj.setCount(curObj.getCount() + count);
                if (price > 0)
                {
                    curObj.setPrice(price);
                }
                writeData(xid, curObj.getKey(), curObj);
                Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
            }
            return true;
        
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
            Room curObj = (Room)readData(xid, Room.getKey(location));
            if (curObj == null)
            {
                // Room location doesn't exist yet, add it
                Room newObj = new Room(location, count, price);
                writeData(xid, newObj.getKey(), newObj);
                Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
            } else {
                // Add count to existing object and update price if greater than zero
                curObj.setCount(curObj.getCount() + count);
                if (price > 0)
                {
                    curObj.setPrice(price);
                }
                writeData(xid, curObj.getKey(), curObj);
                Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
            }
            return true;
        
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return deleteItem(xid, Flight.getKey(flightNum));
        
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return deleteItem(xid, Car.getKey(location));
        
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return deleteItem(xid, Room.getKey(location));
        
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return queryNum(xid, Flight.getKey(flightNum));
        
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return queryNum(xid, Car.getKey(location));
        
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return queryNum(xid, Room.getKey(location));
        
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return queryPrice(xid, Flight.getKey(flightNum));
        
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return queryPrice(xid, Car.getKey(location));
        
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return queryPrice(xid, Room.getKey(location));
        
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
            Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
            if (customer == null)
            {
                Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
                // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
                return "";
            }
            else
            {
                Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
                System.out.println(customer.getBill());
                return customer.getBill();
            }
        
	}

	public int newCustomer(int xid) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::newCustomer(" + xid + ") called");
            // Generate a globally unique ID for the new customer
            int cid = Integer.parseInt(String.valueOf(xid) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
            Customer customer = new Customer(cid);
            writeData(xid, customer.getKey(), customer);
            Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
            return cid;
        
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
            Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
            if (customer == null)
            {
                customer = new Customer(customerID);
                writeData(xid, customer.getKey(), customer);
                Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
                return true;
            }
            else
            {
                Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
                return false;
            }
        
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
            Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
            if (customer == null)
            {
                Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
                return false;
            }
            else
            {
                // Increase the reserved numbers of all reservable items which the customer reserved.
                RMHashMap reservations = customer.getReservations();
                for (String reservedKey : reservations.keySet())
                {
                    ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                    Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times");
                    ReservableItem item  = (ReservableItem)readData(xid, reserveditem.getKey());
                    Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " which is reserved " +  item.getReserved() +  " times and is still available " + item.getCount() + " times");
                    item.setReserved(item.getReserved() - reserveditem.getCount());
                    item.setCount(item.getCount() + reserveditem.getCount());
                    writeData(xid, item.getKey(), item);
                }

                // Remove the customer from the storage
                removeData(xid, customer.getKey());
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
                return true;
            }
        
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
        
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return reserveItem(xid, customerID, Car.getKey(location), location);
        
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return reserveItem(xid, customerID, Room.getKey(location), location);
        
	}

	/* NOTE: The following functions are to support the Client-Middleware-RMs design */
	
	// Function to reserve flight in FlightResourceManager
	public Integer reserveFlight_FlightRM(int xid, int flightNum, int toReserve) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
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

	// Function to reserve flights (multiple) in FlightResourceManager
	public ArrayList<Integer> reserveFlights_FlightRM(int xid, ArrayList<Integer> flightNums, int toReserve) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            ArrayList<Integer> prices = new ArrayList<Integer>();

            for (int i = 0; i < flightNums.size(); i++)
            {
                Flight curObj = (Flight) readData(xid, Flight.getKey(flightNums.get(i)));

                if (curObj == null) return new ArrayList<Integer>();

                int nCount = curObj.getCount() - toReserve;
                int nReserved = curObj.getReserved() + toReserve;

                if (nCount < 0 || nReserved < 0) return new ArrayList<Integer>();
            }

            for (int i = 0; i < flightNums.size(); i++)
            {
                Flight curObj = (Flight) readData(xid, Flight.getKey(flightNums.get(i)));

                int nCount = curObj.getCount() - toReserve;
                int nReserved = curObj.getReserved() + toReserve;
                
                curObj.setCount(nCount);
                curObj.setReserved(nReserved);
                writeData(xid, curObj.getKey(), curObj);

                int price = curObj.getPrice();
                prices.add(price);
            }

            return prices;
        
	}

	// Function to reserve car in CarResourceManager (this returns an integer value as updating in the customer resource manager requires latest reserved price of item)
	public Integer reserveCar_CarRM(int xid, String location, int toReserve) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
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
	public Integer reserveRoom_RoomRM(int xid, String location, int toReserve) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
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
	public boolean reserveFlight_CustomerRM(int xid, int customerID, int flightNum, int price) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return reserveItem_CustomerRM(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum), price);
        
	}

	// Function to reserve flights (multiple) in CustomerResourceManager 
	public boolean reserveFlights_CustomerRM(int xid, int customerID, ArrayList<Integer> flightNums, ArrayList<Integer> prices) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            boolean success = true;

            for (int i = 0; i < flightNums.size(); i++) {

                success = reserveItem_CustomerRM(xid, customerID, Flight.getKey(flightNums.get(i)), String.valueOf(flightNums.get(i)), prices.get(i));
                if (!success) return false;
            }

            return success;
        
	}

	// Function to reserve car in CustomerResourceManager
	public boolean reserveCar_CustomerRM(int xid, int customerID, String location, int price) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return reserveItem_CustomerRM(xid, customerID, Car.getKey(location), location, price);
        
	}

	// Function to reserve room in CustomerResourceManager
	public boolean reserveRoom_CustomerRM(int xid, int customerID, String location, int price) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            return reserveItem_CustomerRM(xid, customerID, Room.getKey(location), location, price);
        
	}

	// Function to reserve item in CustomerResourceManager
	public boolean reserveItem_CustomerRM(int xid, int customerID, String key, String location, int price) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
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

	// Function to delete customer in customer database
	public ArrayList<ReservedItem> deleteCustomer_CustomerRM(int xid, int customerID) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");

            // Retrieve customer
            Customer customer = (Customer) readData(xid, Customer.getKey(customerID));

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

	// Function to bundle (Not used)
	public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException
    {
		return false;
	} 

	// Function to reserve bundle (TCP)
	public boolean bundle(
		int xid, 
		int customerID, 
		Vector<String> flightNumbers, 
		ArrayList<Integer> flightPrices, 
		String location, boolean car, 
		Integer carPrice, 
		boolean room, 
		Integer roomPrice) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException
    {
            Customer customer = (Customer) readData(xid, Customer.getKey(customerID));

            if (customer == null)
            {
                Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ")  failed--customer doesn't exist");
                return false;
            }

            // Reserve flights
            for (int i = 0; i < flightNumbers.size() ; i++) {
                reserveFlight_CustomerRM(xid, customerID, Integer.parseInt(flightNumbers.get(i)), flightPrices.get(i));
            }

            // Reserve car
            if (car)
            {
                reserveCar_CustomerRM(xid, customerID, location, carPrice);
            }

            // Reserve room
            if (room)
            {
                reserveRoom_CustomerRM(xid, customerID, location, roomPrice);
            }

            return true;
        
	}
    
    // Function to get a summary of all customers' item purchases
    public ArrayList<String> getSummary(int xid) {
    
    	Trace.info("RM::getSummary(" + xid + ") called");
	
        // List to store bills in
        ArrayList<String> bills = new ArrayList<String>();
        
        // Add bill for each customer in hashmap
        for (RMItem item : m_data.values()) {
            Customer customer = (Customer) item;
            bills.add(customer.getBill());
        }
        
        return bills;
    }

	// Function to get resource manager's name
	public String getName() throws RemoteException
	{
		return m_name;
	}

	public int startTransaction(String client_id) throws RemoteException
	{
		return -1;
	}

	public boolean commitTransaction(int xid) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		return false;
	}

	public boolean abortTransaction(int xid) throws RemoteException,InvalidTransactionException
	{
		return false;
	}

	public boolean initiateAbort(int xid) throws InvalidTransactionException, TransactionAbortedException
	{
		return false;
	}

	public void updateTransaction(int xid, ArrayList<RESOURCE_MANAGER_TYPE> rms) throws InvalidTransactionException, TransactionAbortedException
	{
		return;
	}

    public boolean shutdownClient(String client_id) throws RemoteException
    {
        return false;
    }

    // Write main memory copy (m_data) to the given file
    public void writeFile(File file) 
    {
        BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));

        for (String key : m_data.keySet()) // write each key and RMItem to the file
        {
            RMItem item = m_data.get(key);

            if (item instanceof ReservableItem) // for Flight, Car, and Room RMs
            {
                // Identify the appropriate ReservableItem subclass
                String type = "";
                if (item instanceof Flight) type = "Flight";
                else if (item instanceeof Car) type = "Car";
                else if (item instanceof Room) type = "Room";

                bw.write(key + "," + type 
                + "," + ((ReservableItem)item).getCount() 
                + "," + ((ReservableItem)item).getPrice()
                + "," + ((ReservableItem)item).getReserved()
                + "," + ((ReservableItem)item).getLocation());
                bw.newLine();
            }

            else if (item instanceof Customer) // for Customer RM
            {
                int id = ((Customer)item).getID();
                RMHashMap reservations = ((Customer)item).getReservations();

                for(String res_key : reservations.keySet()) // write each reservation to the file as a new line
                {
                    ReservableItem res_item = reservations.get(res_key);

                    String type = "";
                    if (res_item instanceof Flight) type = "Flight";
                    else if (res_item instanceeof Car) type = "Car";
                    else if (res_item instanceof Room) type = "Room";

                    bw.write(key + ",Customer," + id + "," + res_key + "," + type 
                    + "," + ((ReservableItem)item).getCount() 
                    + "," + ((ReservableItem)item).getPrice()
                    + "," + ((ReservableItem)item).getReserved()
                    + "," + ((ReservableItem)item).getLocation());
                    bw.newLine();
                }
            }
        }

        bw.flush();
    }

    // write ID of file ("a" or "b") into the master file
    updateMaster(String fileID) 
    {
        BufferedWriter bw = new BufferedWriter(new FileWriter(master, false));
        bw.write(fileID);
        bw.flush();
    }

    // Puts the contents of the last committed copy into main memory (m_data)
    restoreMainMemory() 
    {
        synchronized(m_data) 
        {
            m_data = new RMHashMap();

            String last = "";

            // Get the name of the last committed copy
            try {
                BufferedReader br = new BufferedReader(new FileReader(master)); 
                last = br.readLine();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Read from the last committed copy
            try {
                BufferedReader br = new BufferedReader(new FileReader(last)); 
                
                String line; 
                while ((line = br.readLine()) != null) {
                    String[] entries = line.split("\\s+");

                    String key = entries[0];
                    String item_type = entries[1];

                    switch (item_type) // check what kind of item it is
                    {
                        case "Customer":
                            Customer cus;
                            RMHashMap reservations;

                            if ((cus = m_data.get(key)) != null) // Customer already in m_data: add the ReservableItem
                            {
                                reservations = cus.getReservations();
                            }
                            else // Customer not in m_data: create the customer and put it in m_data
                            {
                                int id = Integer.parseInt(entries[2]);
                                cus = new Customer(id);
                                reservations = cus.getReservations();
                                m_data.put(key, cus);
                            }
                            
                            // Reconstruct the ReservableItem
                            String res_key = entries[3];
                            String res_item_type = entries[4];
                            int count = Integer.parseInt(entries[5]);
                            int price = Integer.parseInt(entries[6]);
                            int reserved = Integer.parseInt(entries[7]);
                            String location = entries[8];

                            switch (res_item_type) // check what kind of item is reserved and put it in "reservations"
                            {
                                case "Flight":
                                    Flight flight = new Flight(location, count, price);
                                    flight.setReserved(reserved);
                                    reservations.put(res_key, flight);
                                    break;
                                case "Car":
                                    Car car = new Car(location, count, price);
                                    car.setReserved(reserved);
                                    reservations.put(res_key, car);
                                    break;
                                case "Room":
                                    Room room = new Room(location, count, price);
                                    room.setReserved(reserved);
                                    reservations.put(res_key, room);
                                    break;
                            }

                            break;
                        case "Flight":
                            // Reconstruct the Flight
                            int count = Integer.parseInt(entries[2]);
                            int price = Integer.parseInt(entries[3]);
                            int reserved = Integer.parseInt(entries[4]);
                            String location = entries[5];

                            Flight flight = new Flight(location, count, price);
                            m_data.put(key, flight);

                            break;
                        case "Car":
                            // Reconstruct the Car
                            int count = Integer.parseInt(entries[2]);
                            int price = Integer.parseInt(entries[3]);
                            int reserved = Integer.parseInt(entries[4]);
                            String location = entries[5];

                            Car car = new Car(location, count, price);
                            m_data.put(key, car);

                            break;
                        case "Room":
                            // Reconstruct the Room
                            int count = Integer.parseInt(entries[2]);
                            int price = Integer.parseInt(entries[3]);
                            int reserved = Integer.parseInt(entries[4]);
                            String location = entries[5];

                            Room room = new Room(location, count, price);
                            m_data.put(key, room);
                            
                            break;
                    }
                } 

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
 
