package Server.Actions;

import Server.Actions.*;

public class ReserveRoomRmAction extends RoomAction
{   
    private String location;
    private int toReserve;

    // Constructor
    public ReserveRoomRmAction(int xid, String location, int toReserve)
	{
        super(ACTION_TYPE.ROOM_ACTION, ACTION_SUBTYPE.RESERVE_ROOM_RM, xid);
        this.location = location;
        this.toReserve = toReserve;
    }
    
    // Getters
    public String getLocation()
    {
        return this.location;
    }

    public int getToReserve() 
    {
        return this.toReserve;
    }
}
