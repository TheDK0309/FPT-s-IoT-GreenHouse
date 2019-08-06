import serial
import paho.mqtt.client as mqtt
import os
import socket
import ssl, time, sys, json
from time import strftime, sleep

port = serial.Serial("/dev/ttyACM1", baudrate=38400, timeout=0.5)

client = mqtt.Client()                                       # client object
client.on_connect = on_connect                               # assign on_connect func
client.on_message = on_message                               # assign on_message func

awshost = "a1eyssf4fxagbt.iot.us-east-1.amazonaws.com"      # Endpoint
awsport = 8883                                              # Port no.   
clientId = "Node1"                                     # thingName
thingName = "Node1"                                    # thingName
caPath = "root-CA.crt"                                      # Root_CA_Certificate_Name
certPath = "Node1.cert.pem"                            # <thingName>.cert.pem
keyPath = "Node1.private.key" 
SHADOW_UPDATE="$aws/things/Node1/shadow/update"
SHADOW_UPDATE_ACCEPTED="$aws/things/"+thingName+"/shadow/update/accepted"
SHADOW_UPDATE_REJECTED="$aws/things/" + thingName + "/shadow/update/rejected"
SHADOW_UPDATE_DELTA="$aws/things/"+thingName+"/shadow/update/delta"
SHADOW_GET=”$aws/things/"+thingName+"/shadow/get"
SHADOW_GET_ACCEPTED = "$aws/things/" + thingName + "/shadow/get/accepted"
SHADOW_GET_REJECTED = "$aws/things/" + thingName + "/shadow/get/rejected"
SHADOW_STATE_DOC_MOTOR_ON = """{"state": {"reported": {"status" : "1"}}}"""
SHADOW_STATE_DOC_MOTOR_OFF = """{"state": {"reported": {"status": "0"}}}"""
connflag = False
active_status = False
def on_connect(client, userdata, flags, rc):                # func for making connection
    global connflag
    print ("Connected to AWS")
    connflag = True
    print("Connection returned result: " + str(rc) )
	client.subscribe(SHADOW_UPDATE_ACCEPTED, 1)
	client.subscribe(SHADOW_UPDATE_DELTA, 1)
	client.subscribe(SHADOW_GET_ACCEPTED, 1)

 
def on_message(client, userdata, msg):                      # Func for Sending msg
    print(msg.topic+" "+str(msg.payload))
	if str(msg.topic) == SHADOW_UPDATE_DELTA:
		 SHADOW_STATE_DELTA = str(msg.payload)
		 MOTOR_Status_Change(SHADOW_STATE_DELTA, "DELTA")
    elif str(msg.topic) == SHADOW_GET_ACCEPTED:
		SHADOW_STATE_DOC = str(msg.payload)
		MOTOR_Status_Change(SHADOW_STATE_DOC, "GET_REQ")
	elif str(msg.topic) == SHADOW_GET_REJECTED:
		SHADOW_GET_ERROR = str(msg.payload)
		print "\n---ERROR--- Unable to fetch Shadow Doc...\nError Response: " + SHADOW_GET_ERROR
	elif str(msg.topic) == SHADOW_UPDATE_ACCEPTED:
		print "\nLED Status Change Updated SUCCESSFULLY in Shadow..."
		print "Response JSON: " + str(msg.payload)
	elif str(msg.topic) == SHADOW_UPDATE_REJECTED:
		SHADOW_UPDATE_ERROR = str(msg.payload)
		print "\n---ERROR--- Failed to Update the Shadow...\nError Response: " + SHADOW_UPDATE_ERROR
	else:
		print "AWS Response Topic: " + str(msg.topic)
		print "QoS: " + str(msg.qos)
		print "Payload: " + str(msg.payload)

def split(str):
	x=str.split(" ")
	return x

def on_subscribe(mosq, obj, mid, granted_qos):
	
	if mid == 3:
		# Fetch current Shadow status. Useful for reconnection scenario. 
		client.publish(SHADOW_GET_TOPIC,"",qos=1)

def on_disconnect(client, userdata, rc):
    if rc != 0:
        print "Diconnected from AWS IoT. Trying to auto-reconnect..."

# Register callback functions
client.on_message = on_message
client.on_connect = on_connect
client.on_subscribe = on_subscribe
client.on_disconnect = on_disconnect

client.tls_set(caPath, certfile=certPath, keyfile=keyPath, cert_reqs=ssl.CERT_REQUIRED, tls_version=ssl.PROTOCOL_TLSv1_2, ciphers=None)  # pass parameters
 
client.connect(awshost, awsport, keepalive=60)               # connect to aws server
 
client.loop_start()     

def MOTOR_Status_Change(Shadow_State_Doc,Type):  #fucntion to change motor status
	DESIRED_MOTOR_STATUS = ""
	SHADOW_State_Doc = json.loads(Shadow_State_Doc)
	#retrieving motor status
	if Type == "DELTA": 
		DESIRED_MOTOR_STATUS= SHADOW_State_Doc['state']['status']
	elif Type == "GET_REQ":
		DESIRED_MOTOR_STATUS= SHADOW_State_Doc['state']['desired']['status']
	 
	if DESIRED_MOTOR_STATUS == "1":
		Serial.write(DESIRED_MOTOR_STATUS)#send Serial message to control the motor
		client.publish(SHADOW_UPDATE, SHADOW_STATE_DOC_MOTOR_ON,qos=1)   #update the status to Thing Shadow
		#client.publish(Motor_Status,DESIRED_MOTOR_STATUS,qos=1)for short range case, send signal to motor control topic
	if DESIRED_MOTOR_STATUS == "0":
		Serial.write(DESIRED_MOTOR_STATUS) #send Serial message to control the motor

	client.publish(SHADOW_UPDATE, SHADOW_STATE_DOC_MOTOR_OFF,qos=1)   #update the status to Thing Shadow
	#client.publish(Motor_Status,DESIRED_MOTOR_STATUS,qos=1)for short range case, send signal to motor control topic


while True:
	RFID = port.readline()
	csvresult = open("/home/pi/data.csv","a")
	data = str(RFID)
   
	x=split(data)
	if(len(x)>=16):
		
		light=(int(x[7])*256+int(x[8]))
		moist=(int(x[9])*256+int(x[10]))
		humid=(int(x[11])*256+int(x[12]))
		temp=(int(x[13])*256+int(x[14]))
		status=(int[15])
		
		csvresult.write("{0},{1},{2},{3},{4},{5}\n".format(strftime("%Y-%m-%d %H:%M:%S"),light,moist,humid,temp,status))
		payload='{{"state":{{"reported":{{"LightIntensity":{0:0.1f},"SoilMoisture":{1:0.1f},"Humidity":{2:0.1f},"Temperature":{3:0.1f}}}}}}}' \
		.format(light,moist,humid,temp,status)
		js = json.dumps(payload)
		if connflag == True:
			
			client.publish(SHADOW_UPDATE, payload, qos=0)
			print("msg sent: data "+"Time:",strftime("%Y-%m-%d %H:%M:%S"), "Light Intensity:" ,light ,"\n" ,"Soil Moisture:", moist , "\n","Humidity:", humid , "\n" ,"Temperature:", temp,"Motor Status:", status ) 
		else:
			print("waiting for connection...")
		
	else: print ("error")
	
	sleep(5)
	fp = open('test.json', 'a')
	fp.write(js)
	# close the connection
    fp.close()
	csvresult.close
	#send_message()
	time.sleep (0.1)
	
	
	


	
	
	
	
	





	