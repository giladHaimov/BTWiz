
BTWiz is an internal library developed & used by my team: www.mobileedge.co.il 
in Android Bluetooth projects for the last 3 years,

It is hereby released as an open source project.

For licensing details please visit http://www.apache.org/licenses/LICENSE-2.0

BTWiz was designed with the following goals in mind:
  1. Simplicity: Make the Bluetooth initial wiring much simpler to code
  2. Correctness: Make correct logic easy and incorrect logic harder to write
  3. Asynchronicity: Allow simple yet robust asynchronous activation of Bluetooth commands


BTWiz deals internally with a lot of the Bluetooth initial wiring complexities.
which are so easy to get wrong even for an experienced Bluetooth developers

- It Force you to check if the device support Bluetooth and, if not, whether or not 
the user may enable Bluetooth on device

- It makes is the default to query the paired device list to see if the desired device 
is already known, before performing full blown discovery.
			
- It will provide you with the correct default (SECURE) when connecting to another 
device and will allow you, but only in a manifest manner, to prefer non-secure communication.

- It internally implements a (messy but effective) fall-through mechanism that solves 
many of the connection problems our team, and others, have encountered. This mechanism 
involves getting a list of supported UUIDs (different handling for < apiVer 15 andf >= apiVer 15 
devices) and, if failed, reverting to default SPP UUID = "00001101-0000-1000-8000-00805F9B34FB" 
and,  if all other fails, attempts to activate "createRfcommSocket" service by reflection.


Using BTWize saves us a significant amount of time and error-handling. You are now 
free to enjoy these benefits in your own Bluetooth apps.


Installation
------------------
Simply add BTWiz_v1.2.jar to your project's libs/ folder

Additionally make sure your manifest contains BLUETOOTH permission and, if admin-level 
ops are used, also BLUETOOTH_ADMIN permission.



Usage
-------

A. Initial check for device Bluetooth support
		try {
			if (!BTWiz.isEnabled(context)) {
				// TODO call startActivity with BTWiz.enableBTIntent() allowing user to enable BT
				return;
			}
		} catch (DeviceNotSupportBluetooth e) {
			// TODO disable Bluetooth functionality in your app
			return;
		}

B. Look for a device. Start with the bonded device list and optionally continue to discovery

	   BTWiz.lookupDeviceAsync(context, comparator, lookupListener, DISCOVER_IF_NEEDED);

  Comparator is used to identify a device and must implement IDeviceComparator.
  The built in DeviceMajorComparator should suffice for many real life tasks.

  DISCOVER_IF_NEEDED is a boolean flag that, if set to true, will move to performing
  discovery if the looked-for device is not in bonded device list.
  

C. Discover all nearbye devices
  
	  BTWiz.startDiscoveryAsync(context, completeListener, deviceDiscoveredListener);

  deviceDiscoveredListener will be called for each newly discovered device
  completeListener will be called when action is completed

  
D. Become a Bluetooth server and accept() new connections

  
	 BTWiz.listenForConnectionsAsync("MyServerName", acceptListener, secureMode);

  if secureMode equals SECURE - a secure RFCOMM Bluetooth socket will be used.
  else: an insecure RFCOMM Bluetooth socket.


E. Get a list of all bonded device

     Set<BluetoothDevice> arr = BTWiz.getAllBondedDevices(context);
	 

F. Perform asynchronous IO:
    
	  BTSocket.readAsync()
      BTSocket.writeAsync();

   BTSocket is a wrapper to the standard Bluetooth socket and is mainly used to
   allow asynchronous IO over the connected/accepted socket


G. IMPORTANT: at the end of Bluetooth processing a cleanup method should be called:

   BTWiz.cleanup()


Gilad Haimov
gilad@mobileedge.co.il
  