package adipoQ_analyzer_jnh;
import java.util.ArrayList;
import java.util.Arrays;
import ij.ImagePlus;

class Adipocyte{
	int voxelNumber [];	// time
	private int xySurface [];
	private int xzyzSurface [];
	double centerX []; // time
	double centerY []; // time
	double centerZ [];	// time
	double averageIntensity [][];	// time, channels
	double integratedIntensity [][];	// time, channels
	double sdIntensity [][];	// time, channels
	double medianIntensity [][];	// time, channels
	double minIntensity [][];	// time, channels
	double maxIntensity [][];	// time, channels
	
	/**
	 * pz >= 0 && pz < number of slices
	 * pt >= 0 && pt < number of frames
	 * */
	public Adipocyte(ArrayList<AdipoPoint> points, ImagePlus imp){
		initializeArrays(imp.getNFrames(), imp.getNChannels());
		determineIntensityParams(points, imp);
	}
	
	private void initializeArrays(int nFrames, int nChannels) {
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
		
		Arrays.fill(voxelNumber, 0);
		Arrays.fill(xySurface, 0);
		Arrays.fill(xzyzSurface, 0);
		Arrays.fill(centerX, 0.0);
		Arrays.fill(centerY, 0.0);
		Arrays.fill(centerZ, 0.0);
		for(int t = 0; t < nFrames; t++) {
			Arrays.fill(averageIntensity [t], 0.0);
			Arrays.fill(integratedIntensity [t], 0.0);
			Arrays.fill(medianIntensity [t], 0.0);
			Arrays.fill(sdIntensity [t], 0.0);
			Arrays.fill(minIntensity [t], Double.POSITIVE_INFINITY);
			Arrays.fill(maxIntensity [t], Double.NEGATIVE_INFINITY);
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
	
	double getSurface(double cal, double zcal, int frame){
		return (cal*cal*(double)xySurface [frame] + cal*zcal*(double)xzyzSurface [frame]); 
	}
}

