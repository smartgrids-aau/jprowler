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
 * Author: Gyorgy Balogh, Gabor Pap, Miklos Maroti
 * Date last modified: 02/09/04
 */
 
package net.tinyos.prowler;



/**
 * This radio model uses the assumption that nodes are moving very often. 
 * It first calculates the ideal radio strength when the 
 * {@link RadioModel#updateNeighborhoods} is called, then it modulates it on a 
 * timely basis (see {@link RayleighRadioModel#COHERENCE_TIME}) using an 
 * exponential distribution to multiply. 
 * 
 * @author Gabor Pap, Gyorgy Balogh, Miklos Maroti
 */
public class RayleighRadioModel extends RadioModel {
	/** 
	 * The simulator, it is needed to access nodes, so that the radio model can
	 * do operations on them.
	 */
	private Simulator sim = null;

	/**
	 * This defines the period after which the dynamic fading is recalculated
	 * for the nodes in the neighborhood. 
	 */
	public static int COHERENCE_TIME = Simulator.ONE_SECOND;
	
	/**
	 * We need to have a clock event so that we can use the 
	 * {@link Simulator#getLastEventTime} method. This variable defines the 
	 * frequency of the timer.
	 */
	private long clockTickTime = Simulator.ONE_SECOND / 100;	

	/**
	 * The exhibitor of the radio signal degradation, typically it is 2/2 if the
	 * motes are well above ground, though at ground level this factor is 
	 * closer to 3/2. For efficiency reasons, this number is the half of the
	 * usually defined falling factor.
	 */ 
	public double fallingFactorHalf = 1.1;
    
	/** 
	 * limits the number of neighbours used to calculate interference ratio
	 */
	public double radioStrengthCutoff = 0.2/6;
	
	/**
	 * Parameterized constructor, besides setting the simulator it also adds its
	 * clock event to it and sets the updateTime to a default value back in the 
	 * past so that it is reset at the first transmission.
	 * 
	 * @param sim
	 */
	public RayleighRadioModel(Simulator sim){
		this.sim = sim;
		sim.addEvent( new RayleighRadioModel.ClockTickEvent((int)(Simulator.random.nextDouble() * clockTickTime)) );		
	}

	/**
	 * Calculates the ideal radio strength between two nodes.
	 * 
	 * @return Returns the ideal radio strength
	 */
	protected double getStaticFading(Node sender, Node receiver){
		return sender.getMaximumRadioStrength() / (1.0 + Math.pow(sender.getDistanceSquare(receiver), fallingFactorHalf));
	}

	/**
	 * Calculates the received radio signal strength using an exponential 
	 * distribution coefficient the signal strength multiplicator used and the 
	 * previously calculated ideal strength.
	 * 
	 * @param signalStrength the signal strength of the sender. These numbers shall 
     * be in the <code>[0,1]</code> interval.
	 * @param staticFading the static fading as returned by {@link #getStaticFading}.
	 * @return The signal strength at the receiver.
	 */
	protected double getDynamicStrength(double signalStrength, double staticFading){
		double gaussian1 = Simulator.random.nextGaussian();
		double gaussian2 = Simulator.random.nextGaussian();
		return signalStrength * staticFading * (gaussian1*gaussian1+gaussian2*gaussian2) / 2;
	}

	/**
	 * This is a factory method for creating radio model specific neigborhoods.
	 */
    protected  RadioModel.Neighborhood createNeighborhood() {
		return new Neighborhood();
    }

	/**
	 * (Re)calculates the neighborhoods of every node in the network. 
	 * This operation should be called whenever the location of the nodes 
	 * changed. This operation is extremely expensive and should be used sparsely.
	 */
    public void updateNeighborhoods() {              
        // count nodes
        int nodeNum = 0;
        Node node1 = sim.firstNode;
        while (node1 != null){
            node1 = node1.nextNode;
            ++nodeNum;
        }
    
        Node[] neighbors = new Node[nodeNum];
        double[] staticFadings = new double[nodeNum];
                    
        node1 = sim.firstNode;
        while (node1 != null){
            int i = 0;
            Node node2 = sim.firstNode;
            
            while (node2 != null){
                double staticRadioStrength = getStaticFading(node1, node2);
                if( staticRadioStrength >= radioStrengthCutoff && node1 != node2){
                    neighbors[i] = node2;
                    staticFadings[i] = staticRadioStrength;                    
                    i++;
                }
                node2 = node2.nextNode;
            }
        
            Neighborhood neighborhood = (Neighborhood)node1.getNeighborhood();
            neighborhood.neighbors = new Node[i];
            System.arraycopy(neighbors, 0, neighborhood.neighbors, 0, i );           
            neighborhood.staticFadings = new double[i];
            System.arraycopy(staticFadings, 0, neighborhood.staticFadings, 0, i );
            neighborhood.dynamicStrengths = new double[i];         
            node1 = node1.nextNode;
        }       
    }
	/**
	 * This class stores all the node related data the RayleighRadioModel needs, 
	 * this includes an array of neighboring notes, the static fading and 
	 * the dynamic strentgh as well for every neighboring nodes plus the entity
	 * being transmitted by the node. 
	 */
	protected class Neighborhood extends RadioModel.Neighborhood{
		
		/** The vector of the neighboring nodes. */
		protected Node[] neighbors;
        
        /** The last time this node was updated, see {@link RayleighRadioModel#COHERENCE_TIME}.*/
        protected long updateTime = 0;
		 
		/**
		 * The vector of static fading factors. These numbers shall 
		 * be in the <code>[0,1]</code> interval.
		 */
		protected double[] staticFadings;

		/**
		 * This vector holds the signal strength values that we 
		 * contributed to the neighbours in the last transmission. 
		 * We must keep these values because we have to use the 
		 * same strengths for the matching {@link Node#receptionEnd} call. 
		 */
		protected double[] dynamicStrengths;

		/**
		 * Contains the stream object during an active transmission,
		 * or <code>null</code> if we do not transmit.
		 */
		protected Object stream = null;
        
        public Neighborhood() { 
            updateTime = sim.getSimulationTime()-COHERENCE_TIME;
        }

		/**
		 * Calculates the dynamic signal strength based on the
		 * static fading factors and a per-transmission dynamic 
		 * random factor. Then calls the {@link Node#receptionBegin}
		 * method on all neighbors.
		 */
        
        
		protected void beginTransmission(double strength, Object stream){
			if( stream == null )
				throw new IllegalArgumentException("The stream object must be non-null");
			else if( this.stream != null )
			{
				throw new IllegalStateException("No nested transmissions are allowed - time: "+sim.getSimulationTime());
			}
			
			this.stream = stream;
			
			boolean recalculate = false;
			long lastEventTime = sim.getSimulationTime();
			if (lastEventTime > updateTime + COHERENCE_TIME){
		    	//System.out.println("CHANNEL UPDATE");
				updateTime = lastEventTime;
				recalculate = true;
			}
			
			int i = neighbors.length;
			while( --i >= 0 ){
				double dynamicStrength;
				if (recalculate){
					dynamicStrength = getDynamicStrength(strength, staticFadings[i]);
					dynamicStrengths[i] = dynamicStrength;
				} else{
					dynamicStrength = dynamicStrengths[i];
				}
				neighbors[i].receptionBegin(dynamicStrength, stream);
			}
		}
		
		/**
		 * Calls the {@link Node#receptionEnd} method on all neighboring nodes.
		 * This method should always be called as the pair of the 
		 * {@link RadioModel.Neighborhood#beginTransmission} method. 
		 */
		protected void endTransmission(){
			int i = neighbors.length;
			while( --i >= 0 )
				neighbors[i].receptionEnd(dynamicStrengths[i], stream);
				
			stream = null;
		}
	}

	/**
	 * Inner class ClockTickEvent. Represents a given time resolution needed.
	 */
	public class ClockTickEvent extends Event{
    	
		public ClockTickEvent(int time){
			super(time);
		}
    	
		/**
		 * Calls the clocktick function of the mote, and makes itself happen again a clockcycle later.
		 */   
		public void execute(){
			// add next tick event
			time += clockTickTime;
			sim.addEvent( this );
		}
        
		public String toString(){
			return Long.toString(time) + "\tRayleighRadioModel.ClockTickEvent\t" + RayleighRadioModel.this;
		}
	}
}
