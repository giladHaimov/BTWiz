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

/**
 * Listens and handles device connection events 
 */
public interface IDeviceConnectionListener {
	/**
	 * Activated on connection success
	 */
	void onConnectSuccess(BTSocket clientSocket);
	
	/**
	 * Activated on connection failure with a failure hint param
	 */
	void onConnectionError(Exception exception, String where);
}