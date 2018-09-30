package Server.Actions;

import Server.Actions.*;

public class QueryFlightAction extends FlightAction
{   
    private int xid;
    private int flightNumber;

    // Constructor
    public QueryFlightAction(int xid, int flightNumber) 
	{
        super(ACTION_TYPE.FLIGHT_ACTION, ACTION_SUBTYPE.QUERY_FLIGHT);
        this.xid = xid;
        this.flightNumber = flightNumber;
    }
    
    // Getters
    public int getXid() 
    {
        return this.xid;
    }

    public int getFlightNumber()
    {
        return this.flightNumber;
    }
}
