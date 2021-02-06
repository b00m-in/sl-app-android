/*
* Copyright (C) 2019 Texas Instruments Incorporated - http://www.ti.com/
*
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions
*  are met:
*
*    Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
*    Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in the
*    documentation and/or other materials provided with the
*    distribution.
*
*    Neither the name of Texas Instruments Incorporated nor the names of
*    its contributors may be used to endorse or promote products derived
*    from this software without specific prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
*  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
*  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
*  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
*  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
*  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
*  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
*  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
*  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
*  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
*  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
*/


package in.b00m.smartconfig.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.net.InetAddress;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.Uri.Builder;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import in.b00m.smartconfig.utils.WifiNetworkUtils.BitbiteNetworkUtilsCallback;
import in.b00m.smartconfig.utils.WifiNetworkUtils.WifiConnectionFailure;
import in.b00m.smartconfig.R;

import javax.net.ssl.SSLContext;
//import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class NetworkUtil {
    // variable to hold context
    private static Context context;

    private static final String TAG = "NetworkUtil";
    public static int NOT_CONNECTED = 0;
    public static int WIFI = 1;
    public static int MOBILE = 2;
    public static boolean didRedirect = false;
    public static final String HTTP_ = "http";
    public static final String HTTPS_ = "https";
    private static Logger mLogger = LoggerFactory.getLogger(NetworkUtil.class);

    public NetworkUtil (Context _context){
        context=_context;
    }

    public static int getConnectionStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //NetworkInfo activeNetwork = cm.getActiveNetworkInfo(); // getActiveNetworkInfo deprecated in 29
        Network activeNetwork = cm.getActiveNetwork();
        NetworkCapabilities nc = cm.getNetworkCapabilities(activeNetwork);
        //if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
        if (activeNetwork == null) {
            Log.i(TAG, "activeNetwork is null");
            return NOT_CONNECTED;
        } else if (activeNetwork != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            Log.i(TAG, "nonnull activeNetwork with wifi transport and internet capability!");
            return WIFI;
        } else if(activeNetwork != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
        //if (activeNetwork != null) {// || mNetwork != null) {
            //if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) // getType deprecated in 28
            Log.i(TAG, "nonnull activeNetwork with wifi transport");
            return WIFI;
            //if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
            //    return MOBILE;
        } else {
            return NOT_CONNECTED;
        }
    }

    public static String getConnectedSSID(Context context) {
        if (context == null)
            return null;
        String networkName = null;
        int networkState = getConnectionStatus(context);
        //Log.i(TAG, "Network State:" + networkState);
        mLogger.info("Network state: " + networkState);
        if (networkState == NetworkUtil.WIFI) { //no wifi connection and alert dialog allowed //i-why no wifi connection?
            //Log.i(TAG, "Calling wifi manager:" + networkState);
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    networkName = wifiInfo.getSSID().replaceAll("\"", "");
                }
            }
        } 
        if (networkName == null || networkName.equals("<unknown ssid>") || networkName.equals("0x") || networkName.equals("")) {
            networkName = null;
        }
        return networkName;
    }

    public static String getConnectionStatusString(Context context) {
        int connectionStatus = NetworkUtil.getConnectionStatus(context);
        if (connectionStatus == NetworkUtil.WIFI)
            return "Connected to Wifi";
        if (connectionStatus == NetworkUtil.MOBILE)
            return "Connected to Mobile Data";
        return "No internet connection";
    }

    public static Boolean wifiNetReq(Context context) {
        //Log.i(TAG, "Creating initial network request" );
        WifiNetworkUtils.getInstance(context).unregisterMnetworkCallback();
        NetworkRequest nreq = getNetworkRequest(context, "guess", SecurityType.WPA2, null, true);
        //Log.i(TAG, "wifiNetReq calling wifi manager ... ");
        final WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
            .setPriority(2).build();
        final List<WifiNetworkSuggestion> suggestionsList =
            new ArrayList<WifiNetworkSuggestion>(); 
        suggestionsList.add(suggestion);
        wifiMgr.addNetworkSuggestions(suggestionsList);
        BitbiteNetworkUtilsCallback bcallback = new BitbiteNetworkUtilsCallback() {
            @Override
            public void successfullyConnectedToNetwork(String ssid) {
                //no need to add toast
                Log.i(TAG, "wifiNetReq succesfully connected to network" + ssid );
            }
            @Override
            public void failedToConnectToNetwork(WifiConnectionFailure failure) {
                Log.i(TAG, "wifiNetReq failed to connect to initial network ");
            }
            @Override
            public void successfulConnectionToNetwork(Network activeNetwork) {
                Log.i(TAG, "wifiNetReq: successful connection to :" + activeNetwork.toString());
            }
        };
        ConnectivityManager.NetworkCallback ncallback = new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(Network network) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //Log.i(TAG, "onAvailable " + network.toString() + " " + network.getClass().getName());
                    WifiNetworkUtils.getInstance(context).setCurrentNetwork(network);
                }
                ConnectivityManager myConnManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkCapabilities nc = myConnManager.getNetworkCapabilities(network);

                if (!nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    Log.i(TAG, "wifiNetReq: mysimplelink available");
                    bcallback.successfullyConnectedToNetwork("mysimplelink");
                } else {
                    Log.i(TAG, "wifiNetReq: Boomin available");
                    bcallback.successfullyConnectedToNetwork("BOOMIN");
                }
                //bcallback.successfulConnectionToNetwork(network);
            }

            @Override
            public void onUnavailable() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //Log.i(TAG, "wifiNetReq: onUnavailable ");
                }
                bcallback.failedToConnectToNetwork(WifiConnectionFailure.Unknown);
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        //Log.i(TAG, "wifiNetReq: onCapabilitiesChanged " + networkCapabilities.getLinkDownstreamBandwidthKbps());
                        //Log.i(TAG, "wifiNetReq: onCapabilitiesChanged has wifi? " + networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                        //Log.i(TAG, "wifiNetReq: onCapabilitiesChanged has internet? " + networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
                    }
                } catch (Exception e) {
                    //Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        //Log.i(TAG, "onLinkPropertiesChanged " + linkProperties.getInterfaceName());
                        //Log.i(TAG, "onLinkPropertiesChanged " + linkProperties.getRoutes());
                        //Log.i(TAG, "onLinkPropertiesChanged " + linkProperties.getDhcpServerAddress()); // only available in Api 30
                        //Log.i(TAG, "onLinkPropertiesChanged " + linkProperties.getDomains());
                        //Log.i(TAG, "onLinkPropertiesChanged " + linkProperties.getDnsServers());
                        //Log.i(TAG, "onLinkPropertiesChanged " + linkProperties.getLinkAddresses());
                    }
                } catch (Exception e) {
                    //Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        WifiNetworkUtils.getInstance(context).connectToWifi29AndUp(nreq, context, bcallback, true, ncallback);
        return true;
    }

    public static List<ScanResult> getWifiScanResults(Boolean sorted, Context context) {
        WifiManager wifiManager = NetworkUtil.getWifiManager(context);
        List<ScanResult> wifiList = wifiManager.getScanResults();
        //Remove results with empty ssid
        List<ScanResult> wifiListNew = new ArrayList<>();
        for (ScanResult scanResult : wifiList) {
            if (!scanResult.SSID.equals(""))
                wifiListNew.add(scanResult);
        }
        wifiList.clear();
        wifiList.addAll(wifiListNew);

        if (!sorted)
            return wifiList;

        ArrayList<ScanResult> wifiWithAPPrefix = new ArrayList<>();
        ArrayList<ScanResult> rest = new ArrayList<>();
        for (ScanResult scanResult : wifiList) {
            if (scanResult.SSID.contains(Constants.DEVICE_PREFIX))
                wifiWithAPPrefix.add(scanResult);
            else
                rest.add(scanResult);
        }
        wifiWithAPPrefix = removeMultipleSSIDsWithRSSI(wifiWithAPPrefix);
        rest = removeMultipleSSIDsWithRSSI(rest);
        wifiWithAPPrefix.addAll(rest);
        wifiList = wifiWithAPPrefix;
        return wifiList;
    }

    /**
     * The removeMultipleSSIDsWithRSSI method is used to remove multiple appearances of identical SSIDs from
     * the list of APs obtained from the wifiManager, and displayed on the smartConfig mode configuration page as WiFi networks.
     * This is due to the possible presence of several APs possessing the same SSID but different BSSIDs, and thus causing
     * the same AP to appear several times on the list.
     */
    public static ArrayList<ScanResult> removeMultipleSSIDsWithRSSI(ArrayList<ScanResult> list) {

        ArrayList<ScanResult> newList = new ArrayList<>();
        boolean contains;
        for (ScanResult ap : list) {
            contains = false;
            for (ScanResult mp : newList) {
                if ((mp.SSID).equals(ap.SSID)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                newList.add(ap);
            }
        }
        Collections.sort(newList, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                return (lhs.level < rhs.level ? 1 : (lhs.level == rhs.level ? 0 : -1));
            }

        });
        return newList;
    }

    public static String getWifiName(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String wifiName = wifiManager.getConnectionInfo().getSSID();
        if (wifiName != null) {
            if (!wifiName.contains("unknown ssid") && wifiName.length() > 2) {
                if (wifiName.startsWith("\"") && wifiName.endsWith("\""))
                    wifiName = wifiName.subSequence(1, wifiName.length() - 1).toString();
                return wifiName;
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    public static String getGateway(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return NetworkUtil.intToIp(wm.getDhcpInfo().gateway);
    }

    public static String intToIp(int i) {
        return ((i >> 24) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                (i & 0xFF);
    }

    public static void startScan(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
    }

    public static WifiManager getWifiManager(Context context) {
        WifiManager wifiManager = null;
        try {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return wifiManager;
    }

    public static void connectToKnownWifi(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
            }
        }
    }

    public static Boolean connectToWifiAfterDisconnecting(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wc;
        wc = new WifiConfiguration();
        wc.SSID = "\"" + ssid + "\"";
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wifiManager.addNetwork(wc);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.enableNetwork(i.networkId, true);
                Boolean flag = wifiManager.reconnect();
                return flag;
            }
        }
        return false;
    }

    public static void removeSSIDFromConfiguredNetwork(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
        List<WifiConfiguration> configuredWifiList = wifiManager.getConfiguredNetworks();
        for (int x = 0; x < configuredWifiList.size(); x++) {
            WifiConfiguration i = configuredWifiList.get(x);
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                //Log.w(TAG, "Removing network: " + i.SSID);
                wifiManager.removeNetwork(i.networkId);
                return;
            }
        }
    }

    public static NetworkRequest getNetworkRequest(Context context, String ssid, SecurityType securityType, String password, Boolean internet) {
        WifiNetworkSpecifier wifiNetworkSpecifier;
        WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
        if(!internet) {
            //builder.setSsidPattern(new PatternMatcher(ssid, PatternMatcher.PATTERN_PREFIX))
            builder.setSsid(ssid);
            switch (securityType) {
                case OPEN:
                    break;
                case WEP:
                    break;
                case WPA1:
                    //Log.i(TAG, "WPA1 unsupported");
                    break;
                case WPA2:
                    if (password != null) {
                        builder.setWpa2Passphrase(password);
                    } else {
                    }
                    break;
                case UNKNOWN:
                    if (password == null) {
                    } else {
                    }
                    break;
                default:
                    break;
            }
        }
        NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
        networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);     
        networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED);            
        if (internet){
            networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);   
        } 
        else{ 
            wifiNetworkSpecifier = builder.build();
            networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);
        }
        NetworkRequest networkRequest = networkRequestBuilder.build();
        return networkRequest;
    }

    public static WifiConfiguration getWifiConfigurationWithInfo(Context context, String ssid, SecurityType securityType, String password) {
        List<WifiConfiguration> configuredWifiList = null;
        WifiManager wifiManager = getWifiManager(context);
        if (wifiManager != null) {
            configuredWifiList = wifiManager.getConfiguredNetworks();
        }
        if (configuredWifiList == null) {
            return null;
        } else {
            for (WifiConfiguration i : configuredWifiList) {
                if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    //Log.i(TAG, "Wifi configuration for " + ssid + " already exist, so we will use it");
                    return i;
                }
            }

            //Log.i(TAG, "Wifi configuration for " + ssid + " doesn't exist, so we will create new one");
            //Log.i(TAG, "SSID: " + ssid);
            //Log.i(TAG, "Security: " + securityType);
            WifiConfiguration wc = new WifiConfiguration();

            wc.SSID = "\"" + ssid + "\"";
            wc.status = WifiConfiguration.Status.ENABLED;
            wc.hiddenSSID = false;

            switch (securityType) {
                case OPEN:
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    break;
                case WEP:
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                    wc.preSharedKey = "\"" + password + "\"";
                    break;
                case WPA1:
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    wc.preSharedKey = "\"" + password + "\"";
                    break;
                case WPA2:
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    wc.preSharedKey = "\"" + password + "\"";
                    break;
                case UNKNOWN:
                    if (password == null) {
                        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    } else {
                        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                        wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                        wc.preSharedKey = "\"" + password + "\"";
                    }
                    break;
                default:
                    break;
            }

            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                wifiManager.addNetwork(wc);
                wifiManager.saveConfiguration();
                //Log.i(TAG, "New wifi configuration with id " + wifiManager.addNetwork(wc));
                //Log.i(TAG, "Saving configuration " + wifiManager.saveConfiguration());
            } else {
                wifiManager.addNetwork(wc);
                wifiManager.saveConfiguration();
            }
            //Log.i(TAG, "wc.networkId " + wc.networkId);

            configuredWifiList = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : configuredWifiList) {
                if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    //Log.i(TAG, "Returning wifiConfiguration with id " + i.networkId);
                    return i;
                }
            }
        }
        return null;
    }

    public static void connectToWifiWithInfo(Context context, String ssid, SecurityType securityType, String password) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);

        int numberOfOcc = 0;

        List<WifiConfiguration> configuredWifiList = wifiManager.getConfiguredNetworks();
        for (int x = 0; x < configuredWifiList.size(); x++) {
            WifiConfiguration i = configuredWifiList.get(x);
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                numberOfOcc++;
            }
        }

        System.out.println("Done checking doubles: " + numberOfOcc);

        for (WifiConfiguration i : configuredWifiList) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                //Log.i(TAG, "Trying to disconnect (success = " + wifiManager.disconnect() + ")");
                //Log.i(TAG, "Trying to connect to " + i.SSID + " (success = " + wifiManager.enableNetwork(i.networkId, true) + ")");
                return;
            }
        }

        WifiConfiguration wc = new WifiConfiguration();

        wc.SSID = "\"" + ssid + "\"";
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.hiddenSSID = false;

        switch (securityType) {
            case OPEN:
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            case WEP:
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                wc.preSharedKey = "\"" + password + "\"";
                break;
            case WPA1:
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wc.preSharedKey = "\"" + password + "\"";
                break;
            case WPA2:
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wc.preSharedKey = "\"" + password + "\"";
                break;
            case UNKNOWN:
                if (password == null) {
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                } else {
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    wc.preSharedKey = "\"" + password + "\"";
                }
                break;
            default:
                break;
        }

        int res = wifiManager.addNetwork(wc);
        //Log.i(TAG, "addNetwork: " + res);
        wifiManager.disconnect();
        wifiManager.enableNetwork(res, true);
        wifiManager.saveConfiguration();

    }

    public static Boolean isLollipopAndUp() {
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        return currentApiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP;
    }

    public static SecurityType getScanResultSecurity(ScanResult scanResult) {
        String cap = scanResult != null ? scanResult.capabilities : "";
        SecurityType newState = scanResult != null ? SecurityType.OPEN : SecurityType.UNKNOWN;

        if (cap.contains("WEP"))
            newState = SecurityType.WEP;
        else if (cap.contains("WPA2"))
            newState = SecurityType.WPA2;
        else if (cap.contains("WPA"))
            newState = SecurityType.WPA1;

        return newState;
    }

    public static Boolean addProfile(String baseUrl, SecurityType securityType, String ssid, String password, String priorityString, DeviceVersion version, String configurer, String coords ) {

        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- addProfile / url: " + url);

        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/profiles_add.html";
                //Log.i(TAG,"REDIRECT- addProfile / url: " + url);
                break;
            case R2:
                //Log.i(TAG,"REDIRECT- addProfile / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                url += "/api/1/wlan/profile_add";
                //Log.i(TAG,"REDIRECT- addProfile / url: " + url);
                break;
            case UNKNOWN:
                break;
        }

        Boolean flag = false;
        if (securityType == SecurityType.UNKNOWN) {
            if (password.matches("")) {
                flag = NetworkUtil.addProfile(baseUrl, SecurityType.OPEN, ssid, password, priorityString, version, configurer, coords);
            } else {
                flag = NetworkUtil.addProfile(baseUrl, SecurityType.WEP, ssid, password, priorityString, version, configurer, coords);
                flag = flag && NetworkUtil.addProfile(baseUrl, SecurityType.WPA1, ssid, password, priorityString, version, configurer, coords);
            }
        } else {
            try {
                /*HttpClient client = getNewHttpClient();
                String addProfileUrl = url;
                HttpPost addProfilePost = new HttpPost(addProfileUrl);
                addProfilePost.setHeader("Referer", configurer);
                addProfilePost.setHeader("Etag", coords);
                mLogger.info("addProfile headers " + configurer + " " + coords); 
                List<NameValuePair> nameValuePairs = new ArrayList<>(4);
                ssid = new String(ssid.getBytes("UTF-8"), "ISO-8859-1");
                nameValuePairs.add(new BasicNameValuePair("__SL_P_P.A", ssid));
                nameValuePairs.add(new BasicNameValuePair("__SL_P_P.B", String.valueOf(SecurityType.getIntValue(securityType))));
                //password = new String(password.getBytes("UTF-8"), "ISO-8859-1");
                password = URLEncoder.encode(password, "UTF-8");
                nameValuePairs.add(new BasicNameValuePair("__SL_P_P.C", password));
                try {
                    int priority = Integer.parseInt(priorityString);
                    nameValuePairs.add(new BasicNameValuePair("__SL_P_P.D", String.valueOf(priority)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    nameValuePairs.add(new BasicNameValuePair("__SL_P_P.D", String.valueOf(0)));
                }
                addProfilePost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                client.execute(addProfilePost);
                flag = true;*/
                // Setting up the parameters and encoded url for post request
                Uri.Builder builder = new Uri.Builder();
                Map<String, String> nameValuePairs = new HashMap<>(4);
                ssid = new String(ssid.getBytes("UTF-8"), "ISO-8859-1");
                nameValuePairs.put("__SL_P_P.A", ssid);
                nameValuePairs.put("__SL_P_P.B", String.valueOf(SecurityType.getIntValue(securityType)));
                password = URLEncoder.encode(password, "UTF-8");
                nameValuePairs.put("__SL_P_P.C", password);
                try {
                    int priority = Integer.parseInt(priorityString);
                    nameValuePairs.put("__SL_P_P.D", String.valueOf(priority));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    nameValuePairs.put("__SL_P_P.D", String.valueOf(0));
                }
                // Is this the best way to iterate over a map?
                Iterator entries = nameValuePairs.entrySet().iterator();
                while(entries.hasNext()) {
                    Map.Entry entry = (Map.Entry) entries.next();
                    builder.appendQueryParameter(entry.getKey().toString(), entry.getValue().toString());
                    entries.remove();
                }
                String reqBody = builder.build().getEncodedQuery();
                //Log.i(TAG, "addProfile - getting network instance to connect to: " + url);
                //Log.i(TAG, "addProfile - reqBody: " + reqBody);
                Network cn = WifiNetworkUtils.getInstance(context).getCurrentNetwork();
                int timeoutConnection = 5000;
                int timeoutSocket = 10000;
                URL urlObj = new URL(url);
                HttpURLConnection httpURLConnection = (HttpURLConnection)cn.openConnection(urlObj);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestProperty("Referer", configurer);
                httpURLConnection.setRequestProperty("Etag", coords);
                OutputStream outpost = new BufferedOutputStream(httpURLConnection.getOutputStream());
                //httpURLConnection.connect(); // presumably getting the ouput stream from the connection makes a connection?
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outpost, "utf-8"));
                writer.write(reqBody);
                writer.flush();
                writer.close();
                outpost.close();
                int urlConResponseCode = httpURLConnection.getResponseCode();
                String ver = "";
                if (urlConResponseCode == HttpURLConnection.HTTP_OK
                   || urlConResponseCode == HttpURLConnection.HTTP_NO_CONTENT 
                   || urlConResponseCode == HttpURLConnection.HTTP_ACCEPTED) {
                    //InputStream in = new BufferedInputStream(httpURLConnection.getInputStream()); 
                    ver = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining("\n"));
                    //Log.i(TAG,"addProfile response: " + ver);
                    flag = true;
                } else {
                    //Log.i(TAG, "addProfile urlResponseCode: " + urlConResponseCode);
                }
            } catch (Exception e) {
                //Log.e(TAG, "addProfile exception:" + e.toString());
                e.printStackTrace();
                flag = false;
            } finally {
            }
        }
        return flag;
    }

    public static Boolean moveStateMachineAfterProfileAddition(String baseUrl, String ssid, DeviceVersion version) throws CertificateException {

        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- moveStateMachineAfterProfileAddition / url: " + url);

        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/add_profile.html";
                //Log.i(TAG,"REDIRECT- moveStateMachineAfterProfileAddition / url: " + url);
                break;
            case R2:
                //Log.i(TAG,"REDIRECT- moveStateMachineAfterProfileAddition / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                url += "/api/1/wlan/confirm_req";
                //Log.i(TAG,"REDIRECT- moveStateMachineAfterProfileAddition / url: " + url);
                break;
            case UNKNOWN:
                break;
        }
        Boolean flag = false;
        //HttpClient client = getNewHttpClient();
        try {
            //String stateMachineUrl = url;
            //HttpPost stateMachinePost = new HttpPost(stateMachineUrl);

            Map<String, String> nameValuePairs = new HashMap<>(1);
            switch (version) {
                case R1:
                    /*List<NameValuePair> stateParam = new ArrayList<>(1);
                    stateParam.add(new BasicNameValuePair("__SL_P_UAN", ssid));
                    stateMachinePost.setEntity(new UrlEncodedFormEntity(stateParam));*/
                    nameValuePairs.put("__SL_P_UAN", ssid);
                    break;
                case R2:
                    // even though this is not needed, it's left here to avoid a null request body further along
                    nameValuePairs.put("__SL_P_UAN", ssid);
                    break;
                case UNKNOWN:
                    break;
            }
            //client.execute(stateMachinePost);
            //flag = true;
            Uri.Builder builder = new Uri.Builder();
            // Is this the best way to iterate over a map?
            Iterator entries = nameValuePairs.entrySet().iterator();
            while(entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                builder.appendQueryParameter(entry.getKey().toString(), entry.getValue().toString());
                entries.remove();
            }
            String reqBody = builder.build().getEncodedQuery();
            //Log.i(TAG, "moveStateMachine - getting network instance to connect to: " + url);
            //Log.i(TAG, "moveStateMachine - reqBody: " + reqBody);
            Network cn = WifiNetworkUtils.getInstance(context).getCurrentNetwork();
            URL urlObj = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection)cn.openConnection(urlObj);
            httpURLConnection.setDoOutput(true);
            OutputStream outpost = new BufferedOutputStream(httpURLConnection.getOutputStream());
            //httpURLConnection.connect(); // presumably getting the ouput stream from the connection makes a connection?
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outpost, "utf-8"));
            writer.write(reqBody);
            writer.flush();
            writer.close();
            outpost.close();
            int urlConResponseCode = httpURLConnection.getResponseCode();
            String ver = "";
            if (urlConResponseCode == HttpURLConnection.HTTP_OK
               || urlConResponseCode == HttpURLConnection.HTTP_NO_CONTENT
               || urlConResponseCode == HttpURLConnection.HTTP_ACCEPTED) {
                //InputStream in = new BufferedInputStream(httpURLConnection.getInputStream()); 
                ver = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining("\n"));
                //Log.i(TAG,"moveStateMachine response: " + ver);
                flag = true;
            } else {
                //Log.i(TAG, "moveStateMachine urlResponseCode: " + urlConResponseCode);
            }
        } catch (Exception e) {
            //Log.e(TAG, "moveStateMachine exception: " + e.toString());
            e.printStackTrace();
            flag = false;
        }
        return flag;
    }

    public static DeviceVersion getSLVersion(String baseUrl) throws IOException, URISyntaxException {
        didRedirect = false;
        //Log.i(TAG,"NOREDIRECT- getSLVersion / did redirect: " + didRedirect);
        //Log.i(TAG,"NOREDIRECT- getSLVersion / base url: " + baseUrl);
        String url = baseUrl + "/param_product_version.txt";
        //Log.i(TAG,"NOREDIRECT- getSLVersion / url: " + url);
        if (!baseUrl.startsWith("http")) {
            url = HTTP_ + baseUrl + "/param_product_version.txt";
            //Log.i(TAG,"REDIRECT- getSLVersion / url: " + url);
        }
        DeviceVersion version = DeviceVersion.UNKNOWN;

        try {
            //Log.i(TAG, "NOREDIRECT - Getting network instance");
            Network cn = WifiNetworkUtils.getInstance(context).getCurrentNetwork();
            InetAddress ip =  cn.getByName("mysimplelink.net");
            //Log.i(TAG, "Ip: " + ip); 
            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            int timeoutConnection = 5000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 10000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            URL urlObj = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection)cn.openConnection(urlObj);
            //HttpURLConnection httpURLConnection = (HttpURLConnection) urlObj.openConnection();
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            //httpURLConnection.addRequestProperty("User-Agent", "Mozilla");
            //httpURLConnection.addRequestProperty("Referer", "google.com");
            //Log.i(TAG, "Connecting to SL via http for device version ..");
            httpURLConnection.connect();
            int urlConResponseCode = httpURLConnection.getResponseCode();
            String ver = "";
            if (urlConResponseCode == HttpURLConnection.HTTP_OK) {
                //InputStream in = new BufferedInputStream(httpURLConnection.getInputStream()); 
                ver = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining("\n"));
                //Log.i(TAG,"slversion: " + ver);
            } else {
                //Log.i(TAG, "urlResponseCode: " + urlConResponseCode);
            }
            httpURLConnection.getInputStream().close();
            httpURLConnection.disconnect();

            //Log.i(TAG,"NOREDIRECT- getSLVersion / response code: " + urlConResponseCode);
            if (urlConResponseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                //wasRedirected
                didRedirect = true;
                //Log.i(TAG,"REDIRECT- getSLVersion / did redirect: " + didRedirect);
                if (baseUrl.startsWith("http")) {
                    baseUrl = baseUrl.substring(baseUrl.indexOf(":"),baseUrl.length());
                }
                url = HTTPS_ + baseUrl + "/param_product_version.txt";
                //Log.i(TAG,"REDIRECT- getSLVersion / redirected url: " + url);
            }
            /*//Log.i(TAG, "Creating http client to connect to SL via http for device version ..");
            HttpClient client = getNewHttpClient();
            HttpGet slResult = new HttpGet(url);
            HttpResponse response = client.execute(slResult);
            String html = EntityUtils.toString(response.getEntity());
            //Log.i(TAG,"REDIRECT- getSLVersion / html: " + html);
            if (html.equals("R1.0") || html.contains("1.0")) {
                version = DeviceVersion.R1;
            } else if (html.equals("R2.0") || html.equals("2.0") || html.contains("2.0")) {
                version = DeviceVersion.R2;
            }*/
            if (ver.equals("R1.0") || ver.contains("1.0")) {
                version = DeviceVersion.R1;
            } else if (ver.equals("R2.0") || ver.equals("2.0") || ver.contains("2.0")) {
                version = DeviceVersion.R2;
            }
        } catch (Exception e) {
            //  solution when we get exception on first try
            e.printStackTrace();
            System.out.println(e);
            //Log.e(TAG, "Exception getSLVersion " + e.toString());

            if(didRedirect) {
                HttpClient client = getNewHttpClient();
                HttpGet slResult = new HttpGet(url);
                HttpResponse response = client.execute(slResult);
                String html = EntityUtils.toString(response.getEntity());
                //Log.i(TAG, "REDIRECT 1- getSLVersion / html: " + html);
                if (html.equals("R1.0") || html.contains("1.0")) {
                    version = DeviceVersion.R1;
                } else if (html.equals("R2.0") || html.equals("2.0") || html.contains("2.0")) {
                    version = DeviceVersion.R2;
                }
            }else{
                HttpClient client = new DefaultHttpClient();
                HttpGet slResult = new HttpGet();
                slResult.setURI(new URI(url));
                HttpResponse response = client.execute(slResult);
                String html = EntityUtils.toString(response.getEntity());
                //Log.i(TAG,"REDIRECT 2- getSLVersion / html: " + html);
                if (html.equals("R1.0") || html.contains("1.0")) {
                    version = DeviceVersion.R1;
                } else if (html.equals("R2.0") || html.equals("2.0") || html.contains("2.0")) {
                    version = DeviceVersion.R2;
                }
            }
        }
        return version;
    }

    public static String getDeviceName(String baseUrl, DeviceVersion version) {
        String deviceName = "";
        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- getDeviceName / url: " + url);

        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/param_device_name.txt";
                //Log.i(TAG,"REDIRECT- getDeviceName / url: " + url);
                break;
            case R2:
                //Log.i(TAG,"REDIRECT- getDeviceName / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                url += "/__SL_G_DNP";
                //Log.i(TAG,"REDIRECT- getDeviceName / url: " + url);
                break;
            case UNKNOWN:
                break;
        }

        try {
            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            int timeoutConnection = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            HttpClient client = getNewHttpClient();
            HttpGet slResult = new HttpGet(url);

            HttpResponse response = client.execute(slResult);
            String name = EntityUtils.toString(response.getEntity());
            deviceName = name;
        } catch (Exception e) {
            e.printStackTrace();
            //Log.e(TAG, "Failed to fetch device name from board");
        }
        return deviceName;
    }

    public static ArrayList<String> getSSIDListFromDevice(String baseUrl, DeviceVersion version) throws IOException {
        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- getSSIDListFromDevice / url: " + url);

        switch (version) {
            case R1:
                url = HTTP_ + url;
                //Log.i(TAG,"REDIRECT- getSSIDListFromDevice / url: " + url);
                break;
            case R2:
                //Log.i(TAG,"REDIRECT- getSSIDListFromDevice / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                //Log.i(TAG,"REDIRECT- getSSIDListFromDevice / url: " + url);
                break;
            case UNKNOWN:
                break;
        }

        url = url + "/netlist.txt";
        //Log.i(TAG,"REDIRECT- getSSIDListFromDevice / url: " + url);

        ArrayList<String> list = new ArrayList<>();

        mLogger.info("*AP* Getting list of available access points from SL device, from url: " + url);
        //Log.e(TAG, "getssidlistfromdevice getting list");

        try {
            /*HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            int timeoutConnection = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            HttpClient client = getNewHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            String responseString = EntityUtils.toString(response.getEntity());
            mLogger.info("*AP* Got netlist with results: " + responseString);
            */
            //Log.i(TAG, "getssidlistfromdevice - Getting network instance");
            Network cn = WifiNetworkUtils.getInstance(context).getCurrentNetwork();
            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            int timeoutConnection = 5000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 10000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            URL urlObj = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection)cn.openConnection(urlObj);
            //HttpURLConnection httpURLConnection = (HttpURLConnection) urlObj.openConnection();
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            //httpURLConnection.addRequestProperty("User-Agent", "Mozilla");
            //httpURLConnection.addRequestProperty("Referer", "google.com");
            httpURLConnection.connect();
            int urlConResponseCode = httpURLConnection.getResponseCode();
            String ver = "";
            if (urlConResponseCode == HttpURLConnection.HTTP_OK) {
                //InputStream in = new BufferedInputStream(httpURLConnection.getInputStream()); 
                ver = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining("\n"));
                //Log.i(TAG,"ssids available: " + ver);
            } else {
                //Log.i(TAG, "getssidlistfromdevice urlResponseCode: " + urlConResponseCode);
            }
            httpURLConnection.getInputStream().close();
            httpURLConnection.disconnect();

            //Log.i(TAG,"NOREDIRECT- getSLVersion / response code: " + urlConResponseCode);
            String[] names = ver.split(";");//responseString.split(";");
            for (String name : names) {
                if (!name.equals("X"))
                    list.add(name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Log.e(TAG, "Exception getssidlistfromdevice " + e.toString());
            mLogger.error("Failed to get netlist: " + e.getMessage());
            return null;
        }
        return list;
    }

    public static Boolean rescanNetworksOnDevice(String url, DeviceVersion version) throws CertificateException {
        Boolean flag = false;
        HttpClient client = getNewHttpClient();
        List<NameValuePair> stateParam;
        String rescanUrl = url;
        //Log.i(TAG,"REDIRECT- rescanNetworksOnDevice / rescanUrl: " + rescanUrl);
        mLogger.info("*AP* Rescanning for list of available access points from SL device with url: " + url);
        switch (version) {
            case R1:
                rescanUrl = HTTP_ + rescanUrl;
                //Log.i(TAG,"REDIRECT- rescanNetworksOnDevice / rescanUrl: " + rescanUrl);
                stateParam = new ArrayList<NameValuePair>(1);
                try {
                    HttpPost rescanPost = new HttpPost(rescanUrl);
                    stateParam.add(new BasicNameValuePair("__SL_P_UFS", "just empty information"));
                    rescanPost.setEntity(new UrlEncodedFormEntity(stateParam));
                    client.execute(rescanPost);
                    flag = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    mLogger.error("*AP* Failed to perform rescan");
                    flag = false;
                }
                break;
            case R2:
                stateParam = new ArrayList<NameValuePair>(2);
                //Log.i(TAG,"REDIRECT- rescanNetworksOnDevice / didRedirect: " + didRedirect);
                if (didRedirect) {
                    rescanUrl = HTTPS_ + rescanUrl;
                } else {
                    rescanUrl = HTTP_ + rescanUrl;
                }
                rescanUrl += "/api/1/wlan/en_ap_scan";
                //Log.i(TAG,"REDIRECT- rescanNetworksOnDevice / rescanUrl: " + rescanUrl);

                try {
                    HttpPost rescanPost = new HttpPost(rescanUrl);
                    stateParam.add(new BasicNameValuePair("__SL_P_SC1", "10"));
                    stateParam.add(new BasicNameValuePair("__SL_P_SC2", "1"));
                    rescanPost.setEntity(new UrlEncodedFormEntity(stateParam));
                    client.execute(rescanPost);
                    flag = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    mLogger.error("*AP* Failed to perform rescan (R2)");
                    flag = false;
                }
                break;
            case UNKNOWN:
                break;
        }
        return flag;
    }

    public static CFG_Result_Enum cfgEnumForResponse(String string) {
        CFG_Result_Enum resultEnum;
        if (string.contains("5") || string.contains("4")) {
            resultEnum = CFG_Result_Enum.Success;
        } else if (string.contains("Unknown Token")) {
            resultEnum = CFG_Result_Enum.Unknown_Token;
        } else if (string.contains("Timeout")) {
            resultEnum = CFG_Result_Enum.Time_Out;
        } else if (string.contains("0")) {
            resultEnum = CFG_Result_Enum.Not_Started;
        } else if (string.contains("1")) {
            resultEnum = CFG_Result_Enum.Ap_Not_Found;
        } else if (string.contains("2")) {
            resultEnum = CFG_Result_Enum.Wrong_Password;
        } else if (string.contains("3")) {
            resultEnum = CFG_Result_Enum.Ip_Add_Fail;
        } else {
            resultEnum = CFG_Result_Enum.Failure;
        }
        return resultEnum;
    }

    public static Boolean setDatetime(String newDate, String baseUrl, DeviceVersion version) throws CertificateException {
        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- setNewDeviceName / url: " + url);
        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/mode_config";
                //Log.i(TAG,"REDIRECT- setNewDeviceName / url: " + url);
                break;
            case R2:
                //Log.i(TAG,"REDIRECT- setDatetime / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                url += "/api/1/wlan/en_ap_scan/set_time";
                //Log.i(TAG,"REDIRECT- setDateTime / url: " + url);
                break;
            case UNKNOWN:
                break;
        }
        Boolean flag = false;
        //HttpClient client = getNewHttpClient();
        try {
            /*String stateMachineUrl = url;
            HttpPost rescanPost = new HttpPost(stateMachineUrl);
            List<NameValuePair> stateParam = new ArrayList<>(1);
            newDate = new String(newDate.getBytes("UTF-8"), "ISO-8859-1");
            stateParam.add(new BasicNameValuePair("__SL_P_S.J", newDate));
            rescanPost.setEntity(new UrlEncodedFormEntity(stateParam));
            client.execute(rescanPost);
            flag = true;*/
            Uri.Builder builder = new Uri.Builder();
            Map<String, String> nameValuePairs = new HashMap<>(1);
            nameValuePairs.put("__SL_P_S.J", newDate);
            // Is this the best way to iterate over a map?
            Iterator entries = nameValuePairs.entrySet().iterator();
            while(entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                builder.appendQueryParameter(entry.getKey().toString(), entry.getValue().toString());
                entries.remove();
            }
            String reqBody = builder.build().getEncodedQuery();
            //Log.i(TAG, "setDateTime- getting network instance to connect to: " + url);
            //Log.i(TAG, "setDateTime- reqBody: " + reqBody);
            Network cn = WifiNetworkUtils.getInstance(context).getCurrentNetwork();
            int timeoutConnection = 5000;
            int timeoutSocket = 10000;
            URL urlObj = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection)cn.openConnection(urlObj);
            httpURLConnection.setDoOutput(true);
            OutputStream outpost = new BufferedOutputStream(httpURLConnection.getOutputStream());
            //httpURLConnection.connect(); // presumably getting the ouput stream from the connection makes a connection?
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outpost, "utf-8"));
            writer.write(reqBody);
            writer.flush();
            writer.close();
            outpost.close();
            int urlConResponseCode = httpURLConnection.getResponseCode();
            String ver = "";
            if (urlConResponseCode == HttpURLConnection.HTTP_OK
               || urlConResponseCode == HttpURLConnection.HTTP_NO_CONTENT
               || urlConResponseCode == HttpURLConnection.HTTP_ACCEPTED) {
                //InputStream in = new BufferedInputStream(httpURLConnection.getInputStream()); 
                ver = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining("\n"));
                //Log.i(TAG,"setDateTime response: " + ver + " responsecode: " + urlConResponseCode);
                flag = true;
            } else {
                //Log.i(TAG, "setDateTime urlResponseCode: " + urlConResponseCode);
            }
        } catch (Exception e) {
            //Log.e(TAG, "setDateTime exception:" + e.toString());
            e.printStackTrace();
            flag = false;
        }
        return flag;
    }

    public static Boolean setOwner(String referer, String coords, String baseUrl, DeviceVersion version) throws CertificateException {
        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- setNewDeviceOwner / url: " + url);
        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/set_owner";
                //Log.i(TAG,"REDIRECT- setNewDeviceOwner / url: " + url);
                break;
            case R2:
                //Log.i(TAG,"REDIRECT- setOwner / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                //url += "/m0v/set_owner?" + coords;
                //coords = "/" + coords;
                //url += coords;
                url += "/m0v/set_owner/";
                mLogger.info("setOwner url " + url); 
                //Log.i(TAG,"REDIRECT- setOwner / url: " + url);
                break;
            case UNKNOWN:
                break;
        }
        Boolean flag = false;
        //HttpClient client = getNewHttpClient();
        try {
            /*String stateMachineUrl = url;
            HttpPost addOwnerPost = new HttpPost(stateMachineUrl);
            addOwnerPost.setHeader("Referer", referer);
            addOwnerPost.setHeader("Etag", coords);
            mLogger.info("setOwner headers " + url + " "  + referer + " " + coords); 
            */
            //addOwnerPost.setHeader("Content-Length", Integer.toString(referer.length()));
            // List<NameValuePair> stateParam = new ArrayList<>(1);
            // configurer = new String(configurer.getBytes("UTF-8"), "ISO-8859-1");
            // stateParam.add(new BasicNameValuePair("configurer", configurer));
            // addOwnerPost.setEntity(new UrlEncodedFormEntity(stateParam));
            //
            /*client.execute(addOwnerPost);
            flag = true;*/
            Uri.Builder builder = new Uri.Builder();
            Map<String, String> nameValuePairs = new HashMap<>(1);
            nameValuePairs.put("referer", referer);
            // Is this the best way to iterate over a map?
            Iterator entries = nameValuePairs.entrySet().iterator();
            while(entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                builder.appendQueryParameter(entry.getKey().toString(), entry.getValue().toString());
                entries.remove();
            }
            String reqBody = builder.build().getEncodedQuery();
            //Log.i(TAG, "setOwner- getting network instance to connect to: " + url);
            //Log.i(TAG, "setOwner- reqBody: " + reqBody);
            Network cn = WifiNetworkUtils.getInstance(context).getCurrentNetwork();
            int timeoutConnection = 5000;
            int timeoutSocket = 10000;
            URL urlObj = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection)cn.openConnection(urlObj);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestProperty("Referer", referer);
            httpURLConnection.setRequestProperty("Etag", coords);
            OutputStream outpost = new BufferedOutputStream(httpURLConnection.getOutputStream());
            //httpURLConnection.connect(); // presumably getting the ouput stream from the connection makes a connection?
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outpost, "utf-8"));
            writer.write(reqBody);
            writer.flush();
            writer.close();
            outpost.close();
            int urlConResponseCode = httpURLConnection.getResponseCode();
            String ver = "";
            if (urlConResponseCode == HttpURLConnection.HTTP_OK
               || urlConResponseCode == HttpURLConnection.HTTP_NO_CONTENT
               || urlConResponseCode == HttpURLConnection.HTTP_ACCEPTED) {
                //InputStream in = new BufferedInputStream(httpURLConnection.getInputStream()); 
                ver = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining("\n"));
                //Log.i(TAG,"setOwner response: " + ver);
                flag = true;
            } else {
                //Log.i(TAG, "setOwner urlResponseCode: " + urlConResponseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Log.e(TAG,"setOwner exception: " + e.toString());
            flag = false;
        }
        return flag;
}

    public static Boolean setNewDeviceName(String newName, String baseUrl, DeviceVersion version) throws CertificateException {
        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- setNewDeviceName / url: " + url);
        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/mode_config";
                //Log.i(TAG,"REDIRECT- setNewDeviceName / url: " + url);
                break;
            case R2:
                //Log.i(TAG,"REDIRECT- setNewDeviceName / didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                url += "/api/1/netapp/set_urn";
                //Log.i(TAG,"REDIRECT- setNewDeviceName / url: " + url);
                break;
            case UNKNOWN:
                break;
        }
        Boolean flag = false;
        //HttpClient client = getNewHttpClient();
        try {
            /*String stateMachineUrl = url;
            HttpPost rescanPost = new HttpPost(stateMachineUrl);
            List<NameValuePair> stateParam = new ArrayList<>(1);
            newName = new String(newName.getBytes("UTF-8"), "ISO-8859-1");
            stateParam.add(new BasicNameValuePair("__SL_P_S.B", newName));
            rescanPost.setEntity(new UrlEncodedFormEntity(stateParam));
            client.execute(rescanPost);
            flag = true;*/
            Uri.Builder builder = new Uri.Builder();
            Map<String, String> nameValuePairs = new HashMap<>(1);
            nameValuePairs.put("__SL_P_S.B", newName);
            // Is this the best way to iterate over a map?
            Iterator entries = nameValuePairs.entrySet().iterator();
            while(entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                builder.appendQueryParameter(entry.getKey().toString(), entry.getValue().toString());
                entries.remove();
            }
            String reqBody = builder.build().getEncodedQuery();
            //Log.i(TAG, "setDeviceName - getting network instance to connect to: " + url);
            //Log.i(TAG, "setDeviceName - reqBody: " + reqBody);
            Network cn = WifiNetworkUtils.getInstance(context).getCurrentNetwork();
            int timeoutConnection = 5000;
            int timeoutSocket = 10000;
            URL urlObj = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection)cn.openConnection(urlObj);
            httpURLConnection.setDoOutput(true);
            OutputStream outpost = new BufferedOutputStream(httpURLConnection.getOutputStream());
            //httpURLConnection.connect(); // presumably getting the ouput stream from the connection makes a connection?
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outpost, "utf-8"));
            writer.write(reqBody);
            writer.flush();
            writer.close();
            outpost.close();
            int urlConResponseCode = httpURLConnection.getResponseCode();
            String ver = "";
            if (urlConResponseCode == HttpURLConnection.HTTP_OK
               || urlConResponseCode == HttpURLConnection.HTTP_NO_CONTENT
               || urlConResponseCode == HttpURLConnection.HTTP_ACCEPTED) {
                //InputStream in = new BufferedInputStream(httpURLConnection.getInputStream()); 
                ver = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining("\n"));
                //Log.i(TAG,"setDeviceName response: " + ver);
                flag = true;
            } else {
                //Log.i(TAG, "setDeviceName urlResponseCode: " + urlConResponseCode);
            }
            flag = true;
        } catch (Exception e) {
            //Log.e(TAG,"setDeviceName exception: " + e.toString());
            e.printStackTrace();
            flag = false;
        }
        return flag;
    }

    public static Boolean setIotUuid(String newName, String baseUrl) throws CertificateException {
        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- setIotUuid / url: " + url);
        //Log.i(TAG,"REDIRECT- setIotUuid / didRedirect: " + didRedirect);
        if(didRedirect) {
            url = HTTPS_ + url;
        } else {
            url = HTTP_ + url;
        }
        url += "/api/1/iotlink/uuid";
        //Log.i(TAG,"REDIRECT- setIotUuid / url: " + url);
        if (newName.equals(""))
            return false;
        Boolean flag = false;
        HttpParams httpParameters = new BasicHttpParams();
        // Set the timeout in milliseconds until a connection is established.
        // The default value is zero, that means the timeout is not used.
        int timeoutConnection = 1000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        int timeoutSocket = 1000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        HttpClient client = getNewHttpClient();
        try {
            String stateMachineUrl = url;
            HttpPost rescanPost = new HttpPost(stateMachineUrl);
            List<NameValuePair> stateParam = new ArrayList<>(1);
            newName = new String(newName.getBytes("UTF-8"), "ISO-8859-1");
            stateParam.add(new BasicNameValuePair("uuid", newName));
            rescanPost.setEntity(new UrlEncodedFormEntity(stateParam));
            HttpResponse response = client.execute(rescanPost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                flag = true;
            }
            client.getConnectionManager().shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            client.getConnectionManager().shutdown();
            flag = false;
        }
        return flag;
    }

    public static String getCGFResultFromDevice(String baseUrl, DeviceVersion version) {
        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- getCGFResultFromDevice / url: " + url);
        if (url.startsWith("http")) {
            url = url.substring(url.indexOf(":"),url.length());
        }
        //Log.i(TAG,"REDIRECT- getCGFResultFromDevice / url: " + url);

        switch (version) {
            case R1:
                url = HTTP_ + url;
                url += "/param_cfg_result.txt";
                //Log.i(TAG,"REDIRECT- getCGFResultFromDevice / R1 - url: " + url);
                break;
            case R2:
                //Log.i(TAG,"REDIRECT- getCGFResultFromDevice / R2 - didRedirect: " + didRedirect);
                if (didRedirect) {
                    url = HTTPS_ + url;
                } else {
                    url = HTTP_ + url;
                }
                url += "/__SL_G_MCR";
                //Log.i(TAG,"REDIRECT- getCGFResultFromDevice / R2 - url: " + url);
                break;
            case UNKNOWN:
                break;
        }

        String result = "";

        try {
            /*HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            int timeoutConnection = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            HttpClient client = getNewHttpClient();
            HttpGet cfgResult = new HttpGet(url);
            HttpResponse response = client.execute(cfgResult);
            result = EntityUtils.toString(response.getEntity());
            if (result.equals("")) {
                //Log.w(TAG, "CFG result returned empty!");
                mLogger.info("CFG result returned empty!");
                result = "Timeout";
            } else {
                //Log.i(TAG, "CFG result returned: " + result);
                mLogger.info("CFG result returned: " + result);
            }*/
            //Log.i(TAG, "getCGFResultFromDevice - Getting network instance");
            Network cn = WifiNetworkUtils.getInstance(context).getCurrentNetwork();
            URL urlObj = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection)cn.openConnection(urlObj);
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            //Log.i(TAG, "Connecting to SL via http for config result ..");
            httpURLConnection.connect();
            int urlConResponseCode = httpURLConnection.getResponseCode();
            if (urlConResponseCode == HttpURLConnection.HTTP_OK) {
                result = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining("\n"));
                //Log.i(TAG,"cfgresult: " + result);
            } else {
                //Log.i(TAG, "cfgresult urlResponseCode: " + urlConResponseCode);
            }
            httpURLConnection.getInputStream().close();
            httpURLConnection.disconnect();

            //Log.i(TAG,"cfgresult / response code: " + urlConResponseCode);
            if (urlConResponseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                //wasRedirected
                didRedirect = true;
                //Log.i(TAG,"REDIRECT - cfgResult / did redirect: " + didRedirect);
                if (baseUrl.startsWith("http")) {
                    baseUrl = baseUrl.substring(baseUrl.indexOf(":"),baseUrl.length());
                }
                url = HTTPS_ + baseUrl + "/param_cfg_result.txt";
                //Log.i(TAG,"REDIRECT - cfgresult / redirected url: " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Log.e(TAG, "Exception failed to get CFG result" + e.getMessage());
            mLogger.info("Failed to get CFG result");
            result = "Timeout";
        }

        return result;
    }

    public static String getCGFResultFromCloud(String baseUrl, DeviceVersion version) {
        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- getCGFResultFromCloud / url: " + url);
        if (url.startsWith("http")) {
            url = url.substring(url.indexOf(":"),url.length());
        }
        //Log.i(TAG,"REDIRECT- getCGFResultFromCloud / url: " + url);
        if(didRedirect) {
            url = HTTPS_ + url;
        } else {
            url = HTTPS_ + url;
        }

        mLogger.info("getCGFResultFromCloud version  " + version + " " + didRedirect);

        String result;

        try {
            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            int timeoutConnection = 6000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            HttpClient client = getNewHttpClient();
            HttpGet cfgResult = new HttpGet(url);
            HttpResponse response = client.execute(cfgResult);
            result = EntityUtils.toString(response.getEntity());
            if (result.equals("")) {
                //Log.w(TAG, "CFG result returned empty!");
                mLogger.info("CFG result returned empty!");
                result = "Timeout";
            } else {
                //Log.i(TAG, "CFG result returned: " + result);
                mLogger.info("CFG result returned: " + result);
            }

        } catch (Exception e) {
            e.printStackTrace();
            //Log.e(TAG, "Failed to get CFG result");
            mLogger.info("Failed to get CFG result");
            result = "Timeout";
        }

        return result;
    }

    public static Boolean loginToCloud(String baseUrl, String email, String pswd) {
        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- loginToCloud / url: " + url);
        mLogger.info("REDIRECT- loginToCloud / didRedirect: " + didRedirect);
        if(didRedirect) {
            url = HTTPS_ + url;
        } else {
            url = HTTPS_ + url;
        }
        url += "/api/login";
        //Log.i(TAG,"REDIRECT- loginToCloud / url: " + url);
        mLogger.info("*AP* loginToCloud: " + url + " " + email + pswd);
        if (email.equals("") || pswd.equals(""))
            return false;
        Boolean flag = false;
        HttpParams httpParameters = new BasicHttpParams();
        // Set the timeout in milliseconds until a connection is established.
        // The default value is zero, that means the timeout is not used.
        int timeoutConnection = 2000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        int timeoutSocket = 2000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        HttpClient client = getNewHttpClient();
        try {
            HttpPost loginPost = new HttpPost(url);
            List<NameValuePair> loginParam = new ArrayList<>(2);
            email = new String(email.getBytes("UTF-8"), "ISO-8859-1");
            pswd = new String(pswd.getBytes("UTF-8"), "ISO-8859-1");
            loginParam.add(new BasicNameValuePair("email", email));
            loginParam.add(new BasicNameValuePair("pswd", pswd));
            loginPost.setEntity(new UrlEncodedFormEntity(loginParam));
            HttpResponse response = client.execute(loginPost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                flag = true;
            }
            mLogger.info("*AP* loginToCloud: response " + response.getStatusLine().getStatusCode());
            client.getConnectionManager().shutdown();
        } catch (Exception e) {
            mLogger.info("*AP* loginToCloud exception: " + e.toString());
            e.printStackTrace();
            client.getConnectionManager().shutdown();
            flag = false;
        }
        return flag;
    }

    public static Boolean registerWithCloud(String baseUrl, String fullname, String phone, String email, String pswd, String confirm) {
        String url = baseUrl;
        //Log.i(TAG,"REDIRECT- RegisterWithCloud / url: " + url);
        mLogger.info("REDIRECT- registerWithCloud / didRedirect: " + didRedirect);
        if(didRedirect) {
            url = HTTPS_ + url;
        } else {
            url = HTTPS_ + url;
        }
        url += "/api/register";
        //Log.i(TAG,"REDIRECT- registerWithCloud / url: " + url);
        mLogger.info("*AP* registerWithCloud: " + url + " " + email + pswd);
        if (email.equals("") || pswd.equals(""))
            return false;
        Boolean flag = false;
        HttpParams httpParameters = new BasicHttpParams();
        // Set the timeout in milliseconds until a connection is established.
        // The default value is zero, that means the timeout is not used.
        int timeoutConnection = 2000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        int timeoutSocket = 2000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        HttpClient client = getNewHttpClient();
        try {
            HttpPost regPost = new HttpPost(url);
            List<NameValuePair> regParam = new ArrayList<>(2);
            fullname = new String(fullname.getBytes("UTF-8"), "ISO-8859-1");
            phone = new String(phone.getBytes("UTF-8"), "ISO-8859-1");
            email = new String(email.getBytes("UTF-8"), "ISO-8859-1");
            pswd = new String(pswd.getBytes("UTF-8"), "ISO-8859-1");
            confirm = new String(confirm.getBytes("UTF-8"), "ISO-8859-1");
            regParam.add(new BasicNameValuePair("fullname", fullname));
            regParam.add(new BasicNameValuePair("phone", phone));
            regParam.add(new BasicNameValuePair("email", email));
            regParam.add(new BasicNameValuePair("pswd", pswd));
            regParam.add(new BasicNameValuePair("confirm", confirm));
            regPost.setEntity(new UrlEncodedFormEntity(regParam));
            HttpResponse response = client.execute(regPost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                flag = true;
            }
            mLogger.info("*AP* registerWithCloud: response " + response.getStatusLine().getStatusCode());
            client.getConnectionManager().shutdown();
        } catch (Exception e) {
            mLogger.info("*AP* loginToCloud exception: " + e.toString());
            e.printStackTrace();
            client.getConnectionManager().shutdown();
            flag = false;
        }
        return flag;
    }

    public static String getErrorMsgForCFGResult(CFG_Result_Enum result) {
        String resultString = null;

        switch (result) {
            case Failure:
                resultString = CFG_Result_Enum.FAILURE_STRING;
                break;
            case Success:
                resultString = CFG_Result_Enum.SUCCESS_STRING;
                break;
            case Time_Out:
                System.out.println("CFG_Result_Enum: Time_Out");
                break;
            case Unknown_Token:
                resultString = CFG_Result_Enum.UNKNOWN_STRING;
                break;
            case Not_Started:
                resultString = CFG_Result_Enum.NOT_STARTED_STRING;
                break;
            case Ap_Not_Found:
                resultString = CFG_Result_Enum.AP_NOT_FOUND_STRING;
                break;
            case Ip_Add_Fail:
                resultString = CFG_Result_Enum.IP_ADD_FAIL_STRING;
                break;
            case Wrong_Password:
                resultString = CFG_Result_Enum.WRONG_PASSWORD_STRING;
                break;
        }
        return resultString;
    }

    public static CFG_Result_Enum getResultTypeCFGString(String resultString) {

        CFG_Result_Enum result = CFG_Result_Enum.Unknown_Token;

        if (resultString.equals(CFG_Result_Enum.WRONG_PASSWORD_STRING)) {
            result = CFG_Result_Enum.Wrong_Password;
        } else if (resultString.equals(CFG_Result_Enum.IP_ADD_FAIL_STRING)) {
            result = CFG_Result_Enum.Ip_Add_Fail;
        } else if (resultString.equals(CFG_Result_Enum.AP_NOT_FOUND_STRING)) {
            result = CFG_Result_Enum.Ap_Not_Found;
        } else if (resultString.equals(CFG_Result_Enum.NOT_STARTED_STRING)) {
            result = CFG_Result_Enum.Not_Started;
        } else if (resultString.equals(CFG_Result_Enum.SUCCESS_STRING)) {
            result = CFG_Result_Enum.Success;
        } else if (resultString.equals(CFG_Result_Enum.FAILURE_STRING)) {
            result = CFG_Result_Enum.Failure;
        }

        return result;
    }

    public static Device_Type_Enum slDeviceOTAAndType(String baseUrl) {
        //Log.i(TAG, "slDeviceOTAAndType started");
        //String url = baseUrl + "/device?appid";
        String url = baseUrl + "/device?appname";
        //Log.i(TAG, "slDeviceOTAAndType url: " + url);
        //Log.i(TAG, "REDIRECT- slDeviceOTAAndType / url: " + url);

        //Log.i(TAG, "REDIRECT- slDeviceOTAAndType / didRedirect: " + didRedirect);
        if (didRedirect) {
            url = HTTPS_ + url;
        } else {
            url = HTTP_ + url;
        }
        //Log.i(TAG, "REDIRECT- slDeviceOTAAndType / url: " + url);

        Device_Type_Enum deviceTypeEnum = null;

        try {
            HttpParams httpParameters = new BasicHttpParams();
            int connectionTimeout = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeout);

            int socketTimeout = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, socketTimeout);
            HttpClient httpClient = getNewHttpClient();
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            String stringResponse = EntityUtils.toString(httpResponse.getEntity());
            //Log.i(TAG, "slDeviceOTAAndType response: " + stringResponse);

            if (stringResponse.equals("appname=out_of_box_fs") ) {
                deviceTypeEnum = Device_Type_Enum.F_Device;

            } else if (stringResponse.contains("appname=out_of_box_rs")||(stringResponse.equals("appname=out_of_box"))) {
                deviceTypeEnum = Device_Type_Enum.S_Device;

            } else if (stringResponse.equals("appname=out_of_box_r")) {
                deviceTypeEnum = Device_Type_Enum.R_Device;

            } else if (stringResponse.equals("appname=out_of_box_3235_fs")) {
                deviceTypeEnum = Device_Type_Enum.F5_Device;

            }else if (stringResponse.equals("appname=out_of_box_3235_rs")) {
                deviceTypeEnum = Device_Type_Enum.S5_Device;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            //Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            //Log.e(TAG, e.getMessage());
        }
        //Log.i(TAG, "slDeviceOTAAndType result: " + deviceTypeEnum);
        return deviceTypeEnum;
    }

    public static String getDeviceIp(String baseUrl) {
        String url = baseUrl;
        url += "/__SL_G_PIP";
        //Log.i(TAG, "REDIRECT- getDeviceIp / url: " + url);
        //Log.i(TAG, "REDIRECT- getDeviceIp / didRedirect: " + didRedirect);
        if (didRedirect) {
            url = HTTPS_ + url;
        } else {
            url = HTTP_ + url;
        }

        //Log.i(TAG, "REDIRECT- getDeviceIp / url: " + url);
        String stringResponse = "";
        try {
            HttpParams httpParams = new BasicHttpParams();
            int connectionTimeout = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout);
            int socketTimeout = 5000;
            HttpConnectionParams.setSoTimeout(httpParams, socketTimeout);
            HttpClient httpClient = getNewHttpClient();
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            stringResponse = EntityUtils.toString(httpResponse.getEntity());
            //Log.i(TAG, "getDeviceIp result: " + stringResponse);

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringResponse;
    }

    public static HttpClient getNewHttpClient()  {
        InputStream caInput = null;
        try {
            // Create a KeyStore containing our Certificate
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            caInput = context.getResources().openRawResource(R.raw.b00m_trusted_ca_cert);
            X509Certificate ca = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(caInput);
            String alias = ca.getIssuerX500Principal().getName();
            //Log.i(TAG, "principal: " + alias);
            //trustStore.setCertificateEntry("DSTRootCAX3"/*"dummyrootcacert"*/, ca);
            trustStore.setCertificateEntry(alias/*"dummyrootcacert"*/, ca);

            // Create a TrustManager that trusts the Certificate in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(trustStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            // Tell the URLConnection to use a SocketFactory from our SSLContext

            SSLSocketFactory sf =new SSLSocketFactory(trustStore);//context.getSocketFactory();
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 5000);
            HttpConnectionParams.setSoTimeout(params, 25000);
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));
            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
            HttpConnectionParams.setConnectionTimeout(params, 5000);
            HttpConnectionParams.setSoTimeout(params, 25000);
            HttpClient client = new DefaultHttpClient(ccm, params);
            return client;
        } catch (Exception e) {
            e.printStackTrace();
            return new DefaultHttpClient();
        }
    }
}
