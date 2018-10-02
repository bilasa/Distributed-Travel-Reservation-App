package Server.Actions;

import Server.Actions.*;

public class ReserveCarCustomerRmAction extends CustomerAction
{   
    private int customerID;
    private String location;
    private int price;

    // Constructor
    public ReserveCarCustomerRmAction(int xid, int customerID, String location, int price)
	{
        super(ACTION_TYPE.RESERVE_ACTION, ACTION_SUBTYPE.RESERVE_CAR_CUSTOMER_RM, xid);
        this.customerID = customerID;
        this.location = location;
        this.price = price;
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

    public int getPrice()
    {
        return this.price;
    }
}
