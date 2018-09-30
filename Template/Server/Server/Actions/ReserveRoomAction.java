package Server.Actions;

import Server.Actions.*;

public class ReserveRoomAction extends RoomAction
{   
    private int xid;
    private int customerID;
    private String location;

    // Constructor
    public ReserveRoomAction(int xid, int customerID, String location)
	{
        super(ACTION_TYPE.ROOM_ACTION, ACTION_SUBTYPE.RESERVE_ROOM);
        this.xid = xid;
        this.customerID = customerID;
        this.location = location;
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

    public String getLocation()
    {
        return this.location;
    }
}
