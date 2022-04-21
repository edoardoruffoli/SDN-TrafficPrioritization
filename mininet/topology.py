#! /usr/bin/python

"""
"""
from __future__ import print_function

import os
import time
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Node, UserSwitch
from mininet.log import setLogLevel, info
from mininet.node import CPULimitedHost
from mininet.link import TCLink
from mininet.cli import CLI
from mininet.link import Intf
from mininet.node import RemoteController, Host, OVSKernelSwitch

# sh ovs-ofctl queue-get-config s2 -O OpenFlow13		Get Queue List
# sh ovs-ofctl dump-flows s2 -O OpenFlow13			Get Flows
# sh ovs-ofctl queue-stats s2 -O OpenFlow13


def topology():
        
	net = Mininet( ipBase="10.0.0.0/8")
        info("*** Adding controller\n")
        c1 = net.addController(name="c1", controller=RemoteController,
                           protocol="tcp",
                           ip="127.0.0.1",
                           port=6653)

        info("*** Adding switches\n")
        s1 = net.addSwitch("s1", cls=UserSwitch, dpid="00:00:00:00:00:06", dpopts='')		
        s2 = net.addSwitch("s2", cls=OVSKernelSwitch, dpid="00:00:00:00:00:00:00:07", protocols="OpenFlow13")
        s3 = net.addSwitch("s3", cls=OVSKernelSwitch, dpid="00:00:00:00:00:00:00:08", protocols="OpenFlow13")
   
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
        net.addLink(s2, s3) #, cls=TCLink, bw=10) https://github.com/faucetsdn/ryu/issues/147
        net.addLink(s3, h5)

        info("*** Starting network\n")
        net.build()
        c1.start()
        s1.start([c1])
        s2.start([c1])
	s3.start([c1])

        # queues	
        info("*** Creating queues\n")
	time.sleep(1)	# wait for the switches to start
	
	# Access Switch (BOFUSS)
	"""
	s1.cmd('dpctl unix:/tmp/s1 queue-del 5 1')	# Less Effort
	s1.cmd('dpctl unix:/tmp/s1 queue-del 5 2')	# Best Effort
	s1.cmd('dpctl unix:/tmp/s1 queue-del 5 7')	# QoS
		
	s1.cmd('dpctl unix:/tmp/s1 queue-mod 5 1 0')	# Less Effort
	s1.cmd('dpctl unix:/tmp/s1 queue-mod 5 2 5')	# Best Effort
	s1.cmd('dpctl unix:/tmp/s1 queue-mod 5 7 800')	# QoS

	# Setting Best Effort Queue as default queue for reaching h5
	s1.cmd('dpctl unix:/tmp/s1 flow-mod cmd=add,prio=1,table=0 eth_type=0x800,ip_dst=10.0.0.5 apply:queue=2,output=5')
	"""
	# Less Effort queue
	#s1.cmd('dpctl unix:/tmp/s1 flow-mod cmd=add,table=1,prio=1 ip_dscp=10 apply:queue=1,output=5')

	# Core Switch (OvS)
	s2.cmd('ovs-vsctl --all destroy Qos')
	s2.cmd('ovs-vsctl --all destroy Queue')
	# q0 QoS
	# q1 Best Effort
	# q2 Less Effort

	s2.cmd('ovs-vsctl set port s2-eth2 qos=@newqos -- --id=@newqos create qos type=linux-htb queues:0=@q0, queues:1=@q1, queues:2=@q2 -- --id=@q0, create queue other-config:priority=7 -- --id=@q1, create queue other-config:priority=1 -- --id=@q2 create queue other-config:priority=2')

        #net.plotGraph(max_x=100, max_y=100)

	# Bad TCP SYN packets generated on veth interfaces in Ubuntu 16.04
	# https://github.com/mininet/mininet/issues/653
	for h in h1, h2, h3, h4, h5:
		#h.cmd( 'ethtool -K', h.defaultIntf(), 'tx', tx, 'rx', rx )
		h.cmd( 'ethtool -K', h.defaultIntf(), 'tx off' )

	CLI( net )

	net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    topology()
