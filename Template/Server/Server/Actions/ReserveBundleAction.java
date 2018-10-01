package Server.Actions;

import Server.Actions.*;

import java.util.*;

public class ReserveBundleAction extends TravelAction
{   
    private int xid;
    private int customerID;
    private Vector<String> flightNumbers;
    private String location;
    private boolean car;
    private boolean room;

    // Constructor
    public ReserveBundleAction(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
	{
        super(ACTION_TYPE.RESERVE_ACTION, ACTION_SUBTYPE.RESERVE_BUNDLE);
        this.xid = xid;
        this.customerID = customerID;
        this.flightNumbers = flightNumbers;
        this.location = location;
        this.car = car;
        this.room = room;
    }
    
    // Getters
    public int getXid() 
    {
        return this.xid;
    }

    public int getCustomerID() 
    {
        return this.customerID;
    }

    public Vector<String> getFlightNumbers() {
        return this.flightNumbers;
    }

    public String getLocation()
    {
        return this.location;
    }

    public boolean getCar() {
        return this.car;
    }

    public boolean getRoom() {
        return this.room;
    }
}
