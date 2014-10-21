package ngat.ims;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * An interface implemented by classes that provide Disk Status. 
 * Clients can call methods in this interface to register and unregister
 * listener methods called when new disk status data is available.
 * @author Chris Mottram
 * @see DiskStatusUpdateListener
 */
public interface DiskStatusProvider extends Remote {
	/**
	 * Add a listener.
	 * @param dsl The class instance implementing the DiskStatusUpdateListener interface. 
	 *        Methods in the interface will be called when disk status is updated.
	 * @throws RemoteException
	 */
	public void addDiskStatusUpdateListener(DiskStatusUpdateListener dsl) throws RemoteException;
	/**
	 * Delete a previously added listener.
	 * @param dsl The class instance implementing the DiskStatusUpdateListener interface to be removed. 
	 * @throws RemoteException
	 */
	public void removeDiskStatusUpdateListener(DiskStatusUpdateListener dsl) throws RemoteException;
}
