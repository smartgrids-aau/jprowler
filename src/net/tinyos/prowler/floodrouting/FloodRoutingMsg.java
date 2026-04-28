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
 * Created on Nov 24, 2003
 * 
 * Contributors: Gyorgy Balogh, Gabor Pap, Miklos Maroti
 */
 
package net.tinyos.prowler.floodrouting;

/**
 * This class represents a TinyOS message with the routing specific details in it.
 * No user of this package has to know much about it. 
 * 
 * @author Gabor Pap (gabor.pap@vanderbilt.edu)
 */
@SuppressWarnings("rawtypes")
public class FloodRoutingMsg {

	/** This field tells which routing application is addressed in the message. */
	Class routingApp;
	
	/** See {@link RoutingPolicy} */
	int location;
	
	/** 
	 * An array of DataPackets. Keep in mind that the number of packets is 
	 * limited in 29, as the payload of TinyOS messages is 29 bytes.  
	 */
	public DataPacket[] data;
	
	/**This parameter is set to true if there is no valid DataPacket in the array. */
	boolean empty = true;
	
	/**
	 * A parameterized constructor, which creates a DataPacket array.
	 * 
	 * @param size the size of the created array
	 */
	FloodRoutingMsg(int size){
		data = new DataPacket[size];
	}
    
    /**
     * A getter method for the location
     */
    public int getLocation() {
        return location;
    }

}
