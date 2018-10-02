package Server.Actions;

import Server.Actions.*;

public class DeleteFlightAction extends FlightAction
{   
    private int flightNumber;

    // Constructor
    public DeleteFlightAction(int xid, int flightNumber) 
	{
        super(ACTION_TYPE.FLIGHT_ACTION, ACTION_SUBTYPE.DELETE_FLIGHT, xid);
        this.flightNumber = flightNumber;
    }
    
    // Getters
    public int getFlightNumber() 
    {
        return this.flightNumber;
    }
}
