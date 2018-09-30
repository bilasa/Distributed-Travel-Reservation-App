package Server.Actions;

import Server.Actions.TravelAction;

import java.util.*;
import java.io.*;

public abstract class FlightAction extends TravelAction implements Serializable
{
    public FlightAction(ACTION_TYPE t, ACTION_SUBTYPE st)
	{
		super(t, st);
	}
}
