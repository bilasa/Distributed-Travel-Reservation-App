package Server.Actions;

import Server.Actions.*;

public class QueryFlightPriceAction extends FlightAction
{   
    private int flightNumber;

    // Constructor
    public QueryFlightPriceAction(int xid, int flightNumber) 
	{
        super(ACTION_TYPE.FLIGHT_ACTION, ACTION_SUBTYPE.QUERY_FLIGHT_PRICE, xid);
        this.flightNumber = flightNumber;
    }
    
    // Getters
    public int getFlightNumber()
    {
        return this.flightNumber;
    }
}
