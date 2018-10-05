package Server.Actions;

import Server.Actions.*;

public class GetSummaryAction extends CustomerAction
{   

    // Constructor
    public GetSummaryAction(int xid) 
	{
        super(ACTION_TYPE.CUSTOMER_ACTION, ACTION_SUBTYPE.GET_SUMMARY, xid);
    }

}
