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

import java.util.ArrayList;

import android.bluetooth.BluetoothDevice;

/**
 * A IDeviceLookupListener implementation class used 
 * to store & fetch all discovered (no filtering)
 */
public class GetAllDevicesListener implements IDeviceLookupListener {
	private final ArrayList<BluetoothDevice> allDiscoveredDevices = new ArrayList<BluetoothDevice>();

	@Override
	public boolean onDeviceFound(BluetoothDevice device, boolean byDiscovery) {
		allDiscoveredDevices.add(device);
		return true; // go on with discovery
	}

	@Override
	public void onDeviceNotFound(boolean byDiscovery) {
		// no op
	}
	
	/**
	 * Get all newly discovered devices. 
	 * Should typically be called after discovery is completed
	 */
	public ArrayList<BluetoothDevice> getAll() {
		return allDiscoveredDevices;
	}
}
