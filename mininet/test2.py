import os
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
		 "src-addr": "10.0.0.1",
		 "dst-addr": "10.0.0.5",
		 "bandwidth": "7000"}
		), headers=header))

	print(requests.post(url_flows, data=json.dumps(
		{"dpid-meter-switch": "00:00:00:00:00:00:00:06", 
		 "dpid-queue-switch": "00:00:00:00:00:00:00:07",
		 "src-addr": "10.0.0.2",
		 "dst-addr": "10.0.0.5",
		 "bandwidth": "3000"}
		), headers=header))

def run_test(net):
	h1 = net.getNodeByName('h1')
	h2 = net.getNodeByName('h2')
	h5 = net.getNodeByName('h5')

	# Configuring Flows
	info("*** Configuring QoS Traffic Flows\n")
	configure_test()

	# Remove previous test results
	if os.path.exists("output/test2/h1.txt"):
		os.remove("output/test2/h1.txt")

	if os.path.exists("output/test2/h2.txt"):
		os.remove("output/test2/h2.txt")

	if os.path.exists("output/test2/h5.txt"):
		os.remove("output/test2/h5.txt")

	# Start iperf test
	info("*** Starting Test 2\n")

	info("*** Started iperf server on h5\n")
	h5.cmd("xterm -T h5 -l -lf output/test2/h5.txt -hold -e iperf -s -i 1 &")
	time.sleep(5)

	info("*** Started iperf client on h1 to saturate link\n")
	h1.cmd("xterm -T h1 -l -lf output/test2/h1.txt -hold -e iperf -c 10.0.0.5 -p 5001 -b 10M -i 5 -t 15 &")

	info("*** Started iperf client on h2\n")
	h2.cmd("xterm -T h2 -l -lf output/test2/h2.txt -hold -e iperf -c 10.0.0.5 -p 5001 -b 6M -i 5 -t 15 &")


if __name__ == '__main__':
	configure_test()

