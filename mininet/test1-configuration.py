import requests
import json

controller_ip = "127.0.0.1:8080"
base_url = "http://" + controller_ip + "/qos/"
header = {"Content-type": "application/json", "Accept": "text/plain"}

url_switches = base_url + "switches/json"
url_flows = base_url + "flows/json"

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
	 "bandwidth": "80000"}
	), headers=header))

print(requests.post(url_flows, data=json.dumps(
	{"dpid-meter-switch": "00:00:00:00:00:00:00:06", 
	 "dpid-queue-switch": "00:00:00:00:00:00:00:07",
	 "src-addr": "10.0.0.3",
	 "dst-addr": "10.0.0.5",
	 "bandwidth": "50000"}
	), headers=header))

print(requests.post(url_flows, data=json.dumps(
	{"dpid-meter-switch": "00:00:00:00:00:00:00:06", 
	 "dpid-queue-switch": "00:00:00:00:00:00:00:07",
	 "src-addr": "10.0.0.4",
	 "dst-addr": "10.0.0.5",
	 "bandwidth": "30000"}
	), headers=header))




