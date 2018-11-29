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
    protected Map<Integer,Boolean> crashes = new HashMap<Integer,Boolean>();

    private HashMap<Integer,Transaction> transactions = new HashMap<Integer,Transaction>();
    private HashMap<Integer,Timer> timers = new HashMap<Integer,Timer>();
    
    private long TRANSACTION_TIME_LIMIT = 120000;
    private long VOTE_REQUEST_TIME_LIMIT = 60000;
    private int count = 0;
    
    public Middleware(String p_name)
    {
        m_name = p_name;

        // Set crash modes
        for (int i = 1; i <= 8; i++) {
            crashes.put(i, false);
        }

        recovery();        
    }

    // Crash functions
    public void resetCrashes() throws RemoteException 
    {   
        for (int i = 1; i <= 7; i++) {
            crashes.put(i, false);
        }

        flightResourceManager.resetCrashes();
        roomResourceManager.resetCrashes();
        carResourceManager.resetCrashes();
        customerResourceManager.resetCrashes();
        return;
    }
   
    public void crashMiddleware(int mode) throws RemoteException
    {
        crashes.put(mode, true);
        return; 
    }
   
    public void crashResourceManager(String name, int mode) throws RemoteException
    {   
        if (name.equals("Flights")) {
            flightResourceManager.crashResourceManager(name, mode);
        }
        else if (name.equals("Rooms")) {
            roomResourceManager.crashResourceManager(name, mode);
        }
        else if (name.equals("Cars")) {
            carResourceManager.crashResourceManager(name, mode);
        }
        else if (name.equals("Customers")) {
            customerResourceManager.crashResourceManager(name, mode);
        }
        return;
    }

     /**
     * THE FOLLOWING INCORPORATE THE IMPLEMENTATION OF TRANSACTION MANAGEMENT
     * - START
     * - COMMIT
     * - ABORT
     * - SHUTDOWN
     */

    // Function to start transaction
    public int startTransaction(String client_id) throws RemoteException
    {   
        synchronized(transactions) {
            int xid = -1;
            int id = this.count++; //(int) new Date().getTime();
            xid = id < 0? -id : id;
            final int xid_t = xid;
            transactions.put(xid, new Transaction(xid,client_id));

            Timer t = new Timer();
            this.timers.put(xid, t);
            t.schedule(new TimerTask(){
            
                @Override
                public void run() {
                    try {
                        initiateAbort(xid_t);
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
            }, TRANSACTION_TIME_LIMIT);

            System.out.println("STARTED TRANSACTION AT middelware - " + xid);

            flightResourceManager.start(xid);
            carResourceManager.start(xid);
            roomResourceManager.start(xid);
            customerResourceManager.start(xid); 
            recordStartOfTransaction(xid);

            return xid;
        }
    }

    // Function to commit transaction
    public boolean commitTransaction(int xid) throws RemoteException,TransactionAbortedException,InvalidTransactionException
    {   
        synchronized(transactions) {
            synchronized (timers) {

                HashSet<RESOURCE_MANAGER_TYPE> set = new HashSet<RESOURCE_MANAGER_TYPE>();

                if (!transactions.containsKey(xid)) {
                    throw new InvalidTransactionException(xid,"Cannot commit to a non-existent transaction xid from middleware)");
                }

                Transaction ts = transactions.get(xid);
                ArrayList<Operation> ops = ts.getOperations();

                // Find out relevant RM(s)
                for (Operation op : ops) {
                    ArrayList<RESOURCE_MANAGER_TYPE> rms = op.getResourceManagers();
                    for (RESOURCE_MANAGER_TYPE rm : rms) {
                        if (!set.contains(rm)) set.add(rm);

                        switch(rm) {
                            case FLIGHT:
                                System.out.println("Middlware interested in flight rm");
                                break;
                            default:
                                break;
                        }
                    }
                }

                // Start of 2PC
                ArrayList<String> record_rms = new ArrayList<String>();
                for (RESOURCE_MANAGER_TYPE rm : set) {
                    switch (rm) {
                        case FLIGHT:
                            record_rms.add("flights");
                            System.out.println("again, interested in flight rm, so save in record");
                            break;
                        case CAR:
                            record_rms.add("cars");
                            break;
                        case ROOM:
                            record_rms.add("rooms");
                            break;
                        case CUSTOMER:
                            record_rms.add("customers");
                            break;
                        default:
                            break;
                    }
                }

                recordStartOf2PC(xid, record_rms);

                // Crash mode 1
                if (crashes.get(1)) System.exit(1);

                /**
                 * Middelware failure handling implementation
                 * Case: Waiting for Votes
                 * Solution: Add a timer to keep track before sending any request and after receiving all requests 
                 */

                // Set VOTE_REQUEST timer
                Timer vote_timer = new Timer();
                vote_timer.schedule(new TimerTask(){
                    
                    @Override
                    public void run() {
                        vote_failure_handler(xid);
                    }
                }, VOTE_REQUEST_TIME_LIMIT);

                // Send vote request
                HashMap<RESOURCE_MANAGER_TYPE,Boolean> votes = new HashMap<RESOURCE_MANAGER_TYPE,Boolean>();
                boolean ack = false;

                for (RESOURCE_MANAGER_TYPE rm : set) {
                    int attempt_vote = 0;
                    while (attempt_vote < 2) {
                        try {
                            switch (rm) {
                                case FLIGHT:
                                    ack = flightResourceManager.prepare(xid);
                                    break;
                                case CAR:
                                    ack = carResourceManager.prepare(xid);
                                    break;
                                case ROOM:
                                    ack = roomResourceManager.prepare(xid);
                                    break;
                                case CUSTOMER:
                                    ack = customerResourceManager.prepare(xid);
                                    break;
                                default:
                                    break;
                            }
                            votes.put(rm, ack);
                            attempt_vote = 2;
                        }
                        catch (RemoteException e) {
                            System.out.println("Exception caught: remote exception at vote request (attempt again in 60s).");
                            votes.put(rm, false);
                            attempt_vote++;
                            if (attempt_vote <= 1) {
                                try {
                                    Thread.sleep(60000);
                                }
                                catch (InterruptedException e2) {
                                    e2.printStackTrace();
                                }
                            }
                        }
                    }
                }

                // Crash mode 3
                if (crashes.get(3)) System.exit(1);

                /* At this point, all votes are received */
                // Cancel VOTE_REQUEST timer
                vote_timer.cancel();

                if (transactions.containsKey(xid)) { // if not found, it means vote_failure_handler removed it already
                    
                     // Crash mode 2
                    if (crashes.get(2)) System.exit(1);

                    // Verify vote request
                    boolean canCommit = true;
                    for (RESOURCE_MANAGER_TYPE rm : votes.keySet()) {
                        boolean vote = votes.get(rm);
                        if (!vote) {
                            canCommit = false;
                            break;
                        }
                    }

                    // Crash mode 4
                    if (crashes.get(4)) System.exit(1);

                    // Middelware records approval or decline vote
                    recordMiddlewareDecision(xid, canCommit);

                    // Crash mode 5
                    if (crashes.get(5)) System.exit(1);

                    // Commit or Abort based on vote request
                    HashMap<RESOURCE_MANAGER_TYPE,Boolean> completed = new HashMap<RESOURCE_MANAGER_TYPE,Boolean>();
                    ack = false;
                    if (canCommit) {
                        for (RESOURCE_MANAGER_TYPE rm : set) {
                            int attempt_commit = 0;
                            while (attempt_commit < 2) {
                                try {
                                    switch (rm)
                                    {
                                        case FLIGHT:
                                            ack = flightResourceManager.commit(xid);
                                            break;
                                        case CAR:
                                            ack = carResourceManager.commit(xid);
                                            break;
                                        case ROOM:
                                            ack = roomResourceManager.commit(xid);
                                            break;
                                        case CUSTOMER:
                                            ack = customerResourceManager.commit(xid);
                                            break;
                                        default:
                                            break;
                                    }
                                    completed.put(rm, ack);
                                    attempt_commit = 2;
                                    // Crash mode 6 
                                    if (crashes.get(6)) System.exit(1);
                                }
                                catch (RemoteException e) {
                                    System.out.println("Exception caught: remote exception at commit (attempt again in 60s).");
                                    completed.put(rm, false);
                                    attempt_commit++;
                                    if (attempt_commit <= 1) {
                                        try {
                                            Thread.sleep(60000);
                                        }
                                        catch (InterruptedException e2) {
                                            e2.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        for (RESOURCE_MANAGER_TYPE rm : votes.keySet()) {   
                            if (votes.get(rm)) {   
                                int attempt_abort = 0;
                                while (attempt_abort < 2) {
                                    try {
                                        switch (rm) {
                                            case FLIGHT:
                                                ack = flightResourceManager.abort(xid);
                                                break;
                                            case CAR:
                                                ack = carResourceManager.abort(xid);
                                                break;
                                            case ROOM:
                                                ack = roomResourceManager.abort(xid);
                                                break;
                                            case CUSTOMER:
                                                ack = customerResourceManager.abort(xid);
                                                break;
                                            default:
                                        }
                                        completed.put(rm,ack);
                                        attempt_abort = 2;
                                    }
                                    catch (RemoteException e) {
                                        System.out.println("Exception caught: remote exception at abort (attempt again in 60s).");
                                        completed.put(rm, false);
                                        attempt_abort++;
                                        if (attempt_abort <= 1) {
                                            try {
                                                Thread.sleep(60000);
                                            }
                                            catch (InterruptedException e2) {
                                                e2.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Crash mode 7
                    if (crashes.get(7)) System.exit(1);

                    this.timers.get(xid).cancel();
                    this.timers.remove(xid); 
                    
                    this.transactions.remove(xid);
                    recordEndOfTransaction(xid);
                    return true;
                }
                return false;
            }
        }
    }

    // Function to record start of transaction
    public void recordStartOfTransaction(int xid) 
    {   
        try {
            File middleware_records = new File("middleware_records_" + m_name + ".txt");
            middleware_records.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(middleware_records, true));
            String record = xid + ":" + "S_O_T";
            bw.write(record);
            bw.newLine();
            bw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Function to record start of 2PC
    public void recordStartOf2PC(int xid, ArrayList<String> resourceManagers) 
    {   
        try {
            File middleware_records = new File("middleware_records_" + m_name + ".txt");
            middleware_records.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(middleware_records, true));
            String record = xid + ":" + "S_O_2PC" + ":";
            
            for (int i = 0; i < resourceManagers.size(); i++) {
                record += resourceManagers.get(i);
                if (i != resourceManagers.size() - 1) record += ";";
            }

            bw.write(record);
            bw.newLine();
            bw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Function to record Middleware decision
    public void recordMiddlewareDecision(int xid, boolean canCommit) 
    {   
        try {
            File middleware_records = new File("middleware_records_" + m_name + ".txt");
            middleware_records.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(middleware_records, true));
            String record = xid + ":" + "MW_DEC" + ":" + (canCommit? "COMMIT" : "ABORT");
            bw.write(record);
            bw.newLine();
            bw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Function to record end of transaction
    public void recordEndOfTransaction(int xid)
    {   
        try {
            File middleware_records = new File("middleware_records_" + m_name + ".txt");
            middleware_records.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(middleware_records, true));
            String record = xid + ":" + "E_O_T";
            bw.write(record);
            bw.newLine();
            bw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Function to recovery from log file
    public void recovery()
    {
        /**
         * Format of record messages:
         * 
         * 1. Start of transaction
         * - xid:S_O_T
         * 
         * 2. Start of 2PC
         * - xid:S_O_2PC:[list of resource managers separated by ';']
         * 
         * 3. Middleware decision
         * - xid:MW_DEC:[COMMIT/ABORT]
         * 
         * 4. End of transaction
         * - xid:E_O_T
         */

        // Log information 
        Set<Integer> s_o_t = new HashSet<Integer>();
        Set<Integer> e_o_t = new HashSet<Integer>();
        Map<Integer,ArrayList<String>> s_o_2pc = new HashMap<Integer,ArrayList<String>>();
        Map<Integer,Boolean> mw_dec = new HashMap<Integer,Boolean>(); // true -> commit, false -> abort

        // Read from Middleware log
        try {
            File middleware_records = new File("middleware_records_" + m_name + ".txt");
            middleware_records.createNewFile();
            BufferedReader br = new BufferedReader(new FileReader(middleware_records));
            String line = null;

            while ((line = br.readLine()) != null) {
                if (line.length() > 0) {
                    String[] record = line.trim().split(":");
                    int xid = Integer.parseInt(record[0]);
                    String record_type = record[1];

                    switch (record_type) {

                        case "S_O_T":
                            s_o_t.add(xid);
                            break;
                        case "S_O_2PC":
                            ArrayList<String> list = new ArrayList<String>();
                            if (record != null && record[2].length() > 0) {
                                String[] rms = record[2].split(";");
                                for (String rm : rms) list.add(rm);
                            }
                            s_o_2pc.put(xid,list);
                            break;
                        case "MW_DEC":
                            if (record[2].equals("COMMIT")) mw_dec.put(xid,true);
                            else mw_dec.put(xid,false);
                            break;
                        case "E_O_T":
                            e_o_t.add(xid);
                            break;
                        default:
                            break;
                    }
                }
            }

            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Attempt communication again, if necessary
        // Phase between START and 2PC
        for (Integer xid : s_o_t) {
            
            if (!s_o_2pc.containsKey(xid)) {

                try {
                    flightResourceManager.abort(xid);
                }
                catch (InvalidTransactionException e) {
                    System.out.println("Error caught: transaction " + xid + " already removed in flight resource manager.");
                }
                catch (RemoteException e) {
                    System.out.println("Exception caught: remote. Deteted during middleware recovery");
                }

                try {
                    roomResourceManager.abort(xid);
                }
                catch (InvalidTransactionException e) {
                    System.out.println("Error caught: transaction " + xid + " already removed in room resource manager.");
                }
                catch (RemoteException e) {
                    System.out.println("Exception caught: remote. Deteted during middleware recovery");
                }

                try {
                    carResourceManager.abort(xid);
                }
                catch (InvalidTransactionException e) {
                    System.out.println("Error caught: transaction " + xid + " already removed in car resource manager.");
                }
                catch (RemoteException e) {
                    System.out.println("Exception caught: remote. Deteted during middleware recovery");
                }

                try {
                    customerResourceManager.abort(xid);
                }
                catch (InvalidTransactionException e) {
                    System.out.println("Error caught: transaction " + xid + " already removed in customer resource manager.");
                }
                catch (RemoteException e) {
                    System.out.println("Exception caught: remote. Deteted during middleware recovery");
                }

                s_o_t.remove(xid);
            }
        }

        // Phase between 2PC and END
        for (Integer xid : s_o_2pc.keySet()) {
            // Transaction not ended
            if (!e_o_t.contains(xid)) {

                List<String> rms = s_o_2pc.get(xid);

                // Middleware already has a decision
                if (mw_dec.containsKey(xid)) {
                    
                    // Commit
                    if (mw_dec.get(xid)) {
                        
                        if (!e_o_t.contains(xid)) {
                            for (String rm : rms) {
                                try {
                                    System.out.println(rm);
                                    switch (rm) {
                                        
                                        case "flights":
                                            flightResourceManager.commit(xid);
                                            break;
                                        case "rooms":
                                            roomResourceManager.commit(xid);
                                            break;
                                        case "cars":
                                            carResourceManager.commit(xid);
                                            break;
                                        case "customers":
                                            customerResourceManager.commit(xid);
                                            break;
                                        default:
                                            break;
                                    }
                                }
                                catch (RemoteException e) {
                                    System.out.println("Exception caught: remote issue found during middleware recovery commit.");
                                }
                                catch (InvalidTransactionException e) {
                                    System.out.println("Exception caught: invalid transaction id issue found during middleware recovery commit.");
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {
                            e_o_t.remove(xid);
                        }
                    }
                    // abort
                    else {
                        for (String rm : rms) {
                            try {
                                switch (rm) {
                                    case "flights":
                                        flightResourceManager.abort(xid);
                                        break;
                                    case "rooms":
                                        roomResourceManager.abort(xid);
                                        break;
                                    case "cars":
                                        carResourceManager.abort(xid);
                                        break;
                                    case "customers":
                                        customerResourceManager.abort(xid);
                                        break;
                                    default:
                                        break;
                                }
                            }
                            catch (RemoteException e) {
                                System.out.println("Exception caught: remote issue found during middleware recovery abort.");
                            }
                            catch (InvalidTransactionException e) {
                                System.out.println("Exception caught: invalid transaction id issue found during middleware recovery abort.");
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    s_o_t.remove(xid);
                    s_o_2pc.remove(xid);
                    mw_dec.remove(xid);
                }
                // Middleware does not have decision yet
                else {
                    for (String rm : rms) {
                        try {
                            switch (rm) {
                                case "flights":
                                    flightResourceManager.abort(xid);
                                    break;
                                case "rooms":
                                    roomResourceManager.abort(xid);
                                    break;
                                case "cars":
                                    carResourceManager.abort(xid);
                                    break;
                                case "customers":
                                    customerResourceManager.abort(xid);
                                    break;
                                default:
                                    break;
                            }
                        }
                        catch (RemoteException e) {
                            System.out.println("Exception caught: remote issue found during middleware recovery abort.");
                        }
                        catch (InvalidTransactionException e) {
                            System.out.println("Exception caught: invalid transaction id issue found during middleware recovery abort.");
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    s_o_t.remove(xid);
                    s_o_2pc.remove(xid);
                }
            }
        }

        // Crash mode 8
        if (crashes.get(8)) System.exit(1);

        // Garbage collection
        try {
            File middleware_records = new File("middleware_records_" + m_name + ".txt");
            middleware_records.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(middleware_records, false));
            StringBuilder sb = new StringBuilder();

            for (Integer xid : s_o_t) {
                
                if (!e_o_t.contains(xid)) {

                    // Start of transaction
                    sb.append(xid + ":" + "S_O_T");
                    sb.append("\n");
                    
                    // Start of 2PC
                    if (s_o_2pc.containsKey(xid)) {
                        sb.append(xid + ":" + "S_O_2PC" + ":");
                        List<String> list = s_o_2pc.get(xid);
                        for (int i = 0; i < list.size(); i++) {
                            sb.append(list.get(i));
                            if (i != list.size() - 1) sb.append(";");
                        }
                    } 
                    sb.append("\n");

                    // MW decision
                    if (mw_dec.containsKey(xid)) {
                        sb.append(xid + ":" + "MW_DEC" + ":" + (mw_dec.get(xid)? "COMMIT" : "ABORT"));
                        sb.append("\n");
                    }
                }
            }
            
            bw.write(sb.toString());
            bw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Function to handle vote failure
    public void vote_failure_handler(int xid) 
    {   
        synchronized(transactions) {
            synchronized(timers) {

                Timer t = timers.get(xid);
                t.cancel();
                timers.remove(t);

                Transaction ts = this.transactions.get(xid);
                ArrayList<Operation> ops = ts.getOperations();
                HashSet<RESOURCE_MANAGER_TYPE> set = new HashSet<RESOURCE_MANAGER_TYPE>();

                for (Operation op : ops) {
                    ArrayList<RESOURCE_MANAGER_TYPE> rms = op.getResourceManagers();
                    for (RESOURCE_MANAGER_TYPE rm : rms) {
                        if (!set.contains(rm)) set.add(rm);
                    }
                }

                for (RESOURCE_MANAGER_TYPE rm : set) {
                    try {
                        switch(rm) {
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
                    catch (InvalidTransactionException e) {
                        e.printStackTrace();
                    }
                    catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                
                transactions.remove(xid);
                recordEndOfTransaction(xid);
            }
        }
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
        synchronized(this.transactions) {   
            for (Integer xid : transactions.keySet())
            {
                if (this.transactions.get(xid).getClientId().equals(client_id)) 
                {
                    return false;
                }
            }
            return true;
        }
    }

    // Function to initiate abort
    public boolean initiateAbort(int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException
    {  
        synchronized(transactions) {
            synchronized(this.timers) {

                HashSet<RESOURCE_MANAGER_TYPE> set = new HashSet<RESOURCE_MANAGER_TYPE>();
                this.timers.get(xid).cancel();
                this.timers.remove(xid);

                Transaction ts = transactions.get(xid);
                ArrayList<Operation> ops = ts.getOperations();

                for (Operation op : ops) {
                    ArrayList<RESOURCE_MANAGER_TYPE> rms = op.getResourceManagers();
    
                    for (RESOURCE_MANAGER_TYPE rm : rms)
                    {
                        if (!set.contains(rm)) set.add(rm);
                    }
                }
    
                transactions.remove(xid);

                for (RESOURCE_MANAGER_TYPE rm : set) {
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

                return true;
            }
        }       
    }

    // Function to add operation and to update timer for a transaction
    public void updateTransaction(int xid, ArrayList<RESOURCE_MANAGER_TYPE> rms) throws RemoteException, InvalidTransactionException, TransactionAbortedException
    {   
        synchronized(transactions) {
            synchronized(timers) {
                if (!transactions.containsKey(xid) || !timers.containsKey(xid)) 
                {
                    throw new InvalidTransactionException(xid,"Cannot identify the transaction xid by transaction manager");
                }

                Transaction ts = transactions.get(xid);
                Timer t = timers.get(xid);

                ts.addOperation(new Operation(rms));
                t.schedule(new TimerTask() {
                
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
                }, TRANSACTION_TIME_LIMIT);
            }
        }
    }

    //====================================================================================================
    //====================================================================================================

    /**
     * THE FOLLOWING INCORPORATE THE IMPLEMENTATION OF MILESTONE 1 API
     */

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
            flightResourceManager.abort(e.getXId());
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
            carResourceManager.abort(e.getXId());
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
            roomResourceManager.abort(e.getXId());
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
            customerResourceManager.abort(e.getXId());
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
            carResourceManager.abort(e.getXId());
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
            roomResourceManager.abort(e.getXId());
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
            flightResourceManager.abort(e.getXId());
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
            carResourceManager.abort(e.getXId());
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
            roomResourceManager.abort(e.getXId());
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
            flightResourceManager.abort(e.getXId());
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
            carResourceManager.abort(e.getXId());
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
            roomResourceManager.abort(e.getXId());
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
            customerResourceManager.abort(e.getXId());
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
            customerResourceManager.abort(e.getXId());
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
            customerResourceManager.abort(e.getXId());
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
            customerResourceManager.abort(e.getXId());
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
            flightResourceManager.abort(e.getXId());
            customerResourceManager.abort(e.getXId());
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
            carResourceManager.abort(e.getXId());
            customerResourceManager.abort(e.getXId());
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
            roomResourceManager.abort(e.getXId());
            customerResourceManager.abort(e.getXId());
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
            flightResourceManager.abort(e.getXId());
            if (car) carResourceManager.abort(e.getXId());
            if (room) roomResourceManager.abort(e.getXId());
            customerResourceManager.abort(e.getXId());
            System.out.println("Exception caught: Middleware bundle catches deadlock Exception"); //e.printStackTrace();
        }

         return false;
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
    public boolean abort(int xid) throws RemoteException, InvalidTransactionException
    {
        return false;
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

    public boolean prepare(int xid) throws RemoteException, InvalidTransactionException
    {
        return false;
    }
}

