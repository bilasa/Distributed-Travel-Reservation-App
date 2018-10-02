package Server.Actions;

import Server.Actions.*;

public class QueryCarLocationAction extends CarAction
{   
    private String location;

    // Constructor
    public QueryCarLocationAction(int xid, String location) 
	{
        super(ACTION_TYPE.CAR_ACTION, ACTION_SUBTYPE.QUERY_CAR_LOCATION, xid);
        this.location = location;
    }
    
    // Getters
    public String getLocation()
    {
        return this.location;
    }
}
