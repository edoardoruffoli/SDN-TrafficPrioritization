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

### Floodlight
1. Add the traffic prioritization module to the floodlight in your system.
```
$ rsync -av $REPO_ROOT/floodlight/ $FLOODLIGHT_ROOT/
```
2. Build and run Floodlight
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
