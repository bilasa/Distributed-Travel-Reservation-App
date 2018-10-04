package Server.Actions;

import Server.Actions.*;

public class ReserveCarRmAction extends CarAction
{   
    private String location;
    private int toReserve;

    // Constructor
    public ReserveCarRmAction(int xid, String location, int toReserve)
	{
        super(ACTION_TYPE.CAR_ACTION, ACTION_SUBTYPE.RESERVE_CAR_RM, xid);
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