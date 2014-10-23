package ngat.ims;
import java.io.Serializable;

import ngat.net.telemetry.StatusCategory;

public class DiskStatus implements Serializable, StatusCategory 
{
	/**
	 * A timestamp (milliseconds since the epoch (1st Jan 1970))
	 * describing when the disk data was retrieved.
	 */
	protected long timeStamp;
	/**
	 * The machine where the disk is located.
	 */
	protected String machineName = null;
	/**
	 * The name of the disk we are reporting on.
	 */
	protected String diskName = null;
	/**
	 * The percentage of the disk that is free, as reported by a 'df -k'.
	 */
	protected double diskPercentUsed = 0.0;
	/**
	 * The number of free kilobytes of space on the disk, as reported by a 'df -k'.
	 */
	protected long diskFreeSpace = 0;
	
	/**
	 * Default constructor.
	 */
	public DiskStatus()
	{
			super();
	}
	
	/**
	 * Constructor. The timeStamp is set to now.
	 * @param machineName Which machine the disk resised on.
	 * @param diskName The name of the disk.
	 * @param diskPercentUsed The percentage of the disk space used.
	 * @param diskFreeSpace The free space on the disk on kilobytes.
	 * @see #machineName
	 * @see #diskName
	 * @see #diskPercentUsed
	 * @see #diskFreeSpace
	 * @see #timeStamp
	 */
	public DiskStatus(String machineName,String diskName,
			double diskPercentUsed,long diskFreeSpace)
	{
		super();
		this.machineName = machineName;
		this.diskName = diskName;
		this.diskPercentUsed = diskPercentUsed;
		this.diskFreeSpace = diskFreeSpace;
		this.timeStamp = System.currentTimeMillis();
	}
	
	/**
	 * Set the time stamp associated with this disk status.
	 * @param ts A timestamp (milliseconds since the epoch (1st Jan 1970))
	 *        when this disk status was sampled.
	 */
	public void setStatusTimeStamp(long ts) 
	{
		this.timeStamp = ts;
	}
	/** 
     * When this status was generated.
     * @return The timestamp for this status.
     * @see #timeStamp
     */ 
    public long getStatusTimeStamp()
    {
    	return timeStamp;
    }
    
    /**
     * Set the name of the machine containing this disk.
     * @param s A string representing the name of the machine.
     * @see #machineName
     */
    public void setMachineName(String s)
    {
    	machineName = s;
    }
    
    /**
     * Return the name of the machine containing the disk.
     * @return A string representing the name of the machine.
     * @see #machineName
     */
    public String getMachineName()
    {
    		return machineName;
    }
    /**
     * Set the name of the disk.
     * @param s A string representing the name of the disk.
     * @see #diskName
     */
    public void setDiskName(String s)
    {
    	diskName = s;
    }
    
    /**
     * Return the name of the disk.
     * @return A string representing the name of the disk.
     * @see #diskName
     */
    public String getDiskName()
    {
    		return diskName;
    }
    
    /**
     * Set the percentage disk space used.
     * @param pc The percentage.
     * @see #diskPercentUsed
     */
    public void setDiskPercentUsed(double pc)
    {
    	diskPercentUsed = pc;
    }
    /**
     * Return the percentage disk space used.
     * @return A percentage, the disk space used.
     * @see #diskPercentUsed
     */
    public double getDiskPercentUsed()
    {
    	return diskPercentUsed;
    }
    
    /**
     * Set the free space on the disk.
     * @param fs The free space in kilobytes.
     * @see #diskPercentUsed
     */
    public void setDiskFreeSpace(long fs)
    {
    	diskFreeSpace = fs;
    }
    /**
     * Return the free space on the disk.
     * @return The free space in kilobytes.
     * @see #diskFreeSpace
     */
    public long getDiskFreeSpace()
    {
    	return diskFreeSpace;
    }
    
    
    /** 
     * Return the category name for this status.
     * @return the name of this status category.
     * @see #machineName
     * @see #diskName
     */
    public String getCategoryName()
    {
    		return new String("DISK:"+machineName+":"+diskName);
    }
    
    /**
     * Print a string representation of this DiskStatus.
     * @see #machineName
     * @see #diskName
     * @see #diskPercentUsed
     * @see #diskFreeSpace
     */
    public String toString()
    {
    		return new String("DISK:"+machineName+":"+diskName+":"+diskPercentUsed+
    				"% used:"+diskFreeSpace+"Kb free space.");
    }
}
