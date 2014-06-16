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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;


/**
 * Encapsulates BluetoothSocket, taking care of stream opening,   
 * registering for cleanup and closing upon error 
 */
public class BTSocket {

	private static final boolean READ_ONCE = true; 

	
	private BluetoothSocket socket;
	private InputStream inStream;
	private OutputStream outStream;	
	private final boolean autoOpenStreams;

	private static ExecutorService readThread;  
	private static ExecutorService writeThread;

	private static final Object readThreadLock = new Object();
	private static final Object writeThreadLock = new Object();


	public BTSocket(BluetoothSocket socket, boolean autoOpenStreams) {
		if (socket==null) {
			throw new RuntimeException("BluetoothSocket param cannot be null!");
		}
		this.autoOpenStreams = autoOpenStreams;
		this.socket = socket;
		BTWiz.registerForCleanup(this);
	}

	/**
	 * Getter for the raw BluetoothSocket inner obj   
	 */
	public BluetoothSocket getBluetoothSocket() {
		return socket;
	}

	/**
	 * Getter for the socket's InputStream.
	 * Note that this method will open the InputStream if it has not yet 
	 * been initialized (requires autoOpenStreams==true) 
	 */
	public InputStream getInputStream() throws IOException {
		openStreamsIfNeeded();
		return inStream;
	}

	/**
	 * Getter for the socket's OutputStream.
	 * Note that this method will open the OutputStream if it has not yet 
	 * been initialized (requires autoOpenStreams==true) 
	 */
	public OutputStream getOutputStream() throws IOException {
		openStreamsIfNeeded();
		return outStream;		
	}

	protected void openStreamsIfNeeded() throws IOException {
		if (!autoOpenStreams) {
			return;
		}
		if (inStream != null && outStream != null) {
			return;
		}
		try {
			inStream = socket.getInputStream();
		} 
		catch (IOException e) {
			Log.e("BTSocket", "Error at getInputStream: " + e); 
			close();
			throw new IOException("getInputStream");
		}			

		try {
			outStream = socket.getOutputStream();
		} 
		catch (IOException e) { 				
			Log.e("BTSocket", "Error at getOutputStream: " + e);
			close();
			throw new IOException("getOutputStream");
		}			            
	}

	
	/**
	 * Perform an asynchronous read of buffer. Raw read command will be called 
	 * once (i.e. not in loop) and for <= buffer.length 
	 */
	public void readAsync(byte[] buffer) {
		readAsync(buffer, 0, buffer.length, READ_ONCE, null);
	}
	
	/**
	 * Perform an asynchronous read of buffer. Raw read command will be called 
	 * once (i.e. not in loop) and for <= buffer.length.
	 * Upon readAsync complete/error the readListener will be activated 
	 */
	public void readAsync(byte[] buffer, final IReadListener readListener) {
		readAsync(buffer, 0, buffer.length, READ_ONCE, readListener);
	}
	
	/**
	 * Perform an asynchronous read of buffer. Raw read command will be called 
	 * either once or in loop according to readOnce param, and for <= buffer.length.
	 * Upon readAsync complete/error the readListener will be activated 
	 */
	public void readAsync(byte[] buffer, final boolean readOnce, final IReadListener readListener) {
		readAsync(buffer, 0, buffer.length, readOnce, readListener);
	}	
	
	/**
	 * Perform an asynchronous read of buffer starting offset and and for <= length.  
	 * Raw read command will be called either once or in loop according to readOnce param.
	 * Upon readAsync complete/error the readListener will be activated 
	 */
	public void readAsync(final byte[] buffer, final int offset, final int length, 
			final boolean readOnce, final IReadListener readListener) { 
		synchronized (readThreadLock) {
			if (readThread==null) {
				readThread = Executors.newSingleThreadExecutor();
				Log.e("BTSocket", "readThread created");
			}
		}		
		readThread.execute(new Runnable() {				
			@Override
			public void run() {
				int totalNumBytes = 0;
				for (;;) {
					try {
						int nBytes = read(buffer, offset, length);
						if (nBytes == -1) {
							throw new IOException("Read error: End of stream reached");
						}
						totalNumBytes += nBytes; 
						if (readOnce || totalNumBytes >= length) {
							if (readListener != null) {
								readListener.onSuccess(totalNumBytes);
							}
							return;
						}
						// else -- go on
					} 
					catch (IOException e) {
						e.printStackTrace();
						Log.e("BTSocket", "read error: " + e);
						if (readListener != null) {
							readListener.onError(totalNumBytes, e);
						}
						return;

					}
				}
			}
		});
	}



	/**
	 * Read a single byte from stream (blocking) 
	 */
	public int read() throws IOException { 
		Utils.assertNotUIThread();
		openStreamsIfNeeded();
		return inStream.read(); // read a single byte
	}

	/**
	 * Equivalent to read(buffer, 0, buffer.length) (blocking) 
	 */
	public int read(byte[] buffer) throws IOException {
		Utils.assertNotUIThread();
		openStreamsIfNeeded();
		return inStream.read(buffer);
	}

	/**
	 * Reads at most length bytes from this stream and stores them in the byte 
	 * array buffer starting at offset (blocking) 
	 */
	public int read(byte[] buffer, int offset, int length) throws IOException {
		Utils.assertNotUIThread();
		openStreamsIfNeeded();
		return inStream.read(buffer, offset, length); 
	}


	/**
	 * Equivalent to write(buffer, 0, buffer.length).
	 */
	public void write(byte[] buffer) throws IOException { 
		openStreamsIfNeeded();
		outStream.write(buffer); 
	}

	/**
	 * Writes a single byte to this stream. (might block)
	 */
	public void write(int oneByte) throws IOException {
		openStreamsIfNeeded();
		outStream.write(oneByte);
	}

	/**
	 * Writes count bytes from the byte array buffer starting at position 
	 * offset to this stream. (might block)
	 */
	public void write(byte[] buffer, int offset, int count) throws IOException {
		openStreamsIfNeeded();
		outStream.write(buffer, offset, count);
	}

	/**
	 * Writes a string to stream (blocking)
	 */
	public void write(String str) throws IOException {
		if (str==null) {
			str = "";
		}
		byte[] buffer = str.getBytes();
		write(buffer);
	}

		
	
	/**
	 * Asynchronously write a buffer to stream 
	 */
	public void writeAsync(byte[] buffer) {
		writeAsync(buffer, null);
	}
	
	/**
	 * Asynchronously write a buffer to stream. Activate writeListener at error/complete 
	 */
	public void writeAsync(byte[] buffer, IWriteListener writeListener) {
		writeAsync(buffer, 0, buffer.length, writeListener);
	}
	
	
	/**
	 * Asynchronously write count bytes from buffer starting at offset 
	 */
	public void writeAsync(byte[] buffer, int offset, int count) {
		writeAsync(buffer, offset, count, null);
	}
	
	/**
	 * Asynchronously write count bytes from buffer starting at offset.
	 * Activate writeListener at error/complete
	 */
	public void writeAsync(final byte[] buffer, final int offset, 
			final int count, final IWriteListener writeListener) { 
		synchronized (writeThreadLock) {
			if (writeThread==null) {
				writeThread = Executors.newSingleThreadExecutor();
				Log.e("BTSocket", "writeThread created");
			}
		}		
		writeThread.execute(new Runnable() {				
			@Override
			public void run() {
				try {
					write(buffer, offset, count);
					writeListener.onSuccess();
				} 
				catch (IOException e) {
					e.printStackTrace();
					Log.e("BTSocket", "write error: " + e);
					if (writeListener != null) {
						writeListener.onError(e);
					}
				}
			}
		});
	}

		 
	/**
	 * Attempt to connect to a remote BT device (blocking).
	 * Note that this method will cancel a running discovery session, if any. 
	 */
	public void connect(Context context) throws IOException {
		BTWiz.cancelDiscovery(context); //!
		Log.i("BTSocket", "connecting.."); 		 		
		boolean success = false;
		try { 
			BTWiz.markConnecting(true);
			socket.connect();
			success = true;
		}
		finally {
			BTWiz.markConnecting(false); 
			if (success) {
				Log.i("BTSocket", "connect success");				
			}
			else {
				Log.e("BTSocket", "connect failure");				
			}
		}
	}


	/**
	 * Asynchronously attempt to connect to a remote BT device (blocking).
	 * Note that this method will cancel a running discovery session, if any. 
	 */
	public void connectAsync(final Context context, final IConnectListener connectListener) {
		Log.i("BTSocket", "connectAsync");
		new Thread() {
			public void run() {
				try {
					connect(context);
					connectListener.onSuccess();
				} catch (IOException e) {
					e.printStackTrace();
					connectListener.onError(e);
				}
			}
		}.start();		
	}


	/**
	 * Closes socket releasing all attached system resources
	 */
	public void close() {
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception e) { 
				// no op
			}
			socket = null;
		}
		inStream = null;
		outStream = null;
	}

	
	static void closeIOThreads() {
		synchronized (writeThreadLock) {
			if (writeThread != null) {
				try { writeThread.shutdown(); } catch(Exception e) {}
				writeThread = null;
			}
		}
		synchronized (readThreadLock) {
			if (readThread != null) {
				try { readThread.shutdown(); } catch(Exception e) {}
				readThread = null;
			}
		}		
		
	}

	/**
	 * Get remote device object
	 */
	public BluetoothDevice getRemoteDevice() {
		return socket.getRemoteDevice(); 
	}

	
	/**
	 * Get remote device name
	 */
	public String getName() {
		return socket.getRemoteDevice().getName();
	}

	/**
	 * Get remote device major number
	 */
	public int getDeviceMajor() {
		int major = socket.getRemoteDevice().getBluetoothClass().getMajorDeviceClass();
		return major;
	}

	
	/**
	 * Get remote device major string (e.g. PHONE)
	 */
	public String getDeviceMajorAsString() {
		int major = socket.getRemoteDevice().getBluetoothClass().getMajorDeviceClass();
		String majorStr = Utils.majorToString(major);
		return majorStr;
	}
	
}
