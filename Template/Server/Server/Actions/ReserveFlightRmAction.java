package Server.Actions;

import Server.Actions.*;

public class ReserveFlightRmAction extends FlightAction
{   
    private int flightNumber;
    private int toReserve;

    // Constructor
    public ReserveFlightRmAction(int xid, int flightNumber, int toReserve)
	{
        super(ACTION_TYPE.FLIGHT_ACTION, ACTION_SUBTYPE.RESERVE_FLIGHT_CUSTOMER_RM, xid);
        this.flightNumber = flightNumber;
        this.toReserve = toReserve;
    }
    
    // Getters
    public int getFlightNumber()
    {
        return this.flightNumber;
    }

    public int getToReserve() 
    {
        return this.toReserve;
    }
}
