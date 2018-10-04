package Server.Actions;

import java.util.*;
import Server.Actions.*;

public class ReserveFlightsRmAction extends FlightAction
{   
    private ArrayList<Integer> flightNumbers;
    private int toReserve;

    // Constructor
    public ReserveFlightsRmAction(int xid, ArrayList<Integer> flightNumbers, int toReserve)
	{
        super(ACTION_TYPE.FLIGHT_ACTION, ACTION_SUBTYPE.RESERVE_FLIGHTS_RM, xid);
        this.flightNumbers = flightNumbers;
        this.toReserve = toReserve;
    }
    
    // Getters
    public ArrayList<Integer> getFlightNumbers()
    {
        return this.flightNumbers;
    }

    public int getToReserve() 
    {
        return this.toReserve;
    }
}
