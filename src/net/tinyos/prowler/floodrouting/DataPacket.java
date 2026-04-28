/*
 * Copyright (c) 2003, Vanderbilt University
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE VANDERBILT UNIVERSITY BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE VANDERBILT
 * UNIVERSITY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE VANDERBILT UNIVERSITY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE VANDERBILT UNIVERSITY HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * Created on Jan 25, 2004
 * 
 * Contributors: Gyorgy Balogh, Gabor Pap, Miklos Maroti
 */
 
package net.tinyos.prowler.floodrouting;

/**
 * This class serves as a base class for messages to be sent using the 
 * Routing Framework. A tipical user should derive his datastructure from
 * this class. It says nothing about the data structure to be sent
 * though it has some framework specific constraints and member variables
 * (which are invisible for the user).  
 * 
 * @author Gabor Pap (gabor.pap@vanderbilt.edu)
 */
public abstract class DataPacket {
	
	/** 
	 * A field used by the framework. It is a state variable, this is part
	 * of a state machine where the actions are defined by the 
	 * {@link RoutingPolicy}. 
	 */
	int priority = 0xFF;
	
	/**
	 * This class is also part of a List data structure used exclusively by
	 * the {@link RoutingApplication}. This variable is a part of that structure.
	 */
	DataPacket next = null;
	
	/**
	 * Checks if to packet are equal from the framework point of view, which is 
     * defined by the user who derives a class from this. If two packets are 
     * equal that means the network will handle them as identical. To put it
     * another way the attributes of the packet used in the implementation of
     * this method (the method compares only these fields) determine a networkwide
     * unique ID in some way.   
	 * 
	 * @param packet the input packet we compare this one to
	 * @return should return true if the input packet equals this packet. 
	 */
	public abstract boolean equals(DataPacket packet);
	
	/**
	 * The content of this packet should be copied to the parameter one.
	 * 
	 * @param packet the packet whose content is changed by this function
	 */
	public abstract void copyTo(DataPacket packet);
}