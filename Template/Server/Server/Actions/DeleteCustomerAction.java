package Server.Actions;

import Server.Actions.*;

public class DeleteCustomerAction extends CustomerAction
{   
    private int customerID;

    // Constructor
    public DeleteCustomerAction(int xid, int customerID) 
	{
        super(ACTION_TYPE.CUSTOMER_ACTION, ACTION_SUBTYPE.DELETE_CUSTOMER, xid);
        this.customerID = customerID;
    }
    
    // Getters
    public int getCustomerID()
    {
        return this.customerID;
    }
}
