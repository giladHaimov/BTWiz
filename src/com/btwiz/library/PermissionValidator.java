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

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Utility class verifying the correct permissions were set by the containing app
 * 
 * Default behavior crashes the app in case of missing permission so to ensure early
 * detection (SecurityExceptions can be catch()ed away..)
 */
public class PermissionValidator {
	
	private static String BTPerm = "android.permission.BLUETOOTH";
	private static String BTAdminPerm = "android.permission.BLUETOOTH_ADMIN";

	private static int hasBTPerm = 0; 
	private static int hasBTAdminPerm = 0;
	
	static final boolean checkBTPermissions = false; // disable permission validation  

	
	private PermissionValidator() {} // non instantiable

	/**
	 * Returns true if android.permission.BLUETOOTH permission is set 
	 */
	public static boolean basicPermissionIsSet(Context context) {
		if (hasBTPerm == 0) {
			hasBTPerm = hasPerm(context, BTPerm) ? 1 : -1; 
		}
		return hasBTPerm > 0;
	}

	
	/**
	 * Returns true if android.permission.BLUETOOTH_ADMIN permission is set 
	 */
	public static boolean adminPermissionIsSet(Context context) {
		if (hasBTAdminPerm == 0) {
			hasBTAdminPerm = hasPerm(context, BTAdminPerm) ? 1 : -1;
		}
		return hasBTAdminPerm > 0;
	}

	private static boolean hasPerm(Context context, String perm) { 
		if (!checkBTPermissions) {
			return true; // test disabled
		}
		PackageManager pm = context.getPackageManager();
		int hasPerm = pm.checkPermission(perm, context.getPackageName());
		boolean granted = (hasPerm == PackageManager.PERMISSION_GRANTED);
		if (!granted) {
			Log.e("Permissions", "\n\n\n\nApp must be granted an " + perm + " permission! \n\n\n\n");
			Utils.killThisProcess();
		}
		return granted;
	}

}
