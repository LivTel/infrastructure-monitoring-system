package ngat.ims;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.Vector;

import ngat.ems.MeteorologyStatusUpdateListener;
import ngat.net.cil.CilService;
import ngat.net.cil.tcs.CollatorResponseListener;
import ngat.net.cil.tcs.TcsStatusPacket;
import ngat.net.cil.tcs.TcsStatusPacket.Segment;
import ngat.net.cil.tcs.CilStatusCollator;
import ngat.util.ControlThread;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * This class implements the interface to provide disk status updates.
 * @author Chris Mottram LJMU
 */
public class BasicDiskStatusProvider extends UnicastRemoteObject 
	implements DiskStatusProvider 
{
	/** 
	 * A list of DiskStatusUpdateListeners, 
	 * to be called when the disk status needs updating. 
	 * @see DiskStatusUpdateListener
	 */
	protected List<DiskStatusUpdateListener> listeners;
	/**
	 * The logger to use for logging.
	 * @see ngat.util.LogGenerator
	 */
	protected LogGenerator logger;

	/**
     * Default constructor. Initialises the listeners list and creates
     * a suitable logger.
     * @see #listeners
     * @see #logger
     */
	public BasicDiskStatusProvider() throws RemoteException 
	{
		super();

		listeners = new Vector<DiskStatusUpdateListener>();

		Logger alogger = LogManager.getLogger("IMS");
		logger = alogger.generate().system("IMS").subSystem("Disk").srcCompClass(getClass().getSimpleName())
				.srcCompId("Disk");
	}

	/**
	 * Adds a client instance implementing DiskStatusUpdateListener to receive
	 * notification when the disk status changes.
	 * @param listener An instance of an object implementing DiskStatusUpdateListener to register. If
	 *        listener is already registered this method returns silently.
	 * @throws RemoteException If anything goes wrong. Implementations may
	 *        decide to automatically de-register listeners which throw an
	 *        exeception on updating.
	 * @see #listeners
	 * @see #logger
	 */
	public void addDiskStatusUpdateListener(DiskStatusUpdateListener listener) throws RemoteException {		// TODO Auto-generated method stub
		if (listeners.contains(listener))
			return;
		listeners.add(listener);
		logger.create().info().level(3).extractCallInfo().msg("Adding listener: " + listener).send();
	}


}
