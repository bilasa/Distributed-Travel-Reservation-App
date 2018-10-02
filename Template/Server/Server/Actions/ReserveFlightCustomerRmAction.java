package Server.Actions;

import Server.Actions.*;

public class ReserveFlightCustomerRmAction extends CustomerAction
{   
    private int customerID;
    private int flightNumber;
    private int price;

    // Constructor
    public ReserveFlightCustomerRmAction(int xid, int customerID, int flightNumber, int price)
	{
        super(ACTION_TYPE.RESERVE_ACTION, ACTION_SUBTYPE.RESERVE_FLIGHT_CUSTOMER_RM, xid);
        this.customerID = customerID;
        this.flightNumber = flightNumber;
        this.price = price;
    }
    
    // Getters
    public int getCustomerID() 
    {
        return this.customerID;
    }

    public int getFlightNumber()
    {
        return this.flightNumber;
    }

    public int getPrice()
    {
        return this.price;
    }
}
