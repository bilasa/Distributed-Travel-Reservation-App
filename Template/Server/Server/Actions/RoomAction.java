package Server.Actions;

import Server.Actions.TravelAction;

import java.util.*;
import java.io.*;

public abstract class RoomAction extends TravelAction implements Serializable
{
    public RoomAction(ACTION_TYPE t)
	{
		super(t);
	}
}
