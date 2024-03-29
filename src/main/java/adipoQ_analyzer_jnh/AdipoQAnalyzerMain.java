package adipoQ_analyzer_jnh;
/** ===============================================================================
* AdipoQ Analyzer Version 0.1.1
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
* Date: January 12, 2021 (This Version: May 29, 2022)
*   
* For any questions please feel free to contact me (jan.hansen@uni-bonn.de).
* =============================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.text.*;

import javax.swing.UIManager;

import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.text.*;

public class AdipoQAnalyzerMain implements PlugIn, Measurements {
	//Name variables
	static final String PLUGINNAME = "AdipoQ Analyzer";
	static final String PLUGINVERSION = "0.1.1";
	
	//Fix fonts
	static final Font SuperHeadingFont = new Font("Sansserif", Font.BOLD, 16);
	static final Font HeadingFont = new Font("Sansserif", Font.BOLD, 14);
	static final Font SubHeadingFont = new Font("Sansserif", Font.BOLD, 12);
	static final Font TextFont = new Font("Sansserif", Font.PLAIN, 12);
	static final Font InstructionsFont = new Font("Sansserif", 2, 12);
	static final Font RoiFont = new Font("Sansserif", Font.PLAIN, 12);
	
	DecimalFormat df6 = new DecimalFormat("#0.000000");
	DecimalFormat df3 = new DecimalFormat("#0.000");
	DecimalFormat df0 = new DecimalFormat("#0");
	DecimalFormat dfDialog = new DecimalFormat("#0.000000");
		
	static SimpleDateFormat NameDateFormatter = new SimpleDateFormat("yyMMdd_HHmmss");
	static SimpleDateFormat FullDateFormatter = new SimpleDateFormat("yyyy-MM-dd	HH:mm:ss");
	static SimpleDateFormat FullDateFormatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	//Progress Dialog
	ProgressDialog progress;	
	boolean processingDone = false;	
	boolean continueProcessing = true;
	
	//-----------------define params-----------------
	static final String[] taskVariant = {"active image in FIJI","multiple images (open multi-task manager)", "all images open in FIJI"};
	String selectedTaskVariant = taskVariant[1];
	int tasks = 1;

	final static String[] settingsMethod = {"manually enter preferences", "load preferences from existing AdipoQ Analyzer metadata file"};
	String selectedSettingsVariant = settingsMethod [0];
	
	static final String[] inputVariant = {"binary mask or background-removed (background = 0)", "label image (object-specific intensity levels)"};
	String chosenInput = inputVariant[0];
	
	static final String[] outputVariant = {"save as filename + suffix 'AQA'", "save as filename + suffix 'AQA' + date"};
	String chosenOutputName = outputVariant[0];
	boolean keepAwake = false;
	
	boolean inputIsLabelImage = false;	
	
	static final String[] nrFormats = {"US (0.00...)", "Germany (0,00...)"};
	String ChosenNumberFormat = nrFormats[0];

	int channelID = 1;
	double minSize = 100.0;
	double minSizeCalibrated = 20.0;
	boolean increaseRange, fuseParticles, quantifySurroundings, saveRois, saveSurrMaps = false;
	double refDistance = 20.0;
	
	static final String[] excludeOptions = {"nothing", "particles touching x or y borders", "particles touching x or y or z borders"};
	String excludeSelection = excludeOptions [1];
	//-----------------define params-----------------
	
	//Variables for processing of an individual task
	Robot robo;
	
public void run(String arg) {

	//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	//---------------------------read preferences---------------------------------
	//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	
	GenericDialog gd = new GenericDialog(PLUGINNAME + " on " + System.getProperty("os.name") + "");	
	//show Dialog-----------------------------------------------------------------
	//.setInsets(top, left, bottom)
	gd.setInsets(0,0,0);	gd.addMessage(PLUGINNAME + ", Version " + PLUGINVERSION + ", \u00a9 2021-2022 JN Hansen", SuperHeadingFont);
	gd.setInsets(5,0,0);	gd.addChoice("process ", taskVariant, selectedTaskVariant);
	gd.setInsets(0,0,0);	gd.addMessage("The plugin processes .tif images with at least one segmented channel for reconstruction.", InstructionsFont);
	
	gd.setInsets(10,0,0);	gd.addChoice("Preferences: ", settingsMethod, selectedSettingsVariant);
	
	gd.setInsets(10,0,0);	gd.addMessage("GENERAL SETTINGS:", HeadingFont);
	gd.setInsets(5,0,0);	gd.addChoice("Segmented channel is ", inputVariant, chosenInput);
	gd.setInsets(5,0,0);	gd.addChoice("Output image name: ", outputVariant, chosenOutputName);
	gd.setInsets(5,0,0);	gd.addChoice("Output number format", nrFormats, nrFormats[0]);
	gd.setInsets(5,0,0);	gd.addCheckbox("Save rois in 2D static mode (time-consuming!)", saveRois);
	gd.setInsets(5,0,0);	gd.addCheckbox("Save maps for surrounding intensities", saveSurrMaps);
	gd.setInsets(5,0,0);	gd.addCheckbox("Keep computer awake during analysis", keepAwake);
	
	gd.showDialog();
	//show Dialog-----------------------------------------------------------------

	//read and process variables--------------------------------------------------
	selectedTaskVariant = gd.getNextChoice();
	selectedSettingsVariant = gd.getNextChoice();
	chosenInput = gd.getNextChoice();	
	chosenOutputName = gd.getNextChoice();	
	ChosenNumberFormat = gd.getNextChoice();
	dfDialog.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
	if(ChosenNumberFormat.equals(nrFormats[0])){ //US-Format
		df6.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		df3.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		df0.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
	}else if (ChosenNumberFormat.equals(nrFormats[1])){
		df6.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.GERMANY));
		df3.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.GERMANY));
		df0.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.GERMANY));
	}
	saveRois = gd.getNextBoolean();
	saveSurrMaps = gd.getNextBoolean();
	keepAwake = gd.getNextBoolean();
	
	//read and process variables--------------------------------------------------
	if (gd.wasCanceled()) return;
	
	if(selectedSettingsVariant.equals(settingsMethod [0])){
		if(!enterSettings()) {
			return;
		}
	}else if(!importSettings()) {
		IJ.error("Preferences could not be loaded due to file error...");
		return;
	}
	
	//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	//------------------------------processing------------------------------------
	//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&

	String name [] = {"",""};
	String dir [] = {"",""};
	ImagePlus allImps [] = new ImagePlus [2];
//	RoiEncoder re;
	{
		//Improved file selector
		try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception e){}
		if(selectedTaskVariant.equals(taskVariant[1])){
			OpenFilesDialog od = new OpenFilesDialog ();
			od.setLocation(0,0);
			od.setVisible(true);
			
			od.addWindowListener(new java.awt.event.WindowAdapter() {
		        public void windowClosing(WindowEvent winEvt) {
		        	return;
		        }
		    });
		
			//Waiting for od to be done
			while(od.done==false){
				try{
					Thread.currentThread().sleep(50);
			    }catch(Exception e){
			    }
			}
			
			tasks = od.filesToOpen.size();
			name = new String [tasks];
			dir = new String [tasks];
			for(int task = 0; task < tasks; task++){
				name[task] = od.filesToOpen.get(task).getName();
				dir[task] = od.filesToOpen.get(task).getParent() + System.getProperty("file.separator");
			}		
		}else if(selectedTaskVariant.equals(taskVariant[0])){
			if(WindowManager.getIDList()==null){
				new WaitForUserDialog("Plugin canceled - no image open in FIJI!").show();
				return;
			}
			FileInfo info = WindowManager.getCurrentImage().getOriginalFileInfo();
			name [0] = info.fileName;	//get name
			dir [0] = info.directory;	//get directory
			tasks = 1;
		}else if(selectedTaskVariant.equals(taskVariant[2])){	// all open images
			if(WindowManager.getIDList()==null){
				new WaitForUserDialog("Plugin canceled - no image open in FIJI!").show();
				return;
			}
			int IDlist [] = WindowManager.getIDList();
			tasks = IDlist.length;	
			if(tasks == 1){
				selectedTaskVariant=taskVariant[0];
				FileInfo info = WindowManager.getCurrentImage().getOriginalFileInfo();
				name [0] = info.fileName;	//get name
				dir [0] = info.directory;	//get directory
			}else{
				name = new String [tasks];
				dir = new String [tasks];
				allImps = new ImagePlus [tasks];
				for(int i = 0; i < tasks; i++){
					allImps[i] = WindowManager.getImage(IDlist[i]); 
					FileInfo info = allImps[i].getOriginalFileInfo();
					name [i] = info.fileName;	//get name
					dir [i] = info.directory;	//get directory
				}		
			}
					
		}
	}
	
	//add progressDialog
		progress = new ProgressDialog(name, tasks);
		progress.setLocation(0,0);
		progress.setVisible(true);
		progress.addWindowListener(new java.awt.event.WindowAdapter() {
	        public void windowClosing(WindowEvent winEvt) {
	        	if(processingDone==false){
	        		IJ.error("Script stopped...");
	        	}
	        	continueProcessing = false;	        	
	        	return;
	        }
		});
	
   	ImagePlus imp;
   	TextPanel tp1, tp2;
   	Date startDate, endDate;
   	
   	if(keepAwake) {
   		try {
			robo = new Robot();
		} catch (AWTException e) {
			progress.notifyMessage("Robot that moves the mouse to keep the computer awake could not be hired - Stay-awake-mode was disabled.", ProgressDialog.NOTIFICATION);
		}
   	}
   	

	if(saveSurrMaps && !quantifySurroundings) {
		progress.notifyMessage("Surroundings Maps cannot be saved since surroundings are not set to be quantified!", ProgressDialog.NOTIFICATION);
		saveSurrMaps = false;
	}
	
	for(int task = 0; task < tasks; task++){
		running: while(continueProcessing){
			startDate = new Date();
			progress.updateBarText("in progress...");
			//Check for problems
			if(name[task].contains(".") && name[task].substring(name[task].lastIndexOf("."),name[task].length()).equals(".txt")){
				progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": A file is no image! Could not be processed!", ProgressDialog.ERROR);
				progress.moveTask(task);	
				break running;
			}
			if(name[task].contains(".") && name[task].substring(name[task].lastIndexOf("."),name[task].length()).equals(".zip")){	
				progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": A file is no image! Could not be processed!", ProgressDialog.ERROR);
				progress.moveTask(task);	
				break running;
			}					
			//Check for problems

			//open Image
		   	try{
		   		if(selectedTaskVariant.equals(taskVariant[1])){
	   				imp = IJ.openImage(""+dir[task]+name[task]+"");
		   			imp.hide();
					imp.deleteRoi();
		   		}else if(selectedTaskVariant.equals(taskVariant[0])){
		   			imp = WindowManager.getCurrentImage();
		   			imp.deleteRoi();
		   		}else{
		   			imp = allImps[task];
		   			imp.deleteRoi();
		   		}
		   	}catch (Exception e) {
		   		progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": file is no image - could not be processed!", ProgressDialog.ERROR);
				progress.moveTask(task);	
				break running;
			}
		   	//open Image
		   	
		   	//Check for problems with the image
		   	if(imp.getNFrames()>1){	
				progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": Could not be processed. Analysis of multi-frame images not yet implemented!", ProgressDialog.ERROR);
				progress.moveTask(task);	
				break running;
			}			
			if(imp.getNSlices()>1){	
				progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": Could not be processed. Analysis of 3D images not yet implemented!", ProgressDialog.ERROR);
				progress.moveTask(task);	
				break running;
			}			
			if(channelID < 1 || channelID > imp.getNChannels()) {
				progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": Could not be processed. Selected channel does not exist in the image!"
						+ " Select a channel number between 1 and the total number of channels in the image.", ProgressDialog.ERROR);
				progress.moveTask(task);	
				break running;
			}
			if(imp.getCalibration().pixelHeight != imp.getCalibration().pixelWidth) {
				progress.notifyMessage("Task " + (task+1) + "/" + tasks + ": WARNING: Pixel width is not equal pixel height "
						+ "- surface/outline calculation may be inaccurate.", ProgressDialog.NOTIFICATION);
			}			
		   	//Check for problems with the image
		   	
		   	//Create Outputfilename
		   	progress.updateBarText("Create output filename");				
			String filePrefix;
			if(name[task].contains(".")){
				filePrefix = name[task].substring(0,name[task].lastIndexOf("."));
			}else{
				filePrefix = name[task];
			}
			
			filePrefix += "_AQA";
			
			if(chosenOutputName.equals(outputVariant[1])){
				//saveDate
				filePrefix += "_" + NameDateFormatter.format(startDate);
			}
			
			filePrefix = dir[task] + filePrefix;
		   	
			//Determine pixelated particle size
			{
				//convert calibrated minSize (minSizeCalibrated) into pixelated minSize
				minSize = (double)minSizeCalibrated / (double)imp.getCalibration().pixelWidth / (double)imp.getCalibration().pixelHeight;
				progress.notifyMessage("Task " + (task+1) + ": Minimum size in " + imp.getCalibration().getUnit() + "^2: " + df6.format(minSizeCalibrated) 
					+ " -> in pixel: " + df6.format(minSize), ProgressDialog.LOG);
			}
			
		/******************************************************************
		*** 						Processing							***	
		*******************************************************************/
			
			//processing
			progress.updateBarText("Analyze " + channelID + " ...");
			ArrayList<Adipocyte> adipocytes = new ArrayList<Adipocyte>(0);
			if(imp.getNSlices()==1 && imp.getNFrames()==1) {
				adipocytes = this.analyzeAdipocytesWithIn2D(imp, channelID, filePrefix, chosenInput.equals(inputVariant [1]));
				if(saveRois) {
					RoiManager.getInstance().runCommand("Save", filePrefix+"r.zip");
					RoiManager.getInstance().reset();
				}
				RoiManager.getInstance().setVisible(true);
			}else {
				adipocytes = this.analyzeAdipocytes(imp, channelID);				
			}
			
			//Saving
			tp1 = new TextPanel("results");
			tp2 = new TextPanel("results short");
			endDate = new Date();
			
			addSettingsBlockToPanel(tp1,  startDate, endDate, name[task], imp);
			tp1.append("");
			
			tp1.append("Results");
			String appText;
			
			appText = ("Image name"
					+ "	" + "ID" 
					+ "	" + "Custom" 
					+ "	" + "Frame" 
					+ "	" + "Total frames" 
					+ "	" + "Center X [" + imp.getCalibration().getUnit() + "]"
					+ "	" + "Center Y [" + imp.getCalibration().getUnit() + "]"
					+ "	" + "Center Z [" + imp.getCalibration().getUnit() + "]"
					+ "	" + "Voxels");
			if(imp.getNSlices()>1) {
				appText += "	" + "Volume [" + imp.getCalibration().getUnit() + "^3]";
				appText += "	" + "Surface [" + imp.getCalibration().getUnit() + "^2]";
				appText += "	" + "3D-Asphericity Index";
			}else {
				appText += "	" + "Area [" + imp.getCalibration().getUnit() + "^2]";
				appText += "	" + "Outline [" + imp.getCalibration().getUnit() + "]";
				appText += "	" + "2D-Asphericity Index";
			}
			for(int c = 0; c < imp.getNChannels(); c++) {
				appText += "	" + "C" + (c+1) + ": Average Intensity";
				appText += "	" + "C" + (c+1) + ": Integrated Intensity";
				appText += "	" + "C" + (c+1) + ": Median Intensity";
				appText += "	" +"C" + (c+1) +  ": SD of Intensities";
				appText += "	" +"C" + (c+1) +  ": Min Intensity";
				appText += "	" +"C" + (c+1) +  ": Max Intensity";
			}
			
			//Output Surroundings data
			if(quantifySurroundings) {
				appText += "	" + "Surr Voxels";
				for(int c = 0; c < imp.getNChannels(); c++) {
					if(c == channelID-1) continue;
					appText += "	" + "C" + (c+1) + ": Surr Average Intensity";
					appText += "	" + "C" + (c+1) + ": Surr Integrated Intensity";
					appText += "	" + "C" + (c+1) + ": Surr Median Intensity";
					appText += "	" +"C" + (c+1) +  ": Surr SD of Intensities";
					appText += "	" +"C" + (c+1) +  ": Surr Min Intensity";
					appText += "	" +"C" + (c+1) +  ": Surr Max Intensity";
					appText += "	" +"C" + (c+1) +  ": Surr Average Intensity Min 5%";
					appText += "	" +"C" + (c+1) +  ": Surr Average Intensity Min 25%";
					appText += "	" +"C" + (c+1) +  ": Surr Average Intensity Max 5%";
					appText += "	" +"C" + (c+1) +  ": Surr Average Intensity Max 25%";
				}
			}
			
			tp1.append(appText);
			tp2.append(appText + this.getOneRowFooter(startDate));
			
			TextRoi txtID;
			double volume, surface, sphereRadius, sphereSurface;
			imp.setOverlay(new Overlay());
			RoiManager.getInstance().setVisible(false);
			RoiManager.getInstance().reset();			
			for(int i = 0; i < adipocytes.size(); i++) {
				if(keepAwake) {
					stayAwake();
				}
				{
					//write ID into image and save ROIs
					txtID = new TextRoi((int)Math.round(adipocytes.get(i).centerX[0]), 
							(int)Math.round(adipocytes.get(i).centerY[0]),
							df0.format(i+1), RoiFont);
					txtID.setStrokeColor(Color.WHITE);
					imp.getOverlay().add(txtID);
					RoiManager.getInstance().addRoi(txtID);			
				}
				for(int t = 0; t < adipocytes.get(i).voxelNumber.length; t++) {
					appText = name[task] + "	" + (i+1);
					appText += "	" + "";
					appText += "	" + (t+1);
					appText += "	" + (adipocytes.get(i).voxelNumber.length);
					appText += "	" + df6.format(adipocytes.get(i).centerX[t] * imp.getCalibration().pixelWidth);
					appText += "	" + df6.format(adipocytes.get(i).centerY[t] * imp.getCalibration().pixelHeight);
					appText += "	" + df6.format(adipocytes.get(i).centerZ[t] * imp.getCalibration().pixelDepth);
					appText += "	" + df0.format(adipocytes.get(i).voxelNumber[t]);
					if(imp.getNSlices()>1) {
						volume = (double)adipocytes.get(i).voxelNumber[t] 
								* imp.getCalibration().pixelWidth 
								* imp.getCalibration().pixelHeight 
								* imp.getCalibration().pixelDepth;
						appText += "	" + df6.format(volume);
						
						surface = adipocytes.get(i).getSurface(imp.getCalibration().pixelWidth,
								imp.getCalibration().pixelHeight,imp.getCalibration().pixelDepth,t);
						appText += "	" + df6.format(surface);
						
						// Calculate asphericity	
						sphereRadius = Math.pow((double)((volume*3.0)/(4.0*Math.PI)), (double)1/3.0);
						sphereSurface = (Math.PI * Math.pow(sphereRadius,2) * 4);
						appText += "	" + df6.format((double)surface/sphereSurface);
					}else {
						volume = (double)adipocytes.get(i).voxelNumber[t] 
								* imp.getCalibration().pixelWidth 
								* imp.getCalibration().pixelHeight;
						appText += "	" + df6.format(volume);
						
						surface = adipocytes.get(i).getSurface(imp.getCalibration().pixelWidth,
								imp.getCalibration().pixelHeight,1.0,t);
						appText += "	" + df6.format(surface);
						
						// Calculate asphericity	
						appText += "	" + df6.format((surface)/(2*Math.sqrt(volume*Math.PI)));
					}
					for(int c = 0; c < imp.getNChannels(); c++) {
						appText += "	" + df6.format(adipocytes.get(i).averageIntensity[t][c]);
						appText += "	" + df6.format(adipocytes.get(i).integratedIntensity[t][c]);
						appText += "	" + df6.format(adipocytes.get(i).medianIntensity[t][c]);
						appText += "	" + df6.format(adipocytes.get(i).sdIntensity[t][c]);
						appText += "	" + df6.format(adipocytes.get(i).minIntensity[t][c]);
						appText += "	" + df6.format(adipocytes.get(i).maxIntensity[t][c]);
					}
					
					if(quantifySurroundings) {
						appText += "	" + df6.format(adipocytes.get(i).voxelNumberSurr[t]);
						for(int c = 0; c < imp.getNChannels(); c++) {
							if(c == channelID-1) continue;

							appText += "	" + df6.format(adipocytes.get(i).averageIntensitySurr[t][c]);
							appText += "	" + df6.format(adipocytes.get(i).integratedIntensitySurr[t][c]);
							appText += "	" + df6.format(adipocytes.get(i).medianIntensitySurr[t][c]);
							appText += "	" + df6.format(adipocytes.get(i).sdIntensitySurr[t][c]);
							appText += "	" + df6.format(adipocytes.get(i).minIntensitySurr[t][c]);
							appText += "	" + df6.format(adipocytes.get(i).maxIntensitySurr[t][c]);
							appText += "	" + df6.format(adipocytes.get(i).min5pIntensitySurr[t][c]);
							appText += "	" + df6.format(adipocytes.get(i).min25pIntensitySurr[t][c]);
							appText += "	" + df6.format(adipocytes.get(i).max5pIntensitySurr[t][c]);
							appText += "	" + df6.format(adipocytes.get(i).max25pIntensitySurr[t][c]);
						}
					}
					
					tp1.append(appText);
					tp2.append(appText);
				}
				
			}
			tp1.append("");
		   	
		   	addFooter(tp1, startDate);				
			tp1.saveAs(filePrefix + ".txt");
			tp2.saveAs(filePrefix + "s.txt");

			IJ.saveAsTiff(imp,filePrefix+"_RP.tif");

			RoiManager.getInstance().runCommand("Save", filePrefix+"_IDs.zip");
			RoiManager.getInstance().reset();
			RoiManager.getInstance().setVisible(true);
			
			progress.updateBarText("Finished ...");
			
			/******************************************************************
			*** 							Finish							***	
			*******************************************************************/			
			{
				imp.unlock();	
				if(selectedTaskVariant.equals(taskVariant[1])){
					imp.changes = false;
					imp.close();
				}
				processingDone = true;
				break running;
			}				
		}	
		progress.updateBarText("finished!");
		progress.setBar(1.0);
		progress.moveTask(task);
	}
}

/**
 * Import settings from existing file
 */
private boolean importSettings() {
	java.awt.FileDialog fd = new java.awt.FileDialog((Frame) null, "Select files to add to list.");
	fd.setDirectory(System.getProperty("user.dir", "."));
	fd.setMultipleMode(false);
	fd.setMode(FileDialog.LOAD);
	fd.setVisible(true);
	File settingsFile = fd.getFiles()[0];
	
	if(settingsFile.equals(null)) {
		return false;
	}	
	
	channelID = -1;
	String readVersion = "none";
	double tempVoxelWidth = 0.0;
	double tempVoxelHeight = 0.0;
	
	//read individual channel settings
	String tempString;
	
	IJ.log("READING PREFERENCES:");
	try {
		FileReader fr = new FileReader(settingsFile);
		BufferedReader br = new BufferedReader(fr);
		String line = "";
		reading: while(true){
			try{
				line = br.readLine();	
				if(!line.equals("") && line.equals(null)){
					break reading;
				}
			}catch(Exception e){
				break reading;
			}
			
			if(line.contains("Plugin version:")) {
				readVersion = line.substring(line.lastIndexOf("	")+1);
				IJ.log("Loaded file comes from version " + readVersion);
			}
						
			if(line.contains("Voxel width:")) {
				tempVoxelWidth = Double.parseDouble(line.substring(line.lastIndexOf("	")+1));
			}
			
			if(line.contains("Voxel height:")) {
				tempVoxelHeight = Double.parseDouble(line.substring(line.lastIndexOf("	")+1));				
			}
			
			if(line.contains("Channel Nr:")){
					tempString = line.substring(line.lastIndexOf("	")+1);
					if(tempString.contains(",") && !tempString.contains("."))	tempString = tempString.replace(",", ".");
					channelID = Integer.parseInt(tempString);	
					IJ.log("Channel nr = " + channelID);
				
				line = br.readLine();	
				if(line.contains("Increase range in particle detection:	TRUE")){
					increaseRange = true;
					IJ.log("Increase Range: " + increaseRange);
				}else if(line.contains("Increase range in particle detection:	FALSE")){
					increaseRange = false;
					IJ.log("Increase Range: " + increaseRange);
				}else {
					IJ.error("Increase range information missing in file.");
					return false;
				}
				
				line = br.readLine();	
				if(line.contains("Minimum particle size")){
					if(readVersion.equals("0.0.1") || readVersion.equals("0.0.2") || readVersion.equals("0.0.3") 
							|| readVersion.equals("0.0.4") || readVersion.equals("0.0.5") || readVersion.equals("0.0.6")) {
						tempString = line.substring(line.lastIndexOf("	")+1);
						if(tempString.contains(",") && !tempString.contains("."))	tempString = tempString.replace(",", ".");
						minSizeCalibrated = Double.parseDouble(tempString);
						IJ.log("Min particle size (pixel) = " + df3.format(minSizeCalibrated));		
						if(tempVoxelWidth == 0.0 || tempVoxelHeight == 0.0) {
							GenericDialog gd = new GenericDialog(PLUGINNAME + " on " + System.getProperty("os.name") + " - set missing parameter");	
							//show Dialog-----------------------------------------------------------------
							gd.setInsets(5,0,0);		gd.addMessage(PLUGINNAME + ", Version " + PLUGINVERSION + ", \u00a9 2021-2022 JN Hansen", SuperHeadingFont);
							gd.setInsets(5,0,0);		gd.addMessage("The loaded file stems from an outdated version of " + PLUGINNAME, InstructionsFont);
							gd.setInsets(0,0,0);		gd.addMessage("Define the following parameter to ensure analysis with the correct settings:", InstructionsFont);
							gd.setInsets(5,0,0);		gd.addNumericField("Minimum particle size (calibrated unit^2, e.g., µm2)", minSizeCalibrated, 2);							
							
							gd.showDialog();
							//show Dialog-----------------------------------------------------------------

							//read and process variables--------------------------------------------------	
							{
								minSizeCalibrated = gd.getNextNumber();
							}
							//read and process variables--------------------------------------------------
							
							if (gd.wasCanceled()) {
								return false;
							}
							
							
							IJ.log("Manually defined: Min particle size (calibrated unit^2) = " + df6.format(minSizeCalibrated));		
						}else {
							minSizeCalibrated *= tempVoxelWidth * tempVoxelHeight;
							IJ.log("Converted: Min particle size (calibrated unit^2) = " + df6.format(minSizeCalibrated));							
						}
					}else if (readVersion.equals("none")){
						IJ.error("Version info missing in file.");
						return false;
					}else {
						tempString = line.substring(0, line.lastIndexOf("	"));
						tempString = tempString.substring(0, tempString.lastIndexOf("	"));
						tempString = tempString.substring(tempString.lastIndexOf("	")+1);
						if(tempString.contains(",") && !tempString.contains("."))	tempString = tempString.replace(",", ".");
						minSizeCalibrated = Double.parseDouble(tempString);
						IJ.log("Min particle size (calibrated unit^2) = " + df6.format(minSizeCalibrated));
					}
				}else {
					IJ.error("Particle size info missing in file.");
					return false;
				}
				

				line = br.readLine();	
				if(line.contains("Exclude option")){
					tempString = line.substring(line.lastIndexOf("	")+1);
					excludeSelection = "TBR";
					for(int i = 0; i < excludeOptions.length; i++) {
						if(tempString.equals(excludeOptions[i])) {
							excludeSelection = excludeOptions [i];
							break;
						}
					}
					if(excludeSelection.equals("TBR")) {
						IJ.error("Read exclude option is incorrect.");
						return false;
					}
					IJ.log("Exclude option = " + excludeSelection);
				}else {
					IJ.error("Exclude option missing.");
					return false;
				}
				
				line = br.readLine();	
				if(line.contains("Quantify Surrounding:	TRUE")){
					quantifySurroundings = true;
					IJ.log("Quantify Surrounding: " + quantifySurroundings);
					
					line = br.readLine();	
					if(line.contains("Surrounding: reference distance")){
						tempString = line.substring(line.lastIndexOf("	")+1);
						if(tempString.contains(",") && !tempString.contains("."))	tempString = tempString.replace(",", ".");
						refDistance = Double.parseDouble(tempString);	
						IJ.log("Surrounding: Reference Distance (calibrated unit) = " + refDistance);
					}else {
						IJ.error("Surrounding Ref Distance missing in file.");
						return false;
					}
				}else if(line.contains("Quantify Surrounding:	FALSE")){
					quantifySurroundings = false;
					IJ.log("Quantify Surrounding: " + quantifySurroundings);
					line = br.readLine();	
				}else {
					IJ.error("Surrounding information missing in file.");
					return false;
				}
				
				line = br.readLine();
				if(line.contains("Fuse included particles into one for quantification:	TRUE")){
					fuseParticles = true;
					IJ.log("Fuse Particles: " + fuseParticles);
				}else if(line.contains("Fuse included particles into one for quantification:	FALSE")){
					fuseParticles = false;
					IJ.log("Fuse Particles: " + fuseParticles);
				}else {
					IJ.error("Fuse-particles information missing in file.");
					return false;
				}
				
				if(quantifySurroundings && fuseParticles) {
					new WaitForUserDialog("Note: Surrounding will not be quantified as particles are fused into one.").show();
					quantifySurroundings = false;
				}
				
				break reading;
			}			
		}					
		br.close();
		fr.close();
	}catch (IOException e) {
		IJ.error("Problem with loading preferences");
		e.printStackTrace();
		return false;
	}
	
	if(channelID != -1) {
		if(readVersion.equals("none")) {
			IJ.error("Loaded file contains no version information - loaded file incorrect.");
			return false;
		}
		return true;
	}else {
		IJ.error("Unclear problem with loading preferences.");
		return false;
	}
}

/**
 * Show dialogs to enter settings
 * */
private boolean enterSettings() {
	GenericDialog gd = new GenericDialog(PLUGINNAME + " on " + System.getProperty("os.name") + " - set parameters");	
	//show Dialog-----------------------------------------------------------------
	//.setInsets(top, left, bottom)
	gd.setInsets(5,0,0);		gd.addMessage(PLUGINNAME + ", Version " + PLUGINVERSION + ", \u00a9 2021-2022 JN Hansen", SuperHeadingFont);
	gd.setInsets(5,0,0);		gd.addNumericField("Channel Nr (>= 1 & <= nr of channels) for quantification", channelID, 0);
	gd.setInsets(5,0,0);		gd.addCheckbox("Increase range for connecting adipocytes", increaseRange);	
	gd.setInsets(5,0,0);		gd.addNumericField("Minimum particle size (calibrated unit^2, e.g., µm2)", minSizeCalibrated, 2);
	gd.setInsets(5,0,0);		gd.addChoice("Additionally exclude...", excludeOptions, excludeSelection);
	gd.setInsets(5,0,0);		gd.addCheckbox("Quantify Surrounding | reference distance (calibrated unit, e.g., µm)", quantifySurroundings);	
	gd.setInsets(-23,100,0);		gd.addNumericField("", refDistance, 2);
	gd.setInsets(5,0,0);		gd.addCheckbox("Fuse included particles into one for quantification", fuseParticles);	
	
	gd.showDialog();
	//show Dialog-----------------------------------------------------------------

	//read and process variables--------------------------------------------------	
	{
		channelID = (int) gd.getNextNumber();
		increaseRange = gd.getNextBoolean();
		minSizeCalibrated = gd.getNextNumber();
		excludeSelection = gd.getNextChoice();
		quantifySurroundings = gd.getNextBoolean();
		refDistance = gd.getNextNumber();
		fuseParticles = gd.getNextBoolean();
	}
	//read and process variables--------------------------------------------------
	
	if (gd.wasCanceled()) return false;
	
	if(quantifySurroundings && fuseParticles) {
		new WaitForUserDialog("Note: Surrounding will not be quantified as particles are fused into one.").show();
		quantifySurroundings = false;
	}
	
	return true;
}

private void addFooter(TextPanel tp, Date currentDate){
	tp.append("");
	tp.append("Datafile was generated on " + FullDateFormatter2.format(currentDate) + " by '"
			+PLUGINNAME+"', an ImageJ plug-in by Jan Niklas Hansen (jan.hansen@uni-bonn.de, https://github.com/hansenjn/AdipoQ_Analyzer).");
	tp.append("The plug-in '"+PLUGINNAME+"' is distributed in the hope that it will be useful,"
			+ " but WITHOUT ANY WARRANTY; without even the implied warranty of"
			+ " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.");
	tp.append("Plug-in version:	V"+PLUGINVERSION);	
}


public String getOneRowFooter(Date currentDate){
	String appendTxt = "		" + ("Datafile was generated on " + FullDateFormatter2.format(currentDate) + " by the imagej plug-in '"+PLUGINNAME+"', " 
			+ "\u00a9 2017 - " + FullDateFormatter2.format(currentDate) + " Jan Niklas Hansen (jan.hansen@uni-bonn.de).");
	appendTxt += "	" + ("The plug-in '"+PLUGINNAME+"' is distributed in the hope that it will be useful,"
			+ " but WITHOUT ANY WARRANTY; without even the implied warranty of"
			+ " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.");
	appendTxt += "	" + ("Plug-in version: V"+PLUGINVERSION);
	return appendTxt;
}

private void addSettingsBlockToPanel(TextPanel tp, Date startDate, Date endDate, String name, ImagePlus imp) {
	tp.append("Plugin name:	" + PLUGINNAME);
	tp.append("Plugin version:	" + PLUGINVERSION);
	tp.append("Starting date:	" + FullDateFormatter.format(startDate));
	tp.append("Finishing date:	" + FullDateFormatter.format(endDate));
	tp.append("Image name:	" + name);
	tp.append("Image metadata:");
	tp.append("	Width [voxel]:	" + imp.getWidth());
	tp.append("	Height [voxel]:	" + imp.getHeight());
	tp.append("	Number of channels:	" + imp.getNChannels());
	tp.append("	Number of Slices:	" + imp.getNSlices());
	tp.append("	Number of Frames:	" + imp.getNFrames());
	tp.append("	Voxel width:	" + imp.getCalibration().pixelWidth);
	tp.append("	Voxel height:	" + imp.getCalibration().pixelHeight);
	tp.append("	Voxel depth:	" + imp.getCalibration().pixelDepth);
	tp.append("	Frame interval:	" + imp.getCalibration().frameInterval);
	tp.append("	Spatial unit:	" + imp.getCalibration().getUnit());
	tp.append("	Temporal unit:	" + imp.getCalibration().getTimeUnit());
	tp.append("");
	tp.append("Preferences:	");
	{
		tp.append("	Channel Nr:	" + df0.format(channelID));
		
		if(increaseRange){
			tp.append("	Increase range in particle detection:	TRUE");
		}else{
			tp.append("	Increase range in particle detection:	FALSE");
		}
		
		tp.append("	Minimum particle size (" + imp.getCalibration().getUnit() + "^2):	" + df6.format(minSizeCalibrated) + "	" + "Minimum particle size (px):	" + df6.format(minSize));
		
		tp.append("	Exclude option:	" + excludeSelection);
		
		if(quantifySurroundings){
			tp.append("	Quantify Surrounding:	TRUE");
			tp.append("	Surrounding: reference distance(" + imp.getCalibration().getUnit() + "):	" + df6.format(refDistance));
		}else{
			tp.append("	Quantify Surrounding:	FALSE");
			tp.append("");
		}
		
		if(fuseParticles){
			tp.append("	Fuse included particles into one for quantification:	TRUE");
		}else{
			tp.append("	Fuse included particles into one for quantification:	FALSE");
		}
	}
	tp.append("");
}

/**
 * @return a container that contains adipocyte objects
 * @param imp: Hyperstack image where one channel is binarized or semi-binarized
 * @param c: defines the channel of the Hyperstack image imp that shall serve for reconstruction; 1 <= c <= number of channels.
 * */
ArrayList<Adipocyte> analyzeAdipocytes (ImagePlus imp, int c){
	if(keepAwake) stayAwake();
	ImagePlus refImp = imp.duplicate();
	int nrOfPoints = 0;
	
	for(int z = 0; z < imp.getNSlices(); z++){
		for(int t = 0; t < imp.getNFrames(); t++){
			for(int x = 0; x < imp.getWidth(); x++){
				if(keepAwake) stayAwake();
				for(int y = 0; y < imp.getHeight(); y++){	
					if(imp.getStack().getVoxel(x, y, imp.getStackIndex(c, z+1, t+1)-1) > 0.0){
						nrOfPoints++;
					}
				}
			}
		}		
	}
	
	ArrayList<Adipocyte> adipos = new ArrayList<Adipocyte>((int)Math.round((double)nrOfPoints/(double)minSize));
	
	int pc100 = nrOfPoints/100; if (pc100==0){pc100 = 1;}
	int pc1000 = nrOfPoints/1000; if (pc1000==0){pc1000 = 1;}
	int floodFilledPc = 0, floodFilledPcOld = 0;
	int[][] floodNodes = new int[nrOfPoints][4];
	int floodNodeX, floodNodeY, floodNodeZ, floodNodeT, index = 0;
//	boolean touchesXY, touchesZ;
	ArrayList<AdipoPoint> preliminaryParticle;
	ArrayList<AdipoPoint> tempParticles = new ArrayList<AdipoPoint>(nrOfPoints);
	
//	int [] sliceCounter = new int [imp.getNSlices()];
	int [] frameCounter = new int [imp.getNFrames()];
	boolean keep;
	
	searchCells: for(int t = 0; t < imp.getNFrames(); t++){
		for(int z = 0; z < imp.getNSlices(); z++){
			for(int x = 0; x < imp.getWidth(); x++){
				for(int y = 0; y < imp.getHeight(); y++){		
					if(imp.getStack().getVoxel(x, y, imp.getStackIndex(c, z+1, t+1)-1) > 0.0){
//						touchesXY = false;
//						touchesZ = false;
						
						preliminaryParticle = new ArrayList<AdipoPoint>(nrOfPoints-floodFilledPc);
						preliminaryParticle.add(new AdipoPoint(x, y, z, t, refImp, c));
						
						imp.getStack().setVoxel(x, y, imp.getStackIndex(c, z+1, t+1)-1, 0.0);
						
//						if(x == 0 || x == imp.getWidth()-1)		touchesXY = true;
//						if(y == 0 || y == imp.getHeight()-1)	touchesXY = true;
//						if(z == 0 || z == imp.getNSlices()-1)	touchesZ = true;
						
						floodFilledPc++;
						
						//Floodfiller					
						floodNodeX = x;
						floodNodeY = y;
						floodNodeZ = z;
						floodNodeT = t;
						 
						index = 0;
						 
						floodNodes[0][0] = floodNodeX;
						floodNodes[0][1] = floodNodeY;
						floodNodes[0][2] = floodNodeZ;
						floodNodes[0][3] = floodNodeT;
						
						while (index >= 0){
							floodNodeX = floodNodes[index][0];
							floodNodeY = floodNodes[index][1];
							floodNodeZ = floodNodes[index][2];		
							floodNodeT = floodNodes[index][3];
							index--;            						
							if ((floodNodeX > 0) 
									&& imp.getStack().getVoxel(floodNodeX-1, floodNodeY, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1) > 0.0){
								
								preliminaryParticle.add(new AdipoPoint(floodNodeX-1,floodNodeY,floodNodeZ,floodNodeT,
										refImp, c));
								imp.getStack().setVoxel(floodNodeX-1, floodNodeY, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1, 0.0);
								
								index++;
								floodFilledPc++;
								
								floodNodes[index][0] = floodNodeX-1;
								floodNodes[index][1] = floodNodeY;
								floodNodes[index][2] = floodNodeZ;
								floodNodes[index][3] = floodNodeT;
							}
							if ((floodNodeX < (imp.getWidth()-1)) 
									&& imp.getStack().getVoxel(floodNodeX+1, floodNodeY, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1) > 0.0){
								
								preliminaryParticle.add(new AdipoPoint(floodNodeX+1,floodNodeY,floodNodeZ,floodNodeT, refImp, c));
								imp.getStack().setVoxel(floodNodeX+1, floodNodeY, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1, 0.0);
								
								index++;
								floodFilledPc++;
								
								floodNodes[index][0] = floodNodeX+1;
								floodNodes[index][1] = floodNodeY;
								floodNodes[index][2] = floodNodeZ;
								floodNodes[index][3] = floodNodeT;
							}
							if ((floodNodeY > 0) 
									&& imp.getStack().getVoxel(floodNodeX, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1) > 0.0){
								
								preliminaryParticle.add(new AdipoPoint(floodNodeX,floodNodeY-1,floodNodeZ,floodNodeT, refImp, c));
								imp.getStack().setVoxel(floodNodeX, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1, 0.0);
								
								index++;
								floodFilledPc++;
								
								floodNodes[index][0] = floodNodeX;
								floodNodes[index][1] = floodNodeY-1;
								floodNodes[index][2] = floodNodeZ;
								floodNodes[index][3] = floodNodeT;
							}                
							if ((floodNodeY < (imp.getHeight()-1)) 
									&& imp.getStack().getVoxel(floodNodeX, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1) > 0.0){
								
								preliminaryParticle.add(new AdipoPoint(floodNodeX,floodNodeY+1,floodNodeZ,floodNodeT, refImp, c));
								imp.getStack().setVoxel(floodNodeX, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1, 0.0);
								
								index++;
								floodFilledPc++;
								
								floodNodes[index][0] = floodNodeX;
								floodNodes[index][1] = floodNodeY+1;
								floodNodes[index][2] = floodNodeZ;
								floodNodes[index][3] = floodNodeT;
							}
							if ((floodNodeZ > 0) 
									&& imp.getStack().getVoxel(floodNodeX, floodNodeY, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1) > 0.0){
								
								preliminaryParticle.add(new AdipoPoint(floodNodeX,floodNodeY,floodNodeZ-1,floodNodeT, refImp, c));
								imp.getStack().setVoxel(floodNodeX, floodNodeY, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1, 0.0);
								
								index++;
								floodFilledPc++;
								
								floodNodes[index][0] = floodNodeX;
								floodNodes[index][1] = floodNodeY;
								floodNodes[index][2] = floodNodeZ-1;
								floodNodes[index][3] = floodNodeT;
							}                
							if ((floodNodeZ < (imp.getNSlices()-1)) 
									&& imp.getStack().getVoxel(floodNodeX, floodNodeY, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1) > 0.0){
								
								preliminaryParticle.add(new AdipoPoint(floodNodeX,floodNodeY,floodNodeZ+1,floodNodeT, refImp, c));
								imp.getStack().setVoxel(floodNodeX, floodNodeY, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1, 0.0);
								
								index++;
								floodFilledPc++;
								
								floodNodes[index][0] = floodNodeX;
								floodNodes[index][1] = floodNodeY;
								floodNodes[index][2] = floodNodeZ+1;
								floodNodes[index][3] = floodNodeT;
							} 
							if ((floodNodeT > 0) 
									&& imp.getStack().getVoxel(floodNodeX, floodNodeY, imp.getStackIndex(c, floodNodeZ+1, (floodNodeT-1)+1)-1) > 0.0){
								
								preliminaryParticle.add(new AdipoPoint(floodNodeX,floodNodeY,floodNodeZ,floodNodeT-1, refImp, c));
								imp.getStack().setVoxel(floodNodeX, floodNodeY, imp.getStackIndex(c, floodNodeZ+1, (floodNodeT-1)+1)-1, 0.0);
								
								index++;
								floodFilledPc++;
								
								floodNodes[index][0] = floodNodeX;
								floodNodes[index][1] = floodNodeY;
								floodNodes[index][2] = floodNodeZ;
								floodNodes[index][3] = floodNodeT-1;
							}                
							if ((floodNodeT < (imp.getNFrames()-1)) 
									&& imp.getStack().getVoxel(floodNodeX, floodNodeY, imp.getStackIndex(c, floodNodeZ+1, ((floodNodeT)+1)+1)-1) > 0.0){
								
								preliminaryParticle.add(new AdipoPoint(floodNodeX,floodNodeY,floodNodeZ,(floodNodeT)+1, refImp, c));
								imp.getStack().setVoxel(floodNodeX, floodNodeY, imp.getStackIndex(c, floodNodeZ+1, ((floodNodeT)+1)+1)-1, 0.0);
								
								index++;
								floodFilledPc++;
								
								floodNodes[index][0] = floodNodeX;
								floodNodes[index][1] = floodNodeY;
								floodNodes[index][2] = floodNodeZ;
								floodNodes[index][3] = (floodNodeT)+1;
							}
							if(increaseRange){
								// X, Y
								if ((floodNodeX > 0) && (floodNodeY > 0)  
										&& imp.getStack().getVoxel(floodNodeX-1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1) > 0.0){
									preliminaryParticle.add(new AdipoPoint(floodNodeX-1,floodNodeY-1,floodNodeZ,floodNodeT,
											refImp, c));
									imp.getStack().setVoxel(floodNodeX-1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX-1;
									floodNodes[index][1] = floodNodeY-1;
									floodNodes[index][2] = floodNodeZ;
									floodNodes[index][3] = floodNodeT;
								}
								if ((floodNodeX < (imp.getWidth()-1)) && (floodNodeY < (imp.getHeight()-1))
										&& imp.getStack().getVoxel(floodNodeX+1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX+1,floodNodeY+1,floodNodeZ,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX+1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX+1;
									floodNodes[index][1] = floodNodeY+1;
									floodNodes[index][2] = floodNodeZ;
									floodNodes[index][3] = floodNodeT;
								}
								if ((floodNodeX < (imp.getWidth()-1)) && (floodNodeY > 0) 
										&& imp.getStack().getVoxel(floodNodeX+1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX+1,floodNodeY-1,floodNodeZ,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX+1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX+1;
									floodNodes[index][1] = floodNodeY-1;
									floodNodes[index][2] = floodNodeZ;
									floodNodes[index][3] = floodNodeT;
								}                
								if ((floodNodeX > 0) && (floodNodeY < (imp.getHeight()-1)) 
										&& imp.getStack().getVoxel(floodNodeX-1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX-1,floodNodeY+1,floodNodeZ,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX-1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX-1;
									floodNodes[index][1] = floodNodeY+1;
									floodNodes[index][2] = floodNodeZ;
									floodNodes[index][3] = floodNodeT;
								}
								// Z-X
								if ((floodNodeX > 0) && (floodNodeZ > 0)
										&& imp.getStack().getVoxel(floodNodeX-1, floodNodeY, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX-1,floodNodeY,floodNodeZ-1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX-1, floodNodeY, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX-1;
									floodNodes[index][1] = floodNodeY;
									floodNodes[index][2] = floodNodeZ-1;
									floodNodes[index][3] = floodNodeT;
								}
								if ((floodNodeX < (imp.getWidth()-1)) && (floodNodeZ > 0)
										&& imp.getStack().getVoxel(floodNodeX+1, floodNodeY, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX+1,floodNodeY,floodNodeZ-1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX+1, floodNodeY, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX+1;
									floodNodes[index][1] = floodNodeY;
									floodNodes[index][2] = floodNodeZ-1;
									floodNodes[index][3] = floodNodeT;
								}
								if ((floodNodeX > 0) && (floodNodeZ < (imp.getNSlices()-1)) 
										&& imp.getStack().getVoxel(floodNodeX-1, floodNodeY, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX-1,floodNodeY,floodNodeZ+1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX-1, floodNodeY, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX-1;
									floodNodes[index][1] = floodNodeY;
									floodNodes[index][2] = floodNodeZ+1;
									floodNodes[index][3] = floodNodeT;
								}
								if ((floodNodeX < (imp.getWidth()-1)) && (floodNodeZ < (imp.getNSlices()-1)) 
										&& imp.getStack().getVoxel(floodNodeX+1, floodNodeY, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX+1,floodNodeY,floodNodeZ+1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX+1, floodNodeY, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX+1;
									floodNodes[index][1] = floodNodeY;
									floodNodes[index][2] = floodNodeZ+1;
									floodNodes[index][3] = floodNodeT;
								} 
								// Z-Y
								if ((floodNodeY > 0) && (floodNodeZ > 0)
										&& imp.getStack().getVoxel(floodNodeX, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX,floodNodeY-1,floodNodeZ-1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX;
									floodNodes[index][1] = floodNodeY-1;
									floodNodes[index][2] = floodNodeZ-1;
									floodNodes[index][3] = floodNodeT;
								}
								if ((floodNodeY < (imp.getHeight()-1)) && (floodNodeZ > 0)
										&& imp.getStack().getVoxel(floodNodeX, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX,floodNodeY+1,floodNodeZ-1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX;
									floodNodes[index][1] = floodNodeY+1;
									floodNodes[index][2] = floodNodeZ-1;
									floodNodes[index][3] = floodNodeT;
								}
								if ((floodNodeY > 0) && (floodNodeZ < (imp.getNSlices()-1)) 
										&& imp.getStack().getVoxel(floodNodeX, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX,floodNodeY-1,floodNodeZ+1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX;
									floodNodes[index][1] = floodNodeY-1;
									floodNodes[index][2] = floodNodeZ+1;
									floodNodes[index][3] = floodNodeT;
								} 
								if ((floodNodeY < (imp.getHeight()-1)) && (floodNodeZ < (imp.getNSlices()-1)) 
										&& imp.getStack().getVoxel(floodNodeX, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX,floodNodeY+1,floodNodeZ+1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX;
									floodNodes[index][1] = floodNodeY+1;
									floodNodes[index][2] = floodNodeZ+1;
									floodNodes[index][3] = floodNodeT;
								} 
								// X, Y - Z down
								if ((floodNodeX > 0) && (floodNodeY > 0) && (floodNodeZ > 0)  
										&& imp.getStack().getVoxel(floodNodeX-1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1) > 0.0){
									preliminaryParticle.add(new AdipoPoint(floodNodeX-1,floodNodeY-1,floodNodeZ-1,floodNodeT,
											refImp, c));
									imp.getStack().setVoxel(floodNodeX-1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX-1;
									floodNodes[index][1] = floodNodeY-1;
									floodNodes[index][2] = floodNodeZ-1;
									floodNodes[index][3] = floodNodeT;
								}
								if ((floodNodeX < (imp.getWidth()-1)) && (floodNodeY < (imp.getHeight()-1)) && (floodNodeZ > 0)
										&& imp.getStack().getVoxel(floodNodeX+1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX+1,floodNodeY+1,floodNodeZ-1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX+1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX+1;
									floodNodes[index][1] = floodNodeY+1;
									floodNodes[index][2] = floodNodeZ-1;
									floodNodes[index][3] = floodNodeT;
								}
								if ((floodNodeX < (imp.getWidth()-1)) && (floodNodeY > 0) && (floodNodeZ > 0)
										&& imp.getStack().getVoxel(floodNodeX+1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX+1,floodNodeY-1,floodNodeZ-1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX+1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX+1;
									floodNodes[index][1] = floodNodeY-1;
									floodNodes[index][2] = floodNodeZ-1;
									floodNodes[index][3] = floodNodeT;
								}                
								if ((floodNodeX > 0) && (floodNodeY < (imp.getHeight()-1)) && (floodNodeZ > 0)
										&& imp.getStack().getVoxel(floodNodeX-1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX-1,floodNodeY+1,floodNodeZ-1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX-1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ-1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX-1;
									floodNodes[index][1] = floodNodeY+1;
									floodNodes[index][2] = floodNodeZ-1;
									floodNodes[index][3] = floodNodeT;
								}
								// X, Y - Z up
								if ((floodNodeX > 0) && (floodNodeY > 0) && (floodNodeZ < (imp.getNSlices()-1)) 
										&& imp.getStack().getVoxel(floodNodeX-1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1) > 0.0){
									preliminaryParticle.add(new AdipoPoint(floodNodeX-1,floodNodeY-1,floodNodeZ+1,floodNodeT,
											refImp, c));
									imp.getStack().setVoxel(floodNodeX-1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX-1;
									floodNodes[index][1] = floodNodeY-1;
									floodNodes[index][2] = floodNodeZ+1;
									floodNodes[index][3] = floodNodeT;
								}
								if ((floodNodeX < (imp.getWidth()-1)) && (floodNodeY < (imp.getHeight()-1)) && (floodNodeZ < (imp.getNSlices()-1)) 
										&& imp.getStack().getVoxel(floodNodeX+1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX+1,floodNodeY+1,floodNodeZ+1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX+1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX+1;
									floodNodes[index][1] = floodNodeY+1;
									floodNodes[index][2] = floodNodeZ+1;
									floodNodes[index][3] = floodNodeT;
								}
								if ((floodNodeX < (imp.getWidth()-1)) && (floodNodeY > 0) && (floodNodeZ < (imp.getNSlices()-1)) 
										&& imp.getStack().getVoxel(floodNodeX+1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX+1,floodNodeY-1,floodNodeZ+1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX+1, floodNodeY-1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX+1;
									floodNodes[index][1] = floodNodeY-1;
									floodNodes[index][2] = floodNodeZ+1;
									floodNodes[index][3] = floodNodeT;
								}                
								if ((floodNodeX > 0) && (floodNodeY < (imp.getHeight()-1)) && (floodNodeZ < (imp.getNSlices()-1)) 
										&& imp.getStack().getVoxel(floodNodeX-1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1) > 0.0){
									
									preliminaryParticle.add(new AdipoPoint(floodNodeX-1,floodNodeY+1,floodNodeZ+1,floodNodeT, refImp, c));
									imp.getStack().setVoxel(floodNodeX-1, floodNodeY+1, imp.getStackIndex(c, (floodNodeZ+1)+1, (floodNodeT)+1)-1, 0.0);
									
									index++;
									floodFilledPc++;
									
									floodNodes[index][0] = floodNodeX-1;
									floodNodes[index][1] = floodNodeY+1;
									floodNodes[index][2] = floodNodeZ+1;
									floodNodes[index][3] = floodNodeT;
								}
							}
						}					
						//Floodfiller
						preliminaryParticle.trimToSize();
						
						keep = true;
						if(imp.getNFrames()>1) {
							/**
							 * Test size of particle in all t
							 * */
							
							Arrays.fill(frameCounter, 0);
							for(int p = 0; p < preliminaryParticle.size(); p++){
								frameCounter[preliminaryParticle.get(p).t]++;
							}
							
							for(int ti = 0; ti < frameCounter.length; ti++){
								if(frameCounter[ti] < minSize && frameCounter [ti] != 0){
									keep = false;
									break;
								}
							}
						}else if(preliminaryParticle.size()<minSize) {
							keep = false;
						}
						
						/**
						 * Check for touching XYZ
						 * */
						if(excludeSelection.equals(excludeOptions[1])){
							for(int p = 0; p < preliminaryParticle.size(); p++){
								if(preliminaryParticle.get(p).x==0) {
									keep = false;
									break;
								}else if(preliminaryParticle.get(p).x==imp.getWidth()-1) {
									keep = false;
									break;
								}else if(preliminaryParticle.get(p).y==0) {
									keep = false;
									break;
								}else if(preliminaryParticle.get(p).y==imp.getHeight()-1) {
									keep = false;
									break;
								}								
							}
						}else if(excludeSelection.equals(excludeOptions[2])) {
							for(int p = 0; p < preliminaryParticle.size(); p++){
								if(preliminaryParticle.get(p).x==0) {
									keep = false;
									break;
								}else if(preliminaryParticle.get(p).x==imp.getWidth()-1) {
									keep = false;
									break;
								}else if(preliminaryParticle.get(p).y==0) {
									keep = false;
									break;
								}else if(preliminaryParticle.get(p).y==imp.getHeight()-1) {
									keep = false;
									break;
								}
								
								if(preliminaryParticle.get(p).z==0) {
									keep = false;
									break;
								}else if(preliminaryParticle.get(p).z==imp.getNSlices()-1) {
									keep = false;
									break;
								}	
							}
						}
						
						if(keep){
							if(!fuseParticles) {
								adipos.add(new Adipocyte(preliminaryParticle, refImp, channelID, quantifySurroundings, refDistance));
							}
							tempParticles.addAll(preliminaryParticle);
						}

						preliminaryParticle.clear();
						preliminaryParticle = null;


						if(floodFilledPc%(pc100)<pc1000){						
							progress.updateBarText("Reconstruction of structures complete: " + df3.format(((double)(floodFilledPc)/(double)(nrOfPoints))*100) + "%");
							progress.addToBar(0.2*((double)(floodFilledPc-floodFilledPcOld)/(double)(nrOfPoints)));
							floodFilledPcOld = floodFilledPc;
						}	
					}				
				}	
			}
			if(keepAwake) stayAwake();
			if(floodFilledPc==nrOfPoints){					
				break searchCells;
			}
		}	
	}
				
	refImp.changes = false;
	refImp.close();
	
	tempParticles.trimToSize();
	
	if(fuseParticles) {
		adipos.add(new Adipocyte(tempParticles, refImp, channelID, false, 0.0));
	}
	
	progress.updateBarText("Reconstruction of particles complete: " + df3.format(((double)(floodFilledPc)/(double)(nrOfPoints))*100) + "%");
	progress.addToBar(0.4*((double)(floodFilledPc-floodFilledPcOld)/(double)(nrOfPoints)));
	
	progress.updateBarText("Write back to image...");
	//write back to image
		{
			for(int j = 0; j < tempParticles.size(); j++){
				imp.getStack().setVoxel(tempParticles.get(j).x,
					tempParticles.get(j).y, 
					imp.getStackIndex(c, tempParticles.get(j).z+1, 
					tempParticles.get(j).t+1)-1, 
					refImp.getStack().getVoxel(tempParticles.get(j).x, tempParticles.get(j).y, 
							imp.getStackIndex(c, tempParticles.get(j).z+1, tempParticles.get(j).t+1)-1));
			}
		}
	//write back to image
		
	return adipos;
}

/**
 * @param channel: 1 <= channel <= # channels
 * */
private static ImagePlus copyChannel(ImagePlus imp, int channel, boolean adjustDisplayRangeTo16bit, boolean copyOverlay){
	ImagePlus impNew = IJ.createHyperStack("channel image", imp.getWidth(), imp.getHeight(), 1, imp.getNSlices(), imp.getNFrames(), imp.getBitDepth());
	int index = 0, indexNew = 0;
	
	for(int x = 0; x < imp.getWidth(); x++){
		for(int y = 0; y < imp.getHeight(); y++){
			for(int s = 0; s < imp.getNSlices(); s++){
				for(int f = 0; f < imp.getNFrames(); f++){
					index = imp.getStackIndex(channel, s+1, f+1)-1;
					indexNew = impNew.getStackIndex(1, s+1, f+1)-1;
					impNew.getStack().setVoxel(x, y, indexNew, imp.getStack().getVoxel(x, y, index));
				}					
			}
		}
	}
	if(adjustDisplayRangeTo16bit)	impNew.setDisplayRange(0, 4095);
	if(copyOverlay)	impNew.setOverlay(imp.getOverlay().duplicate());
	
	imp.setC(channel);
   	impNew.setLut(imp.getChannelProcessor().getLut());
	
	impNew.setCalibration(imp.getCalibration());
	return impNew;
}

/**
 * @param channel: 1 <= channel <= # channels
 * */
private static ImagePlus copyChannelAsBinary(ImagePlus imp, int channel, boolean copyOverlay){
	ImagePlus impNew = IJ.createHyperStack("channel image", imp.getWidth(), imp.getHeight(), 1, imp.getNSlices(), imp.getNFrames(), 8);
	int index = 0, indexNew = 0;
	
	for(int x = 0; x < imp.getWidth(); x++){
		for(int y = 0; y < imp.getHeight(); y++){
			for(int s = 0; s < imp.getNSlices(); s++){
				for(int f = 0; f < imp.getNFrames(); f++){
					index = imp.getStackIndex(channel, s+1, f+1)-1;
					indexNew = impNew.getStackIndex(1, s+1, f+1)-1;
					if(imp.getStack().getVoxel(x, y, index) > 0.0) {
						impNew.getStack().setVoxel(x, y, indexNew, 255.0);
					}else {
						impNew.getStack().setVoxel(x, y, indexNew, 0.0);
					}
				}					
			}
		}
	}
	impNew.setDisplayRange(0, 255);
	if(copyOverlay)	impNew.setOverlay(imp.getOverlay().duplicate());
	
	imp.setC(channel);
//   	impNew.setLut(imp.getChannelProcessor().getLut());
	
	impNew.setCalibration(imp.getCalibration());
	return impNew;
}

/**
 * @param channel: 1 <= channel <= # channels
 * */
private static void emptyChannel(ImagePlus imp, int channel){
	int index = 0;
	for(int x = 0; x < imp.getWidth(); x++){
		for(int y = 0; y < imp.getHeight(); y++){
			for(int s = 0; s < imp.getNSlices(); s++){
				for(int f = 0; f < imp.getNFrames(); f++){
					index = imp.getStackIndex(channel, s+1, f+1)-1;
					imp.getStack().setVoxel(x, y, index, 0.0);
				}					
			}
		}
	}
}

ArrayList<Adipocyte> analyzeAdipocytesWithIn2D (ImagePlus imp, int c, String filePrefix, boolean labelImage){
	if(keepAwake) stayAwake();
	ImagePlus refImp;
	if(labelImage){
		progress.notifyMessage("Input is label image", ProgressDialog.LOG);
		refImp = copyChannel(imp,c,false,false);
	}else {
		progress.notifyMessage("Input is binary or semibinary", ProgressDialog.LOG);
		refImp = copyChannelAsBinary(imp, c, false);
	}
	ImagePlus refImp2 = copyChannel(imp,c, false, false);
	Prefs.blackBackground = true;
	
	//Reset ROIManager
	RoiManager rm = RoiManager.getInstance();
	if (rm==null) rm = new RoiManager();
	rm.setVisible(false);	
	rm.reset();

	int nrOfPoints = 0;
	{
		//Count
		progress.updateBarText("Detecting point number...");
	
		for(int x = 0; x < imp.getWidth(); x++){
			for(int y = 0; y < imp.getHeight(); y++){	
				if(refImp.getStack().getVoxel(x, y, 0) > 0.0){
					nrOfPoints++;
				}
			}
			if(keepAwake) stayAwake();
		}
	}
	
	
	int pc100 = nrOfPoints/100; if (pc100==0){pc100 = 1;}
	int pc1000 = nrOfPoints/1000; if (pc1000==0){pc1000 = 1;}
//	int pointsAdded = 0;
	
	ArrayList<AdipoPoint> preliminaryParticle;
//	ArrayList<AdipoPoint> tempParticleFill, tempParticleDel;
	ArrayList<AdipoPoint> fusedParticles = new ArrayList<AdipoPoint>(0);
	if(fuseParticles) {
		fusedParticles = new ArrayList<AdipoPoint>(nrOfPoints);
	}
		
	int included = 0;

	progress.updateBarText("Connecting " + nrOfPoints + " points ...");
	Roi roi;
	PolygonRoi pRoi;
	PolygonRoi rois [];
	Point tempPoints [];
			
	if(labelImage){
		//Go through image and retrieve ROIs with Wand tool, add the tools to the Manager, then retrieve an ROI array
		Wand wand;
		int wandMode = Wand.FOUR_CONNECTED;
		if(increaseRange) {
			wandMode = Wand.EIGHT_CONNECTED;
		}
		ArrayList<PolygonRoi> tempRois = new ArrayList<PolygonRoi>((int) (nrOfPoints/minSize));
		for(int x = 0; x < refImp.getWidth(); x++){
			for(int y = 0; y < refImp.getHeight(); y++){
				if(refImp.getStack().getVoxel(x, y, 0) > 0.0){
					wand = new Wand(refImp.getProcessor());
					wand.autoOutline(x, y, 0.0, wandMode);
					if (wand.npoints==0){
						IJ.error("Wand error:"+x+"/"+y);
					}
					pRoi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Wand.allPoints()?Roi.FREEROI:Roi.TRACED_ROI);
					tempRois.add(pRoi);
					//delete from image
					tempPoints = pRoi.getContainedPoints();
					for(int tp = 0; tp < tempPoints.length; tp++) {
						refImp.getStack().setVoxel(tempPoints[tp].x, tempPoints[tp].y, 0, 0.0);		
					}
				}
			}
		}
		
		refImp.changes = false;
		refImp.close();		
		refImp = copyChannel(imp,c,false,false);
		
		tempRois.trimToSize();
		rois = new PolygonRoi [tempRois.size()];
		for(int rI = 0; rI < tempRois.size(); rI++) {
			rois [rI] = tempRois.get(rI);
			if(rI%100==0){
				progress.updateBarText("Converting particles: " + df3.format(((double)(rI)/(double)(rois.length))*100) + "%");
				progress.addToBar(0.2/(rois.length));
			}
		}
		tempRois.clear();
		tempRois = null;
	}else{
		//Use Particle Analyzer to determine the ROIs
		int optionsForPA = ParticleAnalyzer.SHOW_NONE;	
		if(!increaseRange) {
			optionsForPA += ParticleAnalyzer.FOUR_CONNECTED;
			optionsForPA += ParticleAnalyzer.INCLUDE_HOLES;	
		}
		optionsForPA += ParticleAnalyzer.ADD_TO_MANAGER;
		if(excludeSelection.equals(excludeOptions[1]) || excludeSelection.equals(excludeOptions[2])){
			optionsForPA += ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
		}
		
		ParticleAnalyzer pA = new ParticleAnalyzer(optionsForPA, 0, null, minSize, Integer.MAX_VALUE, 0.0, 1.0);
		pA.analyze(refImp);
				
//		refImp.show();
//		new WaitForUserDialog("Test").show();
//		refImp.hide();
		
		rois = new PolygonRoi [rm.getRoisAsArray().length];
		for(int rI = 0; rI < rm.getRoisAsArray().length; rI++) {
			rois [rI] = (PolygonRoi) rm.getRoisAsArray()[rI];
			if(rI%20==0){
				progress.updateBarText("Converting particles: " + df3.format(((double)(rI)/(double)(rois.length))*100) + "%");
				progress.addToBar(0.2/(rois.length));
			}
		}
		pA = null;
	}
	progress.notifyMessage("Particles obtained. Detected " + rois.length + " particles!", ProgressDialog.LOG);
	rm.reset();
	
	preliminaryParticle = new ArrayList<AdipoPoint>(0);
	ArrayList<Adipocyte> adipos = new ArrayList<Adipocyte>(rois.length);
	
	
	ImagePlus [] SurrMapAverage = new ImagePlus [imp.getNChannels()];
	ImagePlus [] SurrMapSD = new ImagePlus [imp.getNChannels()];
	if(saveSurrMaps) {
		for(int ci = 0; ci < SurrMapAverage.length; ci++) {
			if(ci == c-1) {
				SurrMapAverage [ci] = new ImagePlus();
				SurrMapSD [ci] = new ImagePlus();
				continue;
			}
			SurrMapAverage [ci] = IJ.createHyperStack("Surr-Map Average", imp.getWidth(), imp.getHeight(), 1, imp.getNSlices(), imp.getNFrames(), imp.getBitDepth());
			SurrMapAverage [ci].setCalibration(imp.getCalibration());
			SurrMapSD [ci] = SurrMapAverage [ci].duplicate();
		}		
	}
	
	//Empty channel first, then write back
	emptyChannel(imp, c);
	for(int r = 0; r < rois.length; r++) {
		roi = rois [r];
				
		preliminaryParticle.ensureCapacity(roi.getContainedPoints().length);
		
		tempPoints = roi.getContainedPoints();
		for(int tp = 0; tp < tempPoints.length; tp++) {
			if(refImp.getStack().getVoxel(tempPoints[tp].x, tempPoints[tp].y, 0) > 0.0){
				preliminaryParticle.add(new AdipoPoint(tempPoints[tp].x, tempPoints[tp].y,0,0, refImp2, 1));
			}			
		}
		
		//Analysis
		preliminaryParticle.trimToSize();
				
		if(preliminaryParticle.size()>=minSize) {
			included++;
			if(!fuseParticles) {
				progress.updateBarText("Making adipocyte: " + df3.format(((double)(r)/(double)(rois.length))*100) + "%");
				adipos.add(new Adipocyte(preliminaryParticle, imp, channelID, quantifySurroundings, refDistance, roi));	
				progress.updateBarText("Made adipocyte: " + df3.format(((double)(r)/(double)(rois.length))*100) + "%");		
				
				// Create Surroundings Maps
				if(saveSurrMaps) {
					for(int ci = 0; ci < imp.getNChannels(); ci++) {
						if(ci == c-1) continue;
						for(int j = 0; j < preliminaryParticle.size(); j++){
							SurrMapAverage [ci].getStack().setVoxel(preliminaryParticle.get(j).x, preliminaryParticle.get(j).y,
								imp.getStackIndex(1, preliminaryParticle.get(j).z+1, preliminaryParticle.get(j).t+1)-1, 
									adipos.get(adipos.size()-1).averageIntensitySurr[preliminaryParticle.get(j).t][ci]);
							
							SurrMapSD [ci].getStack().setVoxel(preliminaryParticle.get(j).x, preliminaryParticle.get(j).y,
									imp.getStackIndex(1, preliminaryParticle.get(j).z+1, preliminaryParticle.get(j).t+1)-1, 
										adipos.get(adipos.size()-1).sdIntensitySurr[preliminaryParticle.get(j).t][ci]);
						}
					}
				}
			}else {
				fusedParticles.addAll(preliminaryParticle);
			}
			if(saveRois) {
				roi.setName("ID " + included);
				rm.addRoi(roi);
			}
			
			//Write to original image
			for(int j = 0; j < preliminaryParticle.size(); j++){
//				try {
					imp.getStack().setVoxel(preliminaryParticle.get(j).x,
							preliminaryParticle.get(j).y,
							imp.getStackIndex(c, preliminaryParticle.get(j).z+1, 
							preliminaryParticle.get(j).t+1)-1, 
							refImp2.getStack().getVoxel(preliminaryParticle.get(j).x, 
									preliminaryParticle.get(j).y, 
									refImp2.getStackIndex(1, preliminaryParticle.get(j).z+1, 
											preliminaryParticle.get(j).t+1)-1));					
//				}catch(Exception e) {
//					IJ.log("PROBLEM " + preliminaryParticle.get(j).x + " - " + preliminaryParticle.get(j).y 
//							+ " - " + preliminaryParticle.get(j).z + " - " + preliminaryParticle.get(j).t);
//				}
			}
		}
		preliminaryParticle.clear();
		
		if(r%20==0){
			progress.updateBarText("Reconstruction of particles complete: " + df3.format(((double)(r)/(double)(rois.length))*100) + "%");
			progress.addToBar(0.2/(rois.length));
//			progress.notifyMessage("Particle conversion done:" + df3.format(((double)(r)/(double)(rois.length))*100) + "%", ProgressDialog.LOG);
//			if(r%60==0){
//				IJ.log("Particle reconstruction done:" + df3.format(((double)(r)/(double)(rois.length))*100) + "%");				
//			}
		}
	}
	preliminaryParticle = null;			
	refImp.changes = false;
	refImp.close();
		
	if(fuseParticles) {
		fusedParticles.trimToSize();
		adipos.add(new Adipocyte(fusedParticles, imp, channelID, false, 0.0));
		fusedParticles.clear();
		fusedParticles = null;
	}
	
	if(saveSurrMaps) {
		for(int ci = 0; ci < SurrMapAverage.length; ci++) {
			progress.updateBarText("Saving surroundings maps now... C" + (ci+1));
			if(ci == c-1) continue;
						
			IJ.run(SurrMapAverage [ci], "Fire","");
			IJ.run(SurrMapSD [ci], "Fire","");
			
			IJ.saveAsTiff(SurrMapAverage [ci],filePrefix+"_SurrAVG_C" + (ci+1) + ".tif");
			IJ.saveAsTiff(SurrMapSD [ci],filePrefix+"_SurrSD_C" + (ci+1) + ".tif");
			
		}
		for(int ci = 0; ci < SurrMapAverage.length; ci++) {
			SurrMapAverage [ci].changes = false;
			SurrMapSD [ci].changes = false;
			SurrMapAverage [ci].close();
			SurrMapSD [ci].close();	
		}
	}
	return adipos;
}

private void stayAwake() {
	try {
		robo.mouseMove(MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y);
	} catch (Exception e) {
	}
}

static double getMaxPercentFromSortedArray(double [] sortedList, double percent){
	return getAverageOfRange(sortedList,sortedList.length-(int)Math.round(sortedList.length/100.0*percent), sortedList.length-1);
}

static double getMaxPercent(double [] list, double percent){			
	double [] array = Arrays.copyOf(list, list.length);
	Arrays.sort(array);
	double maxTenPercent = getAverageOfRange(array,list.length-(int)Math.round(list.length/100.0*percent), list.length-1);
	array = null;
	return maxTenPercent;
}

static double getMinPercentFromSortedArray(double [] sortedList, double percent){
	return getAverageOfRange(sortedList,0,(int)Math.round(sortedList.length/100.0*percent)-1);
}

static double getMinPercent(double [] list, double percent){			
	double [] array = Arrays.copyOf(list, list.length);
	Arrays.sort(array);
	double minTenPercent = getAverageOfRange(array,0,(int)Math.round(array.length/100.0*percent)-1);
	array = null;
	return minTenPercent;
}



static double getAverageOfRange(double [] values, int startIndex, int endIndex){
	double average = 0.0;	
	for(int x = startIndex; x <= endIndex; x++){
		average += values [x];
	}	
	return (average / (double)(endIndex-startIndex+1));		
}

}//end main class
