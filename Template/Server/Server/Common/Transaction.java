package Server.Common;

import java.util.*;

public class Transaction 
{
    private int xid;
    private ArrayList<Operation> operations;

    public Transaction(int xid) 
    {
        this.xid = xid;
        this.operations = new ArrayList<Operation>();
    }

    // Getters
    public int getXid() 
    {
        return this.xid;
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

    public void addOperation(Operation op)
    {
        this.operations.add(op);
    }

    public void removeOperation(int index)
    {
        this.operations.remove(index);
    }
}
