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
import android.os.Looper;

/**
 * Utilities class
 */
public class Utils {

	private Utils() {
	} // non instantiable

	/**
	 * Throws a RuntimeException if this thread is the UI thread
	 */
	public static void assertNotUIThread() {
		if (Looper.getMainLooper().equals(Looper.myLooper())) {
			throw new RuntimeException(
					"This command should not execute on UI thread!");
		}
	}

	public static void sleep(long msecs) {
		try {
			Thread.sleep(msecs);
		} catch (InterruptedException e) {
		}
	}

	public static String majorToString(int major) {
		switch (major) {
		case AUDIO_VIDEO:
			return "AUDIO_VIDEO";
		case COMPUTER:
			return "COMPUTER";
		case HEALTH:
			return "HEALTH";
		case IMAGING:
			return "IMAGING";
		case MISC:
			return "MISC";
		case NETWORKING:
			return "NETWORKING";
		case PERIPHERAL:
			return "PERIPHERAL";
		case PHONE:
			return "PHONE";
		case TOY:
			return "TOY";
		case UNCATEGORIZED:
			return "UNCATEGORIZED";
		case WEARABLE:
			return "WEARABLE";
		default:
			return "Unknown (" + major + ")";
		}
	}

	public static int getApiVersion() {
		return android.os.Build.VERSION.SDK_INT;
	}

	public static void killThisProcess() {
		android.os.Process.killProcess(android.os.Process.myPid());
	}

}
