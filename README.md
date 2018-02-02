# NetwEmbSys-Project
## Environmental comfort in intelligent ambient

Simple smart system to monitor and enhance the environmental comfort of the building occupants.
The application is composed of:
- a **WSN**, developed in [Contiki](http://www.contiki-os.org/), whose goal is to monitor light and temperature of several rooms;
- an **Android application** that allows rooms' occupants to share their opinion about the living environment. Data are sent to a web database;
- a non-constrained node, developed in [Californium](https://github.com/eclipse/californium), which acts as **Relay** between sensor nodes of the WSN, that periodically switch off their radio for energy conservation reasons, and the web database. In particular the relay is responsible for:
	- sending sensor's measurements to the database
	- computing and sending, for every room, the satisfaction index from occupants' opinion
- an actuation system simulated by LEDs placed on board of each sensor node.