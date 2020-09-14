get_system_gosubs2 is called from the occ's crontab, and parses a 'df -k' on that machine to extract some disk statistics which are written to /occ/data/system.data ($DEPLOY_DATA/system.data).
These values are then read by BasicDiskStatusProvider.java, as configured by disk_status.properties (which should be configured to match the contents of get_system_gosubs2.
