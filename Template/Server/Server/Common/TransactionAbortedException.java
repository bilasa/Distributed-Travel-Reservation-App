package Server.Common;

/* The transaction is deadlocked. Somebody should abort it. */

public class TransactionAbortedException extends Exception
{
	private int m_xid = 0;

	public TransactionAbortedException(int xid, String msg)
	{
		super("The transaction " + xid + " is TransactionAbortedException:" + msg);
		m_xid = xid;
	}

	int getXId()
	{
		return m_xid;
	}
}
