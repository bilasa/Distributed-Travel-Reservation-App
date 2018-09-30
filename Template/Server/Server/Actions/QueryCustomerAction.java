package Server.Actions;

import Server.Actions.*;

public class QueryCustomerAction extends CustomerAction
{   
    private int xid;
    private int customerID;

    // Constructor
    public QueryCustomerAction(int xid, int customerID) 
	{
        super(ACTION_TYPE.CUSTOMER_ACTION, ACTION_SUBTYPE.QUERY_CUSTOMER);
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
