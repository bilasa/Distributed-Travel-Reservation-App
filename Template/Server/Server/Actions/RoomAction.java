package Server.Actions;

import Server.Actions.*;

import java.util.*;
import java.io.*;

public abstract class RoomAction extends TravelAction implements Serializable
{
    public RoomAction(ACTION_TYPE t, ACTION_SUBTYPE st)
	{
		super(t, st);
	}
}
