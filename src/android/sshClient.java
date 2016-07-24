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

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.ChannelCondition;

public class sshClient extends CordovaPlugin {
 
  public static final String TAG = "cordovaPluginSshClient";
  public String knownHostPath = "known_hosts";
  public Context context;
  public Activity activity;
  public String filesDir;
  public String verifyMsg;
  public boolean addHost;
  public String hostname;
  public String username;
  private Connection conn;
  private Session sess;
  KnownHosts database = new KnownHosts();

  private int x;
  private int y;
  private InputStream in;
  private OutputStream out;
  private InputStream err;
 
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
  }

  private String readOutput () {

    byte[] buff = new byte[8192];

    try
    {
      while (in.available() > 0) {
        int len = in.read(buff);
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
      Log.v(TAG,"readOutput error: "+e.toString());
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

        String hashedHostname = KnownHosts.createHashedHostname(hostname);

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
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
            try {
              String resp = readOutput();
              int conditions = sess.waitForCondition(ChannelCondition.STDOUT_DATA, 100);
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
      final String command = args.getString(0);
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
            try {
              byte[] cmd = command.getBytes("UTF-8");
              out.write(cmd);
              int conditions = sess.waitForCondition(ChannelCondition.STDOUT_DATA,100);
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
      x = Integer.parseInt(args.getString(0));
      y = Integer.parseInt(args.getString(1));
      final Integer pixels_x = Integer.parseInt(args.getString(2));
      final Integer pixels_y = Integer.parseInt(args.getString(3));
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          String resp = Integer.toString(x)+"x"+Integer.toString(y)+" "+Integer.toString(pixels_x)+"x"+Integer.toString(pixels_y);
          try {
            sess.requestWindowChange(x, y, pixels_x, pixels_y);
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
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          try  {
            in.close();
          } catch (IOException e) {
            Log.v(TAG,"sshCloseSession error: "+e.toString());
          }
          try  {
            out.flush();
          } catch (IOException e) {
            Log.v(TAG,"sshCloseSession error: "+e.toString());
          }
          try  {
            out.close();
          } catch (IOException e) {
            Log.v(TAG,"sshCloseSession error: "+e.toString());
          }
          try  {
            err.close();
          } catch (IOException e) {
            Log.v(TAG,"sshCloseSession error: "+e.toString());
          }

          sess.close();
          conn.close();

          PluginResult result = new PluginResult(PluginResult.Status.OK, "0");
          result.setKeepCallback(true);
          callbackContext.sendPluginResult(result);
        }
      });
      return true;
    }

    if ("sshVerifyHost".equals(action)) {

      hostname = args.getString(0);
      final String strAddHost = args.getString(1);

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

          conn = new Connection(hostname);

          String[] hostkeyAlgos = database.getPreferredServerHostkeyAlgorithmOrder(hostname);

          if (hostkeyAlgos != null) {
            conn.setServerHostKeyAlgorithms(hostkeyAlgos);
          }

          try {   

            conn.connect(new AdvancedVerifier());

            PluginResult result = new PluginResult(PluginResult.Status.OK, verifyMsg);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

            conn.close();

          } catch (IOException e2) {

            //We are going to try verify 3 times becuse I get error: Illegal packet size (1869636974) so often.

            Log.v(TAG,"sshVerifyHost error2: "+e2.getMessage());
            //PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
            //result.setKeepCallback(true);
            //callbackContext.sendPluginResult(result);

            try {   

              conn.connect(new AdvancedVerifier());
  
              PluginResult result = new PluginResult(PluginResult.Status.OK, verifyMsg);
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);

              conn.close();

            } catch (IOException e3) {

              Log.v(TAG,"sshVerifyHost error3: "+e3.getMessage());
              //PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
              //result.setKeepCallback(true);
              //callbackContext.sendPluginResult(result);
              try {   

                conn.connect(new AdvancedVerifier());

                PluginResult result = new PluginResult(PluginResult.Status.OK, verifyMsg);
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);

                conn.close();

              } catch (IOException e4) {

                Log.v(TAG,"sshVerifyHost error4: "+e4.getMessage());
                PluginResult result = new PluginResult(PluginResult.Status.ERROR, e4.getMessage());
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);

              }

            }

          }

          PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Unknown error");
          //PluginResult result = new PluginResult(PluginResult.Status.OK, "-1");
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

      hostname = args.getString(0);
      username = args.getString(1);
      final String password = args.getString(2);
      x = Integer.parseInt(args.getString(3));
      y = Integer.parseInt(args.getString(4));
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          try {
            conn = new Connection(hostname);
            conn.connect();
            conn.setTCPNoDelay(true);
            boolean isAuthenticated = conn.authenticateWithPassword(username, password);
            if (isAuthenticated == false) {
              PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Authentication failed");
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);
            } else {
              sess = conn.openSession();
              in = sess.getStdout();
              out = sess.getStdin();
              err = sess.getStderr();
              try {
                sess.requestPTY("vt100", x, y, 0, 0, null);
                sess.startShell();
                int conditions = sess.waitForCondition(ChannelCondition.STDOUT_DATA, 1000);
                PluginResult result = new PluginResult(PluginResult.Status.OK, "0");
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
              } catch (Exception e) {
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
    return false;
  }
}
