/*
 * Copyright (c) 2002, Vanderbilt University
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
 * Author: Gyorgy Balogh, Gabor Pap, Miklos Maroti, Wilfried Elmenreich
 * Date last modified: 13/05/23
 */
package net.tinyos.prowler;

/**
 * This class represents a mote and all its properties important from the
 * simulation point of view. The MAC layer specific constant are all defined and
 * used here.
 * 
 * @author Gyorgy Balogh, Gabor Pap, Miklos Maroti
 */
public class Mica2Node extends Node{    
	/** 
	 * In this simulation not messages but references to motes are passed.
	 * All this means is that the Mica2Node has to hold the information on 
	 * the sender application which runs on this very mote.
	 */
	protected Application senderApplication = null;
	
    /** 
     * This application if not null will receive all the messages received by the 
     * Node. So this is a packet sniffer perfectly suitable for gathering
     * statistics on messages.
     */
    private SnifferIF listenerApplication = null;
    
	/**
	 * This node is the one that sent the last message or the one this node is
	 * receiving a message from right now. It is mainly used for display purposes, 
	 * as you know this information is not embedded into any TinyOS message. 
	 */
	protected Node parentNode = null;

	/**	
	 * This is the message being sent, on reception it is extracted and the 
	 * message part is forwarded to the appropriate application, see
	 * {@link Application#receiveMessage}. 
	 */
	protected Object    message   = null;

	////////////////////////////////
	// STATE VARIABLES
	////////////////////////////////

	/**	
	 * State variable, true if radio is in sending mode, this means it has a one
	 * message long buffer, which is full and the Node is trying to transmit its
	 * content. 
	 */
	protected boolean   sending          = false;
        
	/**	State variable, true if the radio is transmitting a message right now. */
	protected boolean   transmitting      = false;
        
	/** State variable, true if the radio is in receiving mode */
	protected boolean   receiving        = false;

	/** State variable, true if the last received message got corrupted by noise */
	protected boolean   corrupted        = false;
         
	/**	
	 * State variable, true if radio failed to transmit a message do to high 
	 * radio traffic, this means it has to retry it later, which is done 
	 * using the {@link Mica2Node#generateBackOffTime} function.
	 */
	protected boolean   sendingPostponed = false; 
 
	////////////////////////////////
	// MAC layer specific constants
	////////////////////////////////
    
	/** The constant component of the time spent waiting before a transmission. */
	public static int    sendMinWaitingTime            = 200;

	/** The variable component of the time spent waiting before a transmission. */
	public static int    sendRandomWaitingTime         = 128;

	/** The constant component of the backoff time. */
	public static int    sendMinBackOffTime            = 100;

	/** The variable component of the backoff time. */
	public static int    sendRandomBackOffTime         = 30;

	/** The time of one transmission in 1/{@link Simulator#ONE_SECOND} second. */
	public static int    sendTransmissionTime          = 2133;	//960;

    /** The strength of the radio signal emitted by the sender. */
    public double transmissionStrength          = 1.0;
 
	////////////////////////////////
	// EVENTS
	////////////////////////////////
	
	/** 
	 * Every mote has to test the radio traffic before transmitting a message, if 
	 * there is to much traffic this event remains a test and the mote repeats it
	 * later, if there is no significant traffic this event initiates message
	 * transmission and posts a {@link Mica2Node#EndTransmissionEvent} event.   
	 */
	private TestChannelEvent testChannelEvent = new TestChannelEvent();

	/** 
	 * Signals the end of a transmission.  
	 */
	private EndTransmissionEvent endTransmissionEvent = new EndTransmissionEvent();

	////////////////////////////////
	// Noise and signal
	////////////////////////////////
	
	/**	Signal stregth of transmitting or parent node.  */
	private double signalStrength = 0;

	/**	Noise generated by other nodes. */
	public double noiseStrength = 0;	

	/** 
	 * The constant self noise level. See either the {@link Mica2Node#calcSNR} or
	 * the {@link Mica2Node#isChannelFree} function.
	 */
	public double noiseVariance                 = 0.025;
    
	/** 
	 * The maximum noise level that is allowed on sending. This is actually
	 * a multiplicator of the {@link Mica2Node#noiseVariance}.
	 */    
	public double maxAllowedNoiseOnSending = 5;
    
	/** The minimum signal to noise ratio required to spot a message in the air. */
	public double receivingStartSNR             = 4.0;
    
	/** The maximum signal to noise ratio below which a message is marked corrupted. */
	public double corruptionSNR                 = 2.0;
	
	/**
	 * Inner class TestChannelEvent. Represents a test event, this happens when 
	 * the mote listens for radio traffic to decide about transmission.
	 */    

	class TestChannelEvent extends Event{
		
		/**
		 * If the radio channel is clear it begins the transmission process, 
		 * otherwise generates a backoff and restarts testing later.
		 * It also adds noise to the radio channel if the channel is free.
		 */
		public void execute(){
			if( isChannelFree( noiseStrength ) && !receiving){
				// start transmitting
				transmitting = true;
				beginTransmission(1, Mica2Node.this);
				endTransmissionEvent.time = time + sendTransmissionTime;
				simulator.addEvent( endTransmissionEvent );
			}else{
				// test again
				time += generateBackOffTime();
				simulator.addEvent( this );
			}
		}        
        
		public String toString(){
			return Long.toString(time) + "\tTestChannelEvent\t" + Mica2Node.this;
		}
	}
    
	/**
	 * Inner class EndTransmissionEvent. Represents the end of a transmission.
	 */
	class EndTransmissionEvent extends Event{
		/**
		 * Removes the noise generated by the transmission and sets the state 
		 * variables accordingly.
		 */
		public void execute(){  
            endTransmission();
            message = null; 
            transmitting = false;
            sending = false;
            senderApplication.sendMessageDone();            
		}
        
		public String toString(){
			return Long.toString(time) + "\tNode.EndTransmissionEvent\t" + Mica2Node.this;
		}        
	}
           
	/**
	 * Parameterized constructor, it set both the {@link Simulator} in which this mote
	 * exists and the {@link RadioModel} which is used by this mote.
	 * 
	 * @param sim the Simulator in which the mote exists
	 * @param radioModel the RadioModel used on this mote
	 */
	public Mica2Node(Simulator sim, RadioModel radioModel){
		super(sim, radioModel);
	}
	
	/**
	 * Calls the {@link Mica2Node#addNoise} method. See also {@link Node#receptionBegin}
	 * for more information.
	 */
	protected void receptionBegin(double strength, Object stream) {
		addNoise(strength, stream);
	}

	/**
	 * Calls the {@link Mica2Node#removeNoise} method. See also 
	 * {@link Node#receptionEnd} for more information.
	 */
	protected void receptionEnd(double strength, Object stream) {
		removeNoise(strength, stream);
	}

	/**
	 * Sends out a radio message. If the node is in receiving mode the sending 
	 * is postponed until the receive is finished. This method behaves exactly
	 * like the SendMsg.send command in TinyOS.
	 * 
	 * @param message the message to be sent
	 * @param app the application sending the message
	 * @return If the node is in sending state it returns false otherwise true.
	 */
	public boolean sendMessage( Object message, Application app){
		if( sending )
			return false;
		else{
			sending             = true;
			transmitting         = false;
			this.message = message;
			senderApplication = app;

			if( receiving ){
				sendingPostponed = true;
			}else{
				sendingPostponed = false;
				testChannelEvent.time = simulator.getSimulationTime() + generateWaitingTime();
				simulator.addEvent( testChannelEvent );
			}

            if (listenerApplication != null){
                listenerApplication.sendMessage(message);
            }

			return true;
		}
	}
 
	/** 
	 * Generates a waiting time, adding a random variable time to a constant 
	 * minimum.
	 * 
	 * @return returns the waiting time in milliseconds
	*/
	public static int generateWaitingTime(){
		return sendMinWaitingTime + (int)(Simulator.random.nextDouble() * sendRandomWaitingTime);        
	}

	/** 
	 * Generates a backoff time, adding a random variable time to a constant 
	 * minimum.
	 * 
	 * @return returns the backoff time in milliseconds
	*/
	protected static int generateBackOffTime(){
		return sendMinBackOffTime + (int)(Simulator.random.nextDouble() * sendRandomBackOffTime);        
	}

	/** 
	 * Tells if the transmitting media is free of transmissions based on the 
	 * noise level.
	 * 
	 * @param noiseStrength the level of noise right before transmission
	 * @return returns true if the channel is free
	*/
	protected boolean isChannelFree( double noiseStrength ){
		return noiseStrength < maxAllowedNoiseOnSending*noiseVariance; 
	}
    
	/** 
	 * Tells if the transmitting media is free of transmissions based on the 
	 * noise level.
	 * 
	 * @param signal the signal strength
	 * @param noise the noise level
	 * @return returns true if the message is corrupted
	*/
	public boolean isMessageCorrupted( double signal, double noise ){
		return calcSNR( signal, noise ) < corruptionSNR; 
	}

	/** 
	 * Inner function for calculating the signal noise ratio the following way: <br>
	 * signal / (noiseVariance + noise).
	 * 
	 * @param signal the signal strength
	 * @param noise the noise level
	 * @return returns the SNR
	*/
	protected double calcSNR( double signal, double noise ){
		return signal / (noiseVariance + noise);
	}

	/** 
	 * Tells if the incomming message signal is corrupted by another signal.
	 * 
	 * @param signal the signal strength of the incomming message
	 * @param noise the noise level
	 * @return returns true if the message is corrupted
	*/
	public boolean isReceivable( double signal, double noise ){
		return calcSNR( signal, noise ) > receivingStartSNR;
	}
    
    /**
     * Adds the noice generated by other motes, and breaks up a transmission
     * if the noise level is too high. Also checks if the noise is low enough to
     * hear incomming messages or not. 
     * 
     * @param level the level of noise
     * @param stream a reference to the incomming message
     */
    protected void addNoise(double level, Object stream ){
        if( receiving ){
            noiseStrength += level;
            if( isMessageCorrupted( signalStrength, noiseStrength ) ){
                corrupted = true;
                if (listenerApplication != null){
                    listenerApplication.corruptMessage(((Mica2Node)parentNode).message);
                }                
            }
            if (listenerApplication != null){
                listenerApplication.corruptMessage(((Mica2Node)stream).message);
            }                
        } else{
            if( !transmitting && isReceivable( level, noiseStrength) ){
                // start receiving
				parentNode = (Node)stream;
                receiving      = true;
                corrupted      = false;
                signalStrength = level;
            }else{
                noiseStrength += level;
            }
        }
    }
    
    /**
     * Removes the noise, if a transmission is over, though if the source is 
     * the sender of the message being transmitted there is some post processing
     * accordingly, the addressed application is notified about the incomming message.
     * 
     * @param stream a reference to the incomming messagethe incomming message
     * @param level the level of noise
     */
    protected void removeNoise( double level, Object stream ){
        if( parentNode == stream ){            
            receiving = false;
            if( !corrupted ){
				Application tempApp = getApplication(((Mica2Node)stream).senderApplication.getClass());
                tempApp.receiveMessage(((Mica2Node)stream).message, (Node)stream);
                
                if (listenerApplication != null){
                    listenerApplication.receiveMessage(((Mica2Node)stream).message, (Node)stream);
                }
            }
            signalStrength = 0;
            parentNode = null;
            if( sendingPostponed ){            
                sendingPostponed = false;
                testChannelEvent.time = simulator.getSimulationTime() + generateWaitingTime();
                simulator.addEvent( testChannelEvent );
            }
        }else{
            noiseStrength -= level;
        }       
    }

    /**
     * Returns true if the node is receiving a message. 
     */
    public boolean isReceiving() {
        return receiving;
    }

    /**
     * Returns true if the node is sending a message. This encapsulates the 
     * transmitting phase as this is a flag that shows if the message buffer is
     * full.
     */
    public boolean isSending() {
        return sending;
    }

    /**
     * Returns true if the message sending was postponed due to high radio traffic.
     */
    public boolean isSendingPostponed() {
        return sendingPostponed;
    }

    /**
     * Returns true if the node is in transmitting mode, if this flag is true then
     * the {@link Mica2Node} link is true as well.
     */
    public boolean isTransmitting() {
        return transmitting;
    }

    /**
     * Returns true if the last message got corrupted during the receiving process.
     */
    public boolean isCorrupted() {
        return corrupted;
    }

    /**
     * Returns the listener application which sniffs packages. 
     */
    public SnifferIF getListenerApplication() {
        return listenerApplication;
    }

    /**
     * Sets the listener application which will receive all the messages 
     * received by the node.
     */
    public void setListenerApplication(SnifferIF application) {
        listenerApplication = application;
    }

    /**
     * Removes the listener application.
     */
    public void removeListenerApplication() {
        listenerApplication = null;
    }
    
    /**
     * @return Returns the transmission strength for this node. This value is 
     * between 0 and 1.0 .
     */
    public double getTransmissionStrength() {
        return transmissionStrength;
    }

    /**
     * @param the new transmission strength to be used by this node.
     */
    public void setTransmissionStrength(double d) {
        transmissionStrength = d;
    }

}
