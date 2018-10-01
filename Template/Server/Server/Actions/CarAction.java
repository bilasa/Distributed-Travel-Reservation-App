package Server.Actions;

import Server.Actions.*;

import java.util.*;
import java.io.*;

public abstract class CarAction extends TravelAction implements Serializable
{
    public CarAction(ACTION_TYPE t, ACTION_SUBTYPE st)
	{
		super(t, st);
	}
}
