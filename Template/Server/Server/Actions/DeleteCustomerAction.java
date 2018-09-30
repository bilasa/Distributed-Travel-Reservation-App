package Server.Actions;

import Server.Actions.*;

public class DeleteCustomerAction extends CustomerAction
{   
    private int xid;
    private int customerID;

    // Constructor
    public DeleteCustomerAction(int xid, int customerID) 
	{
        super(ACTION_TYPE.CUSTOMER_ACTION, ACTION_SUBTYPE.DELETE_CUSTOMER);
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
