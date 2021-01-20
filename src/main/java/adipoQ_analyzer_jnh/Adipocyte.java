package adipoQ_analyzer_jnh;

/** ===============================================================================
* AdipoQ Analyzer Version 0.0.1
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
* Date: January 12, 2021 (This Version: January 12, 2021)
*   
* For any questions please feel free to contact me (jan.hansen@uni-bonn.de).
* =============================================================================== */

import java.util.ArrayList;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
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

	int voxelNumberCLS [];	// time
	double averageIntensityCLS [][];	// time, channels
	double integratedIntensityCLS [][];	// time, channels
	double sdIntensityCLS [][];	// time, channels
	double medianIntensityCLS [][];	// time, channels
	double minIntensityCLS [][];	// time, channels
	double maxIntensityCLS [][];	// time, channels
	double min25pIntensityCLS [][];	// time, channels
	double max25pIntensityCLS [][];	// time, channels
	double min5pIntensityCLS [][];	// time, channels
	double max5pIntensityCLS [][];	// time, channels
	
	/**
	 * pz >= 0 && pz < number of slices
	 * pt >= 0 && pt < number of frames
	 * 1 <= maskC <= imp.getNChannels()
	 * 
	 * For the maskC channel, no CLS parameters are determined!
	 * */
	public Adipocyte(ArrayList<AdipoPoint> points, ImagePlus imp, int maskC, boolean CLS, double refDist, Roi roi){
		initializeArrays(imp.getNFrames(), imp.getNChannels(), CLS, maskC);
		determineIntensityParams(points, imp, maskC, CLS, refDist, roi);
	}
	
	private void initializeArrays(int nFrames, int nChannels, boolean CLS, int maskC) {
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

		if(CLS) {
			voxelNumberCLS = new int [nFrames];
			voxelNumberCLS = new int [nFrames];
			averageIntensityCLS  = new double [nFrames][nChannels];	// time, channels
			integratedIntensityCLS  = new double [nFrames][nChannels];	// time, channels
			medianIntensityCLS  = new double [nFrames][nChannels];	// time, channels
			sdIntensityCLS  = new double [nFrames][nChannels];	// time, channels
			minIntensityCLS  = new double [nFrames][nChannels];	// time, channels
			maxIntensityCLS  = new double [nFrames][nChannels];	// time, channels
			min25pIntensityCLS  = new double [nFrames][nChannels];	// time, channels
			max25pIntensityCLS  = new double [nFrames][nChannels];	// time, channels
			min5pIntensityCLS  = new double [nFrames][nChannels];	// time, channels
			max5pIntensityCLS  = new double [nFrames][nChannels];	// time, channels
			
		}
		
		Arrays.fill(voxelNumber, 0);
		Arrays.fill(xySurface, 0);
		Arrays.fill(xzyzSurface, 0);
		Arrays.fill(centerX, 0.0);
		Arrays.fill(centerY, 0.0);
		Arrays.fill(centerZ, 0.0);
		if(CLS) {
			Arrays.fill(voxelNumberCLS, 0);
		}
		for(int t = 0; t < nFrames; t++) {
			Arrays.fill(averageIntensity [t], 0.0);
			Arrays.fill(integratedIntensity [t], 0.0);
			Arrays.fill(medianIntensity [t], 0.0);
			Arrays.fill(sdIntensity [t], 0.0);
			Arrays.fill(minIntensity [t], Double.POSITIVE_INFINITY);
			Arrays.fill(maxIntensity [t], Double.NEGATIVE_INFINITY);
			
			if(CLS) {
				for(int c = 0; c < averageIntensityCLS[0].length; c++) {
					min25pIntensityCLS [t][c] = Double.NaN;
					max25pIntensityCLS [t][c] = Double.NaN;
					min5pIntensityCLS [t][c] = Double.NaN;
					max5pIntensityCLS [t][c] = Double.NaN;
					if(c == maskC-1) {
						averageIntensityCLS [t][c] = Double.NaN;
						integratedIntensityCLS [t][c] = Double.NaN;
						medianIntensityCLS [t][c] = Double.NaN;
						sdIntensityCLS [t][c] = Double.NaN;
						minIntensityCLS [t][c] = Double.NaN;
						maxIntensityCLS [t][c] = Double.NaN;
					}else {
						averageIntensityCLS [t][c] = 0.0;
						integratedIntensityCLS [t][c] = 0.0;
						medianIntensityCLS [t][c] = 0.0;
						sdIntensityCLS [t][c] = 0.0;
						minIntensityCLS [t][c] = Double.POSITIVE_INFINITY;
						maxIntensityCLS [t][c] = Double.NEGATIVE_INFINITY;
					}
				}
			}
		}
	}
	
	private void determineIntensityParams(ArrayList<AdipoPoint> points, ImagePlus imp, int maskC, boolean CLS, double refDist, Roi roi) {
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
		
		// Quantify Crown Like Structures
		if(CLS) {
			Roi bigRoi = RoiEnlarger.enlarge(roi, refDist/((imp.getCalibration().pixelWidth+imp.getCalibration().pixelHeight)/2.0));
//			double dist;
			
//			imp.setRoi(roi);
//			imp.show();
//			new WaitForUserDialog("small").show();
//			imp.hide();
			
//			imp.setRoi(bigRoi);
//			imp.show();
//			new WaitForUserDialog("big").show();
//			imp.hide();
			
			ArrayList<AdipoPoint> surfacePoints = new ArrayList<AdipoPoint>(points.size());
			int xMin = Integer.MAX_VALUE, xMax = 0, yMin = Integer.MAX_VALUE, yMax = 0,
					zMin = Integer.MAX_VALUE, zMax = 0, tMin = Integer.MAX_VALUE, tMax = 0;
			
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
			surfacePoints.trimToSize();
			
//			IJ.log(surfacePoints.size() + " / " + points.size());

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
			

			ArrayList<AdipoPoint> cLSPoints = new ArrayList<AdipoPoint>((1+xMax-xMin)*(1+yMax-yMin)*(1+zMax-zMin)*(1+tMax-tMin)-points.size());
			for(int x = xMin; x <= xMax; x++) {
				for(int y = yMin; y <= yMax; y++) {
					for(int z = zMin; z <= zMax; z++) {
						for(int t = tMin; t <= tMax; t++) {
							if(!bigRoi.contains(x,y)) continue;
							if(roi.contains(x, y)) continue;
							
							cLSPoints.add(new AdipoPoint(x, y, z, t, imp, 1));
						}
					}
				}
			}
			cLSPoints.trimToSize();
//			IJ.log(cLSPoints.size() + " cls / " + points.size() + " total");
			
			//Quantify CLS intensities
			{
				for(int i = 0; i < cLSPoints.size(); i++) {
					voxelNumberCLS [cLSPoints.get(i).t]++;
					for(int c = 0; c < averageIntensityCLS[cLSPoints.get(i).t].length; c++) {
						if(c == maskC-1) continue;
						
						tempInt = imp.getStack().getVoxel(cLSPoints.get(i).x, cLSPoints.get(i).y, 
								imp.getStackIndex(c+1, cLSPoints.get(i).z+1, cLSPoints.get(i).t+1)-1);
						averageIntensityCLS [cLSPoints.get(i).t][c] += tempInt;
						integratedIntensityCLS [cLSPoints.get(i).t][c] += tempInt;
						if(tempInt > maxIntensityCLS [cLSPoints.get(i).t][c])	maxIntensityCLS [cLSPoints.get(i).t][c] = tempInt;
						if(tempInt < minIntensityCLS [cLSPoints.get(i).t][c])	minIntensityCLS [cLSPoints.get(i).t][c] = tempInt;
					}
				}
				for(int t = 0; t < voxelNumberCLS.length; t++){
					for(int c = 0; c < averageIntensityCLS[t].length; c++) {
						if(c == maskC-1) continue;
						
						averageIntensityCLS [t][c] /= (double) voxelNumberCLS [t];
					}
				}
				for(int i = 0; i < cLSPoints.size(); i++) {
					for(int c = 0; c < sdIntensityCLS[cLSPoints.get(i).t].length; c++) {
						if(c == maskC-1) continue;
						
						tempInt = imp.getStack().getVoxel(cLSPoints.get(i).x, cLSPoints.get(i).y, 
								imp.getStackIndex(c+1, cLSPoints.get(i).z+1, cLSPoints.get(i).t+1)-1);
						sdIntensityCLS [cLSPoints.get(i).t][c] += Math.pow(tempInt - averageIntensityCLS [cLSPoints.get(i).t][c], 2.0);
					}
				}
				for(int t = 0; t < voxelNumberCLS.length; t++){
					for(int c = 0; c < averageIntensityCLS[t].length; c++) {
						if(c == maskC-1) continue;
						
						sdIntensityCLS [t][c] /= voxelNumberCLS [t] - 1.0;
						sdIntensityCLS [t][c] = Math.sqrt(sdIntensityCLS [t][c]);
					}
				}
				
				// Calculate median IntensityCLS
				for(int c = 0; c < imp.getNChannels(); c++) {
					if(c == maskC-1) continue;
					for(int t = 0; t < voxelNumberCLS.length; t++){
						tempInts = new double [voxelNumberCLS[t]];
						System.gc();
						
						counter = 0;
						for(int i = 0; i < cLSPoints.size(); i++) {
							if(cLSPoints.get(i).t == t) {
								tempInts [counter] = imp.getStack().getVoxel(cLSPoints.get(i).x, cLSPoints.get(i).y, 
										imp.getStackIndex(c+1, cLSPoints.get(i).z+1, cLSPoints.get(i).t+1)-1);
								counter++;
							}
						}
						Arrays.sort(tempInts);
						if(tempInts.length%2==0){
							medianIntensityCLS [t][c] = (tempInts[(int)((double)(tempInts.length)/2.0)-1]+tempInts[(int)((double)(tempInts.length)/2.0)])/2.0;
						}else{
							medianIntensityCLS [t][c] = tempInts[(int)((double)(tempInts.length)/2.0)];
						}
						
						min25pIntensityCLS [t][c] = AdipoQAnalyzerMain.getMinPercentFromSortedArray(tempInts, 25.0);
						min5pIntensityCLS [t][c] = AdipoQAnalyzerMain.getMinPercentFromSortedArray(tempInts, 5.0); 

						max25pIntensityCLS [t][c] = AdipoQAnalyzerMain.getMaxPercentFromSortedArray(tempInts, 25.0);
						max5pIntensityCLS [t][c] = AdipoQAnalyzerMain.getMaxPercentFromSortedArray(tempInts, 5.0);
					}
				}
			}
			
			surfacePoints.clear();
			cLSPoints.clear();
			surfacePoints = null;
			cLSPoints = null;
			System.gc();
		}
	}
	
	double getSurface(double calX, double calY, double zcal, int frame){
		return (calX*calY*(double)xySurface [frame] + ((calX + calY)/2.0)*zcal*(double)xzyzSurface [frame]); 
	}
}