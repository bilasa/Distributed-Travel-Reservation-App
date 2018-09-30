package Server.Actions;

import Server.Actions.*;

public class DeleteRoomLocationAction extends RoomAction
{   
    private int xid;
    private String location;

    // Constructor
    public DeleteRoomLocationAction(int xid, String location) 
	{
        super(ACTION_TYPE.ROOM_ACTION, ACTION_SUBTYPE.DELETE_ROOM_LOCATION);
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
