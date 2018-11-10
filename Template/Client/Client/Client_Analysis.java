package Client;

import Server.Interface.*;
import Server.Common.*;
import java.util.*;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.ConnectException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;

public abstract class Client_Analysis
{
	IResourceManager m_resourceManager = null;
    String client_id = null;
	Integer iterations = null;
	Integer delay = null;
	HashMap<Integer,Long> stamps;
	
	int xid_ = 0;
	int rmid = 0;
	int n1 = 10;
	int n2 = 10;
	int sleepTime = 10000;

	public Client_Analysis()
	{
		super();
	}

	public abstract void connectServer();

	public void start()
	{
		// Prepare for reading commands
		System.out.println();
		System.out.println(">======== PERFORMANCE ANALYSIS >=========");

		ArrayList<String> transaction = new ArrayList<String>();
		transaction.add("start");
		transaction.add("addflight,0,0,5,5");
		transaction.add("addRooms,0,1,5,5");
		transaction.add("addCars,0,1,5,5");
		transaction.add("addCustomerid,0,1");
		transaction.add("commit,0");

		for (int i = 0; i < (int) this.iterations; i++) // # clients
		{	
			//Thread t = null;
			final int num = delay;
			try {	
				Thread t = new Thread() { 
					
					int r = num;

					@Override 
					public void run()
					{
						for (int j = 0; j < 5; j++) // # of transactions
						{
							for (int k = 0; k < transaction.size(); k++)
							{
								try {
									String command = transaction.get(k);
									Vector<String> arguments = parse(command);
									Command cmd = Command.fromString((String) arguments.elementAt(0));
									
									try {
										execute(cmd, arguments,r);
									}
									catch (ConnectException e) {
										connectServer();
										execute(cmd, arguments,r);
									}
								}
								catch (IllegalArgumentException|ServerException e) {
									System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
								}
								catch (ConnectException|UnmarshalException e) {
									System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mConnection to server lost");
								}
								catch (Exception e) {
									System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mUncaught exception");
									e.printStackTrace();
								}	
							}

							try {
								Thread.sleep(r);
							}
							catch (Exception e) 
							{
								e.printStackTrace();
							}
						}
						/*
						synchronized (this.stamps) 
						{
							Long sum = 0;
							for (Integer ts : this.stamps.keySet())
							{
								sum += this.stamps.get(ts);
							}

							sum /= this.stamps.size();

							try {
								BufferedWriter bw = new BufferedWriter(new FileWriter("performance_2/response_times_vs_clients.csv", true));
								bw.write(i + "," + ((double)(sum / 1e6)) + ",");
								bw.newLine();
								bw.flush();
							} 
							catch (IOException ioe) 
							{
								ioe.printStackTrace();
							} 
							finally 
							{                       
								if (bw != null) {
									try { bw.close(); } 
									catch (IOException ioe2) { }
								}
							}
						} */
					}
				};
				t.start();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			} 
		}
	}

	public void execute(Command cmd, Vector<String> arguments, int round) throws RemoteException, NumberFormatException
	{	
		BufferedWriter bw = null;

		try {

			switch (cmd)
			{
				case Help: {

					if (arguments.size() == 1) {
						System.out.println(Command.description());
					} else if (arguments.size() == 2) {
						Command l_cmd = Command.fromString((String)arguments.elementAt(1));
						System.out.println(l_cmd.toString());
					} else {
						System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
					}
					break;
				}
				// Transaction commands
				case Start: {

					checkArgumentsCount(1, arguments.size());
					System.out.println("Starting Transaction");

					int new_xid = m_resourceManager.startTransaction(this.client_id);
					xid_ = new_xid;

					Long stampA = System.nanoTime();
					
					synchronized (this.stamps) {
						this.stamps.put(xid_,stampA);
					}

					if (xid_ != -1) 
					{
						System.out.println("Transaction [xid=" + new_xid + "] added");
					}
					else 
					{
						System.out.println("Transaction [xid=" + new_xid + "] already exists");
					}
					
					break;
				}
				case Commit: {

					checkArgumentsCount(2, arguments.size());
					System.out.println("Commiting Transaction [xid=" + arguments.elementAt(1) + "]");

					int xid = xid_; //toInt(arguments.elementAt(1));

					if (m_resourceManager.commitTransaction(xid))
					{	
						synchronized (this.stamps) 
						{
							Long stamp_A = this.stamps.get(xid);
							Long stampB = System.nanoTime();
							Long diff = stampB - stamp_A;
							
							try {
								bw = new BufferedWriter(new FileWriter("response_time_" + this.iterations + ".csv", true));
								bw.write(((double)(diff / 1e6)) + ",");
								bw.newLine();
								bw.flush();
							} 
							catch (IOException ioe) 
							{
								ioe.printStackTrace();
							} 
							finally 
							{                       
								if (bw != null) {
									try { bw.close(); } 
									catch (IOException ioe2) { }
								}
							}

							System.out.println("Transaction [xid=" + xid + "] committed");
						}
					}	
					else 
					{
						System.out.println("Transaction [xid=" + xid + "] commit failed");
					}
		
					break;
				}
				case Abort: {

					checkArgumentsCount(2, arguments.size());
					System.out.println("Aborting Transaction [xid=" + arguments.elementAt(1) + "]");

					int xid = xid_;//toInt(arguments.elementAt(1));

					if (m_resourceManager.abortTransaction(xid))
					{	
						synchronized (this.stamps) 
						{
							Long stamp_A = this.stamps.get(xid);
							Long stampB = System.nanoTime();
							Long diff = stampB - stamp_A;
							
							try {
								bw = new BufferedWriter(new FileWriter("response_time.csv", true));
								bw.write(Long.toString(diff) + ",");
								bw.newLine();
								bw.flush();
							} 
							catch (IOException ioe) 
							{
								ioe.printStackTrace();
							} 
							finally 
							{                       
								if (bw != null) {
									try { bw.close(); } 
									catch (IOException ioe2) { }
								}
							}

							System.out.println("Transaction [xid=" + xid + "] aborted");
						}
					}
					else 
					{
						System.out.println("Transaction [xid=" + xid + "] abort failed");
					}

					break;
				}
				case Shutdown: {

					checkArgumentsCount(1, arguments.size());
					System.out.println("Shutdown servers");
					
					if (m_resourceManager.shutdownClient(this.client_id)) 
					{
						System.out.println("Succesful Shutdown");
						System.exit(0);
					}
					else
					{
						System.out.println("Failed Shutdown due to active transactions");
					}

					break;
				}
				// Operation commands
				case AddFlight: {

					checkArgumentsCount(5, arguments.size());

					System.out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Flight Number: " + arguments.elementAt(2));
					System.out.println("-Flight Seats: " + arguments.elementAt(3));
					System.out.println("-Flight Price: " + arguments.elementAt(4));

					int id = xid_; //toInt(arguments.elementAt(1));
					int flightNum = rmid++;//toInt(arguments.elementAt(2));
					int flightSeats = n1++;//toInt(arguments.elementAt(3));
					int flightPrice = n2++;//toInt(arguments.elementAt(4));

					if (m_resourceManager.addFlight(id, flightNum, flightSeats, flightPrice)) {
						System.out.println("Flight added");
					} else {
						System.out.println("Flight could not be added");
					}

					break;
				}
				case AddCars: {

					checkArgumentsCount(5, arguments.size());

					System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Car Location: " + arguments.elementAt(2));
					System.out.println("-Number of Cars: " + arguments.elementAt(3));
					System.out.println("-Car Price: " + arguments.elementAt(4));

					int id = xid_; //toInt(arguments.elementAt(1));
					String location = Integer.toString(rmid++);//arguments.elementAt(2);
					int numCars = n1++;//toInt(arguments.elementAt(3));
					int price = n2++;//toInt(arguments.elementAt(4));

					if (m_resourceManager.addCars(id, location, numCars, price)) {
						System.out.println("Cars added");
					} else {
						System.out.println("Cars could not be added");
					} 

					break;
				}
				case AddRooms: {

					checkArgumentsCount(5, arguments.size());

					System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Room Location: " + arguments.elementAt(2));
					System.out.println("-Number of Rooms: " + arguments.elementAt(3));
					System.out.println("-Room Price: " + arguments.elementAt(4));

					int id = xid_;//toInt(arguments.elementAt(1));
					String location = Integer.toString(rmid++);//arguments.elementAt(2);
					int numRooms = n1++;// toInt(arguments.elementAt(3));
					int price = n2++;//toInt(arguments.elementAt(4));

					if (m_resourceManager.addRooms(id, location, numRooms, price)) {
						System.out.println("Rooms added");
					} else {
						System.out.println("Rooms could not be added");
					}

					break;
				}
				case AddCustomer: {

					checkArgumentsCount(2, arguments.size());

					System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

					int id = xid_;;//toInt(arguments.elementAt(1));

					int customer = m_resourceManager.newCustomer(id);
					System.out.println("Add customer ID: " + customer);

					break;
				}
				case AddCustomerID: {

					checkArgumentsCount(3, arguments.size());

					System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Customer ID: " + arguments.elementAt(2));

					int id = xid_;//toInt(arguments.elementAt(1));
					int customerID = rmid++;//toInt(arguments.elementAt(2));

					if (m_resourceManager.newCustomer(id, customerID)) {
						System.out.println("Add customer ID: " + customerID);
					} else {
						System.out.println("Customer could not be added");
					}

					break;
				}
				case DeleteFlight: {

					checkArgumentsCount(3, arguments.size());

					System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Flight Number: " + arguments.elementAt(2));

					int id = xid_;//toInt(arguments.elementAt(1));
					int flightNum = rmid++;//toInt(arguments.elementAt(2));

					if (m_resourceManager.deleteFlight(id, flightNum)) {
						System.out.println("Flight Deleted");
					} else {
						System.out.println("Flight could not be deleted");
					}

					break;
				}
				case DeleteCars: {

					checkArgumentsCount(3, arguments.size());

					System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Car Location: " + arguments.elementAt(2));

					int id = xid_;//toInt(arguments.elementAt(1));
					String location = Integer.toString(rmid++);//arguments.elementAt(2);

					if (m_resourceManager.deleteCars(id, location)) {
						System.out.println("Cars Deleted");
					} else {
						System.out.println("Cars could not be deleted");
					}

					break;
				}
				case DeleteRooms: {

					checkArgumentsCount(3, arguments.size());

					System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Car Location: " + arguments.elementAt(2));

					int id = xid_;//toInt(arguments.elementAt(1));
					String location = Integer.toString(rmid++);//arguments.elementAt(2);

					if (m_resourceManager.deleteRooms(id, location)) {
						System.out.println("Rooms Deleted");
					} else {
						System.out.println("Rooms could not be deleted");
					}

					break;
				}
				case DeleteCustomer: {

					checkArgumentsCount(3, arguments.size());

					System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Customer ID: " + arguments.elementAt(2));
					
					int id = xid_;// toInt(arguments.elementAt(1));
					int customerID = rmid++;// toInt(arguments.elementAt(2));

					if (m_resourceManager.deleteCustomer(id, customerID)) {
						System.out.println("Customer Deleted");
					} else {
						System.out.println("Customer could not be deleted");
					}

					break;
				}
				case QueryFlight: {
					checkArgumentsCount(3, arguments.size());

					System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Flight Number: " + arguments.elementAt(2));
					
					int id = xid_;//toInt(arguments.elementAt(1));
					int flightNum = rmid++;//toInt(arguments.elementAt(2));

					int seats = m_resourceManager.queryFlight(id, flightNum);

					if (seats == 0)
					{
						System.out.println("Query failed");
					}
					else 
					{
						System.out.println("Number of seats available: " + seats);
					}

					break;
				}
				case QueryCars: {

					checkArgumentsCount(3, arguments.size());

					System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Car Location: " + arguments.elementAt(2));
					
					int id = xid_;//toInt(arguments.elementAt(1));
					String location = Integer.toString(rmid++);// arguments.elementAt(2);

					int numCars = m_resourceManager.queryCars(id, location);

					if (numCars == 0)
					{
						System.out.println("Query failed");
					}
					else 
					{
						System.out.println("Number of cars at this location: " + numCars);
					}

					break;
				}
				case QueryRooms: {

					checkArgumentsCount(3, arguments.size());

					System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Room Location: " + arguments.elementAt(2));
					
					int id = xid_;//toInt(arguments.elementAt(1));
					String location = Integer.toString(rmid++);//arguments.elementAt(2);

					int numRoom = m_resourceManager.queryRooms(id, location);

					if (numRoom == 0)
					{
						System.out.println("Query failed");
					}
					else 
					{
						System.out.println("Number of rooms at this location: " + numRoom);
					}
	
					break;
				}
				case QueryCustomer: {

					checkArgumentsCount(3, arguments.size());

					System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Customer ID: " + arguments.elementAt(2));

					int id = xid_;//toInt(arguments.elementAt(1));
					int customerID = rmid++;//toInt(arguments.elementAt(2));

					String bill = m_resourceManager.queryCustomerInfo(id, customerID);

					if (bill == null)
					{
						System.out.println("Query failed");
					}
					else 
					{
						System.out.print(bill);
					}

					break;               
				}
				case QueryFlightPrice: {

					checkArgumentsCount(3, arguments.size());
					
					System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Flight Number: " + arguments.elementAt(2));

					int id = xid_;//toInt(arguments.elementAt(1));
					int flightNum = rmid++;//toInt(arguments.elementAt(2));

					int price = m_resourceManager.queryFlightPrice(id, flightNum);

					if (price == 0)
					{
						System.out.println("Query failed");
					}
					else 
					{
						System.out.println("Price of a seat: " + price);
					}

					break;
				}
				case QueryCarsPrice: {

					checkArgumentsCount(3, arguments.size());

					System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Car Location: " + arguments.elementAt(2));

					int id = xid_;//toInt(arguments.elementAt(1));
					String location = Integer.toString(rmid++);//arguments.elementAt(2);

					int price = m_resourceManager.queryCarsPrice(id, location);
					
					if (price == 0)
					{
						System.out.println("Query failed");
					}
					else 
					{
						System.out.println("Price of cars at this location: " + price);
					}

					break;
				}
				case QueryRoomsPrice: {

					checkArgumentsCount(3, arguments.size());

					System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Room Location: " + arguments.elementAt(2));

					int id = xid_;// toInt(arguments.elementAt(1));
					String location = Integer.toString(rmid++);// arguments.elementAt(2);

					int price = m_resourceManager.queryRoomsPrice(id, location);

					if (price == 0)
					{
						System.out.println("Query failed");
					}
					else 
					{
						System.out.println("Price of rooms at this location: " + price);
					}

					break;
				}
				case ReserveFlight: {

					checkArgumentsCount(4, arguments.size());

					System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Customer ID: " + arguments.elementAt(2));
					System.out.println("-Flight Number: " + arguments.elementAt(3));

					int id = xid_;//toInt(arguments.elementAt(1));
					int customerID = rmid++;//toInt(arguments.elementAt(2));
					int flightNum = 1;//toInt(arguments.elementAt(3));

					if (m_resourceManager.reserveFlight(id, customerID, flightNum)) {
						System.out.println("Flight Reserved");
					} else {
						System.out.println("Flight could not be reserved");
					}

					break;
				}
				case ReserveCar: {

					checkArgumentsCount(4, arguments.size());

					System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Customer ID: " + arguments.elementAt(2));
					System.out.println("-Car Location: " + arguments.elementAt(3));

					int id = xid_;//toInt(arguments.elementAt(1));
					int customerID = 1;//toInt(arguments.elementAt(2));
					String location = "1";//arguments.elementAt(3);

					if (m_resourceManager.reserveCar(id, customerID, location)) {
						System.out.println("Car Reserved");
					} else {
						System.out.println("Car could not be reserved");
					}

					break;
				}
				case ReserveRoom: {

					checkArgumentsCount(4, arguments.size());

					System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Customer ID: " + arguments.elementAt(2));
					System.out.println("-Room Location: " + arguments.elementAt(3));
					
					int id = xid_;//toInt(arguments.elementAt(1));
					int customerID = 1; //toInt(arguments.elementAt(2));
					String location = "1";//arguments.elementAt(3);

					if (m_resourceManager.reserveRoom(id, customerID, location)) {
						System.out.println("Room Reserved");
					} else {
						System.out.println("Room could not be reserved");
					}

					break;
				}
				case Bundle: {

					if (arguments.size() < 7) {
						System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
						break;
					}

					System.out.println("Reserving an bundle [xid=" + arguments.elementAt(1) + "]");
					System.out.println("-Customer ID: " + arguments.elementAt(2));
					for (int i = 0; i < arguments.size() - 6; ++i)
					{
						System.out.println("-Flight Number: " + arguments.elementAt(3+i));
					}
					System.out.println("-Location for Car/Room: " + arguments.elementAt(arguments.size()-3));
					System.out.println("-Book Car: " + arguments.elementAt(arguments.size()-2));
					System.out.println("-Book Room: " + arguments.elementAt(arguments.size()-1));

					int id = xid_;//toInt(arguments.elementAt(1));
					int customerID = 1;//toInt(arguments.elementAt(2));
					Vector<String> flightNumbers = new Vector<String>();
					for (int i = 0; i < arguments.size() - 6; ++i)
					{
						flightNumbers.addElement((6 - i) + "");//arguments.elementAt(3+i));
					}
					String location = "1";//arguments.elementAt(arguments.size()-3);
					boolean car = true;// toBoolean(arguments.elementAt(arguments.size()-2));
					boolean room = true;// toBoolean(arguments.elementAt(arguments.size()-1));

					if (m_resourceManager.bundle(id, customerID, flightNumbers, location, car, room)) {
						System.out.println("Bundle Reserved");
					} else {
						System.out.println("Bundle could not be reserved");
					}
					
					break;
				}
				case Quit:
					checkArgumentsCount(1, arguments.size());

					System.out.println("Quitting client");
					System.exit(0);
			}
		}
		catch (InvalidTransactionException e)
		{
			System.out.println("Exception caught: client catches invalid transaction exception"); //e.printStackTrace();
		}
		catch (TransactionAbortedException e)
		{
			System.out.println("Exception caught: client catches transaction aborted exception"); //e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static Vector<String> parse(String command)
	{
		Vector<String> arguments = new Vector<String>();
		StringTokenizer tokenizer = new StringTokenizer(command,",");
		String argument = "";
		while (tokenizer.hasMoreTokens())
		{
			argument = tokenizer.nextToken();
			argument = argument.trim();
			arguments.add(argument);
		}
		return arguments;
	}

	public static void checkArgumentsCount(Integer expected, Integer actual) throws IllegalArgumentException
	{
		if (expected != actual)
		{
			throw new IllegalArgumentException("Invalid number of arguments. Expected " + (expected - 1) + ", received " + (actual - 1) + ". Location \"help,<CommandName>\" to check usage of this command");
		}
	}

	public static int toInt(String string) throws NumberFormatException
	{
		return (new Integer(string)).intValue();
	}

	public static boolean toBoolean(String string)
	{
		return (new Boolean(string)).booleanValue();
	}
}
