#! /usr/bin/python

"""
"""
from __future__ import print_function

import os
import time
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Node
from mininet.log import setLogLevel, info
from mininet.link import TCLink
from mininet.cli import CLI
from mininet.link import Intf
from mininet.node import RemoteController, OVSKernelSwitch, Host


def topology():
        
	net = Mininet( ipBase="10.0.1.0/8" )
        info("*** Adding controller\n")
        c1 = net.addController(name="c1", controller=RemoteController,
                           protocol="tcp",
                           ip="127.0.0.1",
                           port=6653)

        info("*** Adding switches\n")
        s1 = net.addSwitch("s1", cls=OVSKernelSwitch, dpid="00:00:00:00:00:00:00:01", protocols="OpenFlow13")
        s2 = net.addSwitch("s2", cls=OVSKernelSwitch, dpid="00:00:00:00:00:00:00:02", protocols="OpenFlow13")
   

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

        net.addLink(s1, s2, cls=TCLink, bw=1000)

        net.addLink(s2, h5)

        info("*** Starting network\n")
        net.build()
        c1.start()
        s1.start([c1])
        s2.start([c1])

        # queues	
	time.sleep(1)	# wait for the switch to start
	
	s1.cmd('ovs-vsctl --all destroy Qos')
	s1.cmd('ovs-vsctl --all destroy Queue')
	s1.cmd('ovs-vsctl set port s1-eth4 qos=@newqos -- --id=@newqos create qos type=linux-htb queues:0=@q0, queues:1=@q1, queues:2=@q2 -- --id=@q0, create queue other-config:priority=7 -- --id=@q1, create queue dscp=10 other-config:priority=1 -- --id=@q2 create queue dscp=46 other-config:priority=2')

        #net.plotGraph(max_x=100, max_y=100)

	CLI( net )

	net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    topology()
