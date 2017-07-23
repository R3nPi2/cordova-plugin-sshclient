/*
 * Cordova SSH Client Plugin for Android.
 * Author: R3n Pi2 <r3npi2@gmail.com> (https://github.com/R3nPi2)
 * Date: Sun, 24 Jul 2016 12:28:46 +0200
 */

var exec = require('cordova/exec');

function sshClient() { 
}

sshClient.prototype.sshOpenSession = function(success,error,hostname,port,username,password,cols,rows,width,height){
  var width = width || 0;
  var height = height || 0;
  exec(success,error,"sshClient",'sshOpenSession',[hostname,port,username,password,cols,rows,width,height]);
}
sshClient.prototype.sshCloseSession = function(success,error,sessionID){
  exec(success,error,"sshClient",'sshCloseSession',[sessionID]);
}
sshClient.prototype.sshVerifyHost = function(success,error,hostname,port,addhost){
  exec(success,error,"sshClient",'sshVerifyHost',[hostname,port,addhost]);
}
sshClient.prototype.sshRead = function(success,error,sessionID){
  exec(success,error,"sshClient",'sshRead',[sessionID]);
}
sshClient.prototype.sshWrite = function(success,error,sessionID,line){
  exec(success,error,"sshClient",'sshWrite',[sessionID,line]);
}
sshClient.prototype.sshResizeWindow = function(success,error,sessionID,cols,rows,width,height){
  var width = width || 0;
  var height = height || 0;
  exec(success,error,"sshClient",'sshResizeWindow',[sessionID,cols,rows,width,height]);
}
sshClient.prototype.sshGetKnownHosts = function(success,error){
  exec(success,error,"sshClient",'sshGetKnownHosts',[]);
}
sshClient.prototype.sshSetKnownHosts = function(success,error,known_hosts){
  exec(success,error,"sshClient",'sshSetKnownHosts',[known_hosts]);
}

var sshClient = new sshClient();
module.exports = sshClient;
