package Server.Actions;

import Server.Actions.*;

public class AddCustomerAction extends CustomerAction
{   
    private int customerID; 

    // Constructors
    public AddCustomerAction(int xid)
	{
        super(ACTION_TYPE.CUSTOMER_ACTION, ACTION_SUBTYPE.ADD_CUSTOMER, xid);
        this.customerID = -1;
    }

    public AddCustomerAction(int xid, int customerID)
	{
        super(ACTION_TYPE.CUSTOMER_ACTION, ACTION_SUBTYPE.ADD_CUSTOMER, xid);
        this.customerID = customerID;
    }
    
    // Getters
    public int getCustomerID() 
    {
        return this.customerID;
    }
}
