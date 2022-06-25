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
		 "src-addr": "10.0.0.2",
		 "dst-addr": "10.0.0.5",
		 "bandwidth": "3500"}
		), headers=header))

	print(requests.post(url_flows, data=json.dumps(
		{"dpid-meter-switch": "00:00:00:00:00:00:00:06", 
		 "dpid-queue-switch": "00:00:00:00:00:00:00:07",
		 "src-addr": "10.0.0.3",
		 "dst-addr": "10.0.0.5",
		 "bandwidth": "3500"}
		), headers=header))

	print(requests.post(url_flows, data=json.dumps(
		{"dpid-meter-switch": "00:00:00:00:00:00:00:06", 
		 "dpid-queue-switch": "00:00:00:00:00:00:00:07",
		 "src-addr": "10.0.0.4",
		 "dst-addr": "10.0.0.5",
		 "bandwidth": "3500"}
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

	# Remove previous test results
	if os.path.exists("output/test3/h1.txt"):
		os.remove("output/test3/h1.txt")

	if os.path.exists("output/test3/h2.txt"):
		os.remove("output/test3/h2.txt")

	if os.path.exists("output/test3/h3.txt"):
		os.remove("output/test3/h3.txt")

	if os.path.exists("output/test3/h4_a.txt"):
		os.remove("output/test3/h4_a.txt")

	if os.path.exists("output/test3/h4_b.txt"):
		os.remove("output/test3/h4_b.txt")

	if os.path.exists("output/test3/h5.txt"):
		os.remove("output/test3/h5.txt")

	# Start iperf test
	info("*** Starting Test\n")

	info("*** Started iperf server on h5\n")
	h5.cmd("xterm -T h5 -l -lf output/test3/h5.txt -hold -e iperf -s -i 1 &")
	time.sleep(5)

	info("*** Started iperf client on h1\n")
	h1.cmd("xterm -T h1 -l -lf output/test3/h1.txt -hold -e iperf -c 10.0.0.5 -p 5001 -b 2M -i 5 -t 30 &")
	time.sleep(5)

	info("*** Started iperf client on h2\n")
	h2.cmd("xterm -T h2 -l -lf output/test3/h2.txt -hold -e iperf -c 10.0.0.5 -p 5001 -b 8M -i 5 -t 25 &")
	time.sleep(5)

	info("*** Started iperf client on h3\n")
	h3.cmd("xterm -T h3 -l -lf output/test3/h3.txt -hold -e iperf -c 10.0.0.5 -p 5001 -b 5M -i 5 -t 20 &")
	time.sleep(5)

	info("*** Started iperf client on h4\n")
	h4.cmd("xterm -T h4_a -l -lf output/test3/h4_a.txt -hold -e iperf -c 10.0.0.5 -p 5001 -b 2M -i 5 -t 10 &")
	time.sleep(10)
	
	info("*** Restarted iperf client on h4\n")
	h4.cmd("xterm -T h4_b -l -lf output/test3/h4_b.txt -hold -e iperf -c 10.0.0.5 -p 5001 -b 3.5M -i 5 -t 5 &")


if __name__ == '__main__':
	configure_test()


