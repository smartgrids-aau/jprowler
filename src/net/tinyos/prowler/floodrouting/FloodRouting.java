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

import net.tinyos.prowler.Application;
import net.tinyos.prowler.Display;
import net.tinyos.prowler.Event;
import net.tinyos.prowler.Node;
import net.tinyos.prowler.Simulator;

/**
 * This is the heart of this package. It demultiplexes the incomming 
 * {@link FloodRoutingMsg}s, it also has a timer to call the
 * {@link RoutingApplication#age} function periodically in the registered
 * RoutingApplications. <br>
 * Be aware that after creating the last RoutingApplication the 
 * {@link FloodRouting#initialize} has to be called explicitly!!!
 * 
 * @author Gabor Pap (gabor.pap@vanderbilt.edu)
 */
public class FloodRouting extends Application{
	
	/** one second timer */
	protected static int clockTickTime = Simulator.ONE_SECOND;
    
    /** the maximum time of processing on a mote, measured in 1/{@link Simulator#ONE_SECOND} second */
    protected static int maxProcessTime = (int) (Simulator.ONE_SECOND/1000);

	/** this is a flag which is true if a message is being sent */
	private boolean sending = false;
	
	/** 
	 * A reference to the last message, with this information at hand 
	 * it is easier to determine which routing application to notify after the\
	 * message is sent. 
	 */
	private FloodRoutingMsg routingMessage;

	/** 
	 * A reference to the list of routing applications. This list data structure
	 * is part of the applications, though it is managed by FloodRouting.
	 */
	private RoutingApplication firstRoutingApp = null; 
	
	/** this tells maximum how many data blocks are there in a message */
	private int maxDataPerMsg;

	/**
	 * Inner class ClockTickEvent. Represents a clocktick of the mote's internal
	 * clock. 
	 */
	class ClockTickEvent extends Event{
    	
    	public ClockTickEvent(int time){
    		super(time);
    	}
    	
		/**
		 * Calls the clocktick function of the mote, and makes itself happen again a clockcycle later.
		 */   
		public void execute(){
			// add next tick event
			time += clockTickTime;
			getNode().getSimulator().addEvent( this );
			age();            
		}
        
		public String toString(){
			return Long.toString(time) + "\tFloodRouting.ClockTickEvent\t" + FloodRouting.this;
		}
	}

    /**
     * Inner class SendMsgEvent. The function of this event is to make the  
     * processing of incomming messages async, and assign a random time to it.
     * So this way the incomming message is processed immediately, but the message
     * sending is delayed somewhat...  
     */    
    class SendMsgEvent extends Event{
        
        public SendMsgEvent(long time){
            super(time);
        }
        
        /**
         * Loads a message in the message buffer if the buffer is free, and there
         * is a message to be sent.
         */
        public void execute(){
            if (!sending){
                RoutingApplication tempApp = firstRoutingApp;
        
                while( tempApp != null){
                    if( tempApp.dirty == RoutingApplication.DIRTY_SENDING ){
                        tempApp.getMessage(routingMessage);
                        if( !routingMessage.empty){
                            sending = true;
                            if (!sendMessage(routingMessage)){
                                sending = false;
                            }
                            return;
                        }
                        tempApp.dirty = RoutingApplication.DIRTY_AGING;
                    }
                    tempApp = tempApp.nextApp;
                }
            }
        }        
    }

	/**
	 * Parameterized constructor. Besides calling the parent's constructor it
	 * starts a one second timer with a random offset;
	 * 
	 * @param node a reference to the Node object on which this Application
	 * runs
	 */
	public FloodRouting(Node node){
		super(node);
	}
	
	/**
	 * When all the {@link RoutingApplication}s are created this has to be called.
	 * Every RoutingApplication registers itself in a given FloodRouting, stating
	 * the maximum number of datapackets needed in a message, so when all of 
	 * them are created then this method creates only one {@link FloodRoutingMsg}
	 * with the overall maximum. 
	 */
	public void initialize(){
		routingMessage = new FloodRoutingMsg(maxDataPerMsg);
        getNode().getSimulator().addEvent( new FloodRouting.ClockTickEvent((int)(Simulator.random.nextDouble() * clockTickTime)) );
	}

	/**
	 * This demultiplexes the incomming messages to the appropriate routing
	 * application. This function also triggers a new message sending. 
	 */
	public void receiveMessage(Object message, Node sender) {
		super.receiveMessage(message,sender);
		FloodRoutingMsg msg = (FloodRoutingMsg)message;
        RoutingApplication  app = getRoutingApplication(msg.routingApp);
		
		if( app != null ){
			app.receiveMessage(msg);
		}
	}

	/**
	 * Signals the sendDone event to the appropriate routing application.
	 */
	public void sendMessageDone() {
		RoutingApplication app = getRoutingApplication(routingMessage.routingApp);
		if (app != null){
			app.sendDone();
		}
		sending = false;
        routingMessage.routingApp = null;
		sendMsg();
	}
	
	/**
	 * When called this iterates through all the routing applications
	 * indicating an aging process in all of them. 
	 */
	void age(){
		RoutingApplication tempApp = firstRoutingApp;
		
		while( tempApp != null){
			tempApp.age();
			if( tempApp.dirty == RoutingApplication.DIRTY_SENDING){
				sendMsg();
			}
			tempApp = tempApp.nextApp;
		}
	}
	
	/**
	 * This function selects a ready to send routing application and sends a 
	 * message full of its data packets. If a routing application has a data
	 * packet, it has to call this function!
	 */
	public void sendMsg(){
        long timeOfEvent = getNode().getSimulator().getSimulationTime() + (int)(Simulator.random.nextDouble()*maxProcessTime); 
        SendMsgEvent sendMsgEvent = this.new SendMsgEvent(timeOfEvent);
        getNode().getSimulator().addEvent(sendMsgEvent);
	}

	/**
	 * Returns the routing application whose class is identical with the parameter.
	 * WARNING! We asume that there is only one RoutingApplication Object
	 * per RoutingApplication subclass.
	 * 
	 * @param appClass the type of application we are looking for
	 * @return returns the routing application instance or null if not found 
	 */
	public RoutingApplication getRoutingApplication(@SuppressWarnings("rawtypes") Class appClass){
		RoutingApplication tempApp = firstRoutingApp;
		while (tempApp != null && tempApp.getClass() != appClass )
			tempApp = tempApp.nextApp;
		return tempApp;
	}

    /**
     * Removes the routing application whose class is identical with the parameter.
     * WARNING! We asume that there is only one RoutingApplication Object
     * per RoutingApplication subclass.
     * 
     * @param appClass the type of application we are looking for
     */
    public void removeRoutingApplication(@SuppressWarnings("rawtypes") Class appClass){
        RoutingApplication tempApp = firstRoutingApp;
        RoutingApplication prevApp = null;
        while (tempApp != null && tempApp.getClass() != appClass ){
            prevApp = tempApp;
            tempApp = tempApp.nextApp;
        }
        if (tempApp != null){
            if (prevApp != null){
                prevApp.nextApp = tempApp.nextApp;
            }else{
                firstRoutingApp = tempApp.nextApp;
            }
        }    
    }

	/**
	 * Adds or we can also say wires a routing application to the mote's
	 * routing application framework. 
	 * 
	 * @param app the routing application
	 */
	public void addRoutingApplication(RoutingApplication app, int maxDataPerMsg){
		app.nextApp = firstRoutingApp;
		firstRoutingApp = app;
		if (maxDataPerMsg > this.maxDataPerMsg)
			this.maxDataPerMsg = maxDataPerMsg;
	}
	
	/**
	 * This calls the display method for all the linked {@link RoutingApplication}s.
	 *  
	 * @param disp
	 */
	public void display(Display disp){
		RoutingApplication tempApp = firstRoutingApp;
		while (tempApp != null){
			tempApp.display(disp);
			tempApp = tempApp.nextApp;
		}
	}
}
