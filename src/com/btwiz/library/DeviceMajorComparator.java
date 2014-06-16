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

import static android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO;
import static android.bluetooth.BluetoothClass.Device.Major.COMPUTER;
import static android.bluetooth.BluetoothClass.Device.Major.HEALTH;
import static android.bluetooth.BluetoothClass.Device.Major.IMAGING;
import static android.bluetooth.BluetoothClass.Device.Major.MISC;
import static android.bluetooth.BluetoothClass.Device.Major.NETWORKING;
import static android.bluetooth.BluetoothClass.Device.Major.PERIPHERAL;
import static android.bluetooth.BluetoothClass.Device.Major.PHONE;
import static android.bluetooth.BluetoothClass.Device.Major.TOY;
import static android.bluetooth.BluetoothClass.Device.Major.UNCATEGORIZED;
import static android.bluetooth.BluetoothClass.Device.Major.WEARABLE;
import android.bluetooth.BluetoothDevice;


/**
 * Allows comparing devices based on their major device class plus an optional device name.   
 * See http://developer.android.com/reference/android/bluetooth/BluetoothClass.Device.Major.html 
 */
public class DeviceMajorComparator implements IDeviceComparator {

	/**
	 * Major device class to be matched against, e.g. AUDIO_VIDEO, COMPUTER, HEALTH   
	 */
	public final int majorDeviceClass;
	
	
	/**
	 * Optional device name to be matched against
	 */
	public final String name;
 
	public DeviceMajorComparator(int majorDeviceClass) {
		this(majorDeviceClass, null);
	}

	public DeviceMajorComparator(String name) {
		this(-1, name);
	}

	public DeviceMajorComparator(int majorDeviceClass, String name) {
		this.majorDeviceClass = majorDeviceClass;
		this.name = name;

	}

	/**
	 * Perform the comparison operation, returning true upon successful match.
	 * If both majorDeviceClass and name were set, success requires BOTH will be matched.  
	 */
	@Override
	public boolean match(BluetoothDevice device) {
		int deviceMajor = device.getBluetoothClass().getMajorDeviceClass(); // e.g. AUDIO_VIDEO, COMPUTER..
		String deviceName = device.getName();
		if (this.majorDeviceClass > -1) {
			if (this.majorDeviceClass != deviceMajor) {
				return false;
			}
		}
		if (this.name != null) {
			if (!this.name.equals(deviceName)) {
				return false;
			}						
		}
		return true;
	}

	
	/**
	 * Utility static factories allowing creation of comparator for each
	 * major device class. See http://developer.android.com/reference/android/bluetooth/BluetoothClass.Device.Major.html   
	 */
	
	/**
	 * An accept-all comparator. Will match true any device whatsoever.
	 */
	public static DeviceMajorComparator anyDevice() {
		return new DeviceMajorComparator(-1);
	}
	
	/**
	 * Create a AUDIO_VIDEO type comparator
	 */
	public static DeviceMajorComparator audioVideo() {
		return new DeviceMajorComparator(AUDIO_VIDEO);
	}
	
	/**
	 * Create a COMPUTER type comparator
	 */
	public static DeviceMajorComparator computer() {
		return new DeviceMajorComparator(COMPUTER);
	}

	/**
	 * Create a HEALTH type comparator
	 */
	public static DeviceMajorComparator health() {
		return new DeviceMajorComparator(HEALTH);
	}
	
	/**
	 * Create a IMAGING type comparator
	 */
	public static DeviceMajorComparator imaging() {
		return new DeviceMajorComparator(IMAGING);
	}
	
	/**
	 * Create a MISC type comparator
	 */
	public static DeviceMajorComparator misc() {
		return new DeviceMajorComparator(MISC);
	}
	
	/**
	 * Create a NETWORKING type comparator
	 */
	public static DeviceMajorComparator networking() {
		return new DeviceMajorComparator(NETWORKING);
	}
	
	/**
	 * Create a PERIPHERAL type comparator
	 */
	public static DeviceMajorComparator peripheral() {
		return new DeviceMajorComparator(PERIPHERAL);
	}
	
	/**
	 * Create a PHONE type comparator
	 */
	public static DeviceMajorComparator phone() {
		return new DeviceMajorComparator(PHONE);
	}
	
	/**
	 * Create a TOY type comparator
	 */
	public static DeviceMajorComparator toy() {
		return new DeviceMajorComparator(TOY);
	}
	
	/**
	 * Create a UNCATEGORIZED type comparator
	 */
	public static DeviceMajorComparator uncategorized() {
		return new DeviceMajorComparator(UNCATEGORIZED);
	}
	
	/**
	 * Create a WEARABLE type comparator
	 */
	public static DeviceMajorComparator wearable() {
		return new DeviceMajorComparator(WEARABLE);
	}
	
	
}
