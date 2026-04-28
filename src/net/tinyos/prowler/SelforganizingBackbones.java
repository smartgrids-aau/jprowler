package net.tinyos.prowler;

import java.awt.Color;
import java.awt.Graphics;
import java.text.DecimalFormat;
import java.util.Random;
import java.lang.Math;

public class SelforganizingBackbones extends Application {

	static int nconnected, noveralltime;

	static Simulator sim;

	static Random rand = new Random(1234);

	static final int number_colors = 2;

	int[] counter = new int[number_colors];

	int code = 0;

	int msg;

	boolean decided = false;

	boolean wait4event = false;

	int decide_extensions = 8;

	int waiting_time = 50;

	int iterations = 4;

	boolean connected = false;

	//private String disptxt = "";

	static int decision_delay = 50; // in milliseconds

	static int METHOD = 3;

	private static int genVariableWaitTime() {

		int d;

		d = decision_delay - decision_delay / 4
				+ rand.nextInt(decision_delay / 2);
		return d * Simulator.ONE_SECOND / 1000;
	}

	event_occured myDecideEvent = new event_occured();

	public SelforganizingBackbones(Node node) {
		super(node);
	}

	public class event_occured extends Event {

		public void execute() {
			Mica2Node mNode = (Mica2Node) node;
			// int j=0;
			int minimum = 100;

			if (decided == false) {

				// find minimum != 0
				for (int i = 0; i < number_colors; i++) {
					if (counter[i] == 0) {
						if (decide_extensions > 0) {
							this.time = sim.getSimulationTime()
									+ genVariableWaitTime();
							decide_extensions--;
							sim.addEvent(this);
							return;
						}
					} else if (counter[i] < minimum)
						minimum = counter[i];
				}

				// count number of colors which have minimum
				int canditates = 0;
				for (int i = 0; i < number_colors; i++)
					if (counter[i] == minimum)
						canditates++;

				// select randomly one of the colors with minimum count

				int k = 0, r = 1 + rand.nextInt(canditates);
				for (int i = 0; i < number_colors; i++)
					if (counter[i] == minimum)
						if (++k == r) {
							code = i + 1;
							break;
						}
				/*
				 * int j=-1, r=rand.nextInt(canditates); for (; r >= 0; r--) {
				 * j++; while(counter[j] != minimum) j++; //skip colors having
				 * not minimum count } code = j+1;
				 */
				decided = true;
			}

			//disptxt = String.valueOf(code);
			msg = code + 1000 * mNode.id;
			sendMessage(String.valueOf(msg));
		}
	}

	public void receiveMessage(Object message, Node sender) {
		int receivedmsg = Integer.parseInt(message.toString()) % 1000;

		if (receivedmsg == 0)
			for (int j = 0; j < number_colors; j++)
				counter[j]++;
		else
			counter[receivedmsg - 1]++;

		if (connected == false) {
			boolean flag = true;
			for (int i = 0; i < number_colors; i++) {
				if (counter[i] <= 0)
					flag = false;
			}

			if (flag == true) {
				connected = true;
				nconnected++;
			}
		}

		if (decided == false) {
			if (receivedmsg == 0) {
				code = 1 + rand.nextInt(number_colors);
				msg = code + 1000 * node.id;
				sendMessage(String.valueOf(msg));
				decided = true;
			} else if (wait4event == false) {
				myDecideEvent.time = sim.getSimulationTime()
						+ genVariableWaitTime();
				sim.addEvent(myDecideEvent);
				wait4event = true;
			}
		}

		if (decided == true) {
			if (connected == false) {
				if (iterations > 0) {

					msg = code + 1000 * node.id;
					sendMessage(String.valueOf(msg));
					iterations--;
				}
			}
		}
	}

	public void display(Display disp) {
		Color[] nodecolor = new Color[] { Color.black, Color.magenta,
				Color.green, Color.orange, Color.gray, Color.cyan };

		Graphics g = disp.getGraphics();
		int x = disp.x2ScreenX(node.x);
		int y = disp.y2ScreenY(node.y);
		Mica2Node mNode = (Mica2Node) node;

		g.setColor(Color.black);

		if (mNode.noiseStrength > 0.0125) {
			g.setColor(Color.red);
			DecimalFormat df = new DecimalFormat("#.00");
			g.drawString(df.format(mNode.noiseStrength), x + 5, y + 10);
		}
		g.setColor(Color.black);
		// g.drawString(disptxt, x + 5, y);
		// g.drawString(counter1+":"+counter2, x + 5, y);

		/*
		 * if( sending ){ g.setColor( Color.blue ); }else if( receiving ){ if(
		 * corrupted ) g.setColor( Color.red ); else g.setColor( Color.green );
		 * }else{ if( sent ) g.setColor( Color.pink ); else g.setColor(
		 * Color.black ); }
		 */
		if (mNode.id == 1) {
			g.setColor(Color.blue);
			// g.fillOval(x - 5, y - 5, 9, 9);
			// g.setColor(Color.black);
			g.fillRect(x - 5, y - 5, 9, 9);
			return;
		}

		if (connected) {
			g.setColor(Color.black);
			g.drawRect(x - 5, y - 5, 9, 9);
			// g.fillOval( x-4, y-4, 7, 7 );
		}
		if (mNode.sending) {
			g.setColor(Color.red);
			g.fillOval(x - 4, y - 4, 7, 7);
		}

		g.setColor(nodecolor[code]);

		/*
		 * if (sent) { g.drawLine(x - 5, y - 5, x + 5, y + 5); g.drawLine(x + 5,
		 * y - 5, x - 5, y + 5); }
		 */

		g.fillOval(x - 3, y - 3, 5, 5);
		/*
		 * if( parent != null ){ g.setColor( Color.black ); int x1 =
		 * disp.x2ScreenX(parent.getX()); int y1 =
		 * disp.y2ScreenY(parent.getY()); g.drawLine(x,y,x1,y1); }
		 */
	}

	/**
	 * Starts up a simulator with a ROOT in the middle of a 300 by 300 meters
	 * field with 1000 motes and runs it in real time mode.
	 * 
	 * @param args
	 * @throws Exception
	 */
public static void main(String[] args) throws Exception {
		long time0 = System.currentTimeMillis();
		int nmotes = 100, fieldsize = 100, simruns = 1; 
		final boolean realTime = false, withDisplay = true, grid = false;
		
		//for (nmotes = 50; nmotes <= 350; nmotes+=50)
		//for (fieldsize = 100; fieldsize <= 350 ; fieldsize+=50)
		{
			nconnected = 0; // count number of connected nodes
			noveralltime = 0; // number of overall timer ticks
		DecimalFormat df = new DecimalFormat("0.0");
		
		float density;
		
		density = (float)nmotes / (fieldsize*fieldsize) * 30*30;
		
		System.out.println("Simulating (" + nmotes + " motes on a " + fieldsize
				+ "x" + fieldsize + " field, using method " + METHOD + ", d="+df.format(density)+")");
		// System.out.print("Thinking");
		for (int j = 1; j <= simruns; j++) {
			
			//if (simruns >= 10)
				// if (j % (simruns / 10) == 0)
					// System.out.print(".");
			sim = new Simulator();

			// creating the desired radio model, uncomment the one you need
			// RayleighRadioModel radioModel = new RayleighRadioModel(sim);
			GaussianRadioModel radioModel = new GaussianRadioModel(sim);
			
			Mica2Node root = (Mica2Node) sim.createNode(Mica2Node.class,
					radioModel, 1, fieldsize / 2, fieldsize / 2, 0);
			// root.visited = true;
			System.out.println("nline=");
			SelforganizingBackbones baseApp = new SelforganizingBackbones(root);
			baseApp.decided = true;
			// base node is always connected

			// creating all the other nodes
			
			if (grid) {
				  int k=2;
				  System.out.println("nline=");
				  int nline=(int)Math.sqrt(nmotes);
				  System.out.println("nline="+nline);
				  float step=(float)fieldsize/nline;
				  for (float x = step/2; x <= fieldsize; x+=step)
				     for (float y = step/2; y <= fieldsize; y+=step){
				        Mica2Node tempNode = (Mica2Node) sim.createNode(Mica2Node.class,
				              radioModel,k, x, y,0);
				        new SelforganizingBackbones(tempNode);
				        k=k+1;
				        } 
			} else {
				Node tempNode = sim.createNodes(Mica2Node.class, radioModel, 2,
						nmotes, fieldsize, 5);
				while (tempNode != null) {
					new SelforganizingBackbones(tempNode);
					tempNode = tempNode.nextNode;
				}				
			}
			
			

			// This call is a must, please do not forget to call it whenever the
			// mote field is set up

			radioModel.updateNeighborhoods();

			root.sendMessage("0", baseApp);

			if (withDisplay) {
				if (realTime)
					sim.runWithDisplayInRealTime();
				else
					sim.runWithDisplay();
			} else {
				if (realTime)
					System.out
							.println("Will not do real-time without display, switching to non-rt...");

				sim.run(20000);
			}

			while (!sim.endOfSimulation())
				System.out.println("waiting");
			// Thread.sleep(200);

			noveralltime += sim.getSimulationTimeInMillisec();
		} // next j

		
		System.out
				.println("("
						+ df
								.format((float) (System.currentTimeMillis() - time0) / 1000)
						+ " s)");
		float success = (float) nconnected / (nmotes * simruns);
		int success_pc = (int) (success * 100);

		System.out.print("Successfully connected nodes: " + success_pc + "%; ");
		System.out.println("Average algorithm running time: "
				+ df.format((float) noveralltime / (simruns * 1000)) + " s");
	} // for loop
	}}