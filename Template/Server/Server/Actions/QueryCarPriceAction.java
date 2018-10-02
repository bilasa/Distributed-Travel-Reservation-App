package Server.Actions;

import Server.Actions.*;

public class QueryCarPriceAction extends CarAction
{   
    private String location;

    // Constructor
    public QueryCarPriceAction(int xid, String location) 
	{
        super(ACTION_TYPE.CAR_ACTION, ACTION_SUBTYPE.QUERY_CAR_PRICE, xid);
        this.location = location;
    }
    
    // Getters
    public String getLocation()
    {
        return this.location;
    }
}
