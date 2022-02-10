 /*

    Basic Power On/Off for Optoma UHD65

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    Version History:
    ================

    Date            Version             By                  Changes
    --------------------------------------------------------------------------------
    2021-02-08      0.5.0               Derek Gilbert       Initial Version
  
*/

import groovy.transform.Field

metadata 
{
	definition (name: "Optoma Projector", namespace: "OptomaDG", author: "Derek Gilbert")
	{
		capability "Initialize"
		capability "Telnet"
        capability "Switch"

        command "refresh"
	}

    preferences 
	{   
		input name: "OptomaIP", type: "text", title: "Optoma IP", required: true, displayDuringSetup: true
		input name: "OptPort", type: "number", title: "Port", defaultValue: 23, required: true, displayDuringSetup: true
        input name: "ProjectorID", type: "text", title: "ProjectorID", defaultValue: "00", required: true, displayDuringSetup: true
 		input name: "textLogging",  type: "bool", title: "Enable description text logging ", required: true, defaultValue: true
        input name: "debugOutput", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def getVersion()
{
    return "0.5.0"
}

def parse(String resp) 
{
    writeLogDebug("parse '${resp}'")

     if (resp.indexOf("\n")==0){
        resp = resp.substring(1)
    }

    if (resp.indexOf("Optoma_PJ> ") == 0){
        resp = resp.substring(11)
    }

     writeLogDebug("parse2 '${resp}'")

    if (resp.indexOf("OK") == 0 && resp.length()==16){
        def pwr = resp.substring(2,3)
        writeLogDebug("power " + pwr)
        if (pwr == "1"){
            sendEvent(name: "switch", value: "on")
        }else if (pwr == "0"){
            sendEvent(name: "switch", value: "off")
        }
    }
}

def on(){
    sendTelnetMsg("00 1")
    runIn(15,refresh)
}

def off(){
    sendTelnetMsg("00 0")
    runIn(15,refresh)
}

def refresh(){
    //150 1
    writeLogDebug('refresh')
    sendTelnetMsg("150 1")
    //returns OK + 14 digita
}

def initialize()
{
	String ip = settings?.OptomaIP as String
	Integer port = settings?.OptPort as Integer

    try {
        writeLogDebug("ip: ${ip} port: ${port}")
        telnetConnect(termChars:[13], ip, port, null, null)
        writeLogDebug("Opening telnet connection with ${ip}:${port}")
        refresh()
    }catch (e) {
        log.error "exception telnet: ${e}"
    }
}


def telnetStatus(message) {
	log.error "Status: ${message}"
	// Reconnect if there is a telnet error
	initialize()
}

def installed()
{
    telnetClose()
    log.warn "${device.getName()} installed..."
	//initialize()
    updated()
}

def uninstalled() {
		telnetClose()
}

def updated()
{
	writeLogInfo("updated...")
    state.version = getVersion()
    unschedule()

	// disable debug logs after 30 min
	if (debugOutput) 
		runIn(1800,logsOff)

    initialize()
}

def logsOff() 
{
    log.warn "${device.getName()} debug logging disabled..."
    device.updateSetting("debugOutput",[value:"false",type:"bool"])	
}

private writeLogDebug(msg) 
{
    if (settings?.debugOutput || settings?.debugOutput == null)
        log.debug "$msg"
}

private writeLogInfo(msg)
{
    if (settings?.textLogging || settings?.textLogging == null)
        log.info "$msg"
}

def sendTelnetMsg(String msg) 
{
    def fullMsg = "~${settings.ProjectorID}${msg}"
    writeLogDebug("sendTelnetMsg with ${fullMsg}")
    try {
     sendHubCommand(new hubitat.device.HubAction(fullMsg, hubitat.device.Protocol.TELNET))
    }catch (e) {
        log.error "exception sending telnet: ${e}"
    }
}