# Cordova SSH Client Plugin

A Cordova SSH Client Plugin based on Ganymed SSH2.

## Supported Platforms

  - Android

## Recomended

If you're going to use this plugin inside an AngularJS project, we recomend you to install **Angular SSH Client** from [here](https://github.com/R3nPi2/angular-ssh).

## Example

Take a look at the example on [Angular SSH Client project](https://github.com/R3nPi2/angular-ssh).

## Installation

  - `cordova plugin add https://github.com/R3nPi2/cordova-plugin-sshclient.git`

## Methods

### `window.sshClient.sshOpenSession(function(success){...},function(error){...},hostname,username,password,cols,rows,width,height)`

Connects to host, request a new PTY and starts a Shell.

**Arguments**

  - `hostname` – Hostname or IP.
  - `user` – Username.
  - `password` – Password.
  - `cols` – PTY columns.
  - `rows` – PTY rows.
  - `width` – (optional: if empty, set to 0) PTY pixels width.
  - `height` – (optional: if empty, set to 0) PTY pixels height.

**Success response**

  - Returns "0". It means everithing was ok.

**Error response**

  - Returns a string describing the error.

### `window.sshClient.sshVerifyHost(function(success){...},function(error){...},hostname,saveHostKey)`

We should use this method to verify hostkeys.

**Arguments**

  - `hostname` – Hostname or IP.
  - `saveHostKey` – This argument should be a string matching "true" or "false". If "false", the verification should be done but hostkey will not be saved into known\_hosts database. If "true", hostkey should be saved into known\_hosts.

**Success response**

  - If `saveHostKey` was set to "true": returns "ADD\_OK" string if everithing goes fine and hostkey is saved into known\_hosts file.
  - If `saveHostKey` was set to "false", and hostkey allready exists into known\_hosts, and hostkey is valid: returns "OK" string.
  - If `saveHostKey` was set to "false", and hostkey allready exists into known\_hosts, but hostkey has changed: returns describing the situation.
  - If `saveHostKey` was set to "false", and hostkey does not exists into known\_hosts, returns a string with hostkey.

**Error response**

  - Returns a string describing the error.

### `window.sshClient.sshResizeWindow(function(success){...},function(error){...},cols,rows,width,height)`

We can use this method to resize PTY created on `window.sshClient.sshOpenSession`.

**Arguments**

  - `cols` – PTY columns.
  - `rows` – PTY rows.
  - `width` – (optional: if empty, set to 0) PTY pixels width.
  - `height` – (optional: if empty, set to 0) PTY pixels height.

**Success response**

  - A string with PTY dimensions.

**Error response**

  - Returns a string describing the error.

### `window.sshClient.sshRead(function(success){...},function(error){...})`

Read stdout and stderr buffers output.

**Success response**

  - Returns characters read from stdout and stderr buffers.

**Error response**

  - Returns a string describing the error.

### `window.sshClient.sshWrite(function(success){...},function(error){...},string)`

Write a string to stdin buffer.

**Arguments**

  - `string` – String that will be written to stdin buffer. If you want to send a `ls` command, you should write "ls\n".

**Success response**

  - Returns written command.

**Error response**

  - Returns a string describing the error.

### `window.sshClient.sshCloseSession(function(success){...},function(error){...})`

Close ssh session.

**Success response**

  - Returns "0". It means everithing was ok.

**Error response**

  - Returns a string describing the error.

## Author

  - R3n Pi2 <r3npi2@gmail.com> (https://github.com/R3nPi2)

## Ganymed SSH-2 for Java

This plugin includes `src/android/libs/ssh2.jar`, which is a compressed package of **Ganymed SSH-2 for Java** libraries. Take a look at [Ganymed SSH-2 for Java](http://www.ganymed.ethz.ch/ssh2/) for more information.

**Ganymed SSH-2 for Java** package was slightly modified to fit the needs of this plugin.

Take a look at [GANYMED-SSH-2-LICENSE](https://github.com/R3nPi2/cordova-plugin-sshclient/blob/master/GANYMED-SSH-2-LICENSE) license file for licensing information.

You can find libraries documentation [here](http://www.ganymed.ethz.ch/ssh2/javadoc/) or in [Ganymed SSH-2 for Java home page](http://www.ganymed.ethz.ch/ssh2/).

Take a look to [NOTES.md](https://github.com/R3nPi2/cordova-plugin-sshclient/blob/master/NOTES.md) file if you want to know how I packaged this libraries.

## License

  - Cordova SSH Client Plugin is released under the GNU Affero General Public License version 3. Read LICENSE file.
  - The Ganymed SSH-2 for Java library is released under a BSD style license. The Java implementations of the AES, Blowfish and 3DES ciphers have been taken (and slightly modified) from the cryptography package released by The [Legion Of The Bouncy Castle](http://www.bouncycastle.org/). Please read GANYMED-SSH-2-LICENSE file.
  - SSH is a registered trademark of SSH Communications Security Corp in the United States and in certain other jurisdictions. Java and J2ME are trademarks or registered trademarks of Sun Microsystems, Inc. in the United States and other countries. All other names and marks are property of their respective owners.

## Issues

Report at the github [issue tracker](https://github.com/R3nPi2/cordova-plugin-sshclient/issues)

