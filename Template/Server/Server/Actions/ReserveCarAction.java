package Server.Actions;

import Server.Actions.*;

public class ReserveCarAction extends CarAction
{   
    private int customerID;
    private String location;

    // Constructor
    public ReserveCarAction(int xid, int customerID, String location)
	{
        super(ACTION_TYPE.RESERVE_ACTION, ACTION_SUBTYPE.RESERVE_CAR, xid);
        this.customerID = customerID;
        this.location = location;
    }
    
    // Getters
    public int getCustomerID() 
    {
        return this.customerID;
    }

    public String getLocation()
    {
        return this.location;
    }
}
