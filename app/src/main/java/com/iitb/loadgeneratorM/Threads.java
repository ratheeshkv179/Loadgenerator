package com.iitb.loadgeneratorM;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.content.Context;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.widget.ListView;



  class WifiScanReceiver extends BroadcastReceiver {
	   public void onReceive(Context c, Intent intent) {
		try{
			Threads.wifiScanList = MainActivity.wifimanager.getScanResults();
			Bundle bundle = new Bundle();
			bundle.putString(Constants.BROADCAST_MESSAGE, "\nSCAN LIST : " + Threads.wifiScanList);
			Intent local = new Intent(Constants.BROADCAST_ACTION)
					.putExtras(bundle);
			LocalBroadcastManager.getInstance(c).sendBroadcast(local);

		}catch(Exception ex){
			Bundle bundle = new Bundle();
			bundle.putString(Constants.BROADCAST_MESSAGE, "\nException3 : " + ex.toString());
			Intent local = new Intent(Constants.BROADCAST_ACTION)
					.putExtras(bundle);
			LocalBroadcastManager.getInstance(c).sendBroadcast(local);
		}
	}
}


public class Threads {

	static  List<ScanResult> wifiScanList;

	ListView lv;
	WifiManager wifi;
	String wifis[];
	
	//keeps listening in background for messages from server and takes necessary action
	public static void ListenServer(final Context ctx){
		while(MainActivity.experimentOn){//listen as a server
			try {
				Log.d(Constants.LOGTAG,"Waiting for connection from server");
				final Socket temp = MainActivity.listen.accept(); 
				Log.d(Constants.LOGTAG,"accepted: will now get config file");
				Runnable r = new Runnable() {
					public void run() {
						Threads.eventRunner(temp, ctx);
					}
				};

				Thread t = new Thread(r);
				t.start();

				//Log.d(Constants.LOGTAG,"Registration request rejected");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.d(Constants.LOGTAG, "ServerListen : timeout");
			}
		}
		Log.d(Constants.LOGTAG, "ListenServer : Experiment Over, Stopped listening ... ");
		Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
				.putExtra(Constants.BROADCAST_MESSAGE, 
				"Experiment Over, Stopped listening ...\n");

		// Broadcasts the Intent to receivers in this application.
		LocalBroadcastManager.getInstance(ctx).sendBroadcast(localIntent);
	}


	//called whenever server connects. handles 3 actions : receive control file, stop experiment, clear registration
	public static void eventRunner(Socket server, final Context ctx){
		Long localTimeInMillis = Calendar.getInstance().getTimeInMillis();
		
		Log.d(Constants.LOGTAG,"Server connection established");
		String data = "";
		try {
			DataInputStream dis= new DataInputStream(server.getInputStream());
			DataOutputStream dout = new DataOutputStream(server.getOutputStream());

			int length = dis.readInt();
			Log.d(Constants.LOGTAG,"Json length " + length + " read from " + server.getInetAddress().getHostAddress());

			for(int i=0;i<length;++i){
				data += (char)dis.readByte();
			}
			Log.d(Constants.LOGTAG,"eventRunner : json received : " + data);

			Map<String, String> jsonMap = Utils.ParseJson(data);

			String action = jsonMap.get(Constants.action);
			
			Log.d(Constants.LOGTAG, "###  ### ### serverTimeDelta = " + MainActivity.serverTimeDelta/1000 + " seconds");

			if( action.compareTo(Constants.action_connectToAp) == 0){

				try {
					// connect to specific Access point
					//
					Bundle bundle = new Bundle();
					bundle.putString(Constants.BROADCAST_MESSAGE, "#$#AP Settings file received\n");
					Intent local = new Intent(Constants.BROADCAST_ACTION)
							.putExtras(bundle);
					LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);

					if (MainActivity.running == true) {
						//this should not happen. As one experiment is already running Send 300 response
						Log.d(Constants.LOGTAG, "Experiment running but received AP settings file ()");
						dout.writeInt(300);
						return;
					}

					int fileSize = dis.readInt();
					Log.d(Constants.LOGTAG, "eventRunner : fileSize " + fileSize);
					StringBuilder fileBuilder = new StringBuilder();
					for (int i = 0; i < fileSize; ++i) {
						fileBuilder.append((char) dis.readByte());
					}
					String apSettingFile = fileBuilder.toString();
					dout.writeInt(200);
					server.close();

					bundle.putString(Constants.BROADCAST_MESSAGE, "\n DATA : " + apSettingFile);
					local = new Intent(Constants.BROADCAST_ACTION)
							.putExtras(bundle);
					LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);

					String[] apConf = apSettingFile.split("\n");
					System.out.println(apConf.length);

					String _ssid = "";//"rkv";
					String _passwd = "";//"cdot1234";
					String _bssid = "";//"00:24:2B:70:02:CF";
					String _usrname= "";
					String _auth ="";

					try{
					for (int i = 0; i < apConf.length; i++) {
						String[] apInfo = apConf[i].split("=");
						if (apInfo[0].equalsIgnoreCase("USERNAME")) {
								_usrname =  apInfo[1].trim();
						} else if (apInfo[0].equalsIgnoreCase("PASSWORD")) {
							_passwd = apInfo[1].trim();
						} else if (apInfo[0].equalsIgnoreCase("AUTHENTICATION")) {
							_auth =  apInfo[1].trim();
						}else if (apInfo[0].equalsIgnoreCase("BSSID")) {
							_bssid = apInfo[1].trim();
						}else if (apInfo[0].equalsIgnoreCase("SSID")) {
							_ssid =  apInfo[1].trim();
						}
					}}
					catch(Exception ex){

						bundle.putString(Constants.BROADCAST_MESSAGE, "\n Exception : " + ex.toString());
						local = new Intent(Constants.BROADCAST_ACTION)
								.putExtras(bundle);
						LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);
					}




				//	    WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();

   						/*	wifiConfig = new WifiConfiguration();
   						wifiConfig.SSID = ssid;
   						wifiConfig.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
   						wifiConfig.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
   						enterpriseConfig.setIdentity(userName);
   						enterpriseConfig.setPassword(passWord);
   						enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
   						wifiConfig.enterpriseConfig = enterpriseConfig;

					 */




//		Scan and Connect to WiFi

					BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
						public void onReceive(Context c, Intent intent) {
							try{
								Threads.wifiScanList = MainActivity.wifimanager.getScanResults();
								Bundle bundle = new Bundle();
								for(int i = 0; i < wifiScanList.size(); i++){
									ScanResult obj =  wifiScanList.get(i);
									bundle.putString(Constants.BROADCAST_MESSAGE, "\n\n\n\nScan  : "+(i+1)
											+"\nBSSID : "
											+obj.BSSID+"\nSSID : "
											+obj.SSID+"\nFrequency  : "
											+obj.frequency+"\nLevel  : "
											+obj.level+"\nCapabilities  : "
											+obj.capabilities);
									Intent local = new Intent(Constants.BROADCAST_ACTION)
											.putExtras(bundle);
									LocalBroadcastManager.getInstance(c).sendBroadcast(local);
								}

							}catch(Exception ex){
								Bundle bundle = new Bundle();
								bundle.putString(Constants.BROADCAST_MESSAGE, "\nException3 : " + ex.toString());
								Intent local = new Intent(Constants.BROADCAST_ACTION)
										.putExtras(bundle);
								LocalBroadcastManager.getInstance(c).sendBroadcast(local);
							}
						}
					};

					ctx.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
					MainActivity.wifimanager.startScan();

					try{
						Thread.sleep(5000, 0); // Five Seconds for Scanning, otherwise the variable wifiScanList will be null
					}catch(Exception ex){
						  bundle = new Bundle();
						bundle.putString(Constants.BROADCAST_MESSAGE, "\nException6 : " + ex.toString());
						  local = new Intent(Constants.BROADCAST_ACTION)
								.putExtras(bundle);
						LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);

					}
					ctx.unregisterReceiver(wifiReceiver); // Unregister


					if(Threads.wifiScanList != null){
						for(int i = 0; i < Threads.wifiScanList.size(); i++){
							//	wifis[i] = ((wifiScanList.get(i)).toString());
							ScanResult obj =  Threads.wifiScanList.get(i);

							if( _bssid.equalsIgnoreCase(obj.BSSID) && _ssid.equalsIgnoreCase(obj.SSID) ){
								WifiConfiguration wfc = new WifiConfiguration();

								wfc.SSID = "\"".concat(_ssid.toString()).concat("\"")  ;  //"\"".concat("rkv").concat("\"");
								wfc.BSSID = _bssid.toString()  ;    // bssid.toString();  // "\"".concat("00:24:2b:70:02:cf").concat("\"");
								wfc.status = WifiConfiguration.Status.DISABLED;
								wfc.priority = 4000;

								if(_auth.equalsIgnoreCase("WPA_PSK")){
									wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
									wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
									wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
									wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
									wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
									wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
									wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
									wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
									wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
									wfc.preSharedKey =  "\"".concat(_passwd.toString()).concat("\"")  ; //  passwd.toString(); //"\"".concat("cdot1234").concat("\"");

								}else if(_auth.equalsIgnoreCase("WPA_EAP")){
									wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
									wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
									wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
									wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
									wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
									wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
									wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
									wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
									wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
									wfc.status = WifiConfiguration.Status.ENABLED;
									wfc.enterpriseConfig.setIdentity(_usrname.toString());
									wfc.enterpriseConfig.setPassword(_passwd.toString());
									wfc.enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
									wfc.enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
								}

								int networkId = MainActivity.wifimanager.addNetwork(wfc);
								if (networkId != -1) {
									bundle.putString(Constants.BROADCAST_MESSAGE, "\n BSSID : "+wfc.BSSID+"\n SSID : "+wfc.SSID +"\n Network ID : "+networkId);
									local = new Intent(Constants.BROADCAST_ACTION)
											.putExtras(bundle);
									LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);
									MainActivity.wifimanager.disconnect();
									MainActivity.wifimanager.enableNetwork(networkId, true);

								} else {

									bundle.putString(Constants.BROADCAST_MESSAGE,"\nAdding to Network Failed!!!");
									local = new Intent(Constants.BROADCAST_ACTION)
											.putExtras(bundle);
									LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);

								}
								break;
							}
							else {
								// The AP with speified SSID , BSSID not found
								bundle.putString(Constants.BROADCAST_MESSAGE,"\nThe AP with speified SSID , BSSID not found!!!");
								local = new Intent(Constants.BROADCAST_ACTION)
										.putExtras(bundle);
								LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);
							}
						}
					}else {

						bundle.putString(Constants.BROADCAST_MESSAGE, "\n Scanning NULL !!!");
						local = new Intent(Constants.BROADCAST_ACTION)
								.putExtras(bundle);
						LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);
					}




			// Directly connected to AP [ Without scan ]

/*					String networkSSID = "rkv";
					String networkPass = "cdot1234";
					String networkBSSID = "00:24:2B:70:02:CF";
					WifiConfiguration wfc = new WifiConfiguration();
					wfc.SSID = "\"".concat(networkSSID.toString()).concat("\"")  ;  // "\"".concat("rkv").concat("\""); //ssid.toString();  //"\"".concat("rkv").concat("\"");
					wfc.BSSID = networkBSSID.toString()  ;   // "\"".concat("00:24:2b:70:02:cf").concat("\""); //bssid.toString();  // "\"".concat("00:24:2b:70:02:cf").concat("\"");
					wfc.status = WifiConfiguration.Status.DISABLED;
					wfc.priority = 4000;
					wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
					wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
					wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
					wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
					wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
					wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
					wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
					wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
					wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
					wfc.preSharedKey =  "\"".concat(networkPass.toString()).concat("\"")  ;//  _passwd.toString();   //  "\"".concat("cdot1234").concat("\"");// passwd.toString(); //"\"".concat("cdot1234").concat("\"");
					int networkId = MainActivity.wifimanager.addNetwork(wfc);
					bundle.putString(Constants.BROADCAST_MESSAGE, "\n BSSID : "+wfc.BSSID+"\n SSID : "+wfc.SSID +"\n Network ID : "+networkId);
					 local = new Intent(Constants.BROADCAST_ACTION)
							.putExtras(bundle);
					LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);
					if (networkId != -1) {
						MainActivity.wifimanager.disconnect();
						boolean status = MainActivity.wifimanager.enableNetwork(networkId, true);

						WifiManager wifim =(WifiManager)ctx.getSystemService(Context.WIFI_SERVICE);

						WifiInfo info =  wifim.getConnectionInfo();

						//MainActivity.textbox.append("\n1Network SSID = " + info.getSSID());

						bundle.putString(Constants.BROADCAST_MESSAGE, "\n Reconnection : "+ status+"\nSSID : "+info.getSSID());
						local = new Intent(Constants.BROADCAST_ACTION)
								.putExtras(bundle);
						LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);
					} else {

						bundle.putString(Constants.BROADCAST_MESSAGE, "\n Adding to Network Failed!!!");
						local = new Intent(Constants.BROADCAST_ACTION)
								.putExtras(bundle);
						LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);
					}

*/
//*************************************************************************


				}catch(Exception ex){
					Bundle bundle = new Bundle();
					bundle.putString(Constants.BROADCAST_MESSAGE, "#$#AP Settings Exception : \n"+ex.toString());
					Intent local = new Intent(Constants.BROADCAST_ACTION)
							.putExtras(bundle);
					LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);
				}

			}	else if(action.compareTo(Constants.action_controlFile) == 0){
				Long serverTimeInMillis = Long.parseLong(jsonMap.get(Constants.serverTime));
				MainActivity.serverTimeDelta = serverTimeInMillis - localTimeInMillis;

				if(MainActivity.running == true){
					//this should not happen. As one experiment is already running Send 300 response
					Log.d(Constants.LOGTAG,"Experiment running but received another control file request");
					dout.writeInt(300);
					return;
				}
				MainActivity.running = true;
				boolean textFileFollow = Boolean.parseBoolean((String) jsonMap.get(Constants.textFileFollow));
				if(textFileFollow){
					int fileSize = dis.readInt();
					Log.d(Constants.LOGTAG,"eventRunner : fileSize " + fileSize);
					StringBuilder fileBuilder = new StringBuilder();
					for(int i=0;i<fileSize;++i){
						fileBuilder.append((char)dis.readByte());
					}
					String controlFile = fileBuilder.toString();

					dout.writeInt(200);

					Log.d(Constants.LOGTAG,controlFile);

					server.close();

					Log.d(Constants.LOGTAG,"eventRunner : Now setting up alarms");
					//display that control file received. Setting up alarms
					Bundle bundle = new Bundle();
					bundle.putString(Constants.BROADCAST_MESSAGE,"#$#Control file received. Setting up alarms\n");
			        
			        Intent local = new Intent(Constants.BROADCAST_ACTION)
			        					.putExtras(bundle);
			        LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);
					server.close();

					//***************
					
					MainActivity.load = RequestEventParser.parseEvents(controlFile);
					MainActivity.numDownloadOver = 0; //reset it
					MainActivity.currEvent = 0;
					
					//send broadcast to trigger alarms
					Intent localIntent = new Intent(Constants.BROADCAST_ALARM_ACTION);
					localIntent.putExtra("eventid", (int) -1); //this is just to trigger first scheduleNextAlarm

					LocalBroadcastManager.getInstance(ctx).sendBroadcast(localIntent);
					
					//restart the alarm for session timeout
					MainActivity.setKillTimeoutAlarm(ctx);
				}
				else{
					Log.d(Constants.LOGTAG,"eventRunner : No control file in response");
				}
			}
			else if (action.compareTo(Constants.action_stopExperiment) == 0){
				Log.d(Constants.LOGTAG, "MainActivity.running boolean set to false. Reset()");
				
				if(MainActivity.running && MainActivity.load != null){
					
					//Display message that experiment is being interrupted
					Bundle bundle = new Bundle();
					bundle.putInt("enable", 0);
					bundle.putString(Constants.BROADCAST_MESSAGE,
							"stop experiment message from server.\n Listening for next experiment to begin.\n");
			        //on complete  
			        Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
			        					.putExtras(bundle);
			        	
			        // Broadcasts the Intent to receivers in this application.
			        LocalBroadcastManager.getInstance(ctx).sendBroadcast(localIntent);
			        
					final String logFileName = Long.toString(MainActivity.load.loadid);
					Runnable r = new Runnable() {
						public void run() {
							Threads.sendLog(logFileName);
						}
					};
					Thread t = new Thread(r);
			        t.start();
					
					MainActivity.reset(ctx);
				}
				dout.writeInt(200);
				server.close();
			}
			else if (action.compareTo(Constants.action_clearRegistration) == 0){
				Log.d(Constants.LOGTAG, "action clearRegistration");
				MainActivity.reset(ctx); //reset all variables and state
				
				//also stop server. 
				MainActivity.experimentOn = false;
				if(MainActivity.listen != null) MainActivity.listen.close();
				
				//Enable StartButton (send broadcast)
				Bundle bundle = new Bundle();
				bundle.putInt("enable", 1);
				bundle.putString(Constants.BROADCAST_MESSAGE,"clear Registration. Resetting everything.\nPlease Register Again");
		        //on complete  
		        Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
		        					.putExtras(bundle);
		        	
		        // Broadcasts the Intent to receivers in this application.
		        LocalBroadcastManager.getInstance(ctx).sendBroadcast(localIntent);
				server.close();
			}
			else if (action.compareTo(Constants.action_refreshRegistration) == 0){
				Log.d(Constants.LOGTAG, "action refreshRegistration");
				dout.writeInt(200);
				server.close();
			}
			else{
				Log.d(Constants.LOGTAG,"eventRunner() : Wrong action code");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//Somehow experiment could not be started due to some IOException in socket transfer. So again reset running variable to false
			MainActivity.running = false;
			e.printStackTrace();
		}
	}
	
	//write log content to log file specified
	static synchronized String writeToLogFile(String logfilename, String log){
		File logfile = new File(MainActivity.logDir, logfilename);
		BufferedWriter logwriter = null;
		String msg = "";
		try {
			logwriter = new BufferedWriter(new FileWriter(logfile, true));
			logwriter.append(log);
			logwriter.close();
			msg += " local log write success\n";
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Log.d(Constants.LOGTAG, "HandleEvent() : can't open log file for writing " + logfilename);
			msg += " Couldn't open log file " + logfilename;
			e1.printStackTrace();
		}
		return msg;
	}

	//completes get request specified in given event(identified by eventid)
	//also writes log to logfile about progress
	//if all downloads over, send the log file
	static int HandleEvent(int eventid, final Context context){
		//Log file will be named   <eventid> . <loadid>
		if(!MainActivity.running){
			Log.d(Constants.LOGTAG, "HandleEvent : But experiment not running");
			return -1;
		}
		
		
		Load currentLoad = MainActivity.load;
		
		if(currentLoad == null){
			return -1;
		}
		
		RequestEvent event = currentLoad.events.get(eventid);
		String logfilename = "" + currentLoad.loadid;
		
		
		Log.d(Constants.LOGTAG, "HandleEvent : just entered thread");
		
		InputStream input = null;
		OutputStream output = null;
		HttpURLConnection connection = null;
		String filename = "unknown"; //file name of file to download in GET request
		//BufferedWriter logwriter = null;
		boolean success = false;

		Calendar startTime = null, endTime = null;
		long responseTime = -1;
		StringBuilder logwriter = new StringBuilder();
		
		try {
			URL url = new URL(event.url);
			
			logwriter.append("details: " + currentLoad.loadid + " " + eventid + " SOCKET" + "\n");
			logwriter.append("url: " + url + "\n");
			
			filename = event.url.substring(event.url.lastIndexOf('/') + 1);
			
			Log.d(Constants.LOGTAG, "HandleEvent : " + event.url + " " + filename);
			
			connection = (HttpURLConnection) url.openConnection();
			connection.setReadTimeout(10000); //10 seconds timeout for reading from input stream
			connection.setConnectTimeout(10000); //10 seconds before connection can be established
			
			//note start time 
			startTime = Utils.getServerCalendarInstance();
			
			connection.connect();
	
			// expect HTTP 200 OK, so we don't mistakenly save error report
			// instead of the file
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				Log.d(Constants.LOGTAG, "HandleEvent : " + " connection response code error");
				endTime = Utils.getServerCalendarInstance();
				
				String startTimeFormatted =  Utils.sdf.format(startTime.getTime());
				String endTimeFormatted =  Utils.sdf.format(endTime.getTime());
				
				logwriter.append(Constants.SUMMARY_PREFIX + event.url + " [ERROR] " + "[ET = " + (endTime.getTimeInMillis()-startTime.getTimeInMillis()) + "]" + " [" + startTimeFormatted + " , " + endTimeFormatted + "] " +
						"[code " + connection.getResponseCode() + "]" + "\n");
				logwriter.append(Constants.SUMMARY_PREFIX + Constants.LINEDELIMITER); //this marks the end of this log
			}
			else{
				// this will be useful to display download percentage
				// might be -1: server did not report the length
				int fileLength = connection.getContentLength();
				logwriter.append("length: " + Integer.toString(fileLength) + " \n");
				
				Log.d(Constants.LOGTAG, "HandleEvent : " + " filelength " + fileLength);
	
				// download the file
				input = connection.getInputStream();
				output = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename);
	
				byte data[] = new byte[4096];
				long total = 0;
				int count;
				int oldprogress = 0, currprogress = 0;
				
				Log.d(Constants.LOGTAG, "HandleEvent : " + " file opened on sd card");
				logwriter.append(Utils.getTimeInFormat() + " " + currprogress + "% " + total + "\n"); //first progress 0%
				while ((count = input.read(data)) != -1) {
	//				Log.d(Constants.LOGTAG, "HandleEvent : " + " Received chunk of size " + count);
					total += count;
	
					// publishing the progress....
					if (fileLength > 0){ // only if total length is known
						currprogress = (int) (total * 100 / fileLength);
						if(currprogress > oldprogress){
							oldprogress = currprogress;
							Log.d(Constants.LOGTAG, currprogress + "% " + total + "\n");
							logwriter.append(Utils.getTimeInFormat() + " " + currprogress + "% " + total + "\n");
						}
						//publishProgress((int) (total * 100 / fileLength));
					}
					output.write(data, 0, count);
				}
				//File download over
				success = true;
				
				//note end time take the difference as response time
				endTime = Utils.getServerCalendarInstance();
				
				responseTime = endTime.getTimeInMillis() - startTime.getTimeInMillis();
				String startTimeFormatted =  Utils.sdf.format(startTime.getTime());
				String endTimeFormatted =  Utils.sdf.format(endTime.getTime());
				logwriter.append("RT " +  responseTime + "\n");
				logwriter.append(Constants.SUMMARY_PREFIX + event.url + " [SUCCESS] " + "[RT = " + (endTime.getTimeInMillis()-startTime.getTimeInMillis()) + "]" + " [" + startTimeFormatted + " , " + endTimeFormatted + "] " +
						"[content-length = " + fileLength + "]" + "\n");
	
				logwriter.append("success\n");
				logwriter.append(Constants.SUMMARY_PREFIX + Constants.LINEDELIMITER); //this marks the end of this log
				
			}
		} catch (IOException e) {
			endTime = Utils.getServerCalendarInstance();
			
			String startTimeFormatted =  Utils.sdf.format(startTime.getTime());
			String endTimeFormatted =  Utils.sdf.format(endTime.getTime());
			
			logwriter.append(Constants.SUMMARY_PREFIX + event.url + " [ERROR] " + "[ET = " + (endTime.getTimeInMillis()-startTime.getTimeInMillis()) + "]" + " [" + startTimeFormatted + " , " + endTimeFormatted + "] " +
					"[" + e.getMessage() + "]" + "\n");
			logwriter.append("failure\n");
			logwriter.append(Constants.SUMMARY_PREFIX + Constants.LINEDELIMITER); //this marks the end of this log
			e.printStackTrace();
		} finally {
			try {
				if (output != null)
					output.close();
				if (input != null)
					input.close();
			} catch (IOException ignored) {
			}
	
			if (connection != null)
				connection.disconnect();
		}
		
		String msg = "GET #" + eventid + " File : " + filename;
		if(!success) msg += "FAILED connection problem/timeout";
		else msg += " SUCCESS with RT=" + responseTime + "\n";
		

		int num = MainActivity.numDownloadOver++;
		Log.d(Constants.LOGTAG, "handle event thread : END . Incrementing numDownloadOver to " + MainActivity.numDownloadOver + " #events is "+ currentLoad.events.size());
		if(num+1 == currentLoad.events.size()){
			//send the consolidated log file
			String n = Integer.toString(currentLoad.events.size());
			msg += "Experiment over : all GET requests (" + n + " of " + n + ") completed\n";
			//msg += "Trying to send log file\n";
			
			logwriter.append(Constants.EOF); //this indicates that all GET requests have been seen without interruption from either user/server
			
			String logString = logwriter.toString(); //get the content of stringbuilder into a string
			
			String retmsg = writeToLogFile(logfilename, logString); //write the log to file. This is a synchronized operation, only one thread can do it at a time
			msg += retmsg;
			Log.d(Constants.LOGTAG, "Threads.HandleEvent() : Sending the log file");
			int ret = Threads.sendLog(logfilename);
			if(ret == 200){
				msg += "log file sent successfully\n";
			}
			else{
				msg += "log file sending failed\n";
			}
		}
		else{//just write the log to log file
			String logString = logwriter.toString(); //get the content of stringbuilder into a string
			
			String retmsg = writeToLogFile(logfilename, logString); //write the log to file. This is a synchronized operation, only one thread can do it at a time
			msg += retmsg;
		}
		
		Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
		.putExtra(Constants.BROADCAST_MESSAGE, msg);

		// Broadcasts the Intent to receivers in this application.
		LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
	
		return 0;
	}
	
	//send log files pending. this is called from a background thread.
	//looks into log folder and sends all log files(except that of current experiment)
	static void sendLogFilesBackground(final Context ctx){
		File storage = new File(Constants.logDirectory); //log dir has already been created in onCreate
    	File[] files = storage.listFiles();
    	int sent = 0;
    	int errors = 0;
    	
    	String currExpLogFile = "-1" ;
		if(MainActivity.load != null) {
			currExpLogFile = Long.toString(MainActivity.load.loadid); 
		}
		
    	for(int i=0; i<files.length; i++){
    		File c = files[i];
    		String logFileName = c.getName();
    		
    		
    		if(!logFileName.equals(currExpLogFile)){ //pending log file is not current experiment's log
    			int status = Threads.sendLog(logFileName);
    			if(status == 200){
    				sent++;
    			}
    			else{
    				errors++;
    			}
    		}
    	}
    	Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
							.putExtra(Constants.BROADCAST_MESSAGE,
									"Background log file sending : success " + sent + " Fail " + errors + "\n");

    	// Broadcasts the Intent to receivers in this application.
    	LocalBroadcastManager.getInstance(ctx).sendBroadcast(localIntent);
	}
	
	//send the log file specified by given name
	@SuppressWarnings("deprecation")
	static int sendLog(String logFileName){
		int statusCode = 404;
		
		String logFilePath = Constants.logDirectory + "/" + logFileName;
		String url = "http://" + MainActivity.serverip + ":" + MainActivity.serverport + "/" + Constants.SERVLET_NAME + "/receiveLogFile.jsp";
		Log.d(Constants.LOGTAG, "Upload url " + url);
//		String url = "http://192.168.0.107/fup.php";
		
		File logFile = new File(logFilePath);
		if(!logFile.exists()){
			Log.d(Constants.LOGTAG, "sendLog : File not found " + logFilePath + " May be sent earlier");
			return 200; //already sent sometime earlier
		}
		
		MultipartEntity mpEntity  = new MultipartEntity();
		HttpClient client = Utils.getClient();
		
		try {
			mpEntity.addPart("expID", new StringBody(logFileName));
			mpEntity.addPart(Constants.macAddress, new StringBody(Utils.getMACAddress()));
			mpEntity.addPart("file", new FileBody(logFile));
			
			HttpPost httppost = new HttpPost(url);
			httppost.setEntity(mpEntity);
			try {
				HttpResponse response = client.execute( httppost );
				statusCode = response.getStatusLine().getStatusCode();
				if(statusCode == 200){
					Log.d(Constants.LOGTAG, "Log file named " + logFileName + " deleted");
					logFile.delete(); //now deleting log file
				}
				else{
					Log.d(Constants.LOGTAG, "Sending Log file " + logFileName + " failed");
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return statusCode;
	}



	public void connectToWiFi(String _bssid,String _ssid, String _passwd){

		try {

			WifiConfiguration wfc = new WifiConfiguration();

			wfc.SSID = "\"".concat(_ssid.toString()).concat("\"")  ;  // "\"".concat("rkv").concat("\""); //ssid.toString();  //"\"".concat("rkv").concat("\"");
			wfc.BSSID = _bssid.toString()  ;   // "\"".concat("00:24:2b:70:02:cf").concat("\""); //bssid.toString();  // "\"".concat("00:24:2b:70:02:cf").concat("\"");
			wfc.status = WifiConfiguration.Status.DISABLED;
			wfc.priority = 4000;

			wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

			wfc.preSharedKey =  "\"".concat(_passwd.toString()).concat("\"")  ;//  _passwd.toString();   //  "\"".concat("cdot1234").concat("\"");// passwd.toString(); //"\"".concat("cdot1234").concat("\"");


			MainActivity.textbox.append("\nBSSID : " + wfc.BSSID);
			MainActivity.textbox.append("\nSSID : " + wfc.SSID);
			MainActivity.textbox.append("\nPassword  : "+wfc.preSharedKey);


			//	WifiManager wfMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			MainActivity.textbox.append("\nNetwork Manager : " + MainActivity.wifimanager);
			int networkId = MainActivity.wifimanager.addNetwork(wfc);
			MainActivity.textbox.append("\nNetwork ID : " + networkId);
			if (networkId != -1) {
				// success, can call wfMgr.enableNetwork(networkId, true) to connect
				MainActivity.wifimanager.disconnect();
				//wifimanager.enableNetwork(networkId, true);
				MainActivity.textbox.append("\nConnect : " + MainActivity.wifimanager.enableNetwork(networkId, true));
				//	textbox.append("\nReconnect : " + wifimanager.reconnect());

				//wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				WifiInfo info = MainActivity.wifimanager.getConnectionInfo();
				MainActivity.textbox.append("\n1Network SSID = " + info.getSSID());
			} else {
				MainActivity.textbox.append("\nAdding to Network Failed!!!");
			}

		}catch(Exception ex){
			MainActivity.textbox.append("\nException2 : " + ex.toString());
		}

	}




/*

	public void scanAndconnectToWiFi(String _bssid,String _ssid, String _passwd){

		/**
		 * WiFi Scanning
		 */

/*		try {

			WifiScanReceiver wifiReciever = new WifiScanReceiver();
			LocalBroadcastManager.getInstance(ctx).registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			MainActivity.wifimanager.startScan();

			if(wifiScanList != null){
				for(int i = 0; i < wifiScanList.size(); i++){
					//	wifis[i] = ((wifiScanList.get(i)).toString());
					ScanResult obj =  wifiScanList.get(i);

					//	textbox.append("\nBSSID : "+ obj.BSSID + " = "+_bssid);
					//	textbox.append("\nSSID : " + obj.SSID  + " = "+_ssid);

					if( _bssid.equalsIgnoreCase(obj.BSSID) && _ssid.equalsIgnoreCase(obj.SSID) ){

						MainActivity.textbox.append("\n\n\n\nScan  : " + (i + 1));
						MainActivity.textbox.append("\nBSSID : "+obj.BSSID);
						MainActivity.textbox.append("\nSSID : "+obj.SSID);
						MainActivity.textbox.append("\nFrequency  : "+obj.frequency);
						MainActivity.textbox.append("\nLevel  : "+obj.level);
						MainActivity.textbox.append("\nCapabilities  : "+obj.capabilities);

						WifiConfiguration wfc = new WifiConfiguration();

						wfc.SSID = "\"".concat(_ssid.toString()).concat("\"")  ;  //"\"".concat("rkv").concat("\"");
						wfc.BSSID = _bssid.toString()  ;    // bssid.toString();  // "\"".concat("00:24:2b:70:02:cf").concat("\"");
						wfc.status = WifiConfiguration.Status.DISABLED;
						wfc.priority = 4000;

						wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
						wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
						wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
						wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
						wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
						wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
						wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
						wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
						wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

						wfc.preSharedKey =  "\"".concat(_passwd.toString()).concat("\"")  ; //  passwd.toString(); //"\"".concat("cdot1234").concat("\"");

						//	WifiManager wfMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
						MainActivity.textbox.append("\nNetwork Manager : " + MainActivity.wifimanager);
						int networkId = MainActivity.wifimanager.addNetwork(wfc);
						MainActivity.textbox.append("\nNetwor ID : " + networkId);
						if (networkId != -1) {
							// success, can call wfMgr.enableNetwork(networkId, true) to connect
							MainActivity.wifimanager.disconnect();
							//wifimanager.enableNetwork(networkId, true);
							MainActivity.textbox.append("\nConnect : " + MainActivity.wifimanager.enableNetwork(networkId, true));
							//	textbox.append("\nReconnect : " + wifimanager.reconnect());


							//MainActivity.wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
							WifiInfo info = MainActivity.wifimanager.getConnectionInfo();
							MainActivity.textbox.append("\n1Network SSID = " + info.getSSID());



						} else {
							MainActivity.textbox.append("\nAdding to Network Failed!!!");
						}
						break;
					}

				}
			}
		}
		catch(Exception ex){
			MainActivity.textbox.append("\nException4 : " + ex.toString());
		}
	}
 */


}
