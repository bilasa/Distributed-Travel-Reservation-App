package Server.TCP;

import java.util.*;
import java.io.*; 
import java.util.*;
import java.net.*;
import Server.Interface.*;
import Server.Actions.*;
import Server.Common.*;

public class TCPResourceManager extends ResourceManager 
{
	private static String s_serverName = "Server";
    private static String s_rmiPrefix = "group32";
    private static int s_serverPort = 1099;

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverName = args[0];
        }
        
        // Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
			
		// Create the RMI server entry
		try {
			// Create a new Server object
            TCPResourceManager server = new TCPResourceManager(s_serverName);
            System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
            
            // Initialize server socket
            ServerSocket ss = new ServerSocket(s_serverPort);

            // Listen to incoming requests
            while (true)
            {   
                // Socket and stream objects to an incoming request
                Socket s = null;
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());

                Boolean res = null;
                Integer res_ = null;

                // Initialize thread
                Thread t = new Thread() {

                    @Override
                    public void run() {

                        TravelAction req = (TravelAction) in.readObject();

                        switch (req.getSubtype()) {
                            
                            case ADD_FLIGHT:

                                res = Boolean(
                                    server.addFlight(
                                        ((AddFlightAction) req).getXid(),
                                        ((AddFlightAction) req).getFlightNumber(),
                                        ((AddFlightAction) req).getFlightSeats(),
                                        ((AddFlightAction) req).getFlightPrice()
                                    )
                                );

                                break;

                            case ADD_CAR_LOCATION:

                                res = Boolean(
                                    server.addCars(
                                        ((AddCarLocationAction) req).getXid(),
                                        ((AddCarLocationAction) req).getLocation(),
                                        ((AddCarLocationAction) req).getNumCars(),
                                        ((AddCarLocationAction) req).getPrice()
                                    )
                                );
                                break;

                            case ADD_ROOM_LOCATION:

                                // TODO
                                break;

                            case ADD_CUSTOMER:

                                // TODO
                                break;

                            case QUERY_FLIGHT:

                                res_ = Integer(
                                    server.queryFlight(
                                        ((QueryFlightAction) req).getXid(),
                                        ((QueryFlightAction) req).getFlightNumber()
                                    )
                                );
                                break;

                            case QUERY_CAR_LOCATION:

                                res_ = Integer(
                                    server.queryCars(
                                        ((QueryCarLocationAction) req).getXid(),
                                        ((QueryCarLocationAction) req).getLocation()
                                    )
                                );
                                break;

                            case QUERY_ROOM_LOCATION:

                                // TODO
                                break;

                            case QUERY_CUSTOMER:

                                // TODO
                                break;

                            case QUERY_FLIGHT_PRICE:

                                res_ = Integer(
                                    server.queryFlightPrice(
                                        ((QueryFlightPriceAction) req).getXid(),
                                        ((QueryFlightPriceAction) req).getFlightNumber()
                                    )
                                );
                                break;

                            case QUERY_CAR_PRICE:

                                res_ = Integer(
                                    server.queryCars(
                                        ((QueryCarPriceAction) req).getXid(),
                                        ((QueryCarPriceAction) req).getLocation()
                                    )
                                );
                                break;

                            case QUERY_ROOM_PRICE:

                                // TODO
                                break;

                            case DELETE_FLIGHT:

                                res = Boolean(
                                    server.deleteFlight(
                                        ((DeleteFlightAction) req).getXid(),
                                        ((DeleteFlightAction) req).getFlightNumber()
                                    )
                                );
                                break;

                            case DELETE_CAR_LOCATION:

                                res = Boolean(
                                    server.deleteCars(
                                        ((DeleteCarLocationAction) req).getXid(),
                                        ((DeleteCarLocationAction) req).getLocation()
                                    )
                                );
                                break;

                            case DELETE_ROOM_LOCATION:

                                // TODO
                                break;

                            case DELETE_CUSTOMER:

                                // TODO
                                break;

                            case RESERVE_FLIGHT:

                                res = Boolean(
                                    server.reserveFlight(
                                        ((ReserveFlightAction) req).getXid(),
                                        ((ReserveFlightAction) req).getCustomerID(),
                                        ((ReserveFlightAction) req).getFlightNumber()
                                    )
                                );
                                break;

                            case RESERVE_CAR: 

                                res = Boolean(
                                    server.reserveCar(
                                        ((ReserveCarAction) req).getXid(),
                                        ((ReserveCarAction) req).getCustomerID(),
                                        ((ReserveCarAction) req).getLocation()
                                    )
                                );
                                break;  

                            case RESERVE_ROOM:

                                // TODO 
                                break;

                            case RESERVE_BUNDLE:

                                // TODO
                                break;

                            default: 
                                break;
                        }
                    }
                };

                if (res != null) {
                    out.writeObject(res);
                    out.flush();
                }
                else if (res_ != null) {
                    out.writeObject(res_);
                    out.flush();
                }
                else {
                    out.writeObject(new String("NULL"));
                    out.flush();
                }
            }
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public TCPResourceManager(String name)
	{
		super(name);
	}
}