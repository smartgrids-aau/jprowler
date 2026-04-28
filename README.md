# JProwler — Java Probabilistic Wireless Network Simulator

JProwler is a compact, event-driven wireless network simulator written in Java.
It was developed at the [Institute for Software Integrated Systems (ISIS)](https://www.isis.vanderbilt.edu/)
at Vanderbilt University as a Java port of PROWLER (Probabilistic Wireless Network Simulator),
which ran on MATLAB. The main motivation for the Java port was a ~1000× speed improvement.

## Features

- **Discrete event simulation** with priority-queue scheduler
- **Two radio propagation models**: Gaussian (static nodes) and Rayleigh (time-varying fading)
- **CSMA MAC layer** modeled after the Mica2 / CC1000 mote
- **Flood routing framework** with pluggable routing policies
- **GUI display** (Java AWT) with real-time and fast-forward modes
- **Reproducible experiments** via seeded PRNG
- Entire simulator in ~4300 lines / 20 Java classes

## Project Structure

```
src/
├── net/tinyos/prowler/           # Core simulator (12 files)
│   ├── Simulator.java            # Event queue and scheduler
│   ├── Node.java                 # Abstract base node
│   ├── Mica2Node.java            # CSMA MAC implementation
│   ├── Application.java          # Base class for protocols
│   ├── Event.java                # Abstract timed event
│   ├── RadioModel.java           # Abstract radio model
│   ├── GaussianRadioModel.java   # Static fading + per-TX noise
│   ├── RayleighRadioModel.java   # Time-varying multipath fading
│   ├── Display.java              # AWT visualization
│   ├── SimpleQeue.java           # Custom priority queue
│   ├── SnifferIF.java            # Sniffer interface
│   ├── TestBroadcastNode.java    # Broadcast demo (main)
│   └── SelforganizingBackbones.java  # Coloring demo (main)
├── net/tinyos/prowler/floodrouting/  # Flood routing (5 files)
│   ├── FloodRouting.java
│   ├── RoutingApplication.java
│   ├── RoutingPolicy.java
│   ├── DataPacket.java
│   └── FloodRoutingMsg.java
└── net/goui/util/                # Utilities (2 files)
    ├── NESRandom.java
    └── StatKeeper.java
doc/                              # Javadoc
```

## Building

```bash
# Compile all sources
mkdir -p bin
find src -name "*.java" | xargs javac -d bin

# Run the broadcast demo (opens GUI window)
java -cp bin net.tinyos.prowler.TestBroadcastNode

# Run the self-organizing backbones demo
java -cp bin net.tinyos.prowler.SelforganizingBackbones
```

Requires Java 8 or later.

## Demo Applications

### TestBroadcastNode

Creates 1000 nodes randomly placed on a 300×300 m field with a root node at the center.
The root broadcasts a message that floods through the network. Nodes change color as
the flood progresses:

- **Black** — not yet reached
- **Blue** — currently transmitting
- **Green** — receiving a message
- **Red** — received a corrupted message (collision)
- **Pink** — has forwarded the message

### SelforganizingBackbones

Distributed graph coloring algorithm where nodes self-organize into a backbone structure.

## Writing Your Own Application

1. Subclass `Application` and implement `receiveMessage()` / `sendMessageDone()`
2. Create a `Simulator`, choose a `RadioModel`, create `Node`s
3. Attach your application to each node
4. Call `radioModel.updateNeighborhoods()` (important!)
5. Inject an initial event or message and call `sim.run(seconds)`

See `TestBroadcastNode.java` for a complete example.

## References

- G. Simon, P. Volgyesi, M. Maroti, and A. Ledeczi,
  "Simulation-based optimization of communication protocols for large-scale wireless sensor networks,"
  *Proceedings of the IEEE Aerospace Conference* 3:1339–1346, March 2003.

- H. Adam, W. Elmenreich, C. Bettstetter, and S. M. Senouci,
  "[CoRe-MAC: A MAC-protocol for cooperative relaying in wireless networks](http://mobile.aau.at/publications/adam-2009-globecom-core-mac.pdf),"
  *Proceedings of the 2009 IEEE Global Communication Conference (Globecom)*, Honolulu, Hawaii, 2009.

## License

Copyright (c) 2003, Vanderbilt University. All rights reserved.

Permission to use, copy, modify, and distribute this software and its documentation
for any purpose, without fee, and without written agreement is hereby granted,
provided that the above copyright notice and the following two paragraphs appear
in all copies of this software.

See [LICENSE](LICENSE) for full text.

## Acknowledgments

JProwler was created by Gyorgy Balogh, Gabor Pap, and Miklos Maroti at the
Institute for Software Integrated Systems, Vanderbilt University.

This repository is maintained by [Wilfried Elmenreich](https://mobile.aau.at/~welmenre/)
at Alpen-Adria-Universität Klagenfurt, who uses JProwler in the course
*700.291 VI Simulation of Networked Systems*.
