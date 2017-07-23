/*
 * Cordova SSH Client Plugin for Android.
 * Author: R3n Pi2 <r3npi2@gmail.com> (https://github.com/R3nPi2)
 * Date: Sun, 24 Jul 2016 12:28:46 +0200
 */

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import android.util.Log;
import android.app.Activity;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.BufferedReader;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.ConnectionMonitor;

public class sshClient extends CordovaPlugin {
 
  public static final String TAG = "cordovaPluginSshClient";
  public String knownHostPath = "known_hosts";
  public Context context;
  public Activity activity;
  public String filesDir;
  public String verifyMsg;
  public boolean addHost;
  KnownHosts database = new KnownHosts();

  private Connection[] connections = new Connection[100];
  private ConnectionMonitor[] connectionMonitors = new ConnectionMonitor[100];
  private boolean[] emptyConnections = new boolean[100];
  private Session[] sessions = new Session[100];
  private InputStream[] inputs = new InputStream[100];
  private OutputStream[] outputs = new OutputStream[100];
  private InputStream[] errors = new InputStream[100];
 
  /**
  * Constructor.
  */
  public sshClient() {
    //Log.v(TAG,"Constructor");
  }
  
  /**
  * Sets the context of the Command. This can then be used to do things like
  * get file paths associated with the Activity.
  *
  * @param cordova The context of the main Activity.
  * @param webView The CordovaWebView Cordova is running in.
  */
 
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    activity = cordova.getActivity();
    context = activity.getApplicationContext();
    filesDir = context.getFilesDir().getAbsolutePath();
    for (int i = 0; i < emptyConnections.length; i++) {
      emptyConnections[i] = true;
    }
  }

  private int newConnectionID () {
    boolean found = false;
    for (int i = 0; i < emptyConnections.length; i++) {
      if (emptyConnections[i] == true) {
        return i;
      }
    }
    // Not available connections
    return -1;
  }

  private String readOutput (int sessionID) {

    byte[] buff = new byte[8192];

    try
    {
      while (inputs[sessionID].available() > 0) {
        int len = inputs[sessionID].read(buff);
        if (len == -1) {
          return "";
        } else {
          //@debug:
          //for (int i = 0; i < len; i++) {
            //char c = (char) (buff[i] & 0xff);
            //Log.v(TAG,"ssh readOutput: read buff[i]: "+String.valueOf(buff[i]));
          //}
          String b = new String(buff, 0, len, "UTF-8");
          return b;
        }
      }
    }
    catch (Exception e)
    {
      Log.v(TAG,"sshReadOutput error: "+e.toString());
    }
    return "";
  }

  class AdvancedVerifier implements ServerHostKeyVerifier
  {

    public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception {

      final String host = hostname;
      final String algo = serverHostKeyAlgorithm;

      verifyMsg = "";

      /* Check database */

      int result = database.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);

      switch (result)
      {
      case KnownHosts.HOSTKEY_IS_OK:
        verifyMsg = "OK";
        return true;

      case KnownHosts.HOSTKEY_IS_NEW:
        verifyMsg = "Do you want to accept the hostkey (type " + algo + ") from " + host + " ?\n";
        break;

      case KnownHosts.HOSTKEY_HAS_CHANGED:
        verifyMsg = "WARNING! Hostkey for " + host + " has changed!\nAccept anyway?\n";
        break;

      default:
        verifyMsg = "Unknown response: "+Integer.toString(result);
        return false;
      }

      /* Include the fingerprints in the message */

      String hexFingerprint = KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey);
      String bubblebabbleFingerprint = KnownHosts.createBubblebabbleFingerprint(serverHostKeyAlgorithm, serverHostKey);

      verifyMsg += "Hex Fingerprint: " + hexFingerprint + "\nBubblebabble Fingerprint: " + bubblebabbleFingerprint;

      if (!addHost) {

        return true;
      
      } else {

        //We don't want it hashed to look for it later.
        //String hashedHostname = KnownHosts.createHashedHostname(hostname);
        String hashedHostname = hostname;

        database.addHostkey(new String[] { hashedHostname }, serverHostKeyAlgorithm, serverHostKey);

        try
        {
          KnownHosts.addHostkeyToFile(new File(knownHostPath), new String[] { hashedHostname }, serverHostKeyAlgorithm, serverHostKey);
          verifyMsg = "ADD_OK";
        }
        catch (IOException e)
        {
          Log.v(TAG,"verifyServerHostKey error: "+e.toString());
          verifyMsg = "ADD_KO";
        }

        return true;
      }

    }      
  }

  public boolean execute(final String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
         
    if ("sshRead".equals(action)) {
      final int sessionID = Integer.parseInt(args.getString(0));
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
            try {
              String resp = readOutput(sessionID);
              int conditions = sessions[sessionID].waitForCondition(ChannelCondition.STDOUT_DATA, 100);
              PluginResult result = new PluginResult(PluginResult.Status.OK, resp);
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);
            } catch (IOException e) {
              PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);
              Log.v(TAG,"sshRead error: "+e.toString());
            }
        }
      });
      return true;
    }

    if ("sshWrite".equals(action)) {
      final int sessionID = Integer.parseInt(args.getString(0));
      final String command = args.getString(1);
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
            try {
              byte[] cmd = command.getBytes("UTF-8");
              outputs[sessionID].write(cmd);
              int conditions = sessions[sessionID].waitForCondition(ChannelCondition.STDOUT_DATA,100);
              PluginResult result = new PluginResult(PluginResult.Status.OK, command);
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);
            } catch (IOException e) {
              PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);
              Log.v(TAG,"sshWrite error: "+e.toString());
            }
            callbackContext.success();
        }
      });
      return true;
    }

    if ("sshResizeWindow".equals(action)) {
      final int sessionID = Integer.parseInt(args.getString(0));
      final int cols = Integer.parseInt(args.getString(1));
      final int rows = Integer.parseInt(args.getString(2));
      final int width = Integer.parseInt(args.getString(3));
      final int height = Integer.parseInt(args.getString(4));
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          String resp = Integer.toString(cols)+"x"+Integer.toString(rows)+" "+Integer.toString(width)+"x"+Integer.toString(height);
          try {
            sessions[sessionID].requestWindowChange(cols,rows,width,height);
          } catch (IOException e) {
            Log.v(TAG,"sshResizeWindow error: "+e.toString());
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
          }
          PluginResult result = new PluginResult(PluginResult.Status.OK, resp);
          result.setKeepCallback(true);
          callbackContext.sendPluginResult(result);
        }
      });
      return true;
    }

    if ("sshCloseSession".equals(action)) {
      final int sessionID = Integer.parseInt(args.getString(0));
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          try  {
            inputs[sessionID].close();
          } catch (IOException e) {
            Log.v(TAG,"sshCloseSession error: "+e.toString());
          }
          try  {
            outputs[sessionID].flush();
          } catch (IOException e) {
            Log.v(TAG,"sshCloseSession error: "+e.toString());
          }
          try  {
            outputs[sessionID].close();
          } catch (IOException e) {
            Log.v(TAG,"sshCloseSession error: "+e.toString());
          }
          try  {
            errors[sessionID].close();
          } catch (IOException e) {
            Log.v(TAG,"sshCloseSession error: "+e.toString());
          }

          sessions[sessionID].close();
          connections[sessionID].close();

          emptyConnections[sessionID] = true;

          PluginResult result = new PluginResult(PluginResult.Status.OK, "0");
          result.setKeepCallback(true);
          callbackContext.sendPluginResult(result);
        }
      });
      return true;
    }

    if ("sshVerifyHost".equals(action)) {

      final String hostname = args.getString(0);
      final int port = Integer.parseInt(args.getString(1));
      final String strAddHost = args.getString(2);

      cordova.getThreadPool().execute(new Runnable() {
        public void run() {

          if ("true".equals(strAddHost)) {
            addHost = true;
          } else {
            addHost = false;
          }

          knownHostPath = filesDir + "/known_hosts";

          File knownHostFile = new File(knownHostPath);

          if (knownHostFile.exists()) {
            try {
              database.addHostkeys(knownHostFile);
            } catch (IOException e) {
              Log.v(TAG,"sshVerifyHost error1: "+e.toString());
            }
          } 

          Connection dummyConnection = new Connection(hostname,port);

          String[] hostkeyAlgos = database.getPreferredServerHostkeyAlgorithmOrder(hostname);

          if (hostkeyAlgos != null) {
            dummyConnection.setServerHostKeyAlgorithms(hostkeyAlgos);
          }

          try {   

            dummyConnection.connect(new AdvancedVerifier());

            dummyConnection.close();

            PluginResult result = new PluginResult(PluginResult.Status.OK, verifyMsg);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

          } catch (IOException e2) {

            //We are going to try verify 3 times becuse I get error: Illegal packet size (1869636974) so often.

            Log.v(TAG,"sshVerifyHost error2: "+e2.getMessage());

            try {   

              dummyConnection.connect(new AdvancedVerifier());

              dummyConnection.close();
  
              PluginResult result = new PluginResult(PluginResult.Status.OK, verifyMsg);
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);

            } catch (IOException e3) {

              Log.v(TAG,"sshVerifyHost error3: "+e3.getMessage());
              try {   

                dummyConnection.connect(new AdvancedVerifier());

                dummyConnection.close();

                PluginResult result = new PluginResult(PluginResult.Status.OK, verifyMsg);
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);

              } catch (IOException e4) {

                Log.v(TAG,"sshVerifyHost error4: "+e4.getMessage());
                PluginResult result = new PluginResult(PluginResult.Status.ERROR, e4.getMessage());
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);

              }

            }

          }

          PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Unknown error");
          result.setKeepCallback(true);
          callbackContext.sendPluginResult(result);
        }

      });
      return true;
    }

    if ("sshOpenSession".equals(action)) {

      File knownHostFile = new File(knownHostPath);
      if (knownHostFile.exists())
      {
        try
        {
          database.addHostkeys(knownHostFile);
        }
        catch (IOException e)
        {
          Log.v(TAG,"sshOpenSession error1: "+e.toString());
        }
      }

      final String hostname = args.getString(0);
      final int port = Integer.parseInt(args.getString(1));
      final String username = args.getString(2);
      final String password = args.getString(3);
      final int cols = Integer.parseInt(args.getString(4));
      final int rows = Integer.parseInt(args.getString(5));
      final int width = Integer.parseInt(args.getString(6));
      final int height = Integer.parseInt(args.getString(7));

      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          try {
            int connectionID = newConnectionID();
            if (connectionID < 0) {
              PluginResult result = new PluginResult(PluginResult.Status.ERROR, "No more connections available");
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);
            }
            emptyConnections[connectionID] = false;
            connections[connectionID] = new Connection(hostname,port);
            /*
            connections[connectionID].addConnectionMonitor(new ConnectionMonitor()
            {            
              @Override
              public void connectionLost(Throwable reason)
              {
                Log.v(TAG,"sshConnectionLost: "+reason.getMessage());
              }
            });
            */
            connections[connectionID].connect();
            connections[connectionID].setTCPNoDelay(true);
            boolean isAuthenticated = connections[connectionID].authenticateWithPassword(username, password);
            if (isAuthenticated == false) {
              emptyConnections[connectionID] = true;
              PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Authentication failed");
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);
            } else {
              sessions[connectionID] = connections[connectionID].openSession();
              inputs[connectionID] = sessions[connectionID].getStdout();
              outputs[connectionID] = sessions[connectionID].getStdin();
              errors[connectionID] = sessions[connectionID].getStderr();
              try {
                sessions[connectionID].requestPTY("vt100", cols, rows, width, height, null);
                sessions[connectionID].startShell();
                int conditions = sessions[connectionID].waitForCondition(ChannelCondition.STDOUT_DATA, 1000);

                PluginResult result = new PluginResult(PluginResult.Status.OK, Integer.toString(connectionID));
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
              } catch (Exception e) {
                emptyConnections[connectionID] = true;
                Log.v(TAG,"sshOpenSession error2: \n"+e.toString());
                PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
              }
            }
          } catch (IOException e) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            Log.v(TAG,"sshOpenSession error3: "+e.toString());
          }
          PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Unknown error");
          result.setKeepCallback(true);
          callbackContext.sendPluginResult(result);
        }
      });
      return true;
    }

    if ("sshGetKnownHosts".equals(action)) {

      cordova.getThreadPool().execute(new Runnable() {
        public void run() {

          knownHostPath = filesDir + "/known_hosts";

          String known_hosts = "";

          try {

            BufferedReader br = new BufferedReader(new FileReader(knownHostPath));
            String line = null;
            try {
              while ((line = br.readLine()) != null) {
                known_hosts += line+"\n";
              }

              PluginResult result = new PluginResult(PluginResult.Status.OK, known_hosts);
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);

            } catch (IOException e) {
              PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Cannot read known_hosts file.");
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);           
            }

          } catch (IOException e) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Cannot open known_hosts file.");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
          }

        }

      });
      return true;
    }

    if ("sshSetKnownHosts".equals(action)) {

      final String keys = args.getString(0);

      cordova.getThreadPool().execute(new Runnable() {
        public void run() {

          knownHostPath = filesDir + "/known_hosts";

          try {
            PrintWriter known_hosts_writer = new PrintWriter(knownHostPath);
            known_hosts_writer.print("");
            known_hosts_writer.close();
          } catch (IOException e) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Cannot open known_hosts for clearing.");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);           
          }

          try {
            PrintWriter known_hosts_writer = new PrintWriter(knownHostPath);
            known_hosts_writer.print(keys);
            known_hosts_writer.close();
            database = new KnownHosts();
          } catch (IOException e) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Cannot open known_hosts for writing.");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);           
          }

          String known_hosts = "";

          try {

            BufferedReader br = new BufferedReader(new FileReader(knownHostPath));
            String line = null;
            try {
              while ((line = br.readLine()) != null) {
                known_hosts += line+"\n";
              }

              PluginResult result = new PluginResult(PluginResult.Status.OK, known_hosts);
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);

            } catch (IOException e) {
              PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Cannot read known_hosts file.");
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);           
            }

          } catch (IOException e) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Cannot open known_hosts file.");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
          }

        }

      });
      return true;
    }
    return false;
  }
}
