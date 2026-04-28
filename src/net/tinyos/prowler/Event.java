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
 * This class represents an event in the simulator. This is a base class from 
 * which concrete events has to be derived. The derived event classes should/can
 * be inner classes of Node classes so they can access all friendly member variables
 * and methods of the Node. Use static Event objects and reuse them to speed up 
 * the simulation.
 * 
 * @author Gyorgy Balogh, Gabor Pap, Miklos Maroti
 */
public class Event implements Comparable{

	/** the time of the event */
    protected long time;

	/** a serial number that should be increased for each new event */
    static protected long serialCounter=0;
    
    /** the serial number of this event */
    protected long serialNumber=0;
    
    /** 
     * Basic constructor, sets the time property to zero
     */
    public Event(){       
        time = 0;
    }

	/** 
	 * Parameterized constructor.
	 * 
	 * @param time the time of the event
	 */
    public Event( long time ){
        this.time = time;
        this.serialNumber = serialCounter++;
    }
	
	/** 
	 * This function is called when the event occurs. If you want to have a clock
	 * event for example, the execute function may look like this: <br>
	 * public void execute(){ <br>
	 * &nbsp;&nbsp; //here you can call whatever function you want
	 * &nbsp;&nbsp; time += clockTickTime;<br>
	 * &nbsp;&nbsp; sim.addEvent( this );<br>
	 * }<br> 
	 */
    public void execute(){
    }
    
	/**
	 * @return returns the time of the event as a String
	 */ 
    public String toString(){
        return Long.toString(time);        
    }

	/**
	 * This makes earlier events happen earlier :) Do not override this method. 
	 */  
    public final int compareTo(Object arg0){
        long arg_time = ((Event)arg0).time;
        if( arg_time < time )
            return 1;
        else if( arg_time > time )
            return -1;
        else
        	//any two events must be different in order to allow events with same time
        	//if they are at the same time, the event is identified by its serial number
        	return (int)(serialNumber - ((Event)arg0).serialNumber);
    }
}