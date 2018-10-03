package Server.TCP;

import java.util.*;
import java.io.*;
import java.net.*;
import Server.Interface.*;
import Server.Common.*;
import Server.Actions.*;

public class TCPMiddleware {

    private static String s_serverName = "TCPMiddleware";
    private static String  s_rmiPrefix = "group32";
    private static int s_serverPort= 2127;
    private static int s_serverPort_flight = 2124;
    private static int s_serverPort_car = 2125;
    private static int s_serverPort_room = 2126;
    private static int s_serverPort_customer = 2128;
    private static String flightServerName = "Flights";
    private static String carServerName = "Cars";
    private static String roomServerName = "Rooms";
    private static String customerServerName = "Customers";

    public static void main(String args[])
    {
        // Set the server host names based on arguments
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
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java server.Middleware [server_hostname [server_rmiobject]]");
            System.exit(1);
        }
        
        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }

        // Initiate Middleware
        try {
            TCPMiddleware server = new TCPMiddleware(s_serverName);
            server.start();
            System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start()
    {   
        ServerSocket ss = null;

        // Initiate server socket
        try {
            ss = new ServerSocket(s_serverPort);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        
        while (true)
        {
            Socket s_client = null;

            try {
                s_client = ss.accept();
                ObjectInputStream in_client = new ObjectInputStream(s_client.getInputStream());
                ObjectOutputStream out_client = new ObjectOutputStream(s_client.getOutputStream());

                Thread t = new Thread() {

                    @Override
                    public void run() {
 
                    // Handle action
                    try {
                        // Initiate client responses
                        //Boolean res_client = null;
                        //Integer res_client_ = null;

                        // Incoming action
                        TravelAction req = (TravelAction) in_client.readObject();

                        switch (req.getType()) {

                            case FLIGHT_ACTION:

                                Socket s_flight = new Socket(flightServerName, s_serverPort_flight);
                                singleRmRequest(req, out_client, s_flight);
                                s_flight.close();
                                break;

                            case CAR_ACTION:

                                Socket s_car = new Socket(carServerName, s_serverPort_car);
                                singleRmRequest(req, out_client, s_car);
                                s_car.close();
                                break;

                            case ROOM_ACTION:

                                Socket s_room = new Socket(roomServerName, s_serverPort_room);
                                singleRmRequest(req, out_client, s_room);
                                s_room.close();
                                break;

                            case CUSTOMER_ACTION:

                                multipleRmRequest(req, out_client);
                                break;

                            default:
                                break;
                        }
                        /*
                        if (res_client != null) {
                            out_client.writeObject(res_client); 
                        }
                        else if (res_client_ != null) {
                            out_client.writeObject(res_client_);
                        }
                        else {
                            out_client.writeObject(new String("NULL"));
                        }
                        */
                        out_client.flush();
                    }
                    catch (IOException e) {
                    e.printStackTrace();
                    }
                    catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    }
                    catch (Exception e) {
                    e.printStackTrace();
                    }
                    }
                };

                t.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + "[0mUncaught exception");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    // Function to handle request to one RM
    public static void singleRmRequest(TravelAction req, ObjectOutputStream dest, Socket s) 
    { 
        try {
            ObjectInputStream in = new ObjectInputStream(s.getInputStream()); 
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());

            // Send request to RM
            out.writeObject(req);
            out.flush();
        
            // Relay RM response to Client
            dest.writeObject(in.readObject());
            dest.flush();

            in.close();
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Function to handle request to multiple RMs
    public static void multipleRmRequest(TravelAction req, ObjectOutputStream dest) 
    { 
        try {
            Integer price = null; 

            Socket s_customerRm = null;
            ObjectInputStream in_customerRm = null;
            ObjectOutputStream out_customerRm = null;

            switch (req.getSubtype()) {

                case DELETE_CUSTOMER:
                    // Customer RM
                    s_customerRm = new Socket(customerServerName, s_serverPort_customer);
                    in_customerRm = new ObjectInputStream(s_customerRm.getInputStream());
                    out_customerRm = new ObjectOutputStream(s_customerRm.getOutputStream());

                    // Flight RM
                    Socket sf = new Socket(flightServerName, s_serverPort_flight);
                    ObjectInputStream inf = new ObjectInputStream(sf.getInputStream());
                    ObjectOutputStream outf = new ObjectOutputStream(sf.getOutputStream());

                    // Car RM
                    Socket sc = new Socket(carServerName, s_serverPort_car);
                    ObjectInputStream inc = new ObjectInputStream(sc.getInputStream());
                    ObjectOutputStream outc = new ObjectOutputStream(sc.getOutputStream());

                    // Room RM
                    Socket sr = new Socket(roomServerName, s_serverPort_room);
                    ObjectInputStream inr = new ObjectInputStream(sr.getInputStream());
                    ObjectOutputStream outr = new ObjectOutputStream(sr.getOutputStream());

                    ArrayList<ReservedItem> items = new ArrayList<ReservedItem>();
                    
                    out_customerRm.writeObject(
                        new DeleteCustomerAction( 
                            ((DeleteCustomerAction) req).getXid(),
                            ((DeleteCustomerAction) req).getCustomerID()
                        )
                    );
                    out_customerRm.flush();

                    items = (ArrayList<ReservedItem>) in_customerRm.readObject();

                    // Update items in respective RMs
                    for (ReservedItem item : items) {
                        
                        String key = item.getKey().toLowerCase();
                        int count = item.getCount();
                        String[] parts = key.split("-");
                        Boolean deleted = null;

                        

                        // Flight item
                        if (parts[0].equals("flight")) 
                        {
                            outf.writeObject(
                                new ReserveFlightRmAction(
                                    req.getXid(),
                                    Integer.parseInt(parts[1]),
                                    -count
                                )    
                            );
                            outf.flush();

                            deleted = (Boolean) inf.readObject();
                        }

                        // Car item
                        if (parts[0].equals("car"))
                        {
                            outc.writeObject(
                                new ReserveCarRmAction(
                                    req.getXid(),
                                    parts[1],
                                    -count
                                )
                            );
                            outc.flush();

                            deleted = (Boolean) inc.readObject();
                        }

                        // Room item
                        if (parts[0].equals("room"))
                        {
                            outr.writeObject(
                                new ReserveRoomRmAction(
                                    req.getXid(),
                                    parts[1],
                                    -count
                                )
                            );
                        }
                    }

                    dest.writeObject(new Boolean(true));
                    break;

                case RESERVE_FLIGHT_CUSTOMER_RM:

                    Socket s_flightRm = new Socket(flightServerName, s_serverPort_flight);
                    ObjectInputStream in_flightRm = new ObjectInputStream(s_flightRm.getInputStream());
                    ObjectOutputStream out_flightRm = new ObjectOutputStream(s_flightRm.getOutputStream());

                    s_customerRm = new Socket(customerServerName, s_serverPort_customer);
                    in_customerRm = new ObjectInputStream(s_customerRm.getInputStream());
                    out_customerRm = new ObjectOutputStream(s_customerRm.getOutputStream());

                    out_flightRm.writeObject(
                        new ReserveFlightRmAction(
                            ((ReserveFlightCustomerRmAction) req).getXid(),
                            ((ReserveFlightCustomerRmAction) req).getFlightNumber(),
                            1    
                        )
                    );
                    out_flightRm.flush();

                    price = (Integer) in_flightRm.readObject();

                    if (!price.equals(new Integer(-1))) {

                        out_customerRm.writeObject(
                            new ReserveFlightCustomerRmAction(
                                ((ReserveFlightCustomerRmAction) req).getXid(),
                                ((ReserveFlightCustomerRmAction) req).getCustomerID(),
                                ((ReserveFlightCustomerRmAction) req).getFlightNumber(),
                                (int) price
                            )
                        );

                        dest.writeObject(in_customerRm.readObject());
                    }
                    else {
                        dest.writeObject(new Boolean(false));
                    }

                    out_customerRm.flush();
                    dest.flush();

                    in_flightRm.close();
                    out_flightRm.close();
                    s_flightRm.close();

                    break;
                    
                case RESERVE_CAR_CUSTOMER_RM:

                    Socket s_carRm = new Socket(carServerName, s_serverPort_car);
                    ObjectInputStream in_carRm = new ObjectInputStream(s_carRm.getInputStream());
                    ObjectOutputStream out_carRm = new ObjectOutputStream(s_carRm.getOutputStream());

                    s_customerRm = new Socket(customerServerName, s_serverPort_customer);
                    in_customerRm = new ObjectInputStream(s_customerRm.getInputStream());
                    out_customerRm = new ObjectOutputStream(s_customerRm.getOutputStream());
                    
                    out_carRm.writeObject(
                        new ReserveCarRmAction(
                            ((ReserveCarCustomerRmAction) req).getXid(),
                            ((ReserveCarCustomerRmAction) req).getLocation(),
                            1    
                        )
                    );
                    out_carRm.flush();

                    price = (Integer) in_carRm.readObject();

                    if (!price.equals(new Integer(-1))) {

                        out_customerRm.writeObject(
                            new ReserveCarCustomerRmAction(
                                ((ReserveCarCustomerRmAction) req).getXid(),
                                ((ReserveCarCustomerRmAction) req).getCustomerID(),
                                ((ReserveCarCustomerRmAction) req).getLocation(),
                                (int) price
                            )
                        );

                        dest.writeObject(in_customerRm.readObject());
                    }
                    else {
                        dest.writeObject(new Boolean(false));
                    }

                    out_customerRm.flush();
                    dest.flush();

                    in_carRm.close();
                    out_carRm.close();
                    s_carRm.close();

                    break;
                    
                case RESERVE_ROOM_CUSTOMER_RM:

                    Socket s_roomRm = new Socket(roomServerName, s_serverPort_room);
                    ObjectInputStream in_roomRm = new ObjectInputStream(s_roomRm.getInputStream());
                    ObjectOutputStream out_roomRm = new ObjectOutputStream(s_roomRm.getOutputStream());

                    s_customerRm = new Socket(customerServerName, s_serverPort_customer);
                    in_customerRm = new ObjectInputStream(s_customerRm.getInputStream());
                    out_customerRm = new ObjectOutputStream(s_customerRm.getOutputStream());
                    
                    out_roomRm.writeObject(
                        new ReserveRoomRmAction(
                            ((ReserveRoomCustomerRmAction) req).getXid(),
                            ((ReserveRoomCustomerRmAction) req).getLocation(),
                            1    
                        )
                    );
                    out_roomRm.flush();

                    price = (Integer) in_roomRm.readObject();

                    if (!price.equals(new Integer(-1))) {

                        out_customerRm.writeObject(
                            new ReserveRoomCustomerRmAction(
                                ((ReserveRoomCustomerRmAction) req).getXid(),
                                ((ReserveRoomCustomerRmAction) req).getCustomerID(),
                                ((ReserveRoomCustomerRmAction) req).getLocation(),
                                (int) price
                            )
                        );

                        dest.writeObject(in_customerRm.readObject());
                    }
                    else {
                        dest.writeObject(new Boolean(false));
                    }

                    out_customerRm.flush();
                    dest.flush();

                    in_roomRm.close();
                    out_roomRm.close();
                    s_roomRm.close();
                    
                    break;
                    
                case RESERVE_BUNDLE_CUSTOMER_RM:
                    
                    int xid = ((ReserveBundleCustomerRmAction) req).getXid();
                    int customer = ((ReserveBundleCustomerRmAction) req).getCustomerID();
                    Vector<String> flights = ((ReserveBundleCustomerRmAction) req).getFlightNumbers();
                    String loc = ((ReserveBundleCustomerRmAction) req).getLocation();
                    boolean car = ((ReserveBundleCustomerRmAction) req).getCar();
                    boolean room = ((ReserveBundleCustomerRmAction) req).getRoom();

                    // Convert flight numbers from string format to integer format
                    ArrayList<Integer> flights_ = new ArrayList<Integer>();
                    for (String s : flights) flights_.add(Integer.parseInt(s));
                   
                    ArrayList<Integer> prices = new ArrayList<Integer>();
                    Integer carPrice = null;
                    Integer roomPrice = null;
                    boolean customerExists = true;
                    int len = flights.size();

                    if (len > 0)
                    {   
                        Socket s_f = new Socket(flightServerName, s_serverPort_flight);
                        ObjectInputStream in_f = new ObjectInputStream(s_f.getInputStream());
                        ObjectOutputStream out_f = new ObjectOutputStream(s_f.getOutputStream());

                        out_f.writeObject(
                            new ReserveFlightsRmAction(
                                xid, 
                                flights_, 
                                1
                            ) 
                        );
                        out_f.flush();

                        prices = (ArrayList<Integer>) in_f.readObject();

                        in_f.close();
                        out_f.close();
                        s_f.close();
                    }

                    if (car) 
                    {
                        Socket s_c = new Socket(carServerName, s_serverPort_car);
                        ObjectInputStream in_c = new ObjectInputStream(s_c.getInputStream());
                        ObjectOutputStream out_c = new ObjectOutputStream(s_c.getOutputStream());

                        out_c.writeObject(
                            new ReserveCarRmAction(
                                xid,
                                loc, 
                                1
                            )
                        );
                        out_c.flush();

                        carPrice = (Integer) in_c.readObject();

                        in_c.close();
                        out_c.close();
                        s_c.close();
                    }

                    if (room)
                    {
                        Socket s_r = new Socket(roomServerName, s_serverPort_room);
                        ObjectInputStream in_r = new ObjectInputStream(s_r.getInputStream());
                        ObjectOutputStream out_r = new ObjectOutputStream(s_r.getOutputStream());

                        out_r.writeObject(
                            new ReserveRoomRmAction(
                                xid,
                                loc,
                                1
                            )
                        );
                        out_r.flush();

                        roomPrice = (Integer) in_r.readObject();

                        in_r.close();
                        out_r.close();
                        s_r.close();
                    }

                    // Check if customer exists
                    out_customerRm.writeObject(
                        new QueryCustomerAction(xid, customer)
                    );
                    out_customerRm.flush();
                    customerExists = !(((String) in_customerRm.readObject()).isEmpty());

                    if (
                        (prices.size() != flights_.size()) || 
                        (car && carPrice.equals(new Integer(-1))) || 
                        (room && roomPrice.equals(new Integer(-1))) ||
                        customerExists == false
                    ) {
                        // Invalid bundle
                        dest.writeObject(new Boolean(false));
                        dest.flush();

                        // Reset
                        if (prices.size() == flights_.size()) 
                        {
                            Socket s_f = new Socket(flightServerName, s_serverPort_flight);
                            ObjectInputStream in_f = new ObjectInputStream(s_f.getInputStream());
                            ObjectOutputStream out_f = new ObjectOutputStream(s_f.getOutputStream());

                            out_f.writeObject(
                                new ReserveFlightsRmAction(
                                    xid, 
                                    flights_, 
                                    -1
                                ) 
                            );
                            out_f.flush();

                            prices = (ArrayList<Integer>) in_f.readObject();

                            in_f.close();
                            out_f.close();
                            s_f.close();
                        }

                        if (car && !carPrice.equals(new Integer(-1)))
                        {
                            Socket s_c = new Socket(carServerName, s_serverPort_car);
                            ObjectInputStream in_c = new ObjectInputStream(s_c.getInputStream());
                            ObjectOutputStream out_c = new ObjectOutputStream(s_c.getOutputStream());

                            out_c.writeObject(
                                new ReserveCarRmAction(
                                    xid,
                                    loc, 
                                    -1
                                )
                            );
                            out_c.flush();

                            carPrice = (Integer) in_c.readObject();

                            in_c.close();
                            out_c.close();
                            s_c.close();
                        }

                        if (room && !roomPrice.equals(new Integer(-1)))
                        {
                            Socket s_r = new Socket(roomServerName, s_serverPort_room);
                            ObjectInputStream in_r = new ObjectInputStream(s_r.getInputStream());
                            ObjectOutputStream out_r = new ObjectOutputStream(s_r.getOutputStream());

                            out_r.writeObject(
                                new ReserveRoomRmAction(
                                    xid,
                                    loc,
                                    -1
                                )
                            );
                            out_r.flush();

                            roomPrice = (Integer) in_r.readObject();

                            in_r.close();
                            out_r.close();
                            s_r.close();
                        }
                    }

                    // Update customer flights
                    out_customerRm.writeObject(
                        new ReserveFlightsCustomerRmAction(
                            xid,
                            customer,
                            flights_,
                            prices
                        )
                    );
                    out_customerRm.flush();
                    
                    // Update customer car
                    if (car)
                    {
                        out_customerRm.writeObject(
                            new ReserveCarCustomerRmAction(
                                xid, 
                                customer, 
                                loc,
                                (int) carPrice
                            )
                        );
                        out_customerRm.flush();
                    }
                    
                    // Update customer room
                    if (room)
                    {
                        out_customerRm.writeObject(
                            new ReserveRoomCustomerRmAction(
                                xid, 
                                customer, 
                                loc, 
                                roomPrice
                            )
                        );
                        out_customerRm.flush();
                    }

                    dest.writeObject(new Boolean(true));
                    dest.flush();
                    break;

                default:
                    break;
            }

            in_customerRm.close();
            out_customerRm.close();
            s_customerRm.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TCPMiddleware(String name)
    {
        super();
    }
}
