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

import ij.ImagePlus;

class AdipoPoint{
	int x = 0; 
	int y = 0; 
	int z = 0; 
	int t = 0;
	int xySurface = 0;
	int xzyzSurface = 0;
	
	/**
	 * pz >= 0 && pz < number of slices
	 * pt >= 0 && pt < number of frames
	 * */
	public AdipoPoint(int px, int py, int pz, int pt, ImagePlus imp, int channel){
		x = px;
		y = py;
		z = pz;
		t = pt;
		
		if(pz > 0
				&& imp.getStack().getVoxel(px, py, imp.getStackIndex(channel, (pz-1)+1, pt+1)-1) == 0.0){	
			xySurface++;
		}
		
		if(pz < imp.getNSlices() - 1
				&& imp.getStack().getVoxel(px, py, imp.getStackIndex(channel, (pz+1)+1, pt+1)-1) == 0.0){	
			xySurface++;
		}

		if(px > 0
				&& imp.getStack().getVoxel(px-1, py, imp.getStackIndex(channel, (pz)+1, pt+1)-1) == 0.0){	
			xzyzSurface++;
		}

		if(px < imp.getWidth() - 1
				&& imp.getStack().getVoxel(px+1, py, imp.getStackIndex(channel, (pz)+1, pt+1)-1) == 0.0){	
			xzyzSurface++;
		}

		if(py > 0
				&& imp.getStack().getVoxel(px, py-1, imp.getStackIndex(channel, (pz)+1, pt+1)-1) == 0.0){	
			xzyzSurface++;
		}
		
		if(py < imp.getHeight() - 1
				&& imp.getStack().getVoxel(px, py+1, imp.getStackIndex(channel, (pz)+1, pt+1)-1) == 0.0){	
			xzyzSurface++;
		}
	}
	
	double getSurface(double cal, double zcal){
		return (cal*cal*(double)xySurface + cal*zcal*(double)xzyzSurface); 
	}
}

