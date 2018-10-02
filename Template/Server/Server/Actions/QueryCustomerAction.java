package Server.Actions;

import Server.Actions.*;

public class QueryCustomerAction extends CustomerAction
{   
    private int customerID;

    // Constructor
    public QueryCustomerAction(int xid, int customerID) 
	{
        super(ACTION_TYPE.CUSTOMER_ACTION, ACTION_SUBTYPE.QUERY_CUSTOMER, xid);
        this.customerID = customerID;
    }
    
    // Getters
    public int getCustomerID()
    {
        return this.customerID;
    }
}
