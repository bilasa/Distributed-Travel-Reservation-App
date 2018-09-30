package Server.Actions;

import Server.Actions.*;

public class AddCarLocationAction extends CarAction
{   
    private int xid;
    private String location;
    private int numCars;
    private int price;

    // Constructor
    public AddCarLocationAction(int xid, String location, int numCars, int price)
	{
        super(ACTION_TYPE.CAR_ACTION, ACTION_SUBTYPE.ADD_CAR_LOCATION);
        this.xid = xid;
        this.location = location;
        this.numCars = numCars;
        this.price = price;
    }
    
    // Getters
    public int getXid() 
    {
        return this.xid;
    }

    public String getLocation() 
    {
        return this.location;
    }

    public int getNumCars() 
    {   
        return this.numCars;
    }

    public int getPrice()
    {
        return this.price;
    }
}
