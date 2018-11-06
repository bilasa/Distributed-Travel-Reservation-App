package Server.Common;

/* The transaction is deadlocked. Somebody should abort it. */

public class InvalidTransactionException extends Exception
{
	private int m_xid = 0;

	public InvalidTransactionException(int xid, String msg)
	{
		super("The transaction " + xid + " is InvalidTransactionException:" + msg);
		m_xid = xid;
	}

	int getXId()
	{
		return m_xid;
	}
}
