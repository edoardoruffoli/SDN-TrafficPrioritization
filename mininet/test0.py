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
		 "bandwidth": "2000"}
		), headers=header))

def run_test(net):
	h1 = net.getNodeByName('h1')
	s2 = net.getNodeByName('s2')
	h5 = net.getNodeByName('h5')	

	# Configuring Flows
	info("*** Configuring QoS Traffic Flows\n")
	configure_test()

	# Iperf tests
	info("*** Starting Test 0\n")

	info("*** Started iperf server on h5\n")
	h5.cmd("xterm -T h5 -l -lf output/test0.txt -hold -e iperf -s -i 1 &")
	time.sleep(3)

	info("*** Started iperf client on h1\n")
	h1.cmd("xterm -T h1 -hold -e iperf -c 10.0.0.5 -p 5001 -b 5M -t 30 &")

	info("*** Launching wireshark to inspect packets on s2 s2-eth1\n")
	info("*** Use as packet filter: ip.src == 10.0.0.1 && ip.dsfield == 0x10 \n")
	info("*** Use as packet filter: ip.src == 10.0.0.1 && ip.dsfield == 0x08 \n")
	s2.cmd("xterm -hold -e wireshark")	
	

if __name__ == '__main__':
	configure_test()
