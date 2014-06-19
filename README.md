<!---
/*
 *
 *
 *
 *
 * BTWiz for asynchronouse Bluetooth in Android
 * http://www.mobileEdge.co.il
 *
 *
 *
 *
 *
 * Copyright 2014 Gilad Haimov and the Mobile Edge team
 *
 * Gilad Haimov licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
-->
<p align="center"><img src="http://i.imgur.com/c6NmMh8.jpg"></p>

# BTWiz - Async Bluetooth for Android

### Abstract

BTWiz is an internal library developed & used by my team: www.mobileedge.co.il in Android Bluetooth projects for the last 3 years.
It is hereby released as an open source project.

For licensing details please visit http://www.apache.org/licenses/LICENSE-2.0

If you wish to comment on/contribute to BTWiz, feel free to contact me directly.

Gilad Haimov<br/>
gilad@MobileEdge.co.il
<br/>



### Benefits
BTWiz was designed with the following goals in mind:
 * **Simplicity**<br/>
   Make the Bluetooth initial wiring much simpler to code.
 * **Correctness**<br/>
   Make correct logic easy and incorrect logic harder to write
 * **Asynchronicity**<br/>
   Allow simple yet robust asynchronous activation of Bluetooth commands

BTWiz deals internally with a lot of the Bluetooth initial wiring complexities which are so easy to get wrong even for an experienced Bluetooth developers:

 * **Bluetooth Support**<br/>
   It Force you to check if the device support Bluetooth and, if not, whether or not the user may enable Bluetooth on device.
 * **Efficient Discovery**<br/>
   BTWiz starts device detection by querying the paired device list to see if the desired device is already known, before performing full blown discovery.
 * **Secure as Default**<br/>
   It will provide you with the correct default (SECURE) when connecting to another device and will allow you, but only in a manifest manner, to prefer non-secure communication.
 * **Connection Failover**<br/>
   BTWiz uses highly effective fall-through mechanism that solves many of the BT connection
   problems encountered by our team as well as others and roughly follows the following lines<br/>
   - If in SECURE mode create socket via createRfcommSocketToServiceRecord<br/>
   - If in INSECURE mode create socket via createInsecureRfcommSocketToServiceRecord<br/>
   - For API versions >= 15 call device.getUuids() to get supported features<br/>
   - For API versions < 15 attempt to activate "getUuids" via java reflection<br/>
   - If the above failed, re-attempt connection using the default SSP = "00001101-0000-1000-8000-00805F9B34FB"<br/>
   - If the above failed in SECURE mode activate by reflection "createRfcommSocket" to create socket<br/>
   - If the above failed in INSECURE mode activate by reflection "createInsecureRfcommSocket" to create socket<br/>


Using BTWize saves us a significant amount of time and error-handling. We encourage you to enjoy these benefits in your next Android Bluetooth project.


### Installation
Simply add BTWiz_xxx.jar to your project's libs/ folder. Make sure your manifest contains BLUETOOTH permission and, if admin-level ops are used, also BLUETOOTH_ADMIN permission.

### Usage
 * **Initial check for device Bluetooth support**<br/>
```java


try {
  if (!BTWiz.isEnabled(context)) {
     // TODO call startActivity with BTWiz.enableBTIntent() allowing user to enable BT
     return;
  }
} catch (DeviceNotSupportBluetooth e) {
   // TODO disable Bluetooth functionality in your app
   return;
}


```

 * **Device lookup**<br/>
  Start with the bonded device list and optionally continue to discovery:
```java

BTWiz.lookupDeviceAsync(context, comparator, lookupListener, DISCOVER_IF_NEEDED);

```

Comparator is used to identify a device and must implement IDeviceComparator.
The built in DeviceMajorComparator should suffice for many real life tasks.

DISCOVER_IF_NEEDED is a boolean flag that, if set to true, will move to performing discovery if the looked-for device is not in bonded device list.

 * **Discover all nearby devices**<br/>
```java

BTWiz.startDiscoveryAsync(context, completeListener, deviceDiscoveredListener);

```

deviceDiscoveredListener will be called for each newly discovered device
completeListener will be called when action is completed

 * **Establish a Bluetooth server**<br/>
 And accept() new connections
```java

BTWiz.listenForConnectionsAsync("MyServerName", acceptListener, secureMode);

```

If secureMode equals SECURE - a secure RFCOMM Bluetooth socket will be used.
Else: an insecure RFCOMM Bluetooth socket.


 * **Get list of all bonded device**<br/>
```java

Set<BluetoothDevice> arr = BTWiz.getAllBondedDevices(context);

```

 * **Perform asynchronous IO**<br/>
```java

BTSocket.readAsync()
BTSocket.writeAsync();

```

Where BTSocket is a wrapper to the standard Bluetooth socket and is mainly used to allow asynchronous IO over the connected/accepted socket.

 * **Cleanup**<br/>
IMPORTANT: at the end of Bluetooth processing a cleanup method should be called.
```java

BTWiz.cleanup();

```

### Code Example
Taken from BTWiz test class<br/><br/>
```java

	/**
	 * Test looking up and connecting to a single device, identified by major number & name, via getBTDeviceAsync.
	 * Note that if device is not part of the bonded list a discovery process will be initiated.
	 *  Set name to null to disable comparison by name
	 *  Set major to -1 to disable comparison by major
	 */
	public static void connectToDevice(final Context context, final int major, 
			final String name, final SecureMode secureMode, final UUID serviceUuid) { 		
		try {
			if (!BTWiz.isEnabled(context)) {
				// TODO call startActivity with BTWiz.enableBTIntent() allowing user to enable BT
				return; 
			}
		} catch (DeviceNotSupportBluetooth e) {
			// TODO disable BT functionality in your app
			return;
		}
		
		final IDeviceConnectionListener deviceConnectionListener = new IDeviceConnectionListener() {			
			@Override
			public void onConnectionError(Exception exception, String where) {
				// TODO handle connection error
				Log.e("Tester", "Connection error: " + exception + " at " + where);  
			}			
			@Override
			public void onConnectSuccess(BTSocket clientSocket) {
				// TODO work with new connection e.g. using
				// async IO methods: readAsync() & writeAsync()
				// or synchronous read() & write()
				Log.d("Tester", "Connected to new device"); 
			}
		};
		

		// declare a connecting listener
		IDeviceLookupListener lookupListener = new IDeviceLookupListener() {
			@Override
			public boolean onDeviceFound(BluetoothDevice device, boolean byDiscovery) {
				// log
				String name = device.getName();
				String addr = device.getAddress();
				int major = device.getBluetoothClass().getMajorDeviceClass();
				String majorStr = Utils.majorToString(major);
				Log.d("Tester", "Discovered device: " + name + ", " + addr + ", " + majorStr);				
				// and connect to the newly found device
				BTWiz.connectAsClientAsync(context, device, deviceConnectionListener, secureMode, serviceUuid); 
				return false; // and terminate discovery
			}
			@Override
			public void onDeviceNotFound(boolean byDiscovery) {
				// TODO handle discovery failure
				Log.d("Tester", "Failed to discover device"); 				
			}
		}; 
		 
		final boolean DISCOVER_IF_NEEDED = true; // start discovery if device not found in bonded-devices list 
		DeviceMajorComparator comparator = new DeviceMajorComparator(major, name);

		BTWiz.lookupDeviceAsync(context, comparator, lookupListener, DISCOVER_IF_NEEDED);
		
		// TODO call BTWiz.cleanup() at end of BT processing 
	}

	
	/**
	 * Retrieves a list of all currently bonded devices (no discovery)
	 */
	public static void getAllBondedDevices(final Context context) {
		try {
			if (!BTWiz.isEnabled(context)) { 
				// TODO call startActivity with BTWiz.enableBTIntent() allowing user to enable BT
				return;
			}
		} catch (DeviceNotSupportBluetooth e) {
			// TODO disable BT functionality in your app
			return;
		}
		
		Set<BluetoothDevice> allBondedDevices = BTWiz.getAllBondedDevices(context);
		
		if (allBondedDevices != null) {
			int num = allBondedDevices.size();
			Log.d("Tester", num + " bonded devices were found");
			for (BluetoothDevice device: allBondedDevices) {
				// TODO do something with device
				String name = device.getName();
				String addr = device.getAddress();
				int major = device.getBluetoothClass().getMajorDeviceClass();
				String majorStr = Utils.majorToString(major);
				Log.d("Tester", "Bonded device: " + name + ", " + addr + ", " + majorStr);				
			}
		}
		else {
			// error had occurred
		}
		
		// TODO call BTWiz.cleanup() at end of BT processing 
	}

		
	/**
	 * Run discovery and returns a list of all discovered devices
	 */
	public static void discoverNearbyDevices(final Context context) {		
		try {
			if (!BTWiz.isEnabled(context)) {
				// TODO call startActivity with BTWiz.enableBTIntent() allowing user to enable BT
				return; 
			}
		} catch (DeviceNotSupportBluetooth e) {
			// TODO disable BT functionality in your app
			return; 
		}
		
		
		final MarkCompletionListener completeListener = new MarkCompletionListener(); 
		final GetAllDevicesListener deviceDiscoveredListener = new GetAllDevicesListener(); 
		
		boolean started = BTWiz.startDiscoveryAsync(context, completeListener, deviceDiscoveredListener);				
		if (!started) {
			// TODO handle discovery error
			return; 
		}
		

		
		// start a thread that will block until discovery has completed. 
		// note: this is for demonstration only. standard apps should typically 
		// create a IDeviceLookupListener implementation class to handle each new discovered
		// device as it arrives and not block a thread until discovery completion   
		new Thread() {
			public void run() {
				completeListener.blockUntilCompletion(); 				
				// and iterate results
				ArrayList<BluetoothDevice> allDiscoveredDevices = deviceDiscoveredListener.getAll(); 
				for (BluetoothDevice device: allDiscoveredDevices) {
					String name = device.getName();
					String addr = device.getAddress();
					int major = device.getBluetoothClass().getMajorDeviceClass();
					String majorStr = Utils.majorToString(major);
					Log.d("Tester", "Discovered device: " + name + ", " + addr + ", " + majorStr); 
				}
				
				BTWiz.cleanup(context); // cleanup BT resources
				
			};
		}.start();	 	
	}
	

	/**
	 * Test becoming a BT server and accept()ing new connections
	 * If secureMode equals SECURE: listenUsingRfcommWithServiceRecord will be internally activated
	 * Else: listenUsingInsecureRfcommWithServiceRecord
	 */
	public static void listenForConnections(final Context context, SecureMode secureMode) {		
		try {
			if (!BTWiz.isEnabled(context)) {
				// TODO call startActivity with BTWiz.enableBTIntent() allowing user to enable BT
				return;
			}
		} catch (DeviceNotSupportBluetooth e) {
			// TODO disable BT functionality in your app
			return;
		}

		IAcceptListener acceptListener = new IAcceptListener() {			
			@Override
			public void onNewConnectionAccepted(BTSocket newConnection) {
				// log
				BluetoothDevice device = newConnection.getRemoteDevice(); 
				String name = device.getName();
				String addr = device.getAddress();
				int major = device.getBluetoothClass().getMajorDeviceClass();
				String majorStr = Utils.majorToString(major);
				Log.d("Tester", "New connection: " + name + ", " + addr + ", " + majorStr);
				
				// TODO work with new connection e.g. using
				// async IO methods: readAsync() & writeAsync()
				// or synchronous read() & write()
			} 
			@Override
			public void onError(Exception e, String where) {
				// TODO handle error
				Log.e("Tester", "Connection error " + e + " at " + where);
			}
		};
		
		BTWiz.listenForConnectionsAsync("MyServerName", acceptListener, secureMode);
		
		Log.d("Tester", "Async listener activated"); 

		// Note: to terminate listen session call BTWiz.stopListening()
		
		// TODO call BTWiz.cleanup() at end of BT processing 
	}



```

