/*
 * Modify the SocketForwarder of SandroproxyLib
 * Copyright (C) 2014  Yihang Song

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.PrivacyGuard.Application.Network.Forwader;


import com.PrivacyGuard.Plugin.IPlugin;
import com.PrivacyGuard.Plugin.LocationDetection;
import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.MyVpnService;

import org.sandrop.webscarab.model.ConnectionDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;


public class MySocketForwarder extends Thread {

  private static String TAG = MySocketForwarder.class.getSimpleName();
  private static boolean EVALUATE = false;
  private static boolean DEBUG = false;
  private static boolean PROTECT = true;
  private boolean outgoing = false;
  private ArrayList<IPlugin> plugins;
  private MyVpnService vpnService;
  private String appName = null;
  private String packageName = null;

  private Socket inSocket;
  private InputStream in;
  private OutputStream out;
  private String destIP;
  private static final String TIME_STAMP_FORMAT = "MM-dd HH:mm:ss.SSS";
  private SimpleDateFormat df = new SimpleDateFormat(TIME_STAMP_FORMAT, Locale.CANADA);

  public static void connect(Socket clientSocket, Socket serverSocket, MyVpnService vpnService) throws Exception {
    if (clientSocket != null && serverSocket != null && clientSocket.isConnected() && serverSocket.isConnected()){
      clientSocket.setSoTimeout(0);
      serverSocket.setSoTimeout(0);
      MySocketForwarder clientServer = new MySocketForwarder(clientSocket, serverSocket, true, vpnService);
      MySocketForwarder serverClient = new MySocketForwarder(serverSocket, clientSocket, false, vpnService);
      clientServer.start();
      serverClient.start();

      Logger.d(TAG, "Start forwarding");
      while (clientServer.isAlive())
        clientServer.join();
      while (serverClient.isAlive())
        serverClient.join();
      clientSocket.close();
      serverSocket.close();
        Logger.d(TAG, "Stop forwarding");
    }else{
        Logger.d(TAG, "skipping socket forwarding because of invalid sockets");
      if (clientSocket != null && clientSocket.isConnected()){
        clientSocket.close();
      }
      if (serverSocket != null && serverSocket.isConnected()){
        serverSocket.close();
      }
    }
  }

  public MySocketForwarder(Socket inSocket, Socket outSocket, boolean isOutgoing, MyVpnService vpnService) {
    this.inSocket = inSocket;
    try {
      this.in = inSocket.getInputStream();
      this.out = outSocket.getOutputStream();
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.outgoing = isOutgoing;
    this.destIP = outSocket.getInetAddress().getHostAddress();
    if(outSocket.getPort() == 443) destIP += " (SSL)";
    this.vpnService = vpnService;
    this.plugins = vpnService.getNewPlugins();
    setDaemon(true);
  }

  public void run() {
    try {
      byte[] buff = new byte[4096];
      int got;
      while ((got = in.read(buff)) > -1){
        String msg = new String(Arrays.copyOfRange(buff, 0, got));
        if(EVALUATE) {
            if(outgoing) {
                if (appName == null) {
                    ConnectionDescriptor des = vpnService.getClientAppResolver().getClientDescriptorBySocket(inSocket);
                    if (des != null) appName = des.getNamespace();
                }
                Logger.logTraffic(TAG, "IP : " + destIP + "\nRequest : " + msg, ((LocationDetection) plugins.get(0)).getLocations());
            }
        } else {
            for(IPlugin plugin : plugins) {
                String ret = outgoing ? plugin.handleRequest(msg) : plugin.handleResponse(msg);
                Logger.d(TAG, "" + (outgoing) + " " + got + " " + msg);
                if(ret != null && outgoing) {
                    if(appName == null) {
                        ConnectionDescriptor des = vpnService.getClientAppResolver().getClientDescriptorBySocket(inSocket);
                        if(des != null) {
                            appName = des.getName();
                            packageName = des.getNamespace();
                        }
                    }
                    if (ret.contains("IMEI IMEI")){
                        Logger.i(TAG,"OMG WTF WHY IS THIS HAPPENING " + ret);
                    }
                    vpnService.notify(appName, ret);
                    Logger.i(TAG,"ICETEST " + ret);

                    Logger.logTraffic(appName, "IP : " + destIP + "\nRequest : " + msg + "\nType : " + ret, ((LocationDetection) plugins.get(0)).getLocations());
                }
                msg = outgoing ? plugin.modifyRequest(msg) : plugin.modifyResponse(msg);
            }
            //buff = msg.getBytes();
            //got = buff.length;
            if(PROTECT && outgoing) Logger.d(TAG, new String(Arrays.copyOfRange(buff, 0, got)));
        }
        out.write(buff, 0, got);
        out.flush();
      }
        Logger.d(TAG, "SocketForwarder stop, got : " + got);
    } catch (Exception ignore) {
      ignore.printStackTrace();
        Logger.d(TAG, "outgoing : " + outgoing);
    }
    /*
    finally {
      try {
        this.inSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    */
  }
}
