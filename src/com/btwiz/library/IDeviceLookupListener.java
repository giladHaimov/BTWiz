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

import android.bluetooth.BluetoothDevice;

/**
 * Interface for all lookup listeners
 */
public interface IDeviceLookupListener {
	/**
	 * Called when the sought after device was detected. 
	 * If true is returned - the lookup procedure is to continue
	 */
	boolean onDeviceFound(BluetoothDevice device, boolean byDiscovery); 

	/**
	 * Called when the lookup procedure was completed without detecting the sought after device
	 */
	void onDeviceNotFound(boolean byDiscovery);
}
