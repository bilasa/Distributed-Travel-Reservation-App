package Server.Actions;

import java.util.*;
import Server.Actions.*;

public class ReserveFlightsCustomerRmAction extends CustomerAction
{   
    private int customerID;
    private ArrayList<Integer> flightNumbers;
    private ArrayList<Integer> prices;

    // Constructor
    public ReserveFlightsCustomerRmAction(int xid, int customerID, ArrayList<Integer> flightNumbers, ArrayList<Integer> prices)
	{
        super(ACTION_TYPE.CUSTOMER_ACTION, ACTION_SUBTYPE.RESERVE_FLIGHTS_CUSTOMER_RM, xid);
        this.customerID = customerID;
        this.flightNumbers = flightNumbers;
        this.prices = prices;
    }
    
    // Getters
    public int getCustomerID() 
    {
        return this.customerID;
    }

    public ArrayList<Integer> getFlightNumbers()
    {
        return this.flightNumbers;
    }

    public ArrayList<Integer> getPrices()
    {
        return this.prices;
    }
}
