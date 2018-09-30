package Server.Actions;

import Server.Actions.*;

public class QueryRoomPriceAction extends RoomAction
{   
    private int xid;
    private String location;

    // Constructor
    public QueryRoomPriceAction(int xid, String location) 
	{
        super(ACTION_TYPE.ROOM_ACTION, ACTION_SUBTYPE.QUERY_ROOM_PRICE);
        this.xid = xid;
        this.location = location;
    }
    
    // Getters
    public int getXid() 
    {
        return this.xid;
    }

    public String getLocation()
    {
        return this.location;
    }
}
