# SDN-based Traffic Prioritization

Final project for the course in Advanced Network Architectures and Wireless 
Systems @University of Pisa.

## Objective
The objective of this project is to design and develop a Floodlight module that implements a traffic prioritization behaviour inside a network domain. The network should manage traffic flows according to the scenario "A. Traffic Prioritization" of paper [1]. 
Checkout the [assignmenet](assignment.pdf) and the [report](report.pdf) for all the information about the project.

## Repository structure
 - `floodlight` contains the code of the traffic prioritization floodlight module.
 - `mininet` contains the code to create an emulated network to test all the functionalities of the module. 
 - `docs` contains the assignement and the report of the project.
 
## Setup instructions
Clone this repository:
```
$ git clone https://github.com/edoardoruffoli/SDN-TrafficPrioritization
```

Update apt-get:
```
$ sudo apt-get update
```

### Floodlight
1. Clone the Floodlight controller:
```
$ git clone https://github.com/floodlight/floodlight
```

2. Add the traffic prioritization module to the Floodlight controller:
```
$ rsync -av $REPO_ROOT/floodlight/ $FLOODLIGHT_ROOT/
```

### OvS
1. Download the latest version of OvS:
```
$ sudo apt update
$ sudo apt upgrade
$ sudo apt install openvswitch-switch
```

### Bofuss
The following steps are mostly taken from https://github.com/CPqD/ofsoftswitch13.

1. Download build requirements:
```
$ sudo apt-get install cmake libpcap-dev libxerces-c3.2 libxerces-c-dev libpcre3 libpcre3-dev flex bison pkg-config autoconf libtool libboost-dev
```

2. Setup Netbee:
```
$ git clone https://github.com/netgroup-polito/netbee.git
$ cd netbee/src
$ cmake .
$ make
$ sudo cp ../bin/libn*.so /usr/local/lib
$ sudo ldconfig
$ sudo cp -R ../include/* /usr/include/
```

3. Build OfSoftSwitch13
```
$ git clone https://github.com/CPqD/ofsoftswitch13
$ ./boot.sh
$ ./configure
$ make
$ sudo make install
```

### Mininet
1. Download pip:
```
$ sudo apt install python-pip
```

2. Install requirements:
```
$ pip install -r requirements.txt
```

### Iperf
1. Install Iperf:
```
$ sudo apt-get install iperf
```

2. Install ethtool:
```
$ sudo apt install ethtool
```
Ethtool is needed to fix a bug with the usage of TCP with Iperf [#653](https://github.com/mininet/mininet/issues/653)

## Running
### Floodlight
1. Build and run Floodlight
```
$ cd $FLOODLIGHT_ROOT
$ ant run
```
### Mininet
1. Run the script to create the topology with:
```
$ python topology.py
```
2. Choose one of the execution options:
```
  ┌─────────────────────────────────────────────────────────────────────────┐
  │                                                                         │
  │  SDN-based traffic prioritization                                       │
  │                                                                         │
  │  Here you can run some simulations                                      │
  │                                                                         │
  │                                                                         │
  │    1 - Test A: DSCP Remark                                              │
  │    2 - Test B: QoS guarantees                                           │
  │    3 - Test C: Traffic Prioritization                                   │
  │    4 - Test D: Comprehensive tests                                      │
  │    5 - Clear Mininet Configuration                                      │
  │    6 - Exit                                                             │
  │                                                                         │
  │                                                                         │
  └─────────────────────────────────────────────────────────────────────────┘
  >> 
```
 
 ## Bibliography
[1] H. Krishna, N. L. M. van Adrichem and F. A. Kuipers, "Providing bandwidth guarantees
with OpenFlow," 2016 Symposium on Communications and Vehicular Technologies (SCVT),
2016, pp. 1-6, doi: 10.1109/SCVT.2016.7797664.
 
 ## Authors
 - Gianluca Cometa [@gianlucacometa](https://github.com/gianlucacometa)
 - Edoardo Ruffoli [@edoardoruffoli](https://github.com/edoardoruffoli)
