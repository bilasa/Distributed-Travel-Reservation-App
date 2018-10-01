package Server.Actions;

import java.util.*;
import java.io.*;
import Server.Actions.*;

public abstract class TravelAction implements Serializable, Cloneable
{   
    private ACTION_TYPE TYPE;
    private ACTION_SUBTYPE SUB_TYPE;
    private int xid;

    // Constructor
    TravelAction(ACTION_TYPE t, ACTION_SUBTYPE st, int xid)
    {
        super();
        this.TYPE = t;
        this.SUB_TYPE = st;
        this.xid = xid;
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

    // Getter
    public int getXid() 
    {
        return this.xid;
    }

    // Function to clone
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
