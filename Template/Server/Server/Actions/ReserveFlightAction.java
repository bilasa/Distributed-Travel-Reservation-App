package Server.Actions;

import Server.Actions.*;

public class ReserveFlightAction extends FlightAction
{   
    private int xid;
    private int customerID;
    private int flightNumber;

    // Constructor
    public ReserveFlightAction(int xid, int customerID, int flightNumber)
	{
        super(ACTION_TYPE.RESERVE_ACTION, ACTION_SUBTYPE.RESERVE_FLIGHT);
        this.xid = xid;
        this.customerID = customerID;
        this.flightNumber = flightNumber;
    }
    
    // Getters
    public int getXid() 
    {
        return this.xid;
    }

    public int getCustomerID() 
    {
        return this.customerID;
    }

    public int getFlightNumber()
    {
        return this.flightNumber;
    }
}
