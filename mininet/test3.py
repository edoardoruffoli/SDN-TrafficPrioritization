import requests
import json
import time
from mininet.log import setLogLevel, info
from mininet.util import pmonitor
from signal import SIGINT

def configure_test() :
	controller_ip = "127.0.0.1:8080"
	base_url = "http://" + controller_ip + "/qos/"
	header = {"Content-type": "application/json", "Accept": "text/plain"}

	url_switches = base_url + "switches/json"
	url_flows = base_url + "flows/json"

	# Cleaning previous config
	print(requests.delete(url_switches, data=json.dumps(
		{"dpid-meter-switch": "00:00:00:00:00:00:00:06", 
		 "dpid-queue-switch": "00:00:00:00:00:00:00:07"}
		), headers=header))

	# Enabling Switches
	print(requests.post(url_switches, data=json.dumps(
		{"dpid-meter-switch": "00:00:00:00:00:00:00:06", 
		 "dpid-queue-switch": "00:00:00:00:00:00:00:07"}
		), headers=header))

	# Registering QoS traffic flows
	print(requests.post(url_flows, data=json.dumps(
		{"dpid-meter-switch": "00:00:00:00:00:00:00:06", 
		 "dpid-queue-switch": "00:00:00:00:00:00:00:07",
		 "src-addr": "10.0.0.2",
		 "dst-addr": "10.0.0.5",
		 "bandwidth": "3000"}
		), headers=header))

	print(requests.post(url_flows, data=json.dumps(
		{"dpid-meter-switch": "00:00:00:00:00:00:00:06", 
		 "dpid-queue-switch": "00:00:00:00:00:00:00:07",
		 "src-addr": "10.0.0.3",
		 "dst-addr": "10.0.0.5",
		 "bandwidth": "3000"}
		), headers=header))

	print(requests.post(url_flows, data=json.dumps(
		{"dpid-meter-switch": "00:00:00:00:00:00:00:06", 
		 "dpid-queue-switch": "00:00:00:00:00:00:00:07",
		 "src-addr": "10.0.0.4",
		 "dst-addr": "10.0.0.5",
		 "bandwidth": "3000"}
		), headers=header))

def run_test(net):
	h1 = net.getNodeByName('h1')
	h2 = net.getNodeByName('h2')
	h3 = net.getNodeByName('h3')
	h4 = net.getNodeByName('h4')
	h5 = net.getNodeByName('h5')	

	# Configuring Flows
	info("*** Configuring QoS Traffic Flows\n")
	configure_test()

	# Iperf tests
	info("*** Starting Test\n")
	endTime = time.time() + 35

	info("*** Started iperf server on h5\n")
	h5.cmd("xterm -T h5 -l -lf output/test1.txt -hold -e iperf -s -i 1 &")
	time.sleep(3)

	info("*** Started iperf client on h1\n")
	h1.cmd("xterm -T h1 -hold -e iperf -c 10.0.0.5 -p 5001 -b 2M -t 30 &")
	time.sleep(5)

	info("*** Started iperf client on h2\n")
	h2.cmd("xterm -T h2 -hold -e iperf -c 10.0.0.5 -p 5001 -b 8M -t 25 &")
	time.sleep(5)

	info("*** Started iperf client on h3\n")
	h3.cmd("xterm -T h3 -hold -e iperf -c 10.0.0.5 -p 5001 -b 5M -t 20 &")
	time.sleep(10)

	info("*** Started iperf client on h4\n")
	h3.cmd("xterm -T h4 -hold -e iperf -c 10.0.0.5 -p 5001 -b 2M -t 5 &")
	time.sleep(5)
	
	#info("*** Restarted iperf client on h4\n")
	#popens[h4.name + "b" ] = h4.popen("iperf -c 10.0.0.5 -p 5001 -b 2M -t 10", shell=False)	
	#time.sleep(10)

if __name__ == '__main__':
	configure_test()
