package ngat.ims;

import java.rmi.RemoteException;

/**
 * Interface implemented by classes wanting to receive disk status updates.
 * @author Chris Mottram
 */
public interface DiskStatusUpdateListener {
	/**
	 * Handle an update of disk status information.
	 * @param status The disk status update.
	 * @throws RemoteException
	 * @see DiskStatus
	 */
	public void diskStatusUpdate(DiskStatus status) throws RemoteException;

}
