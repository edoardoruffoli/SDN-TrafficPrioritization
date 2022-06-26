#! /usr/bin/python

"""
"""
from __future__ import print_function

import os
import sys
import time
import sys

from consolemenu import *
from consolemenu.items import *
from consolemenu.prompt_utils import PromptUtils

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Node, UserSwitch
from mininet.log import setLogLevel, info
from mininet.link import TCLink
from mininet.cli import CLI
from mininet.node import RemoteController, Host, OVSSwitch

import test0
import test1
import test2
import test3

# sh ovs-ofctl queue-get-config s2 -O OpenFlow13		Get Queue List
# sh ovs-ofctl dump-flows s2 -O OpenFlow13			Get Flows
# sh ovs-ofctl queue-stats s2 -O OpenFlow13
# sh ovs-vsctl list queue

def action(test):
    print("##### SIMULATION Now Starting ######: ", test)
    topology(test)
    PromptUtils(Screen()).enter_to_continue()
    
def main():
    # Create the root menu
    menu = ConsoleMenu("SDN-based traffic prioritization", "Here you can run some simulations")

     # Add all the items to the root menu
    menu.append_item(FunctionItem("Test A: DSCP Remark", action, args=[0]))
    menu.append_item(FunctionItem("Test B: QoS guarantees", action, args=[1]))
    menu.append_item(FunctionItem("Test C: Traffic Prioritization", action, args=[2]))
    menu.append_item(FunctionItem("Test D: Comprehensive tests", action, args=[3]))
    menu.append_item(CommandItem("Clear Mininet Configuration", "sudo mn -c"))

    # Create a menu item that calls a function
    #function_item = FunctionItem("Fun item", input_handler)
    #test1_item = FunctionItem("Test1 : QoS Guarantees", topology(test=0))

    # Show the menu
    menu.start()
    menu.join()


def topology(test):
        
	net = Mininet(ipBase="10.0.0.0/8", link=TCLink)
        info("*** Adding controller\n")
        c1 = net.addController(name="c1", controller=RemoteController,
                           protocol="tcp",
                           ip="127.0.0.1",
                           port=6653)

        info("*** Adding switches\n")
        s1 = net.addSwitch("s1", cls=UserSwitch, dpid="00:00:00:00:00:06", dpopts='')		
        s2 = net.addSwitch("s2", cls=OVSSwitch, dpid="00:00:00:00:00:00:00:07", protocols="OpenFlow13")
        s3 = net.addSwitch("s3", cls=OVSSwitch, dpid="00:00:00:00:00:00:00:08", protocols="OpenFlow13")
   
        info("*** Adding hosts\n")
        h1 = net.addHost("h1", cls=Host, ip="10.0.0.1", mac="00:00:00:00:00:01", defaultRoute="h1-eth0")
        h2 = net.addHost("h2", cls=Host, ip="10.0.0.2", mac="00:00:00:00:00:02", defaultRoute="h2-eth0")
        h3 = net.addHost("h3", cls=Host, ip="10.0.0.3", mac="00:00:00:00:00:03", defaultRoute="h3-eth0")
        h4 = net.addHost("h4", cls=Host, ip="10.0.0.4", mac="00:00:00:00:00:04", defaultRoute="h4-eth0")
        h5 = net.addHost("h5", cls=Host, ip="10.0.0.5", mac="00:00:00:00:00:05", defaultRoute="h5-eth0")
   
        info("*** Creating links\n")
        net.addLink(h1, s1)
        net.addLink(h2, s1)
        net.addLink(h3, s1)
        net.addLink(h4, s1)

	net.addLink(s1, s2)
        net.addLink(s2, s3)
        net.addLink(s3, h5)

        info("*** Starting network\n")
        net.build()
#       c1.start()
#       s1.start([c1])
#       s2.start([c1])
#	s3.start([c1])
	net.start()

        # Queues	
        info("*** Creating queues\n")

	# wait for the switches to start
	time.sleep(1)		

	for s in s2, s3:

		# Clean Up
		s.cmd('ovs-vsctl --all destroy Qos')
		s.cmd('ovs-vsctl --all destroy Queue')

		qos_id = s.cmd('ovs-vsctl create qos type=linux-htb other-config:max-rate=10000000 queues=0=@be,1=@le,2=@hi -- --id=@be create queue other-config:priority=1 -- --id=@le create queue other-config:priority=10 -- --id=@hi create queue other-config:priority=0').splitlines()[0]

		for link in net.links:
		    print("link: {} <-> {}".format(link.intf1.name, link.intf2.name))
		    s.cmd('ovs-vsctl set Port %s qos=%s' % (link.intf1.name, qos_id))
		    s.cmd('ovs-vsctl set Port %s qos=%s' % (link.intf2.name, qos_id))

	# Iperf mininet BUG: Bad TCP SYN packets generated on veth interfaces in Ubuntu 16.04
	# https://github.com/mininet/mininet/issues/653
	for h in h1, h2, h3, h4, h5:
		h.cmd( 'ethtool -K', h.defaultIntf(), 'tx off' , 'rx off' )

	net.pingAll()
	print("TEST: ", test)
	# Run specified test
	if test == 0:
		test0.run_test(net)
	elif test == 1:
		test1.run_test(net)
	elif test == 2:
		test2.run_test(net)
	elif test == 3:
		test3.run_test(net)

	CLI(net)	
	net.stop()

if __name__ == '__main__':
    	setLogLevel( 'info' )
    	main()
#	test = 1
#	if len(sys.argv) > 1:
#		test = int(sys.argv[1])

#    	topology(test)
