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
 * Created on 01/23/2004
 * 
 * Contributors: Gabor Pap, Miklos Maroti
 */
 
package net.tinyos.prowler.floodrouting;

/**
 * The implementation of this class defines a state automaton, where the actions
 * are predetermined by this interface.
 * 
 * @author Gabor Pap (gabor.pap@vanderbilt.edu)
 */
public interface RoutingPolicy {

	/**
	 * It is used by the {@link FloodRouting} engine to get the "location" of
	 * this node. The meaning of the "location" is policy dependent:
	 * it can be a constant value, hop count from a root, 2D offset
	 * of the physical location from some center, or something else.
	 */
	public int getLocation();

	/**
	 * It is used by the {@link FloodRouting} engine when a data has been successfully
	 * sent. The policy implementation should change the priority so it
	 * does not send the same packets in an infinite loop. 
	 * IMPORTANT: This command is called when the packet was sent, and by
	 * this time the priority might not be the same as it was when this
	 * packet was selected for sending. In particular, the packet
	 * can be odd, if the old even value was changed in received() or age().
	 *
	 * @param priority The old priority of the data packet.
	 * @return The new priority of the data packet. Return 0xFF to drop
	 *	this packet.
	 */
	public int sent(int priority);

	/**
	 * It is used by the {@link FloodRouting} enginge when a data packet is received.
	 * The policy implementation can indicate to drop this packet.
	 * 
	 * @param location The location of the sender.
	 * @return false to drop this packet, or true to start searching
	 *	for a match and the packet table.
	 */
	public boolean accept(int location);

	/**
	 * It is used by the {@link FloodRouting} engine when a data packet is received,
	 * and this policy returned SUCCESS in accept(). The policy 
	 * implementation can change the priority of the packet.
	 *
	 * @param location The location of the sender.
	 * @param priority 0x00 if this data packet is new at this node,
	 *	or the existing priority of the matching packet.
	 * @return The new priority of the data packet. Return 0xFF to drop
	 *	this packet.
	 */
	public int received(int location, int priority);

	/**
	 * It is used by the {@link FloodRouting} engine when a data packet is aged.
	 * The policy implementation should "increase" the priority and
	 * eventually set it to 0xFF (free).
	 *
	 * @param priority The old priority of the data packet.
	 * @return The new priority of the data packet. Return 0xFF to drop
	 *	this packet.
	 */
	public int age(int priority);

}
