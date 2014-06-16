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
package com.btwiz.test;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import com.btwiz.library.BTSocket;
import com.btwiz.library.BTWiz;
import com.btwiz.library.DeviceMajorComparator;
import com.btwiz.library.DeviceNotSupportBluetooth;
import com.btwiz.library.GetAllDevicesListener;
import com.btwiz.library.IAcceptListener;
import com.btwiz.library.IDeviceConnectionListener;
import com.btwiz.library.IDeviceLookupListener;
import com.btwiz.library.MarkCompletionListener;
import com.btwiz.library.SecureMode;
import com.btwiz.library.Utils;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;


/**
 * Test class 
 */
public class Tester {

	
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

}
