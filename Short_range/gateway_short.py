import serial
import paho.mqtt.client as mqtt
import os
import socket
import ssl, time, sys, json
from time import strftime, sleep

client1 = mqtt.Client()                                       # client object
client1.on_connect = on_connect1                               # assign on_connect func
client1.on_message = on_message1 

client2 = mqtt.Client()                                       # client object
client2.on_connect = on_connect2                              # assign on_connect func
client2.on_message = on_message2 
clientq.on_subscribe = on_subscribe
client2.on_disconnect = on_disconnect

mqtt_username = "pi"
mqtt_password = "raspberry"
mqtt_broker_ip = "192.168.4.1"
port = 1883

motor_status=0

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


#clientID = "broker" 
mqtt_topic=[("node1/temp",0), ("node1/humid",0), ("node1/light",0), ("node1/soil", 0), ("node1/motor_statuss",0)]
node_timestamps = {}
node_stats = {}
node_codes = {
	'node1': 'temp',
	'node1': 'humid',
	'node1': 'light',
	'node1': 'soil',
	'node1': 'motor_status'
}
path_to_db = '/home/pi/database/'
# Callback Function on Connection with MQTT Server
def on_connect1( client, userdata, flags, rc):
	print ("Connected with Code :" +str(rc))
	client1.subscribe(mqtt_topic)

def on_connect2(client, userdata, flags, rc):
	global connflag
    print ("Connected to AWS")
    connflag = True
    print("Connection returned result: " + str(rc) )
	client2.subscribe(SHADOW_UPDATE_ACCEPTED, 1)
	client2.subscribe(SHADOW_UPDATE_DELTA, 1)
	client2.subscribe(SHADOW_GET_ACCEPTED, 1)

# Callback Function on Receiving the Subscribed Topic/Message
def on_message1(client1, userdata, msg):
	# stamp the time message arrive wrt topic
	# global time
	node_name = msg.topic.split('/')[0]
	node_timestamps[node_name] = time()
	node_stats[node_name] = 1
	if (msg.topic == "node1/motor_status"):
		motor_status = int(msg.payload)
		print(msg.topic + ": " + str(motor_status) + "\n")
	else:
		data = float(msg.payload)
		t = round(float(strftime('%H')) + float(strftime('%M'))/60 + float(strftime('%S'))/3600, 2)
		mess = strftime('%Y,%m,%d') + ',' + str(t) + ',' + str(data) + '\n'
		print(mess)
		filename = path_to_db + msg.topic.split('/')[1] + '.csv'
		print(filename)
		with open(filename, 'a+') as file: 
			if file.tell() == 0:
				file.write('Year,Month,Day,Time,Value\n')
			file.write(mess) 
		file.close()
		
	if(msg.topic=='node1/temp'):
		temp=msg.payload
	if(msg.topic=='node1/humid'):
		humid=msg.payload
	if(msg.topic=='node1/light'):
		light=msg.payload
	if(msg.topic=='node1/soil'):
		soil=msg.payload
	if(msg.topic=='node1/motor_status'):
		motor_status=msg.payload
		
	payload1='{{"state":{{"reported":{{"LightIntensity":{0:0.1f},"SoilMoisture":{1:0.1f},"Humidity":{2:0.1f},"Temperature":{3:0.1f}}}}}}}' \
	.format(light,moist,humid,temp,status)
	js = json.dumps(payload)
	if connflag == True:
			
		client.publish(SHADOW_UPDATE, payload1, qos=0)
		print("msg sent: data "+"Time:",strftime("%Y-%m-%d %H:%M:%S"), "Light Intensity:" ,light ,"\n" ,"Soil Moisture:", moist , "\n","Humidity:", humid , "\n" ,"Temperature:", temp,"Motor Status:", status ) 
	else:
		print("waiting for connection...")
		
def on_message2(client2, userdata, msg):		
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
		
def on_subscribe(mosq, obj, mid, granted_qos):
	
	if mid == 3:
		# Fetch current Shadow status. Useful for reconnection scenario. 
		client2.publish(SHADOW_GET_TOPIC,"",qos=1)

def on_disconnect(client2, userdata, rc):
    if rc != 0:
        print "Diconnected from AWS IoT. Trying to auto-reconnect..."	
		
def MOTOR_Status_Change(Shadow_State_Doc,Type):  #fucntion to change motor status
	DESIRED_MOTOR_STATUS = ""
	SHADOW_State_Doc = json.loads(Shadow_State_Doc)
	#retrieving motor status
	if Type == "DELTA": 
		DESIRED_MOTOR_STATUS= SHADOW_State_Doc['state']['status']
	elif Type == "GET_REQ":
		DESIRED_MOTOR_STATUS= SHADOW_State_Doc['state']['desired']['status']
	 
	if DESIRED_MOTOR_STATUS == "1":
		
		client2.publish(SHADOW_UPDATE, SHADOW_STATE_DOC_MOTOR_ON,qos=1)   #update the status to Thing Shadow
		client2.publish(Motor,DESIRED_MOTOR_STATUS,qos=1)for short range case, send signal to motor control topic
	if DESIRED_MOTOR_STATUS == "0":
		Serial.write(DESIRED_MOTOR_STATUS) #send Serial message to control the motor
		client2.publish(SHADOW, SHADOW_STATE_DOC_MOTOR_OFF,qos=1)   #update the status to Thing Shadow
		client2.publish(Motor,DESIRED_MOTOR_STATUS,qos=1)for short range case, send signal to motor control topic		

def clean_csv_files(files='/home/pi/GUI/database/*.csv'):
	for item in glob.glob(files):
		print('remove file ' + item)
		os.remove(item)
		
while True:
	client1.username_pw_set(mqtt_username,mqtt_password)
	client1.connect(host=mqtt_broker_ip, port=port, keepalive=60)

	# client.loop_forever()
	client1.loop_start()
	while run:
		for node, val in node_timestamps.items():
			if time() - node_timestamps[node] >= 5:
				node_stats[node] = 0;
		filename = path_to_db + "device_list.csv"
		with open(filename, 'w') as file:
			for node, stat in node_stats.items():
				print("node " + node + ": " + str(stat))
				mess = node + ',' + node_codes[node] + ',unknown,' + str(stat) + '\n'
				file.write(mess)
			file.close()

		sleep(5)

	client1.loop_stop(force=False)
	client2.tls_set(caPath, certfile=certPath, keyfile=keyPath, cert_reqs=ssl.CERT_REQUIRED, tls_version=ssl.PROTOCOL_TLSv1_2, ciphers=None)  # pass parameters
 	client2.connect(awshost, awsport, keepalive=60)               # connect to aws server
 	client2.loop_start()