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
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.Vector;

import ngat.ems.CloudStatus;
import ngat.ems.MeteorologyStatus;
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
	public void addDiskStatusUpdateListener(DiskStatusUpdateListener listener) throws RemoteException 
	{
		if (listeners.contains(listener))
			return;
		listeners.add(listener);
		logger.create().info().level(3).extractCallInfo().msg("Adding listener: " + listener).send();
	}

	/**
	 * De-registers an instance of an object implementing DiskStatusUpdateListener from
	 * receiving notification when the disk status changes.
	 * @param listener An instance of DiskStatusUpdateListener to de-register.
	 *            If listener is not-already registered this method returns silently.
	 * @throws RemoteException If anything goes wrong.
	 */
	public void removeDiskStatusUpdateListener(DiskStatusUpdateListener listener) throws RemoteException
	{
		if (!listeners.contains(listener))
			return;
		listeners.remove(listener);
		logger.create().info().level(4).extractCallInfo().msg("Removed listener: " + listener).send();
	}

	/**
	 * Notify registered listeners that a status change has occcurred. Any
	 * listeners which fail to take the update are summarily removed from the
	 * listener list.
	 * @param status A status object.
	 */
	public void notifyListeners(DiskStatus status)
	{
		DiskStatusUpdateListener dsul = null;
		Iterator ilist = listeners.iterator();
		while (ilist.hasNext())
		{
			dsul = (DiskStatusUpdateListener) ilist.next();
			logger.create().info().level(4).extractCallInfo().msg("Notify listener: " + dsul).send();

			try {
				dsul.diskStatusUpdate(status);
			} catch (Exception e) {
				e.printStackTrace();
				logger.create().info().level(1).extractCallInfo()
						.msg("Removing unresponsive listener: " + dsul + " due to: " + e).send();
				ilist.remove();
			}
		}
	}

	public void startMonitoringThread(URL propertiesURL)
	{
		FileInputStream fis = null;
		Properties properties = null;
		DiskStatusMonitorThread dsmt = null;

		properties = new Properties();
		fis = new FileInputStream(propertiesURL);
		properties.load(fis);
		dsmt = new DiskStatusMonitorThread();
		dsmt.setURL(properties.getProperty("disk.status.url"));
		dsmt.start();
	}
	
	/**
	 * Inner class that runs as a separate thread in the RCS. This
	 * polls the data file written by the cronjob.
	 * @author Chris Mottram
	 */
	protected class DiskStatusMonitorThread
	{
		/** 
		 * Polling interval in milliseconds. 
		 */
		protected long pollingInterval;
		/**
		 * The location of the data file to poll.
		 */
		protected URL url = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");
		
		public void setURL(URL u)
		{
			url = u;
		}
		
		public void setPollingInterval(long pi)
		{
			pollingInterval = pi;
		}
		
		/** 
		 * Poll the status source URL with requests. 
		 * This involves entering a loop that does the following:
		 * <ul>
		 * <li>Sleeping the thread for pollingInterval.
		 * <li>Opening the URL.
		 * <li>Creating a buffered reader and reading a string in from the URL.
		 * <li>Parsing the string. This string is written by the following script on the occ:
		 *     <b>/home/occ/get_system_gosubs2</b>.
		 * </ul>
		 * @see #pollingInterval
		 * @see #url
		 */
		public void run()
		{
			double percentUsed;
			long timeStamp,freeSpace;
			int nstat = 0; // count requests
			DiskStatus diskStatus = null;
			
			while (true) 
			{
				try 
				{
					Thread.sleep(pollingInterval);
				} 
				catch (InterruptedException ix) 
				{
				}
				nstat++;

				try 
				{

					URLConnection uc = url.openConnection();

					uc.setDoInput(true);
					uc.setAllowUserInteraction(false);

					InputStream in = uc.getInputStream();
					BufferedReader din = new BufferedReader(new InputStreamReader(in));

					String line = din.readLine();
					logger.create().info().level(4).extractCallInfo().msg("Read Disk Status Line:" + line).send();

					StringTokenizer st = new StringTokenizer(line);

					try 
					{
						in.close();
						din.close();
						uc = null;
						
					} 
					catch (Exception e) 
					{
						logger.create().info().level(4).extractCallInfo().msg("DISK: WARNING: Failed to close URL input stream: " + e).send();
					}
					// date stamp
					String stime = st.nextToken();
					timeStamp = sdf.parse(stime).getTime();
					// occ:/
					freeSpace = Long.parseLong(st.nextToken());
					percentUsed = Double.parseDouble(st.nextToken());
					diskStatus = new DiskStatus();
					diskStatus.setStatusTimeStamp(timeStamp);
					diskStatus.setMachineName("occ");
					diskStatus.setDiskName("/")
					diskStatus.setDiskPercentUsed(percentUsed);
					diskStatus.setDiskFreeSpace(freeSpace);
					logger.create().info().level(4).extractCallInfo().msg("DISK: Sending listeners:" + diskStatus).send();
					notifyListeners(diskStatus);
					// ltnas2:/mnt/archive2
					// rise:/mnt/rise-image
					// ringo3-1:/mnt/ringo3-1-image
					// ringo3-2:/mnt/ringo3-2-image
					// autoguider1:/mnt/autoguider-image
				} 
				catch (Exception e) 
				{
					logger.create().info().level(4).extractCallInfo().msg("DISK: Error:" + e).send();
				}
			} // next sample
		} // run
		
	}
}
