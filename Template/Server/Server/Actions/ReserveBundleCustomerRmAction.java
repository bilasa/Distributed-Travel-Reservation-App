package Server.Actions;

import Server.Actions.*;

import java.util.*;

public class ReserveBundleCustomerRmAction extends CustomerAction
{   
    private int customerID;
    private Vector<String> flightNumbers;
    private String location;
    private boolean car;
    private boolean room;
    private ArrayList<Integer> flightPrices;
    private int carPrice;
    private int roomPrice;

    // Constructor
    public ReserveBundleCustomerRmAction(
        int xid, 
        int customerID, 
        Vector<String> flightNumbers, 
        ArrayList<Integer> flightPrices, 
        String location, boolean car, 
        Integer carPrice, 
        boolean room, 
        Integer roomPrice
    ) {
        super(ACTION_TYPE.CUSTOMER_ACTION, ACTION_SUBTYPE.RESERVE_BUNDLE_CUSTOMER_RM, xid);
        this.customerID = customerID;
        this.flightNumbers = flightNumbers;
        this.location = location;
        this.car = car;
        this.room = room;
        this.flightPrices = flightPrices;
        this.carPrice = carPrice;
        this.roomPrice = roomPrice;
    }
    
    // Getters
    public int getCustomerID() 
    {
        return this.customerID;
    }

    public Vector<String> getFlightNumbers() 
    {
        return this.flightNumbers;
    }

    public ArrayList<Integer> getFlightPrices() 
    {
        return this.flightPrices;
    }

    public String getLocation()
    {
        return this.location;
    }

    public boolean getCar() 
    {
        return this.car;
    }

    public Integer getCarPrice()
    {
        return this.carPrice;
    }

    public boolean getRoom() 
    {
        return this.room;
    }

    public Integer getRoomPrice()
    {
        return this.roomPrice;
    }
}
