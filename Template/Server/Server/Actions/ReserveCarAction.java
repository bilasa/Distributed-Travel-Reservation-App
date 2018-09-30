package Server.Actions;

import Server.Actions.*;

public class ReserveCarAction extends CarAction
{   
    private int xid;
    private int customerID;
    private String location;

    // Constructor
    public ReserveCarAction(int xid, int customerID, String location)
	{
        super(ACTION_TYPE.CAR_ACTION, ACTION_SUBTYPE.RESERVE_CAR);
        this.xid = xid;
        this.customerID = customerID;
        this.location = location;
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

    public String getLocation()
    {
        return this.location;
    }
}
