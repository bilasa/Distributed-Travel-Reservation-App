// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;
import Server.LockManager.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.rmi.RemoteException;
import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

public class ResourceManager extends LockManager implements IResourceManager
{
	protected String m_name = "";
    protected RMHashMap m_data = new RMHashMap();
    protected Map<Integer,RMHashMap> local = new HashMap<Integer,RMHashMap>();  // Hashmap indexed by xid to store the local histories of each transaction
    protected Map<Integer,Boolean> crashes = new HashMap<Integer,Boolean>();
    protected Lock global_lock = new ReentrantLock();

    private HashMap<Integer,Timer> timers = new HashMap<Integer,Timer>(); // timers for vote request timeout

    private long VOTE_REQUEST_TIME_LIMIT = 120000; // max allowed time for VOTE_REQ to come after starting transaction (2 mins)
    
	public ResourceManager(String p_name)
	{   
        System.out.println("ATTENTION: Starting/Restarting Participant");
        m_name = p_name;

        // Set crash mdoes
        for (int i = 1; i <= 5; i++) {
            crashes.put(i, false);
        }

        writeMainMemory();
        recoverLocalHistory();
        readLog();
	}

    public void readLog() {
        synchronized (local) {
            try {
                System.out.println("ATTENTION: Reading the resource manager log");
                File record_file = new File("rm_records_" + m_name + ".txt");
                record_file.createNewFile();
                BufferedReader br = new BufferedReader(new FileReader(record_file));
                String line = null;
                HashMap<Integer, Boolean> map = new HashMap<Integer, Boolean>();
                while ((line = br.readLine()) != null) {
                    if (line.length() > 0) {
                        String[] record = line.trim().split("\\:");
                        int xid = Integer.parseInt(record[0]);
                        map.put(xid, true);
                    }
                }

                // Crash mode 5
                if (crashes.get(5)) System.exit(1);

                for (int xid : local.keySet()) {
                    if (!map.containsKey(xid)) {
                        try {
                            abort(xid);
                        }
                        catch (InvalidTransactionException e) {
                            e.printStackTrace();
                        }
                        catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println("FILE NAME: " + record_file.getName());
                br.close();
                PrintWriter pw = new PrintWriter(record_file);
                pw.print("");
                pw.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	// Start a transaction, add the a local history for the transaction in the hashmap of local histories
    public boolean start(int xid) throws RemoteException 
    {
        synchronized(local) {
            System.out.println("Started transaction at rms: " + xid);
            RMHashMap local_data = new RMHashMap();
            local.put(xid, local_data); // update the hashmap of local histories

            // Set VOTE_REQUEST timer
            Timer vote_timer = new Timer();
            timers.put(xid, vote_timer);
            vote_timer.schedule(new TimerTask(){
                
                @Override
                public void run() {
                    vote_failure_handler(xid);
                }
            }, VOTE_REQUEST_TIME_LIMIT);

            return true;
        }
    }
    
    // Commits a transaction
    public boolean commit(int xid) throws RemoteException, InvalidTransactionException
    {   
        synchronized(local) {
            synchronized(m_data) { 

                System.out.println("ATTENTION: Participant commit function is called");

                 // Crash mode 4
                if (crashes.get(4)) System.exit(1);

                if (!local.containsKey(xid)) {
                    throw new InvalidTransactionException(xid,"Cannot commit to a non-existent transaction xid");
                }

                boolean transaction_completed = false;
                while (!transaction_completed) {
                    
                    System.out.println("ATTENTION: Participant checking if global lock is free.");
                    boolean free = global_lock.tryLock();

                    if (free) {
                        
                        System.out.println("ATTENTION: Participant has accessed global lock and will proceed to shadowing.");
                        
                        global_lock.lock();
                
                        RMHashMap local_data = local.get(xid);
                        if (local_data == null) {
                            global_lock.unlock();
                            throw new InvalidTransactionException(xid,"Cannot commit to a non-existent transaction xid");
                        }

                        // Write from local history to main memory 
                        for (String key : local_data.keySet()) {
                            RMItem item = local_data.get(key);
                            if (item == null) {
                                m_data.remove(key);
                            }
                            else {
                                m_data.put(key, item);
                            }
                        }

                        /**
                         * FORMAT DETAILS
                         * 
                         * --> MASTER RECORD
                         * (File):T-(xid)
                         * e.g. 
                         * - A:1
                         * - B:2
                         * 
                         * --> DATA
                         * (key):(value)
                         * e.g. values
                         * - ReservableItem: key:lm_strLocation#m_nCount#m_nPrice#m_nReserved
                         * - ReservedItem: key:id:key#m_location#m_nCount#m_nPrice-m_nReserved;...;...
                         */

                        // Write from main memory to disk
                        // Shadowing
                        try {
                            // Retrieve previous master record
                            System.out.println("ATTENTION: Participant reading master record");
                            File master_file = new File("master_" + m_name + ".txt");
                            master_file.createNewFile();
                            BufferedReader br = new BufferedReader(new FileReader(master_file)); 
                            String line = null;
                            String master_ptr = null;
                            int master_transaction = -1;
                            
                            while ((line = br.readLine()) != null) {
                                if (line.length() > 0) {
                                    String[] cur = line.trim().split("\\:");
                                    master_ptr = cur[0];
                                    master_transaction = Integer.parseInt(cur[1]);  
                                } 
                            }

                            br.close();

                            BufferedWriter bw = null;
                            String updated_ptr = "A";
                            // Nothing has been recorded previously to master record
                            if (master_ptr != null && master_transaction != -1) {
                                updated_ptr = master_ptr.equals("A")? "B" : "A";
                            }
                            // Update new master record
                            System.out.println("Updating master record from " + master_ptr + " to " + updated_ptr);
                            bw = new BufferedWriter(new FileWriter(master_file, false));
                            bw.write(updated_ptr + ":" + xid);
                            bw.newLine();
                            bw.close();
                            
                            // Store data to disk
                            System.out.println("ATTENTION: Participant is recoring main memory to to shadowing files.");
                            File data_file = new File("data_" + m_name + "_" + updated_ptr + ".txt");
                            data_file.createNewFile();
                            bw = new BufferedWriter(new FileWriter(data_file, false));
                            StringBuilder sb = new StringBuilder();

                            for (String key : m_data.keySet()) {

                                RMItem item = m_data.get(key);
                                if (item != null) {

                                    if (item instanceof Flight) {
                                        Flight flight = (Flight) item;
                                        sb.append(flight.getKey() + ":" + flight.getLocation() + "#" + flight.getCount() + "#" + flight.getPrice() + "#" + flight.getReserved());
                                    }

                                    if (item instanceof Room) {
                                        Room room = (Room) item;
                                        sb.append(room.getKey() + ":" + room.getLocation() + "#" + room.getCount() + "#" + room.getPrice() + "#" + room.getReserved());
                                    }

                                    if (item instanceof Car) {
                                        Car car = (Car) item;
                                        sb.append(car.getKey() + ":" + car.getLocation() + "#" + car.getCount() + "#" + car.getPrice() + "#" + car.getReserved());
                                    }

                                    if (item instanceof Customer) {
                                        Customer customer = (Customer) item;
                                        int id = customer.getID();
                                        RMHashMap reservations = customer.getReservations();
                                        StringBuilder customer_sb = new StringBuilder();

                                        ArrayList<ReservedItem> reservedItems = new ArrayList<ReservedItem>();
                                        for (String reservedItem : reservations.keySet()) {
                                            reservedItems.add((ReservedItem) reservations.get(reservedItem));
                                        }

                                        for (int i = 0; i < reservedItems.size(); i++) {
                                            ReservedItem reserved = reservedItems.get(i);
                                            customer_sb.append(
                                                reserved.getKey() + "#" + reserved.getLocation() + "#" + reserved.getCount() + "#" + reserved.getPrice()
                                            );

                                            if (i != reservedItems.size() - 1) customer_sb.append(";");
                                        }

                                        sb.append(customer.getKey() + ":" + id + ":" + customer_sb.toString());
                                    }
                                }

                                sb.append("\n");
                            }

                            bw.write(sb.toString());
                            bw.close();

                            UnlockAll(xid); // Restore locks and local history
                            local.remove(xid);

                            System.out.println("ATTENTION: Participant decision log is recorded");
                            recordDecision(xid, true); // log a COMMIT
                            transaction_completed = true;
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        finally {  
                            global_lock.unlock();
                        }
                        global_lock.unlock();
                    }
                    else {
                        try {
                            System.out.println("ATTENTION: Participant cannot access global lock and will sleep.");
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        } 
                    }

                    transaction_completed = true;
                }
                Trace.info("RM::commit(" + xid + ") succeeded");
                return true;
            }
        }
    }
    
    // Aborts a transaction
    public boolean abort(int xid) throws RemoteException, InvalidTransactionException
    {   
        synchronized(local) {

            System.out.println("ATTENTION: Participation abort function is called for XID: " + xid);

             // Crash mode 4
             if (crashes.get(4)) System.exit(1);

             if (local.get(xid) != null) {
                // Discard main memory copy, put latest committed copy into main memory (this step is unecessary for our implementation)
                synchronized(m_data) {
                    // No need to write contents of master file to m_data, as m_data not modified until commit
                }
            
                local.remove(xid);
                UnlockAll(xid);
            }
            else {
                throw new InvalidTransactionException(xid,"Cannot abort to a non-existent transaction xid");
            }

            System.out.println("ATTENTION: Participation abort is recording abort decision for XID: " + xid);
            recordDecision(xid, false); // log an ABORT

            return true;
        }
    }

    // Prepare to commit 
    public boolean prepare(int xid) throws RemoteException, InvalidTransactionException
    {   
        synchronized(local) {
            synchronized(timers) {

                System.out.println("ATTENTION: Participant prepare function is called for XID[" + xid + "]");

                // Cancel the vote request timer
                Timer t = timers.get(xid);
                if (t != null) {
                    t.cancel();
                    timers.remove(t);
                }

                 // Crash mode 1
                if (crashes.get(1)) System.exit(1);

                boolean canCommit = false;
                canCommit = this.local.containsKey(xid);


                if (!canCommit) {
                    System.out.println("ATTENTION: Participant vote decision for XID[" + xid + "] is record");
                    recordDecision(xid, false); // vote NO => log an ABORT
                }
                else {
                    System.out.println("ATTENTION: Participant vote decision for XID[" + xid + "] is record");
                    recordYes(xid); // vote YES => log a YES 
                    System.out.println("ATTENTION: Participant for XID[" + xid + "] is recording local history");
                    recordLocalHistory();
                } 

                // Crash mode 2
                if (crashes.get(2)) {
                    System.exit(1);
                }

                System.out.println("ATTENTION: Participant vote decision for XID[" + xid + "] is " + (canCommit? "YES" : "NO"));
                return canCommit; // send vote
            }
        }
    }

    // Function to write to main memory
    public void writeMainMemory() 
    {
        synchronized(m_data) {

            System.out.println("ATTENTION: WriteMainMemory is initiated");

            BufferedReader br = null;
            File master_file = null;
            m_data = new RMHashMap();

            String record_ptr = "A";
           
            try {
                master_file = new File("master_" + m_name + ".txt");
                master_file.createNewFile();
                br = new BufferedReader(new FileReader(master_file));
                String master_record = br.readLine();
                if (master_record != null && master_record.length() > 0) {
                    System.out.println("WRITE_MAIN_MEMORY on restart (should only be read if already existing");
                    record_ptr = master_record.trim().split("\\:")[0].toUpperCase();
                }
                br.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            File data_file = null;
            
            System.out.println("ATTENTION: WriteMainMemory has read master record with pointer " + record_ptr);

            try {
                String line = null;
                data_file = new File("data_" + m_name + "_" + record_ptr + ".txt");
                data_file.createNewFile();
                br = new BufferedReader(new FileReader(data_file));

                // Flights
                if (m_name.equals("Flights")) {

                    while ((line = br.readLine()) != null) {
                        if (line.length() > 0) {
                            System.out.println("DEBUG: Reading line: " + line);
                            String[] record = line.trim().split("\\:");
                            String key = record[0];
                            String[] value = record[1].split("\\#");
                            Flight flight = new Flight(Integer.parseInt(value[0]), Integer.parseInt(value[1]), Integer.parseInt(value[2]));
                            flight.setReserved(Integer.parseInt(value[3]));
                            m_data.put(key, flight);
                        }
                    }
                } 

                // Rooms
                if (m_name.equals("Rooms")) {

                    while ((line = br.readLine()) != null) {
                        if (line.length() > 0) {
                            System.out.println("DEBUG: Reading line: " + line);
                            String[] record = line.trim().split("\\:");
                            String key = record[0];
                            String[] value = record[1].split("\\#");
                            Room room = new Room(value[0], Integer.parseInt(value[1]), Integer.parseInt(value[2]));
                            room.setReserved(Integer.parseInt(value[3]));
                            m_data.put(key, room);
                        }
                    }
                } 

                // Cars
                if (m_name.equals("Cars")) {

                    while ((line = br.readLine()) != null) {
                        if (line.length() > 0) {
                            System.out.println("DEBUG: Reading line: " + line);
                            String[] record = line.trim().split("\\:");
                            String key = record[0];
                            String[] value = record[1].split("\\#");
                            Car car = new Car(value[0], Integer.parseInt(value[1]), Integer.parseInt(value[2]));
                            car.setReserved(Integer.parseInt(value[3]));
                            m_data.put(key, car);
                        }
                    }
                }

                // Customers
                if (m_name.equals("Customers")) {

                    while ((line = br.readLine()) != null) {
                        if (line.length() > 0) {
                            System.out.println("DEBUG: Reading line: " + line);
                            String[] record = line.trim().split("\\:");
                            String key = record[0];
                            int customer_id = Integer.parseInt(record[1]);

                            Customer customer = new Customer(customer_id);

                            if (record.length > 2 && record[2].length() > 0 && (record[2].indexOf(';') != -1)) {
                                String[] list_of_reserved = record[2].split("\\;");
                                for (String reserved : list_of_reserved) {

                                    String[] data = reserved.split("\\#");
                                    int reserve_count = Integer.parseInt(data[2]);

                                    while (reserve_count > 0) {
                                        customer.reserve(data[0], data[1], Integer.parseInt(data[3]));
                                        reserve_count--;
                                    }
                                }
                            }

                            m_data.put(key, customer);
                        }
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void recordLocalHistory() 
    {   
        synchronized(local) {

            System.out.println("ATTENTION: RecordLocalHistory is initiated");

            try {
                File local_history_file = new File("local_history_" + m_name + ".txt");
                local_history_file.createNewFile();
                BufferedWriter bw = new BufferedWriter(new FileWriter(local_history_file, false));
                StringBuilder sb = new StringBuilder();

                // For each transaction XID
                for (Integer xid : local.keySet()) {

                    sb.append(xid + ":");

                    RMHashMap xid_map = local.get(xid);

                    for (String key : xid_map.keySet()) {

                        RMItem item = xid_map.get(key);
                        sb.append(key + "$");

                        if (item != null) {

                            if (item instanceof Flight) {
                                Flight flight = (Flight) item;
                                sb.append(flight.getKey() + "#" + flight.getLocation() + "#" + flight.getCount() + "#" + flight.getPrice() + "#" + flight.getReserved());
                            }

                            if (item instanceof Room) {
                                Room room = (Room) item;
                                sb.append(room.getKey() + "#" + room.getLocation() + "#" + room.getCount() + "#" + room.getPrice() + "#" + room.getReserved());
                            }

                            if (item instanceof Car) {
                                Car car = (Car) item;
                                sb.append(car.getKey() + "#" + car.getLocation() + "#" + car.getCount() + "#" + car.getPrice() + "#" + car.getReserved());
                            }

                            if (item instanceof Customer) {
                                Customer customer = (Customer) item;
                                int id = customer.getID();
                                RMHashMap reservations = customer.getReservations();
                                StringBuilder customer_sb = new StringBuilder();

                                ArrayList<ReservedItem> reservedItems = new ArrayList<ReservedItem>();
                                for (String reservedItem : reservations.keySet()) {
                                    reservedItems.add((ReservedItem) reservations.get(reservedItem));
                                }

                                for (int i = 0; i < reservedItems.size(); i++) {
                                    ReservedItem reserved = reservedItems.get(i);
                                    customer_sb.append(
                                        reserved.getKey() + "#" + reserved.getLocation() + "#" + reserved.getCount() + "#" + reserved.getPrice()
                                    );

                                    if (i != reservedItems.size() - 1) customer_sb.append("/");
                                }

                                sb.append(customer.getKey() + "%" + id + "%" + customer_sb.toString());
                            }
                        }
                        sb.append(";");
                    }
                    sb.append("\n");
                }

                bw.write(sb.toString());
                bw.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }  
    }

    public void recoverLocalHistory()
    {
        try {
            System.out.println("ATTENTION: RecoverLocalHistory is reading master record");

            File local_history_file = new File("local_history_" + m_name + ".txt");
            local_history_file.createNewFile();
            BufferedReader br = new BufferedReader(new FileReader(local_history_file));
            String line = null;

            if (m_name.equals("Flights")) {

                while ((line = br.readLine()) != null) {
                    if (line.length() > 0) {
                        System.out.println("DEBUG: Reading line: " + line);
                        String[] record = line.trim().split("\\:");
                        int key = Integer.parseInt(record[0]);
                        RMHashMap new_map = new RMHashMap();
                        
                        if (record.length > 1) {

                            String[] list_items = record[1].split("\\;");

                            for (String item : list_items) {
                                System.out.println(item);
                            }

                            for (String item : list_items) {

                                if (item.length() > 0) {

                                    String[] data = item.split("\\$");
                                    String key_data = data[0];
                                    String[] details = data[1].split("\\#");
                                    Flight f = new Flight(Integer.parseInt(details[1]), Integer.parseInt(details[2]), Integer.parseInt(details[3]));
                                    f.setReserved(Integer.parseInt(details[4]));
                                    new_map.put(key_data,f);
                                }
                            }
                        }
                        local.put(key, new_map);
                    }
                }
            } 

            // Rooms
            if (m_name.equals("Rooms")) {

                while ((line = br.readLine()) != null) {
                    if (line.length() > 0) {
                        System.out.println("DEBUG: Reading line: " + line);
                        String[] record = line.trim().split("\\:");
                        int key = Integer.parseInt(record[0]);
                        RMHashMap new_map = new RMHashMap();

                        if (record.length > 1) {

                            String[] list_items = record[1].split("\\;");

                            for (String item : list_items) {

                                if (item.length() > 0) {

                                    String[] data = item.split("\\$");
                                    String key_data = data[0];
                                    String[] details = data[1].split("\\#");
                                    Room r = new Room(details[1], Integer.parseInt(details[2]), Integer.parseInt(details[3]));
                                    r.setReserved(Integer.parseInt(details[4]));
                                    new_map.put(key_data,r);
                                }
                            }
                        }
                        local.put(key, new_map);
                    }
                }
            } 

            // Cars
            if (m_name.equals("Cars")) {

                while ((line = br.readLine()) != null) {
                    if (line.length() > 0) {
                        System.out.println("DEBUG: Reading line: " + line);
                        String[] record = line.trim().split("\\:");
                        int key = Integer.parseInt(record[0]);
                        RMHashMap new_map = new RMHashMap();

                        if (record.length > 1) {

                            String[] list_items = record[1].split("\\;");

                            for (String item : list_items) {

                                if (item.length() > 0) {
    
                                    String[] data = item.split("\\$");
                                    String key_data = data[0];
                                    String[] details = data[1].split("\\#");
                                    Car c = new Car(details[1], Integer.parseInt(details[2]), Integer.parseInt(details[3]));
                                    c.setReserved(Integer.parseInt(details[4]));
                                    new_map.put(key_data,c);
                                }
                            }
                        }
                        local.put(key, new_map);
                    }
                }
            }

            // Customers
            if (m_name.equals("Customers")) {

                while ((line = br.readLine()) != null) {
                    if (line.length() > 0) {
                        System.out.println("DEBUG: Reading line: " + line);
                        String[] record = line.trim().split("\\:");
                        int key = Integer.parseInt(record[0]); // xid
                        RMHashMap new_map = new RMHashMap();
                        
                        if (record.length > 1) {

                            String[] customers = record[1].split("\\;");

                            for (String customer : customers) {

                                if (customer.length() > 0) {

                                    String[] data = customer.split("\\$");
                                    String customer_key = data[0];
                                    
                                    if (data.length > 1) {

                                        String item_data = data[1];
                                        String[] item_data_details = item_data.split("\\%");
    
                                        String customer_key2 = item_data_details[0];
                                        int customer_id = Integer.parseInt(item_data_details[1]);

                                        Customer c = new Customer(customer_id);

                                        if (item_data_details.length > 2) {

                                            String[] reserveds = item_data_details[2].split("\\/");

                                            for (String reserved : reserveds) {

                                                if (reserved.length() > 0) {

                                                    String[] rs = reserved.split("\\#");
                                                    int reserve_count = Integer.parseInt(rs[2]);
                                                    while (reserve_count > 0) {
                                                        c.reserve(rs[0], rs[1], Integer.parseInt(rs[3]));
                                                        reserve_count--;
                                                    }
                                                }   
                                            }
                                        }
                                        new_map.put(customer_key,c);
                                    }
                                }     
                            }
                        }
                        local.put(key,new_map);
                    }
                }
            }

            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetCrashes() throws RemoteException 
    {     
        for (int i = 1; i <= 5; i++) {
            crashes.put(i, false);
        }
        System.out.println("ATTENTION: Participant crashes reset");
        return;
    }
   
    public void crashMiddleware(int mode) throws RemoteException
    {
        return; // do nothing
    }
   
    public void crashResourceManager(String name, int mode) throws RemoteException
    {   
        crashes.put(mode, true);
        System.out.println("ATTENTION: Participant[" + name + "] set crash for mode" + mode);
        return;
    }

    // Reads a data item
	protected RMItem readData(int xid, String key) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
    {   
        synchronized(local) {
            synchronized(m_data) {

                try {
                    Lock(xid, key, TransactionLockObject.LockType.LOCK_READ);
                }
                catch (DeadlockException deadlock) {
                    throw deadlock;
                }

                // Get the local history for the transaction
                RMHashMap local_data = local.get(xid);
                if (local_data == null)
                {
                    throw new InvalidTransactionException(xid,"Cannot read data for a non-existent transaction xid");
                }
                else
                {
                    // Check if local history already contains the item
                    if (local_data.containsKey(key)) {
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
                    else {
                        // otherwise, check the main memory
                        RMItem item = m_data.get(key);
                        if (item != null) {
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
	}

	// Writes a data item
	protected void writeData(int xid, String key, RMItem value) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
    {   
        synchronized(local) {
            try {
                Lock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
            }
            catch (DeadlockException deadlock) {
                throw deadlock;
            }
        
            // Get the local history for the transaction
            RMHashMap local_data = local.get(xid);
            if (local_data == null)
            {
                throw new InvalidTransactionException(xid,"Cannot write data for a non-existent transaction xid");
            }
            
            local_data.put(key, value);
            local.put(xid, local_data); // update the hashmap of local histories
        }
	}

	// Remove the item out of storage
	protected void removeData(int xid, String key) throws DeadlockException, InvalidTransactionException, TransactionAbortedException
	{   
        synchronized(local) {
            try {
                Lock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
            }
            catch (DeadlockException deadlock) {
                throw deadlock;
            }
        
            // Get the local history for the transaction
            RMHashMap local_data = local.get(xid);
            if (local_data == null) // Transaction doesn't exist
            {
                throw new InvalidTransactionException(xid,"Cannot write data for a non-existent transaction xid");
            }

            local_data.put(key, null);
            local.put(xid, local_data); // update the hashmap of local histories
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

     // Exits the server
     public void shutdown() throws RemoteException
     {   
         // Do nothing
     }

     // Function to record commit/abort decision
    public void recordDecision(int xid, boolean commit) 
    {   
        try {
            File record_file = new File("rm_records_" + m_name + ".txt");
            record_file.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(record_file, true));
            String record = xid + ":" + (commit? "COMMIT" : "ABORT");
            bw.write(record);
            bw.newLine();
            bw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Function to record YES vote
    public void recordYes(int xid) 
    {   
        try {
            File record_file = new File("rm_records_" + m_name + ".txt");
            record_file.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(record_file, true));
            String record = xid + ":" + "YES";
            bw.write(record);
            bw.newLine();
            bw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Function to handle vote request failure
    public void vote_failure_handler(int xid) 
    {   
        try {
            abort(xid);
            recordDecision(xid, false); // write ABORT to log
        }
        catch (InvalidTransactionException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        return;
    }
}
 
