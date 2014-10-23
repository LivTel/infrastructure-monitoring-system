package ngat.ims;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
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
import ngat.ims.DiskStatus;
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
	/**
	 * Method to start a thread to monitor the disk status data file, end generate 
	 * updates to appropriate registered listeners.
	 * @param propertiesURL The URL of a properties file containing configuration data
	 *        for the thread.
	 * @exception Exception Thrown if the configure method fails.
	 * @see DiskStatusMonitorThread
	 * @see DiskStatusMonitorThread#configure
	 */
	public void startMonitoringThread(URL propertiesURL) throws Exception
	{
		DiskStatusMonitorThread dsmt = null;

		dsmt = new DiskStatusMonitorThread();
		dsmt.configure(propertiesURL);
		dsmt.start();
	}
	
	/**
	 * Inner class that runs as a separate thread in the RCS. This
	 * polls the data file written by the cronjob.
	 * @author Chris Mottram
	 */
	protected class DiskStatusMonitorThread extends Thread
	{
		/** 
		 * Polling interval in milliseconds. 
		 */
		protected long pollingInterval;
		/**
		 * The location of the data file to poll.
		 */
		protected URL url = null;
		/**
		 * Date format used for parsing the timestamp.
		 */
		protected SimpleDateFormat sdf = null;
		/**
		 * Timezone used for parsing the timestamp.
		 */
		protected SimpleTimeZone UTC = null;
		/**
		 * An ordered list of machine name:disk name combinations,
		 * the data for which is in the data file. The list is a list of
		 * DiskStatus objects, but only the machineName and diskName fields
		 * are filled in.
		 */
		protected Vector<DiskStatus> diskList = null;
		/**
		 * Default constructor.
		 */
		protected DiskStatusMonitorThread()
		{
			super();
		}
		
		/**
		 * Configure the thread (before starting it) from the supplied
		 * properties file.
		 * <ul>
		 * <li>The data file URL is read from the "disk.status.url" property.
		 * <li>The polling interval is read from the "disk.status.polling_interval" property.
		 * <li>The time format is read from the "disk.status.time.format" property.
		 * <li>A UTC timezone is created.
		 * <li>The number of machine:disk combinations to read is read from the "disk.status.count" property.
		 * <li>We loop over the number of machine:disk combinations:
		 *     <ul>
		 *     <li>A machine name is retrieved from the property: "disk.status.machine_name."+i
		 *     <li>A disk name is retrieved from the property: "disk.status.disk_name."+i
		 *     <li>An instance of DiskStatus is created and added to the diskList.
		 *     </ul>
		 * </ul>
		 * @param propertiesURL A URL pointing to a properties file containing
		 *        the configuration for the thread.
		 * @throws Exception Thrown if an error occurs.
		 * @see #diskList
		 * @see #url
		 * @see #pollingInterval
		 * @see #sdf
		 * @see #UTC
		 */
		protected void configure(URL propertiesURL) throws Exception
		{
			DiskStatus diskStatus = null;
			/**
			 * Properties containing the configuration for this thread.
			 */
			Properties properties = null;
			String timeFormatString = null;
			String machineName = null;
			String diskName = null;
			int count;
			
			// load properties from propertiesURL
			properties = new Properties();
			properties.load(propertiesURL.openConnection().getInputStream());
			// get data URL
			url = new URL(properties.getProperty("disk.status.url"));
			// polling interval
			pollingInterval = Long.parseLong(properties.getProperty("disk.status.polling_interval"));
			// configure timezone
			timeFormatString = properties.getProperty("disk.status.time.format");
			sdf = new SimpleDateFormat(timeFormatString);
			UTC = new SimpleTimeZone(0, "UTC");
			// load list of machine:disks to parse
			count = Integer.parseInt(properties.getProperty("disk.status.count"));
			diskList = new Vector<DiskStatus>();
			for(int i = 0; i < count; i++)
			{
				machineName = properties.getProperty("disk.status.machine_name."+i);
				diskName = properties.getProperty("disk.status.disk_name."+i);
				diskStatus = new DiskStatus();
				diskStatus.setMachineName(machineName);
				diskStatus.setDiskName(diskName);
				diskList.add(diskStatus);
			}
		}
		
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
		 * @see #sdf
		 * @see #diskList
		 * @see BasicDiskStatusProvider#logger
		 * @see BasicDiskStatusProvider#notifyListeners
		 */
		public void run()
		{
			double percentUsed;
			long timeStamp,freeSpace;
			int nstat = 0; // count requests
			DiskStatus diskStatus = null;
			
			while (true) 
			{
				// wait a bit before reading the file
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
					// Open the data file
					URLConnection uc = url.openConnection();

					uc.setDoInput(true);
					uc.setAllowUserInteraction(false);

					InputStream in = uc.getInputStream();
					BufferedReader din = new BufferedReader(new InputStreamReader(in));
					// read the current data from the file.
					String line = din.readLine();
					logger.create().info().level(4).extractCallInfo().msg("Read Disk Status Line:" + line).send();
					// close the file
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
					// create a tokenizer to parse the read string
					StringTokenizer st = new StringTokenizer(line);
					// parse the date stamp
					String stime = st.nextToken();
					timeStamp = sdf.parse(stime).getTime();
					// interate over the list of machine:disk's to read data for.
					for(int index = 0; index < diskList.size(); index ++)
					{
						freeSpace = Long.parseLong(st.nextToken());
						percentUsed = Double.parseDouble(st.nextToken());
						diskStatus = new DiskStatus();
						diskStatus.setStatusTimeStamp(timeStamp);
						diskStatus.setMachineName(diskList.get(index).getMachineName());
						diskStatus.setDiskName(diskList.get(index).getDiskName());
						diskStatus.setDiskPercentUsed(percentUsed);
						diskStatus.setDiskFreeSpace(freeSpace);
						logger.create().info().level(4).extractCallInfo().msg("DISK: Sending listeners:" + diskStatus).send();
						notifyListeners(diskStatus);
					}// end for
				} 
				catch (Exception e) 
				{
					logger.create().info().level(4).extractCallInfo().msg("DISK: Error:" + e).send();
				}
			} // next sample
		} // run
		
	}
}
