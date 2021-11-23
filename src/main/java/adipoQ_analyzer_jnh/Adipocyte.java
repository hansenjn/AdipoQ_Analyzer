package adipoQ_analyzer_jnh;

/** ===============================================================================
* AdipoQ Analyzer Version 0.0.4
* 
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*  
* See the GNU General Public License for more details.
*  
* Copyright (C) Jan Niklas Hansen
* Date: January 12, 2021 (This Version: January 19, 2021)
*   
* For any questions please feel free to contact me (jan.hansen@uni-bonn.de).
* =============================================================================== */

import java.util.ArrayList;
import java.util.Arrays;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.RoiEnlarger;


class Adipocyte{
	int voxelNumber [];	// time
	private int xySurface [];	// time
	private int xzyzSurface [];	// time
	double centerX []; // time
	double centerY []; // time
	double centerZ [];	// time
	double averageIntensity [][];	// time, channels
	double integratedIntensity [][];	// time, channels
	double sdIntensity [][];	// time, channels
	double medianIntensity [][];	// time, channels
	double minIntensity [][];	// time, channels
	double maxIntensity [][];	// time, channels

	int voxelNumberSurr [];	// time
	double averageIntensitySurr [][];	// time, channels
	double integratedIntensitySurr [][];	// time, channels
	double sdIntensitySurr [][];	// time, channels
	double medianIntensitySurr [][];	// time, channels
	double minIntensitySurr [][];	// time, channels
	double maxIntensitySurr [][];	// time, channels
	double min25pIntensitySurr [][];	// time, channels
	double max25pIntensitySurr [][];	// time, channels
	double min5pIntensitySurr [][];	// time, channels
	double max5pIntensitySurr [][];	// time, channels
	
	/**
	 * pz >= 0 && pz < number of slices
	 * pt >= 0 && pt < number of frames
	 * 1 <= maskC <= imp.getNChannels()
	 * 
	 * For the maskC channel, no Surr parameters are determined!
	 * 
	 * U
	 * */
	public Adipocyte(ArrayList<AdipoPoint> points, ImagePlus imp, int maskC, boolean Surr, double refDist, Roi roi){
		initializeArrays(imp.getNFrames(), imp.getNChannels(), Surr, maskC);
		determineIntensityParams(points, imp);
		
		// Quantify Crown Like Structures
		if(Surr) {
			if(imp.getNSlices()==1 && imp.getNFrames()==1) {
				quantifyCrownLikeStructuresIn2DStatic(imp,maskC,refDist,roi);
			}else {
				quantifyCrownLikeStructuresIn3DTimelapse(points,imp,maskC,refDist);				
			}
			System.gc();
		}
	}
	
	/**
	 * pz >= 0 && pz < number of slices
	 * pt >= 0 && pt < number of frames
	 * 1 <= maskC <= imp.getNChannels()
	 * 
	 * For the maskC channel, no Surr parameters are determined!
	 * 
	 * Uses a 3D and/or timelapse implementation to quantify Crown-Like structures
	 * */
	public Adipocyte(ArrayList<AdipoPoint> points, ImagePlus imp, int maskC, boolean Surr, double refDist){
		initializeArrays(imp.getNFrames(), imp.getNChannels(), Surr, maskC);
		determineIntensityParams(points, imp);
		
		// Quantify Crown Like Structures
		if(Surr) {
			quantifyCrownLikeStructuresIn3DTimelapse(points,imp,maskC,refDist);			
			System.gc();
		}
	}
	
	private void initializeArrays(int nFrames, int nChannels, boolean Surr, int maskC) {
		voxelNumber = new int [nFrames];
		xySurface = new int [nFrames];
		xzyzSurface = new int [nFrames];
		centerX = new double [nFrames];
		centerY = new double [nFrames];
		centerZ = new double [nFrames];
		averageIntensity  = new double [nFrames][nChannels];	// time, channels
		integratedIntensity  = new double [nFrames][nChannels];	// time, channels
		medianIntensity  = new double [nFrames][nChannels];	// time, channels
		sdIntensity  = new double [nFrames][nChannels];	// time, channels
		minIntensity  = new double [nFrames][nChannels];	// time, channels
		maxIntensity  = new double [nFrames][nChannels];	// time, channels

		if(Surr) {
			voxelNumberSurr = new int [nFrames];
			voxelNumberSurr = new int [nFrames];
			averageIntensitySurr  = new double [nFrames][nChannels];	// time, channels
			integratedIntensitySurr  = new double [nFrames][nChannels];	// time, channels
			medianIntensitySurr  = new double [nFrames][nChannels];	// time, channels
			sdIntensitySurr  = new double [nFrames][nChannels];	// time, channels
			minIntensitySurr  = new double [nFrames][nChannels];	// time, channels
			maxIntensitySurr  = new double [nFrames][nChannels];	// time, channels
			min25pIntensitySurr  = new double [nFrames][nChannels];	// time, channels
			max25pIntensitySurr  = new double [nFrames][nChannels];	// time, channels
			min5pIntensitySurr  = new double [nFrames][nChannels];	// time, channels
			max5pIntensitySurr  = new double [nFrames][nChannels];	// time, channels
			
		}
		
		Arrays.fill(voxelNumber, 0);
		Arrays.fill(xySurface, 0);
		Arrays.fill(xzyzSurface, 0);
		Arrays.fill(centerX, 0.0);
		Arrays.fill(centerY, 0.0);
		Arrays.fill(centerZ, 0.0);
		if(Surr) {
			Arrays.fill(voxelNumberSurr, 0);
		}
		for(int t = 0; t < nFrames; t++) {
			Arrays.fill(averageIntensity [t], 0.0);
			Arrays.fill(integratedIntensity [t], 0.0);
			Arrays.fill(medianIntensity [t], 0.0);
			Arrays.fill(sdIntensity [t], 0.0);
			Arrays.fill(minIntensity [t], Double.POSITIVE_INFINITY);
			Arrays.fill(maxIntensity [t], Double.NEGATIVE_INFINITY);
			
			if(Surr) {
				for(int c = 0; c < averageIntensitySurr[0].length; c++) {
					min25pIntensitySurr [t][c] = Double.NaN;
					max25pIntensitySurr [t][c] = Double.NaN;
					min5pIntensitySurr [t][c] = Double.NaN;
					max5pIntensitySurr [t][c] = Double.NaN;
					if(c == maskC-1) {
						averageIntensitySurr [t][c] = Double.NaN;
						integratedIntensitySurr [t][c] = Double.NaN;
						medianIntensitySurr [t][c] = Double.NaN;
						sdIntensitySurr [t][c] = Double.NaN;
						minIntensitySurr [t][c] = Double.NaN;
						maxIntensitySurr [t][c] = Double.NaN;
					}else {
						averageIntensitySurr [t][c] = 0.0;
						integratedIntensitySurr [t][c] = 0.0;
						medianIntensitySurr [t][c] = 0.0;
						sdIntensitySurr [t][c] = 0.0;
						minIntensitySurr [t][c] = Double.POSITIVE_INFINITY;
						maxIntensitySurr [t][c] = Double.NEGATIVE_INFINITY;
					}
				}
			}
		}
	}
	
	private void determineIntensityParams(ArrayList<AdipoPoint> points, ImagePlus imp) {
		double tempInt;
		for(int i = 0; i < points.size(); i++) {
			voxelNumber [points.get(i).t]++;
			xySurface [points.get(i).t] += points.get(i).xySurface;
			xzyzSurface [points.get(i).t] += points.get(i).xzyzSurface;
			centerX [points.get(i).t] += (double) points.get(i).x;
			centerY [points.get(i).t] += (double) points.get(i).y;
			centerZ [points.get(i).t] += (double) points.get(i).z;
			for(int c = 0; c < averageIntensity[points.get(i).t].length; c++) {
				tempInt = imp.getStack().getVoxel(points.get(i).x, points.get(i).y, 
						imp.getStackIndex(c+1, points.get(i).z+1, points.get(i).t+1)-1);
				averageIntensity [points.get(i).t][c] += tempInt;
				integratedIntensity [points.get(i).t][c] += tempInt;
				if(tempInt > maxIntensity [points.get(i).t][c])	maxIntensity [points.get(i).t][c] = tempInt;
				if(tempInt < minIntensity [points.get(i).t][c])	minIntensity [points.get(i).t][c] = tempInt;
			}
		}
		for(int t = 0; t < voxelNumber.length; t++){
			centerX [t] /= (double) voxelNumber [t];
			centerY [t] /= (double) voxelNumber [t];
			centerZ [t] /= (double) voxelNumber [t];
			for(int c = 0; c < averageIntensity[t].length; c++) {
				averageIntensity [t][c] /= (double) voxelNumber [t];
			}
		}
		for(int i = 0; i < points.size(); i++) {
			for(int c = 0; c < sdIntensity[points.get(i).t].length; c++) {
				tempInt = imp.getStack().getVoxel(points.get(i).x, points.get(i).y, 
						imp.getStackIndex(c+1, points.get(i).z+1, points.get(i).t+1)-1);
				sdIntensity [points.get(i).t][c] += Math.pow(tempInt - averageIntensity [points.get(i).t][c], 2.0);
			}
		}
		for(int t = 0; t < voxelNumber.length; t++){
			for(int c = 0; c < averageIntensity[t].length; c++) {
				sdIntensity [t][c] /= voxelNumber [t] - 1.0;
				sdIntensity [t][c] = Math.sqrt(sdIntensity [t][c]);
			}
		}
		
		// Calculate median intensity
		double tempInts [];
		int counter;
		for(int c = 0; c < imp.getNChannels(); c++) {
			for(int t = 0; t < voxelNumber.length; t++){
				tempInts = new double [voxelNumber[t]];
				counter = 0;
				for(int i = 0; i < points.size(); i++) {
					if(points.get(i).t == t) {
						tempInts [counter] = imp.getStack().getVoxel(points.get(i).x, points.get(i).y, 
								imp.getStackIndex(c+1, points.get(i).z+1, points.get(i).t+1)-1);
						counter++;
					}
				}
				Arrays.sort(tempInts);
				if(tempInts.length%2==0){
					medianIntensity [t][c] = (tempInts[(int)((double)(tempInts.length)/2.0)-1]+tempInts[(int)((double)(tempInts.length)/2.0)])/2.0;
				}else{
					medianIntensity [t][c] = tempInts[(int)((double)(tempInts.length)/2.0)];
				}
				tempInts = null;
				System.gc();
			}
		}
	}
	
	/**
	 * 
	 * */
	private void quantifyCrownLikeStructuresIn2DStatic(ImagePlus imp, int maskC, double refDist, Roi roi){
		Roi bigRoi = RoiEnlarger.enlarge(roi, refDist/((imp.getCalibration().pixelWidth+imp.getCalibration().pixelHeight)/2.0));
		
		int xMin = Integer.MAX_VALUE, xMax = 0, yMin = Integer.MAX_VALUE, yMax = 0;

		xMin = bigRoi.getBounds().x-1;
		xMax = bigRoi.getBounds().x+bigRoi.getBounds().width+1;

		yMin = bigRoi.getBounds().y-1;
		yMax = bigRoi.getBounds().y+bigRoi.getBounds().height+1;
		
		if(xMin < 0) xMin = 0;
		if(yMin < 0) yMin = 0;
		
		if(xMax > imp.getWidth()-1) xMax = imp.getWidth()-1;
		if(yMax > imp.getHeight()-1) yMax = imp.getHeight()-1;
		

		ArrayList<AdipoPoint> surrPoints = new ArrayList<AdipoPoint>((1+xMax-xMin)*(1+yMax-yMin)-voxelNumber[0]);
		for(int x = xMin; x <= xMax; x++) {
			for(int y = yMin; y <= yMax; y++) {
				if(!bigRoi.contains(x,y)) continue;
				if(roi.contains(x, y)) continue;
				
				surrPoints.add(new AdipoPoint(x, y, 0, 0, imp, 1));
			}
		}
		surrPoints.trimToSize();
		
		//Quantify Surr intensities
		calculateSurrParameters(surrPoints, imp, maskC);
		
		surrPoints.clear();
		surrPoints = null;
	}
	
	/**
	 * 
	 * */
	private void quantifyCrownLikeStructuresIn3DTimelapse(ArrayList<AdipoPoint> points, ImagePlus imp, int maskC, double refDist) {
		int xMin = Integer.MAX_VALUE, xMax = 0, yMin = Integer.MAX_VALUE, yMax = 0,
				zMin = Integer.MAX_VALUE, zMax = 0, tMin = Integer.MAX_VALUE, tMax = 0;
		
		ArrayList<AdipoPoint> surfacePoints = new ArrayList<AdipoPoint>(points.size());
		for(int i = 0; i < points.size(); i++) {
			if(points.get(i).getSurface(1.0, 1.0) > 0) {
				surfacePoints.add(points.get(i));
				if(points.get(i).x < xMin)	xMin = points.get(i).x;
				if(points.get(i).x > xMax)	xMax = points.get(i).x;
				if(points.get(i).y < yMin)	yMin = points.get(i).y;
				if(points.get(i).y > yMax)	yMax = points.get(i).y;
				if(points.get(i).z < zMin)	zMin = points.get(i).z;
				if(points.get(i).z > zMax)	zMax = points.get(i).z;
				if(points.get(i).t < tMin)	tMin = points.get(i).t;
				if(points.get(i).t > tMax)	tMax = points.get(i).t;
			}
		}

		xMin -= Math.round(refDist / imp.getCalibration().pixelWidth) + 1;
		xMax += Math.round(refDist / imp.getCalibration().pixelWidth) + 1;
		
		yMin -= Math.round(refDist / imp.getCalibration().pixelHeight) + 1;
		yMax += Math.round(refDist / imp.getCalibration().pixelHeight) + 1;
		
		zMin -= Math.round(refDist / imp.getCalibration().pixelDepth) + 1;
		zMax += Math.round(refDist / imp.getCalibration().pixelDepth) + 1;
		
		if(xMin < 0) xMin = 0;
		if(yMin < 0) yMin = 0;
		if(zMin < 0) zMin = 0;
		
		if(xMax > imp.getWidth()-1) xMax = imp.getWidth()-1;
		if(yMax > imp.getHeight()-1) yMax = imp.getHeight()-1;
		if(zMax > imp.getNSlices()-1) zMax = imp.getNSlices()-1;
		

		ArrayList<AdipoPoint> surrPoints = new ArrayList<AdipoPoint>((1+xMax-xMin)*(1+yMax-yMin)*(1+zMax-zMin)*(1+tMax-tMin)-points.size());
		double temp;
		for(int x = xMin; x <= xMax; x++) {
			for(int y = yMin; y <= yMax; y++) {
				for(int z = zMin; z <= zMax; z++) {
					for(int t = tMin; t <= tMax; t++) {
						for(int i = 0; i < surfacePoints.size(); i++) {
							if(t != surfacePoints.get(i).t) continue;
							
							//Determine distance to surface
							temp = Math.sqrt(Math.pow((x-surfacePoints.get(i).x)*imp.getCalibration().pixelWidth, 2.0)
									+ Math.pow((y-surfacePoints.get(i).y)*imp.getCalibration().pixelHeight,2.0) 
											+ Math.pow((z-surfacePoints.get(i).z)*imp.getCalibration().pixelDepth, 2.0));
							
							//Add if within user-defined range
							if(temp < refDist) {
								surrPoints.add(new AdipoPoint(x, y, z, t, imp, 1));
								break;
							}
						}
					}
				}
			}
		}
		surrPoints.trimToSize();
		surfacePoints.clear();
		surfacePoints = null;
		
		//Quantify Surr intensities
		calculateSurrParameters(surrPoints, imp, maskC);
		
		surrPoints.clear();
		surrPoints = null;
	}
	
	private void calculateSurrParameters (ArrayList<AdipoPoint> surrPoints, ImagePlus imp, int maskC) {
		double temp;
		for(int i = 0; i < surrPoints.size(); i++) {
			voxelNumberSurr [surrPoints.get(i).t]++;
			for(int c = 0; c < averageIntensitySurr[surrPoints.get(i).t].length; c++) {
				if(c == maskC-1) continue;
				
				temp = imp.getStack().getVoxel(surrPoints.get(i).x, surrPoints.get(i).y, 
						imp.getStackIndex(c+1, surrPoints.get(i).z+1, surrPoints.get(i).t+1)-1);
				averageIntensitySurr [surrPoints.get(i).t][c] += temp;
				integratedIntensitySurr [surrPoints.get(i).t][c] += temp;
				if(temp > maxIntensitySurr [surrPoints.get(i).t][c])	maxIntensitySurr [surrPoints.get(i).t][c] = temp;
				if(temp < minIntensitySurr [surrPoints.get(i).t][c])	minIntensitySurr [surrPoints.get(i).t][c] = temp;
			}
		}
		for(int t = 0; t < voxelNumberSurr.length; t++){
			for(int c = 0; c < averageIntensitySurr[t].length; c++) {
				if(c == maskC-1) continue;
				
				averageIntensitySurr [t][c] /= (double) voxelNumberSurr [t];
			}
		}
		for(int i = 0; i < surrPoints.size(); i++) {
			for(int c = 0; c < sdIntensitySurr[surrPoints.get(i).t].length; c++) {
				if(c == maskC-1) continue;
				
				temp = imp.getStack().getVoxel(surrPoints.get(i).x, surrPoints.get(i).y, 
						imp.getStackIndex(c+1, surrPoints.get(i).z+1, surrPoints.get(i).t+1)-1);
				sdIntensitySurr [surrPoints.get(i).t][c] += Math.pow(temp - averageIntensitySurr [surrPoints.get(i).t][c], 2.0);
			}
		}
		for(int t = 0; t < voxelNumberSurr.length; t++){
			for(int c = 0; c < averageIntensitySurr[t].length; c++) {
				if(c == maskC-1) continue;
				
				sdIntensitySurr [t][c] /= voxelNumberSurr [t] - 1.0;
				sdIntensitySurr [t][c] = Math.sqrt(sdIntensitySurr [t][c]);
			}
		}
		
		// Calculate median IntensitySurr
		double [] tempArray;
		int counter;
		for(int c = 0; c < imp.getNChannels(); c++) {
			if(c == maskC-1) continue;
			for(int t = 0; t < voxelNumberSurr.length; t++){
				tempArray = new double [voxelNumberSurr[t]];
				System.gc();
				
				counter = 0;
				for(int i = 0; i < surrPoints.size(); i++) {
					if(surrPoints.get(i).t == t) {
						tempArray [counter] = imp.getStack().getVoxel(surrPoints.get(i).x, surrPoints.get(i).y, 
								imp.getStackIndex(c+1, surrPoints.get(i).z+1, surrPoints.get(i).t+1)-1);
						counter++;
					}
				}
				Arrays.sort(tempArray);
				if(tempArray.length%2==0){
					medianIntensitySurr [t][c] = (tempArray[(int)((double)(tempArray.length)/2.0)-1]+tempArray[(int)((double)(tempArray.length)/2.0)])/2.0;
				}else{
					medianIntensitySurr [t][c] = tempArray[(int)((double)(tempArray.length)/2.0)];
				}
				
				min25pIntensitySurr [t][c] = AdipoQAnalyzerMain.getMinPercentFromSortedArray(tempArray, 25.0);
				min5pIntensitySurr [t][c] = AdipoQAnalyzerMain.getMinPercentFromSortedArray(tempArray, 5.0); 

				max25pIntensitySurr [t][c] = AdipoQAnalyzerMain.getMaxPercentFromSortedArray(tempArray, 25.0);
				max5pIntensitySurr [t][c] = AdipoQAnalyzerMain.getMaxPercentFromSortedArray(tempArray, 5.0);
			}
		}
		tempArray = null;
	}
	
	double getSurface(double calX, double calY, double zcal, int frame){
		return (calX*calY*(double)xySurface [frame] + ((calX + calY)/2.0)*zcal*(double)xzyzSurface [frame]); 
	}
}