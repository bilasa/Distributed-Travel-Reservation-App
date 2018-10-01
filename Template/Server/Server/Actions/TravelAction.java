package Server.Actions;

import java.util.*;
import java.io.*;
import Server.Actions.*;

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
