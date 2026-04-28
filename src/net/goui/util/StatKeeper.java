/*
 * Copyright (C) 2009 Istvan Fehervari, Wilfried Elmenreich
 * Original project page: http://www.frevotool.tk
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License Version 3 as published
 * by the Free Software Foundation http://www.gnu.org/licenses/gpl-3.0.txt
 *
 * There is no warranty for this free software. The GPL requires that 
 * modified versions be marked as changed, so that their problems will
 * not be attributed erroneously to authors of previous versions.
 */
package net.goui.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;

public class StatKeeper implements Comparable<StatKeeper> {
	private double sum = 0.0;
	private double sum2 = 0.0;
	int n = 0;
	double min = Double.MAX_VALUE;
	double max = -Double.MAX_VALUE;
	int countHit0 = 0;
	private boolean recordValues = false;
	private String statName;	//used for labeling column headings
	boolean touched = false;
	
	ArrayList<Double> values = new ArrayList<Double>();
	
	public StatKeeper() {
		this(false,"untitled"); // call the long constructor
	}

	/**
	 * Creates a new StatKeeper object.
	 * @param recordValues defines if values should be recorded, otherwise just a statistic of min, max, mean, sdev is kept
	 * @param statName gives the value a name which is used for labeling the columns in the exported csv
	 * @param addToNotableStats if true, this stat is added to a list of notable stats which can be exported to a csv together 
	 */
	public StatKeeper(boolean recordValues, String statName) {
		this.recordValues = recordValues;
		this.statName = statName;
	}
	
	/** Returns the number of elements */
	public int elementNumber() {
		return values.size();
	}
	
	/** Returns the given element from the series */
	public double getElement(int e) {
		return values.get(e);
	}

	public void add(double x) {
		touched=true;
		if (x == 0.0)
			countHit0++;
		if (x < min)
			min = x;
		if (x > max)
			max = x;
		sum += x;
		sum2 += x * x;
		n++;
		if (recordValues) {
			values.add(x);
		}
	}

	public double mean() {
		if (n > 0)
			return sum / n;
		else
			return 0.0;
	}

	public double sdev() {
		if (n > 0)
			return Math.sqrt((n * sum2 - sum * sum) / (n * (n - 1)));
		else
			return 0.0;
	}
	
	public void clear() {
		sum = 0.0;
		sum2 = 0.0;
		n = 0;
		min = Double.MAX_VALUE;
		max = -Double.MAX_VALUE;
		countHit0 = 0;
	}

	public String result(boolean wHitrate) {
		DecimalFormat fm = new DecimalFormat("0.0000");
		String smin, smax;
		if (min == Double.MAX_VALUE)
			smin = "--";
		else
			smin = fm.format(min);
		if (max == -Double.MAX_VALUE)
			smax = "--";
		else
			smax = fm.format(max);
		if (wHitrate)
			return fm.format(mean()) + " " + n + " " + smin + " " + smax + " "
					+ fm.format(sdev()) + " "
					+ fm.format(countHit0 / (double) n);
		else
			return fm.format(mean()) + " " + n + " " + smin + " " + smax + " "
					+ fm.format(sdev());
	}
	
	/**
	 * Opens a file for writing and returns the FileOutputStream
	 * If the file exists and cannot be overwritten (locked by another application or read-only),
	 * an alternative filename with the same file extension containing "(x)", where x is the 
	 * number of the trial starting at 2, is generated.
	 * 
	 * @param the intendedFilename, which is tried first
	 * @return the FileOutputStream of the opened file
	 */
	private static FileOutputStream openFile(String intendedFilename) {
		String filename,filebody,extension;
		final int maxTrials=3;
		FileOutputStream fos=null;

		int extPos=intendedFilename.lastIndexOf('.');

		if (extPos==-1) {
			extension="";
			filebody=intendedFilename;
		}
		else {
			extension = intendedFilename.substring(extPos, intendedFilename.length());
			filebody=intendedFilename.substring(0,extPos);
		}

		filename=intendedFilename;

		for(int trial=1; trial <= maxTrials+1;trial++)
			try {
				fos = new FileOutputStream(filename);
				break;
			} catch (FileNotFoundException e) {
				//If the file cannot be overwritten, we get an exception, adjust filename in this case
				if (trial<maxTrials) {
					filename=filebody+" ("+(trial+1)+")"+extension;
					System.err.println("Cannot write recorded values to disk, adjusting filename to '"+filename+"'!");
				}
				else {
					System.err.println("Cannot write recorded values to disk - given up after "+maxTrials+" trials!");
					e.printStackTrace();
					return null;
				}
			}	
		return fos;
	}
	
	/**
	 * Saves the current statistic into a file 
	 * @param intendedFilename, the filename might be changed if the file is busy
	 */
	public void saveResults(String intendedFilename) {
		FileOutputStream fos=openFile(intendedFilename);
		
		try {
			PrintStream ps = new PrintStream(fos);
			PrintStream out = System.out;
			System.setOut(ps);
			dumpValues();
			System.setOut(out);
			fos.close();
		} catch (IOException e) {
			System.err.println("Cannot write recorded values to disk!");
			e.printStackTrace();
		}
	}

	private void dumpValues() {
		DecimalFormat fm = new DecimalFormat("0.0000");

		if (statName!=null) System.out.println(";"+statName);
		
		if (recordValues==false) {
			System.out.println("mean;"+mean());
			System.out.println("n;"+n);
			System.out.println("sdev;"+sdev());
		}
		else	
		for (int i = 0; i < values.size(); i++)
			System.out.println(fm.format(values.get(i)));
	}
	
	static void dumpMultiValues(ArrayList<StatKeeper> statList, boolean addStatColumns) {
		DecimalFormat fm = new DecimalFormat("0.0000");
		
		//write header line
		String lastName=null, sep="";
		int n=0;
		double sum=0.0;
		
		for(StatKeeper s:statList) {
			int pos=s.statName.indexOf('.');
			if (pos == -1) pos=s.statName.length();
			String currentName=s.statName.substring(0, pos);
			if ((lastName!=null) && addStatColumns)
				if (lastName.compareTo(currentName)!=0) {
					if (n>1) System.out.print(";AVG."+lastName);
					n=0;
				}
			lastName=currentName;
			System.out.print(sep+s.statName);
			sep=";";
			n++;
		}
		if (addStatColumns && (n>1))
			System.out.print(";AVG."+lastName);
		
		System.out.println();
		
		//write columns
		for(int i=0; i<statList.get(0).n; i++) {
			lastName=null;
			sep="";
			n=0;
			sum=0.0;
			for(StatKeeper s:statList) {
				int pos=s.statName.indexOf('.');
				if (pos == -1) pos=s.statName.length();
				String currentName=s.statName.substring(0, pos);
				if ((lastName!=null) && addStatColumns)
					if (lastName.compareTo(currentName)!=0) {
						if (n>1) System.out.print(";"+fm.format(sum/n));
						n=0;
						sum=0.0;
					}
				lastName=currentName;
				System.out.print(sep);
				sep=";";
				if (i < s.values.size()) {
					System.out.print(fm.format(s.values.get(i)));
					n++;
					sum+=s.values.get(i);
				}
				
			}
			if (addStatColumns && (n>1))
				System.out.print(";"+fm.format(sum/n));
			System.out.println();		
		}
	}	
	
	static void dumpRBoxplotFile(ArrayList<StatKeeper> statList, int step) {
		
		//make sure that the decimal separator is a dot
		DecimalFormatSymbols mySymbols = new DecimalFormatSymbols();
		mySymbols.setDecimalSeparator('.');
		DecimalFormat fm = new DecimalFormat("0.0000",mySymbols);
		
		for(StatKeeper s:statList) {
			String sep="";
			if (s.statName.contains("eneration")) continue;
			for(int i=0; i<s.values.size(); i+=step) {
				System.out.print(sep+fm.format(s.values.get(i)));
				sep="\t";
			}
			System.out.println();
		}
	}
	
	public static void saveNotableStats(String filename, ArrayList<StatKeeper> statList) {
		saveNotableStats(filename,statList,false);
	}
	
	public static void saveNotableStats(String filename, ArrayList<StatKeeper> statList, boolean addStatColumns) {
		//sort according to names
		Collections.sort(statList);
		
		FileOutputStream fos=openFile(filename);
		
		try {
			PrintStream ps = new PrintStream(fos);
			PrintStream out = System.out;
			System.setOut(ps);
			dumpMultiValues(statList, addStatColumns);
			System.setOut(out);
			fos.close();
		} catch (IOException e) {
			System.err.println("Cannot write recorded values to disk!");
			e.printStackTrace();
		}
	}
	
	/**
	 * creates a text file in a format to be read by the statistical program R
	 * the stat with the generation count is omitted
	 * @param filename filename and path of the file to be created
	 * @param statList an ArrayList of statkeeper objects
	 * @param step only every step-th generation is saved, this is useful to keep a comprehensible boxplot
	 */
	public static void saveRboxFile(String filename, ArrayList<StatKeeper> statList, int step) {
		FileOutputStream fos=openFile(filename);
		
		try {
			PrintStream ps = new PrintStream(fos);
			PrintStream out = System.out;
			System.setOut(ps);
			dumpRBoxplotFile(statList, 5);
			System.setOut(out);
			fos.close();
		} catch (IOException e) {
			System.err.println("Cannot write recorded values to disk!");
			e.printStackTrace();
		}
	}

	public int compareTo(StatKeeper o) {
//		int pos=s.statName.indexOf('.');
//		if (pos == -1) pos=s.statName.length();
//		String currentName=s.statName.substring(0, pos);		
		
		return statName.compareTo(o.getStatName());
	}

	public String getStatName() {
		return statName;
	}
	
	/**
	 * @return the minimum of the series
	 */
	public double getMin() {
		return min;
	}

	/**
	 * @return the maximum of the series
	 */
	public double getMax() {
		return max;
	}

}
