package Server.Common;

import java.util.*;
import Server.TransactionManager.*;
import sun.net.www.content.text.plain;

public class Operation 
{   
    private ArrayList<RESOURCE_MANAGER_TYPE> rms = new ArrayList<RESOURCE_MANAGER_TYPE>();

    public Operation(ArrayList<RESOURCE_MANAGER_TYPE> rms)
    {
        this.rms = rms;
    }

    // Getters
    public ArrayList<RESOURCE_MANAGER_TYPE> getResourceManagers()
    {
        return this.rms;
    }

    // Setters
    public void setResourceManagers(ArrayList<RESOURCE_MANAGER_TYPE> rms)
    {
        this.rms = rms;
    }
}
