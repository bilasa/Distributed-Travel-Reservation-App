package Server.Actions;

import Server.Actions.*;

import java.util.*;
import java.io.*;

public abstract class CustomerAction extends TravelAction implements Serializable
{
    public CustomerAction(ACTION_TYPE t, ACTION_SUBTYPE st, int xid)
	{
		super(t, st, xid);
	}
}
