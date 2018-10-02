package Server.Actions;

import Server.Actions.*;

public class QueryFlightAction extends FlightAction
{   
    private int flightNumber;

    // Constructor
    public QueryFlightAction(int xid, int flightNumber) 
	{
        super(ACTION_TYPE.FLIGHT_ACTION, ACTION_SUBTYPE.QUERY_FLIGHT, xid);
        this.flightNumber = flightNumber;
    }
    
    // Getters
    public int getFlightNumber()
    {
        return this.flightNumber;
    }
}
