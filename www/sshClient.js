/*
 * Cordova SSH Client Plugin for Android.
 * Author: R3n Pi2 <r3npi2@gmail.com> (https://github.com/R3nPi2)
 * Date: Sun, 24 Jul 2016 12:28:46 +0200
 */

var exec = require('cordova/exec');

function sshClient() { 
}

sshClient.prototype.sshOpenSession = function(success,error,hostname,username,password,cols,rows,width,height){
  var width = width || 0;
  var height = height || 0;
  exec(success,error,"sshClient",'sshOpenSession',[hostname,username,password,cols,rows,width,height]);
}
sshClient.prototype.sshCloseSession = function(success,error){
  exec(success,error,"sshClient",'sshCloseSession',[]);
}
sshClient.prototype.sshVerifyHost = function(success,error,hostname,addhost){
  exec(success,error,"sshClient",'sshVerifyHost',[hostname,addhost]);
}
sshClient.prototype.sshRead = function(success,error){
  exec(success,error,"sshClient",'sshRead',[]);
}
sshClient.prototype.sshWrite = function(success,error,line){
  exec(success,error,"sshClient",'sshWrite',[line]);
}
sshClient.prototype.sshResizeWindow = function(success,error,cols,rows,width,height){
  var width = width || 0;
  var height = height || 0;
  exec(success,error,"sshClient",'sshResizeWindow',[cols,rows,width,height]);
}

var sshClient = new sshClient();
module.exports = sshClient;
