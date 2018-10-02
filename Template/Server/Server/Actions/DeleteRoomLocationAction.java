package Server.Actions;

import Server.Actions.*;

public class DeleteRoomLocationAction extends RoomAction
{   
    private String location;

    // Constructor
    public DeleteRoomLocationAction(int xid, String location) 
	{
        super(ACTION_TYPE.ROOM_ACTION, ACTION_SUBTYPE.DELETE_ROOM_LOCATION, xid);
        this.location = location;
    }
    
    // Getters
    public String getLocation()
    {
        return this.location;
    }
}
