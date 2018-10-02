package Server.Actions;

import Server.Actions.*;

public class QueryRoomLocationAction extends RoomAction
{   
    private String location;

    // Constructor
    public QueryRoomLocationAction(int xid, String location) 
	{
        super(ACTION_TYPE.ROOM_ACTION, ACTION_SUBTYPE.QUERY_ROOM_LOCATION, xid);
        this.location = location;
    }
    
    // Getters
    public String getLocation()
    {
        return this.location;
    }
}
