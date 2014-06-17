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

# BTWiz: Async Bluetooth Lib for Android

BTWiz is an internal library developed & used by my team: www.mobileedge.co.il in Android Bluetooth projects for the last 3 years.
It is hereby released as an open source project.

For licensing details please visit http://www.apache.org/licenses/LICENSE-2.0


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
   BTWiz uses highly effective fall-through mechanism that solves many of the connection problems our team, as well as others, have encountered. This mechanism involves getting a list of supported UUIDs (which is implemented differently pre- and post- ICS versions) and, if failed, reverting to default SPP UUID 00001101-0000-1000-8000-00805F9B34FB. If that fails, it then attempts to activate hidden method createRfcommSocket() using Java reflection.


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


Gilad Haimov
gilad@mobileedge.co.il
  