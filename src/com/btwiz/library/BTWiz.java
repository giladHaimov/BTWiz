/*******************************************************************************
 * Copyright 2014 Gilad Haimov  gilad@mobileEdge.co.il
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.btwiz.library;

import static com.btwiz.library.DiscoveryStatus.FINISHED;
import static com.btwiz.library.DiscoveryStatus.NOT_STARTED;
import static com.btwiz.library.DiscoveryStatus.STARTED;
import static com.btwiz.library.SecureMode.SECURE;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build.VERSION_CODES;
import android.os.ParcelUuid;
import android.util.Log;

/**
 * The main execution class of library.
 * Dealing with initialization, main-flow methods and cleanup.
 * Checks and bail out if BT is not enabled 
 * Main device connection method: getBTDeviceAsync()
 *  
 * 
 * Typical execution flow might look like this:
 * 
 *			if (!BTWiz.isEnabled(context)) {
 *				call startActivity with BTWiz.enableBTIntent() allowing user to enable BT
 *				return;
 *			}
 *
 *			IDeviceConnectionListener connListener = define a connection state listener
 *
 *			IDeviceLookupListener lookupListener = define a device lookup listener
 *
 *			DeviceMajorComparator comparator = use any of the DeviceMajorComparator static factory methods
 *
 *			// get a reference to a specific device by testing for bonded devices 
 *_         // before performing a (more costly) full blown discovery. the last param 
 *          // of this method allows disabling of discovery-if-no-bonded-device behavior
 *			BTWiz.getBTDeviceAsync(context, comparator, lookupListener, DISCOVER_IF_NEEDED);
 * 
 * 			....
 * 
 * 			BTWiz.cleanup(); // must be called when BT resources are no longer needed
 * 
 * Note the Async notationL: methods which perform background operation are postfix-ed Async
 * 
 * 
 */

 
public class BTWiz {
		 
	private static volatile UUID appUuid = UUID.randomUUID();
	 
	private static BluetoothAdapter bluetoothAdapter;

	private static volatile DiscoveryStatus discoveryStatus = NOT_STARTED;

	private static BroadcastReceiver discoveryReceiver;
	
	private static ArrayList<BluetoothDevice> allDiscoveredDevices;
	
	private static final ArrayList<BTSocket> allSockets = new ArrayList<BTSocket>();  
	
	private static BluetoothServerSocket btServerSocket;
	
	private static boolean autoOpenSocketStreams = true;
	
	private static volatile boolean listeningIsOn;
	
	private static volatile boolean connectingNow;

	private static volatile boolean protectAgainstDuplicates = false;  

	private static final UUID DEFAULT_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	
	private BTWiz() {} // non instantiable

	
	/**
	 * Set app UUID to the well-known SPP (serial port profile) UUID. 
	 * To be used when connecting to a BT serial board.
	 * Not to be used when connecting to an Android peer
	 */
	public static void setUuidToSPP() { 
		setUuid(DEFAULT_SPP_UUID);
	}

	
	/**
	 * If set to true: protect against scan operation returning duplicate device reads
	 */
	public static void setProtectAgainstDuplicates(boolean protect) { 
		protectAgainstDuplicates = protect;
	}

	
	/**
	 * Set app UUID to a user defined value 
	 */
	public static void setUuid(UUID newVal) {
		if (newVal==null) {
			throw new RuntimeException("Cannot use null UUID!");
		}
		appUuid = newVal;
	}

	/**
	 * Getter for the UUID used for app BT related actions 
	 */
	public static UUID getAppUUID() {
		return appUuid;
	}
	
	
	
	/**
	 * Toggle auto opening of inputStream and outputStream for newly acquired BT sockets 
	 */
	public static void setAutoOpenSocketStreams(boolean autoOpen) {
		autoOpenSocketStreams = autoOpen;
	}

//	/**
//	 * Disable checking for BT permissions (and throwing an exception if missing)
//	 */
//	public static void disablePermissionsCheck() {
//		PermissionValidator.checkBTPermissions = false;
//	}

	
	/**
	 * Returns true if BT is enabled on device.
	 * 
	 * Recommended handling for not-enabled device: startActivity 
	 * with BTWiz.enableBTIntent() to allow enabling by the user
	 */
	public static boolean isEnabled(Context context) throws DeviceNotSupportBluetooth {
		init(context);		
		boolean isEnabled = bluetoothAdapter.isEnabled(); // Equivalent to: getBluetoothState() == STATE_ON 
		if (!isEnabled) { 
			Log.e("BTWiz", "BT is not enabled on this device");
		}
		return isEnabled;
	}

	
	/**
	 * Looks for a BT device passing IDeviceComparator.match()
	 * If a BondedDevice is found it is used.
	 * If no, and discoverIfNeeded==true, a discovery procedure is initiated
	 */
	public static void lookupDeviceAsync(Context context, IDeviceComparator comparator,  
			IDeviceLookupListener lookupListener, boolean discoverIfNeeded) {
		assertInitialized();
		
		// Before performing device discovery, its worth querying the set of 
		// paired devices to see if the desired device is already known
		
		BluetoothDevice device = findBondedDevice(context, comparator);  
		if (device != null) {
			lookupListener.onDeviceFound(device, false);
			return;
		}		
		
		if (discoverIfNeeded) {
			discoverBTDeviceAsync(context, comparator, lookupListener);
		}
		else {
			lookupListener.onDeviceNotFound(false); 			
		}
	}

	
	/**
	 * Perform discovery looking for a specific device to be identified via comparator 
	 */
	public static boolean discoverBTDeviceAsync(Context context, 
			IDeviceComparator comparator, IDeviceLookupListener lookupListener) {
		assertInitialized();
		FindAndCompareListener fcListener = new FindAndCompareListener(lookupListener, comparator);    

		boolean started = startDiscoveryAsync(context, null, fcListener);
		if (!started) {
			lookupListener.onDeviceNotFound(false); 
		}
		return started;
	}


	/**
	 * Create a secure BT socket and connect to a remote BT device (server). 
	 * Spawns a dedicated connect thread 
	 */
	public static void connectAsClientAsync(final Context context, final BluetoothDevice device, 
			final IDeviceConnectionListener connectionListener) {
		connectAsClientAsync(context, device, connectionListener, SECURE, null);
	}
	
	
	/**
	 * Create a BT socket and connect to a remote BT device (server).
	 * 
	 * The secureMode param controls the type of RFC socket created i.e. weather 
	 * createRfcommSocketToServiceRecord or createInsecureRfcommSocketToServiceRecord services will be called.
	 *   
	 * Spawns a dedicated connect thread 
	 */
	public static void connectAsClientAsync(final Context context, final BluetoothDevice device, 
			final IDeviceConnectionListener connectionListener, final SecureMode secureMode, final UUID user_serviceUuid) {
		new Thread() {
			public void run() {
				boolean connected;
				BluetoothSocket sock;
				UUID serviceUuid = user_serviceUuid;
				if (serviceUuid != null) {
					sock = createClientSocket(device, connectionListener, secureMode, serviceUuid);
					if (sock == null) {
						connectionListener.onConnectionError(null, "createClientSocket");
						return; // operation failed
					} 
					connected = innerConnectAsClient(context, connectionListener, sock);
					if (!connected) {
						connectionListener.onConnectionError(null, "ConnectAsClient");
						return; // operation failed
					}
					// else: success
				}
				else {
					UUID[] uuids = getSupportedUuids(context, device);
					if (uuids != null && uuids.length > 0) {
						serviceUuid = uuids[0]; 
						sock = createClientSocket(device, connectionListener, secureMode, serviceUuid);
						if (sock != null) {
							connected = innerConnectAsClient(context, connectionListener, sock);
							if (connected) {
								return; // success
							}
						}
					}
					
					// failover 1: use DEFAULT_SPP_UUID 
					sock = createClientSocket(device, connectionListener, secureMode, DEFAULT_SPP_UUID);
					if (sock != null) {
						connected = innerConnectAsClient(context, connectionListener, sock);
						if (connected) {
							return; // success
						}
					}
					
					// failover 2: use createRfcommSocket via reflection
					sock = createRfcommSocketViaReflection(device, secureMode);
					if (sock == null) {
						connectionListener.onConnectionError(null, "createRfcommSocket");
						return; // failed
					}
					connected = innerConnectAsClient(context, connectionListener, sock);
					if (!connected) {
						connectionListener.onConnectionError(null, "ConnectAsClient");
						return; // failed
					}
					// else: success
				}				
			}				

		}.start();
	}
	
	
	protected static BluetoothSocket createRfcommSocketViaReflection(BluetoothDevice device, SecureMode secureMode) {
		try {
			// see http://stackoverflow.com/questions/14906721/android-bluetooth-connection-refused 
			// for explanation of call to createRfcommSocketToServiceRecord 
			// device.createRfcommSocketToServiceRecord(uuid);

			Method createMethod;
			if (secureMode == SecureMode.SECURE) {
				createMethod = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class });
			}
			else {			
				createMethod = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
			}
			if (createMethod == null) {
				throw new RuntimeException("createMethod not found");
			}
			BluetoothSocket sock = (BluetoothSocket)createMethod.invoke(device, 1);
			if (sock==null) {
				throw new RuntimeException("createRfcommSocket activation failed");
			}
			return sock;
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("BTWiz", "Activation of createRfcommSocket via reflection failed: " + e);
			return null; 
		}		         
	}


	private static boolean innerConnectAsClient(Context context, IDeviceConnectionListener connectionListener, BluetoothSocket sock) {		
		BTSocket clientSocket = new BTSocket(sock, autoOpenSocketStreams); 
		// You should always ensure that the device is not performing device discovery when you call connect().
		cancelDiscovery(context);								
		try {
			// block until success
			BTWiz.markConnecting(true);
			clientSocket.connect(context);
			BTWiz.markConnecting(false);
			connectionListener.onConnectSuccess(clientSocket);
			return true; // success
		} catch (Exception e) {
			Log.e("BTWiz", "Connect error: " + e);
			clientSocket.close();
			//connectionListener.onConnectionError(e, "connect");
		}		 
		finally {
			BTWiz.markConnecting(false);
		}
		return false; // failure
	}
	
	private static BluetoothSocket createClientSocket(BluetoothDevice device, IDeviceConnectionListener connectionListener, 
			SecureMode secureMode, UUID serviceUuid) {
		BluetoothSocket sock;
		try {
			if (secureMode == SECURE) {				
				sock = device.createRfcommSocketToServiceRecord(serviceUuid);
			}
			else { // INSECURE					
				sock = device.createInsecureRfcommSocketToServiceRecord(serviceUuid);
			}
			if (sock == null) {
				Log.e("BTWiz", "Null socket error after createRfcommSocket" );
				//connectionListener.onConnectionError(null, "Null socket");
			}
			return sock;

		} catch (IOException e) { 
			Log.e("BTWiz", "Error in createRfcommSocket: " + e);
			//connectionListener.onConnectionError(e, "createRfcomm");
			return null;
		}
	}


   	/**
   	 * Returns the supported features (UUIDs) of the remote device (no discovery!)
   	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@SuppressLint("NewApi")
	public static UUID[] getSupportedUuids(Context context, BluetoothDevice device) {
		ParcelUuid[] pUuids = null;
		if (Utils.getApiVersion() >= VERSION_CODES.ICE_CREAM_SANDWICH_MR1) { // apiVer 15
			pUuids = device.getUuids();
		}
		else {
			// getUuids is (?) an hidden api; use reflection
			try { 
				Class cl = Class.forName("android.bluetooth.BluetoothDevice");
				Class[] params = {};
				Method method = cl.getMethod("getUuids", params);
				Object[] args = {};
				pUuids = (ParcelUuid[])method.invoke(device, args);
			}
			catch (Exception e) { 
				// no op
				Log.e("BTWiz", "Activation of getUuids() via reflection failed: " + e);
			}
		}
		
		if (pUuids == null || pUuids.length == 0) {
			return null;
		}
		int len = pUuids.length; 
		UUID[] results = new UUID[len];
		for (int i = 0; i < len; i++) {
			results[i] = pUuids[i].getUuid(); 
		}
		return results;
	}


	/**
	 * Create a secure-mode BT server, enter an accept loop and listen for BT connections 
	 * If AcceptMode==MANY: will stay in loop indefinitely  
	 * Spawns a dedicated connect thread 
	 */
	public static void listenForConnectionsAsync(final String name, final IAcceptListener acceptListener) {
		listenForConnectionsAsync(name, acceptListener, SECURE); 
	}
	
	/**
	 * Create a BT server, enter an accept loop and listen for BT connections 
	 * If AcceptMode==MANY: will stay in loop indefinitely
	 * 
	 * The secureMode param controls the secure mode of the server, i.e. weather listenUsingRfcommWithServiceRecord
	 * or listenUsingInsecureRfcommWithServiceRecord services will be called.
	 *    
	 * Spawns a dedicated connect thread 
	 */
	public static void listenForConnectionsAsync(final String name, final IAcceptListener acceptListener, final SecureMode secureMode) {
		listeningIsOn = true;
		new Thread() {
			public void run() {
				try { 
					createBTServerSocket(name, secureMode);
				}
				catch (Exception e) {
					Log.e("BTWiz", "Socket creation error: " + e);
					acceptListener.onError(e, "createBTServerSocket");
					return;
				}
				
				try { 
					boolean goOn = true;
					while (listeningIsOn && goOn) {
						goOn = acceptConnection(acceptListener); 
					}
				}
				finally {
					closeBTServerSocket();
				}
			}
		}.start();
	}

	/**
	 * Accepts an incoming BT connection
	 */ 
	public static boolean acceptConnection(IAcceptListener acceptListener) {
		Utils.assertNotUIThread();
		assertInitialized();
		BluetoothSocket sock;
		
		try {
			sock = btServerSocket.accept(); // returns a connected socket
		} catch (IOException e) {
			Log.e("BTWiz", "Socket accept error: " + e);
			acceptListener.onError(e, "accept");
			e.printStackTrace();
			return false; // exit accept loop
		}

		if (sock == null) {
			return true; // accept() failed; re-enter function
		}
		
		BTSocket newConnection = new BTSocket(sock, autoOpenSocketStreams); 

		acceptListener.onNewConnectionAccepted(newConnection);
		return true; // go on
	}
	

	/**
	 * Creates a server socket to be used by the accept thread
	 */
	public static void createBTServerSocket(String name, SecureMode secureMode) throws IOException {
		Utils.assertNotUIThread();
		assertInitialized();
		
		BluetoothServerSocket tmp;
		btServerSocket = null;
		try {
			if (secureMode == SECURE) {
				tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(name, appUuid);
			}
			else { // INSECURE
				tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(name, appUuid);
			}
			btServerSocket = tmp;
		} catch (IOException e) {
			Log.e("BTWiz", "listenUsingRfcomm error: " + e);
			e.printStackTrace();
			throw e;
		}		
	}

	/**
	 * Cleanup all BT resources. 
	 * Should be called once BT usage by the app is completed
	 */
	public static void cleanup(Context context) {
		stopDiscovery(context); //  an application should always call cancelDiscovery() even if it did not directly request a discovery, just to be sure
		closeBTServerSocket();
		closeAllOpenSockets();
		BTSocket.closeIOThreads();
		bluetoothAdapter = null;
		allDiscoveredDevices = null;
	}

	
	/**
	 * Terminate BT listening server session  
	 */
	public static void stopListening() {
		closeBTServerSocket();
	}
	

	private static void closeBTServerSocket() {
		listeningIsOn = false;
		if (btServerSocket != null) {
			try { 
				btServerSocket.close(); // thread safe
			}
			catch (Exception e) {
				// no op
			}
			btServerSocket = null;
		}
	}
	
	
	/**
	 * Getter for the BluetoothServerSocket (possibly null)
	 */
	public static BluetoothServerSocket getBTServerSocket() {
		return btServerSocket;
	}

	public static boolean startDiscoveryAsync(Context context) {
		return startDiscoveryAsync(context, null);
	}


	public static boolean startDiscoveryAsync(Context context, Runnable onFinished) {
		return startDiscoveryAsync(context, onFinished, null);
	}


	/**
	 * Starts a BT discovery procedure  
	 */
	public static boolean startDiscoveryAsync(Context context, Runnable onFinished, IDeviceLookupListener foundHandler) {
		assertInitialized();
//		if (!PermissionValidator.adminPermissionIsSet(context)) {
//			Log.e("BTWiz", "App must be granted an android.permission.BLUETOOTH_ADMIN permission!");
//		}
		allDiscoveredDevices = new ArrayList<BluetoothDevice>(); 
		registerDiscoveryReceiver(context, onFinished, foundHandler);
		
		// cancel a prior scan, if any
		cancelDiscovery(context);
		
		if (connectingNow) {
			Log.i("BTWiz", "\n\n\n Performing discovery while connection is in process. This may lead to bad performance.\n\n\n");
		}
		boolean started = bluetoothAdapter.startDiscovery(); //async call!
		if (!started) {
			unregisterDiscoveryReceiver(context);
			discoveryStatus = NOT_STARTED;
		}
		return started;
	}


	/**
	 * Returns all devices discovered by a (possibly still active) discovery procedure  
	 */
	public static ArrayList<BluetoothDevice> getAllDiscoveredDevices() {
		return allDiscoveredDevices;
	}


	private static void unregisterDiscoveryReceiver(Context context) {
		if (discoveryReceiver != null) {
			try { 
				context.unregisterReceiver(discoveryReceiver);
			}
			catch (Exception e) {
				// no op
			}
			discoveryReceiver = null;
		}
	}

	public static DiscoveryStatus getDiscoveryStatus() {
		return discoveryStatus;
	}

	private static Intent registerDiscoveryReceiver(final Context context, final Runnable onFinished, final IDeviceLookupListener foundListener) {
		discoveryReceiver = new BroadcastReceiver() {
			private boolean wasFound = false;
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				System.out.println(action);
				if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
					discoveryStatus = STARTED;
				}
				else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
					discoveryStatus = FINISHED;
					// activate onFinished callback
					if (onFinished != null) {
						onFinished.run();
					}
					if (!wasFound && foundListener != null) {
						foundListener.onDeviceNotFound(true);
					}
				}
				else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					if (protectAgainstDuplicates) {
						if (deviceAlreadyInList(device)) {
							return;
						}
					}
					allDiscoveredDevices.add(device);
					IDeviceLookupListener cur_foundListener = foundListener;
					if (cur_foundListener == null) {
						return;
					}
					if (cur_foundListener instanceof IFindAndCompareListener) {
						// force onDeviceFound() called only for a single device
						IFindAndCompareListener fcListener = (IFindAndCompareListener)cur_foundListener;
						if (!fcListener.match(device)) { 
							// go on with discovery
							return;
						}
						wasFound = true;
						cur_foundListener = fcListener.getLookupListener(); 
					}
					boolean goOn = cur_foundListener.onDeviceFound(device, true);
					if (!goOn) {
						stopDiscovery(context);
					}
				}
			}	
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		return context.registerReceiver(discoveryReceiver, filter);  
	}


	/**
	 * Test for existence of a newly discovered device in the 'discovered' list 
	 * Matched by name & major fields
	 */
	protected static boolean deviceAlreadyInList(BluetoothDevice device) {
		int deviceMajor = device.getBluetoothClass().getMajorDeviceClass();
		String deviceName = device.getName();
		deviceName = deviceName==null ? "" : deviceName;		
		for (BluetoothDevice prior: allDiscoveredDevices) {
			int priorMajor = prior.getBluetoothClass().getMajorDeviceClass();
			String priorName = prior.getName();
			if (deviceMajor != priorMajor) {
				continue;
			}
			if (!deviceName.equals(priorName)) {
				continue;
			}
			// match
			return true;
		}
		return false;
	}


	/**
	 * Terminates a running discovery procedure  
	 */
	public static void stopDiscovery(Context context) {
		cancelDiscovery(context);
		unregisterDiscoveryReceiver(context); //!
	}


	public static void cancelDiscovery(Context context) {
		if (bluetoothAdapter != null) {
			try { 
				if (bluetoothAdapter.isDiscovering()) { 
					bluetoothAdapter.cancelDiscovery();
				}
			}
			catch (Exception e) {
				// no op
			}			
		}
	}


	/**
	 * Gets a bonded BT device according to the IDeviceComparator criteria   
	 */
	public static BluetoothDevice findBondedDevice(Context context, IDeviceComparator comparator) {
		Set<BluetoothDevice> pairedDevices = getAllBondedDevices(context);
		if (pairedDevices==null) {
			return null;
		}
		for (BluetoothDevice device: pairedDevices) {
			if (comparator.match(device)) {
				return device;
			}
		}
		return null;
	}


	/**
	 * Return the set of BluetoothDevice objects that are bonded (paired) to the local adapter   
	 */
	public static Set<BluetoothDevice> getAllBondedDevices(Context context) {
		assertInitialized();
		Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
		if (bondedDevices == null) {
			// null is returned only upon error
			Log.e("BTWiz", "Error while calling bluetoothAdapter.getBondedDevices!");
		}
		return bondedDevices;
	}

	
	/**
	 * getter for a raw bluetoothAdapter   
	 */
	public static BluetoothAdapter getBluetoothAdapter() {
		assertInitialized();
		return bluetoothAdapter;
	}
	
	
	public static void closeProfileProxy(int profile, BluetoothProfile profileProxy) {
		if (bluetoothAdapter != null) {
			try { 
				bluetoothAdapter.closeProfileProxy(profile, profileProxy);
			}
			catch (Exception e) {
				// no op
			}
		}
	}


	/**
	 * Checks for device BT support and initializes the bluetoothAdapter
	 * Should not, typically, be called directly by the using app    
	 */
	public static void init(Context context) throws DeviceNotSupportBluetooth {
		if (bluetoothAdapter != null) {
			return; // already initialized
		}
		
//		if (!PermissionValidator.basicPermissionIsSet(context)) {
//			Log.e("BTWiz", "App must be granted an android.permission.BLUETOOTH permission!");
//		}
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Log.e("BTWiz", "Device does not support BT");
			throw new DeviceNotSupportBluetooth(); 
		}
	}

	/**
	 * Validates that init() was called prior to the current operation
	 */
	private static void assertInitialized() {
		if (bluetoothAdapter == null) {
			throw new RuntimeException("Init() must be called prior to this operation!"); 
		}		
	}

	
	/**
	 * Registered a BluetoothSocket for future cleanup 
	 */
	public static void registerForCleanup(BTSocket socket) {
		if (socket==null) {
			throw new RuntimeException("Registered socket cannot be null!"); 
		}
		synchronized (allSockets) {
			allSockets.add(socket);
		}
	}
	
	
 
	/**
	 * Cleans up all opened BluetoothSocket 
	 */
	public static void closeAllOpenSockets() {
		synchronized (allSockets) {
			for (BTSocket sock: allSockets) {
				sock.close();
			}
			allSockets.clear();
		}
	}

	
	/**
	 * Returns an Intent used (startActivity) to allow the user BT enabling 
	 */
	public static Intent enableBTIntent() {
		return new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	}

	/**
	 * Returns an Intent used (startActivity) to allow the user to set 
	 * this device for BT discoverability for default interval =  120 seconds 
	 */
	public static Intent enableBTDiscoverabilityIntent() {
		return enableBTDiscoverabilityIntent(-1);
	}

	/**
	 * Returns an Intent used (startActivity) to allow the user to set 
	 * this device for BT discoverability for a user set interval. Values above
	 * 3600 secs will automatically be set to default value (120 secs)  
	 */
	public static Intent enableBTDiscoverabilityIntent(int duration_secs) {
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		if (duration_secs != -1) {
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration_secs);
		}
		return discoverableIntent;
	}

	
	/**
	 * Returns an Intent used (startActivity) to allow the user to set this device for 
	 * BT discoverability with no time limit 
	 */
	public static Intent enableBTDiscoverabilityIntentForEver() {
		return enableBTDiscoverabilityIntent(0); // a value of 0 means the device is always discoverable
	}


	public static void markConnecting(boolean connecting) {
		connectingNow = connecting; 		
	}
	
}

