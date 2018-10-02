package Server.Actions;

import Server.Actions.*;

public class DeleteCarLocationAction extends CarAction
{   
    private String location;

    // Constructor
    public DeleteCarLocationAction(int xid, String location) 
	{
        super(ACTION_TYPE.CAR_ACTION, ACTION_SUBTYPE.DELETE_CAR_LOCATION, xid);
        this.location = location;
    }
    
    // Getters
    public String getLocation()
    {
        return this.location;
    }
}
