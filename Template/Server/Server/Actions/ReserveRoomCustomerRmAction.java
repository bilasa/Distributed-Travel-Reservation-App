package Server.Actions;

import Server.Actions.*;

public class ReserveRoomCustomerRmAction extends CustomerAction
{   
    private int customerID;
    private String location;
    private int price;

    // Constructor
    public ReserveRoomCustomerRmAction(int xid, int customerID, String location, int price)
	{
        super(ACTION_TYPE.RESERVE_ACTION, ACTION_SUBTYPE.RESERVE_ROOM_CUSTOMER_RM, xid);
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
