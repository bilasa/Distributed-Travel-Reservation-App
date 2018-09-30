package Server.Actions;

import Server.Actions.*;

public class AddRoomLocationAction extends RoomAction
{   
    private int xid;
    private String location;
    private int numRooms;
    private int price;

    // Constructor
    public AddRoomLocationAction(int xid, String location, int numRooms, int price)
	{
        super(ACTION_TYPE.ROOM_ACTION, ACTION_SUBTYPE.ADD_ROOM_LOCATION);
        this.xid = xid;
        this.location = location;
        this.numRooms = numRooms;
        this.price = price;
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

    public int getNumRooms() 
    {   
        return this.numRooms;
    }

    public int getPrice()
    {
        return this.price;
    }
}
