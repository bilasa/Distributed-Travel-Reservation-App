package Server.Actions;

import java.util.*;
import java.io.*;

/* NOTE:
 * The following enumerations define the action types and action sub-types.
 * Every request that is sent from the client contains respective attributes for them,
 * along with associated payload. 
 */ 

enum ACTION_TYPE {
    FLIGHT_ACTION,
    CAR_ACTION,
    ROOM_ACTION,
    CUSTOMER_ACTION
}

enum ACTION_SUBTYPE {
    ADD_FLIGHT,
    ADD_CAR_LOCATION,
    ADD_ROOM_LOCATION,
    ADD_CUSTOMER,
    QUERY_FLIGHT,
    QUERY_CAR_LOCATION,
    QUERY_ROOM_LOCATION,
    QUERY_CUSTOMER,
    QUERY_FLIGHT_PRICE,
    QUERY_CAR_PRICE,
    QUERY_ROOM_PRICE,
    DELETE_FLIGHT,
    DELETE_CAR_LOCATION,
    DELETE_ROOM_LOCATION,
    DELETE_CUSTOMER,
    RESERVE_FLIGHT,
    RESERVE_CAR,    
    RESERVE_ROOM,
    RESERVE_BUNDLE
}

public abstract class TravelAction implements Serializable, Cloneable
{   
    private ACTION_TYPE TYPE;
    private ACTION_SUBTYPE SUB_TYPE;

    // Constructor
    TravelAction(ACTION_TYPE t, ACTION_SUBTYPE st)
    {
        super();
        this.TYPE = t;
        this.SUB_TYPE = st;
    }

    // Getters
    public ACTION_TYPE getType() 
    {
        return this.TYPE;
    }

    public ACTION_SUBTYPE getSubtype() 
    {
        return this.SUB_TYPE;
    }
    
    // Clone function
    public Object clone()
    {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
