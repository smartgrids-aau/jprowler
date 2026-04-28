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
 * Created on Jan 21, 2004
 * 
 * Contributors: Gyorgy Balogh, Gabor Pap, Miklos Maroti
 */
 
package net.tinyos.prowler.floodrouting;

import net.tinyos.prowler.Display;

/**
 * This class serves as a base class for all the routing applications, all 
 * such application should be derived from this.
 * 
 * @author Gabor Pap (gabor.pap@vanderbilt.edu)
 */
@SuppressWarnings("rawtypes")
public class RoutingApplication {
	// states

    /** Only aging is required, no sending */
	public static byte DIRTY_AGING = 1;	
	
	/** No action is needed for this routing application */ 
	public static byte DIRTY_CLEAN = 0;
	
	/** Some packets are ready to be sent */
	public static byte DIRTY_SENDING = 3;

	/** 
	 * This field tells what this routing application is doing, it can either
	 * be {@link RoutingApplication#DIRTY_CLEAN}, or {@link RoutingApplication#DIRTY_AGING},
	 * or {@link RoutingApplication#DIRTY_SENDING}.
	 */
	byte dirty;
	
	/** This is a reference to the start of the {@link DataPacket} list. */
	private DataPacket firstPacket = null;

	/** This is a reference to the last sent message */
	private FloodRoutingMsg lastMessage;

    /** This field is to remember the data packet class whenever the {@link RoutingApplication#setBufferSize}
     * method is called. 
     */
    private Class dataPacketClass;
	
	/** 
	 * This defines the maximum number of data packets handled by this routing
	 * application. Keep it real low!
	 */
	protected int maxDataPacketNum;
	
	/** 
	 * This tells maximum how many data packets are there in a message.
	 * On a real mica2 mote the payload of a message is 29 at maximum, so keep
	 * it in mind when assigning value to this variable!
	 */
	protected int maxDataPerMsg;

	/** This field is used by the {@link FloodRouting} engine */
	RoutingApplication nextApp;
	
	/** 
	 * The driving policy determines the behavior of the routing application,
	 * for more details see {@link RoutingPolicy}. 
	 */
	protected RoutingPolicy policy;
	
	/** Points to the FloodRouting entity where this application is registered to */
	protected FloodRouting routingFramework;
	
	/** 
	 * This field here only serves performance purposes, this is only used by 
	 * the {@link RoutingApplication#selectData} method.
	 */
	private DataPacket[] selection;

	/**
	 * A parameterized constructor. <br> 
	 * WARNING! After constructing all the neccessary routing applications 
	 * don't forget to call the {@link FloodRouting#initialize} method, 
	 * otherwise the whole simulation will just not work as expected.
	 * 
	 * @param maxDataPerMsg the maximum number of data packets per message
	 * @param maxDataPacketNum the maximum number of data packets per application
	 * @param policy the routing policy
	 * @param framework the floodrouting application to which this routing application is attached to
	 * @param dataPacketClass the implementation class of the DataPacket abstract class
	 */
	public RoutingApplication(int maxDataPerMsg, int maxDataPacketNum, RoutingPolicy policy, FloodRouting framework, Class dataPacketClass)  throws Exception{
		this.maxDataPerMsg = maxDataPerMsg;
		this.dirty = DIRTY_CLEAN;
		this.maxDataPacketNum = maxDataPacketNum;
		this.policy = policy;
		this.selection = new DataPacket[maxDataPerMsg];
        this.dataPacketClass = dataPacketClass;
		routingFramework = framework;
		routingFramework.addRoutingApplication(this, maxDataPerMsg);
		
		for(int i = 0; i <  maxDataPacketNum; i++){
			DataPacket tempFirst = firstPacket;
			firstPacket = (DataPacket)dataPacketClass.newInstance();
			firstPacket.next = tempFirst;
		} 
	}
    
    /**
     * This method is called to modify the size of the buffer in a mote.
     * 
     * @param newBufferSize the new size of the internal buffer of a mote.
     */
    public void setBufferSize(int newBufferSize){
        this.maxDataPacketNum = newBufferSize;
        this.firstPacket = null;
        
        for(int i = 0; i <  maxDataPacketNum; i++){
            DataPacket tempFirst = firstPacket;
            try {
                firstPacket = (DataPacket)dataPacketClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            firstPacket.next = tempFirst;
        }
    }

    /**
     * Called inbetween two simulation runs. Clears the memory allocated to the 
     * application and resets the variables to their default value.
     */
    public void reset() throws Exception{
        selection = new DataPacket[maxDataPerMsg];
        dirty = DIRTY_CLEAN;
        
        if (firstPacket != null){
            Class dataPacketClass = firstPacket.getClass();
            firstPacket = null;
            for(int i = 0; i <  maxDataPacketNum; i++){
                DataPacket tempFirst = firstPacket;
                firstPacket = (DataPacket)dataPacketClass.newInstance();
                firstPacket.next = tempFirst;
            }
        }
    }

	/**
	 * This is called from {@link FloodRouting#age}, where the {@link FloodRouting}
	 * iterates through routing applications when the time comes. Calls the 
	 * {@link RoutingPolicy#age(int)} method for all the DataPackets present
	 * in the list of RoutingApplication.
	 */
	void age(){
		if( dirty != DIRTY_CLEAN){
			dirty = DIRTY_CLEAN;
			DataPacket tempPacket = firstPacket;

			while (tempPacket != null){
				if( tempPacket.priority != 0xFF ){
					tempPacket.priority = policy.age(tempPacket.priority);

					if( (tempPacket.priority & 0x01) == 0 )
						dirty = DIRTY_SENDING;
					else
						dirty |= DIRTY_AGING;
				}
				tempPacket = tempPacket.next;
			}
		}
	}

	/**
	 * Used to retrieve a DataPacket entity, to check if two DataPackets are
	 * identical the {@link DataPacket#equals} method is invoked. If there is
	 * no such entity this function returns the DataPacket with the highest 
	 * priority, meaning the less significant DataPacket in this case.
	 * 
	 * @param data the data we are looking for
	 * @return returns match or block with lowest priority (set to 0xFF)
	 */
	private DataPacket getDataPacket(DataPacket data){
		DataPacket tempPacket = firstPacket;
		DataPacket selected = firstPacket;
		
		while (tempPacket != null && !(tempPacket.equals(data) && tempPacket.priority != 0xFF)){
			if (tempPacket.priority > selected.priority)
				selected = tempPacket;
			tempPacket = tempPacket.next;
		}
		if (tempPacket != null){
            return tempPacket;
		}else{
			return selected;
		}
	}

	/**
	 * This is called from {@link FloodRouting#sendMsg} only if the state of this
	 * application is {@link RoutingApplication#DIRTY_SENDING}. Returning the 
	 * message it modifies the location parameter using the 
	 * {@link RoutingPolicy#getLocation()} method.
	 * 
	 * @param routingMessage returns a message with DataPackets ready to be sent
	 */
	void getMessage(FloodRoutingMsg routingMessage){
		DataPacket[] selection = selectData();
        routingMessage.empty = true;

        for (int i = 0; i < maxDataPerMsg; i++){
            routingMessage.data[i] = selection[maxDataPerMsg-1-i];
            if (selection[maxDataPerMsg-1-i] != null){
                routingMessage.empty = false;
            }
        }
        if (!routingMessage.empty){
            routingMessage.location = policy.getLocation();
            routingMessage.routingApp = this.getClass();
            lastMessage = routingMessage;
        }
	}

	/**
	 * Called from {@link RoutingApplication#receiveMessage}, the user who
	 * derives his class from this one has to handle this event overriding this
	 * function.
	 * 
	 * @param data the DataPacket received as a part of a message
	 */
	protected boolean receiveDataPacket(DataPacket data){
        return false;
    }

	/**
	 * Called when a message arrives for this routing application. It signals 
	 * this event further dividing it to {@link RoutingApplication#receiveDataPacket}
	 * "events".
	 * 
	 * @param message the message just received
	 */
	void receiveMessage(FloodRoutingMsg message){
		if( policy.accept(message.location) ){
			for(int i = 0; i < message.data.length; i++){
				DataPacket data = message.data[i];
				if (data != null){
					DataPacket tempPacket = getDataPacket(data);
                    if( !tempPacket.equals(data) || tempPacket.priority == 0xFF){
                        data.copyTo(tempPacket);
                        if (!receiveDataPacket(tempPacket)){
                            continue;
                        }
						tempPacket.priority = 0x00;
                        tempPacket.priority = policy.received(message.location, tempPacket.priority);
					}else{
                        tempPacket.priority = policy.received(message.location, tempPacket.priority);
                    }
				}
			}
			dirty = DIRTY_SENDING;
			routingFramework.sendMsg();
		}
	}

	/**
	 * This function selects DataPackets for sending.
	 * 
	 * @return returns an array of data DataPackets which are selected for sending
	 */
	private DataPacket[] selectData(){
		int maxPriority = 0xFF;
		int priority;

		for (int i = 0; i < maxDataPerMsg; i++){
			selection[i] = null;
		}

		DataPacket tempPacket = firstPacket;
		while (tempPacket != null){
			priority = tempPacket.priority;
			if( (priority & 0x01) == 0 && priority < maxPriority ){
				int j = 0;
                while ( j < maxDataPerMsg-1 && (selection[j+1] == null || priority < selection[j+1].priority)){
                    selection[j] = selection[j+1];
                    j++;
                }
				selection[j] = tempPacket;
                if (selection[0] != null){
                    maxPriority = selection[0].priority;
                }
			}
			tempPacket = tempPacket.next;
		}
		return selection;
	}

	/**
	 * This simply adds the DataPacket to the list of packets (if it is not
	 * already there) and tries to send it.  
	 * 
	 * @param data the DataPacket to be sent
     * @return true if the data was succesfully scheduled to be sent,
     *  false if the same data is already scheduled or if there is not enough
     *  buffer space to store the message.
	 */
	public final boolean sendDataPacket(DataPacket data) {
		DataPacket tempPacket = getDataPacket(data);
        if ( tempPacket.equals(data) && tempPacket.priority != 0xFF){
            return false;
        }else {
            data.copyTo(tempPacket);
            tempPacket.priority = 0x00;
            dirty = DIRTY_SENDING;
            routingFramework.sendMsg();
            return true;
        }
	}

	/**
	 * Processes the sendDone event, which happens when a message is sent. 
	 * This method is called from {@link FloodRouting#sendDone}. <br> 
     * IMPORTANT NOTE! Every user who derives his own class and overrides this 
     * method must start the implementation with a super.sendDone() call!
	 */
	protected void sendDone() {
		DataPacket tempPacket;
		for(int i = 0; i < lastMessage.data.length; i++){
			if (lastMessage.data[i] != null){
				tempPacket = getDataPacket(lastMessage.data[i]);
				if( tempPacket.equals(lastMessage.data[i]) ){
					tempPacket.priority = policy.sent(tempPacket.priority);
				}
			}
		}
        lastMessage = null;
	}
	
	/**
	 * This method is to display whatever information is necessary regarding
	 * this application.
	 *  
	 * @param disp
	 */
	protected void display(Display disp){
	}
    
    /**
     * this returns the {@link FloodRouting} application in which this routing
     * application works.
     */
    public FloodRouting getRoutingFramework() {
        return routingFramework;
    }

}
