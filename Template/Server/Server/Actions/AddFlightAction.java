package Server.Actions;

import Server.Actions.*;

public class AddFlightAction extends FlightAction
{   
    private int flightNumber;
    private int flightSeats;
    private int flightPrice;

    // Constructor
    public AddFlightAction(int xid, int flightNumber, int flightSeats, int flightPrice) 
	{
        super(ACTION_TYPE.FLIGHT_ACTION, ACTION_SUBTYPE.ADD_FLIGHT, xid);
        this.flightNumber = flightNumber;
        this.flightSeats = flightSeats;
        this.flightPrice = flightPrice;
    }
    
    // Getters
    public int getFlightNumber() 
    {
        return this.flightNumber;
    }

    public int getFlightSeats() 
    {   
        return this.flightSeats;
    }

    public int getFlightPrice() 
    {
        return this.flightPrice;
    }
}
