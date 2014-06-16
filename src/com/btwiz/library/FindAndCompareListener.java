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
 * A listener used by the system for the discovery procedure and which encapsulates
 * both lookupListener & deviceComparator capabilities
 */
public class FindAndCompareListener implements IDeviceLookupListener, IFindAndCompareListener {
	private final IDeviceLookupListener lookupListener;
	private final IDeviceComparator comparator;
	
	public FindAndCompareListener(IDeviceLookupListener lookupListener, IDeviceComparator comparator) {
		if (lookupListener==null || comparator==null) {
			throw new RuntimeException("Bad FindAndCompareListener params!");
		}
		this.lookupListener = lookupListener;
		this.comparator = comparator;
	}
	
	@Override
	public IDeviceLookupListener getLookupListener() {
		return lookupListener;
	}

	@Override
	public boolean onDeviceFound(BluetoothDevice device, boolean byDiscovery) {
		return lookupListener.onDeviceFound(device, byDiscovery);
	}

 
	@Override
	public boolean match(BluetoothDevice device) {
		return comparator.match(device);
	}

	@Override
	public void onDeviceNotFound(boolean byDiscovery) {
		lookupListener.onDeviceNotFound(byDiscovery);		
	}

}
