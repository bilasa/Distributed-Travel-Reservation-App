package Server.Actions;

import Server.Actions.*;

public class AddCustomerAction extends CustomerAction
{   
    private int xid;
    private int customerID; 

    // Constructors
    public AddCustomerAction(int xid)
	{
        super(ACTION_TYPE.CUSTOMER_ACTION, ACTION_SUBTYPE.ADD_CUSTOMER);
        this.xid = xid;
        this.customerID = -1;
    }

    public AddCustomerAction(int xid, int customerID)
	{
        super(ACTION_TYPE.CUSTOMER_ACTION, ACTION_SUBTYPE.ADD_CUSTOMER);
        this.xid = xid;
        this.customerID = customerID;
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
}
