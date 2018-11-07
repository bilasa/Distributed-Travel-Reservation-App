package Server.Common;

import java.util.*;

public class Transaction 
{
    private int xid;
    private String client_id;
    private ArrayList<Operation> operations;

    public Transaction(int xid, String client_id) 
    {
        this.xid = xid;
        this.client_id = client_id;
        this.operations = new ArrayList<Operation>();
    }

    // Getters
    public int getXid() 
    {
        return this.xid;
    }

    public String getClientId()
    {
        return this.client_id;
    }

    public ArrayList<Operation> getOperations()
    {
        return this.operations;
    }

    // Setters
    public void setXid(int xid)
    {
        this.xid = xid;
    }

    public void setClientId(String client_id)
    {
        this.client_id = client_id;
    }

    public void addOperation(Operation op)
    {
        this.operations.add(op);
    }

    public void removeOperation(int index)
    {
        this.operations.remove(index);
    }
}
