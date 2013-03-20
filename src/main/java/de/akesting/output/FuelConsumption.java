package de.akesting.output;

// package de.tudresden.output;
//
// import java.io.PrintWriter;
// import java.util.Locale;
//
// import org.jdom.Element;
//
// import de.tudresden.consumption.Consumption;
// import de.tudresden.utils.FileUtils;
// import de.tudresden.xml.XmlElements;
// import de.tudresden.xml.XmlUtils;
//
// public class FuelConsumption {
//
// private Consumption consumption = null;
//
// private boolean withJanteCalc; //parameter
//
//
// private double dt; // integration step
// private double dtOut; // output step
//
// private PrintWriter fstr=null;
//
// private double totalAccumFuel_l;
// private double trajAccumFlow_l;
// private double trajAccumFlow100;
//
// private OutputGrid grid;
//
//
//
// private boolean isReverseDirection;
// private double xStart;
// private double xEnd;
// private double tStart;
// private double tEnd;
//
// public FuelConsumption(String filename, Element elem, OutputGrid grid){
//
// this.grid = grid;
// isReverseDirection = grid.isReverseDirection();
//
// fstr = FileUtils.getWriter(filename);
//
// consumption = new Consumption(elem);
//
// xStart = grid.xStartGrid();
// xEnd = grid.xEndGrid();
// tStart = grid.tStartGrid();
// tEnd = grid.tEndGrid();
//
//
//
// dt = XmlUtils.getDoubleValue(elem, XmlElements.ConsumptionDt);
// dtOut = XmlUtils.getDoubleValue(elem, XmlElements.ConsumptionDtOut);
//
// withJanteCalc= XmlUtils.getBooleanValue(elem, XmlElements.ConsumptionCalcWithJante);
//
//
// if(fstr!=null){
// fstr.printf(Locale.US, "# Engine filename = %s %n", consumption.getEngineFilename());
// fstr.printf(Locale.US, "# Conversion factor for carbon dioxide: 1liter diesel = 2.65 kg CO_2, 1liter gasoline = 2.32 kg CO_2%n");
// fstr.printf("# Spatiotemporal intervals for calculating the fuel consumption: %n");
// fstr.printf(Locale.US, "# Space x : [%.2f, %.2f]m = [%.2f, %.2f]km %n",
// xStart, xEnd, xStart/1000, xEnd/1000);
// fstr.printf(Locale.US, "# Time  t : [%.2f, %.2f]s = [%.3f, %.3f]h %n",
// tStart, tEnd, tStart/3600, tEnd/3600);
// fstr.printf(Locale.US, "# integration dt = %fs %n", dt);
// fstr.printf(Locale.US, "# output dt      = %fs %n", dtOut);
// fstr.printf(Locale.US, "# %-7s  %-8s  %-8s  %-8s %-8s  %-8s%n", "t[s]", "travelTime[s]", "meanSpeed[m/s]", "fuelRate[l/s]",
// "l/100km/veh", "accumFuel[l]");
// fstr.flush();
// }
//
// calcFuelCons();
//
//
// }
//
// public double calcMinFuelFlow(double v, double acc){
// double[] result = consumption.getMinFuelFlowLimited(v, acc, withJanteCalc);
// return result[0]*1000; // in liter per second
// }
//
// private void calcFuelCons(){
// System.out.println("FuelConsumption: calcFuelCons() .... ");
// // main loop:
// totalAccumFuel_l = 0; //global
// double t=tStart;
// while(t<=tEnd){
//
// double travelTime = integrateTraj(t);
// //System.out.printf("calcFuelCons: t=%.2f, tEnd=%.2f travelTime=%f%n", t, tEnd, travelTime);
// if(travelTime>=0){
// totalAccumFuel_l += trajAccumFlow_l*dtOut;
// double vMean=Math.abs(xEnd-xStart)/Math.abs(travelTime);
// double fuelCons100km = 1e5*trajAccumFlow_l/Math.abs(xEnd-xStart);//1e5*trajAccumFlow_l/vMean;
// fstr.printf(Locale.US, "%.5e  %.5e  %.5e  %.5e  %.5e  %.5e", t, travelTime, vMean, trajAccumFlow_l, fuelCons100km,
// trajAccumFlow100/*totalAccumFuel_l*/ );
// fstr.printf(Locale.US, " %s %n", FormatUtils.getFormatedTime(t) );
// fstr.flush();
// }
// t += dtOut;
// }; //of while
// fstr.close();
// }
//
//
//
// // return travel time !!!
// private double integrateTraj(double t0){
// trajAccumFlow_l = 0; // global var
// trajAccumFlow100 = 0;
// double x = (isReverseDirection)? xEnd : xStart;
// double t = t0;
// double vOld=0;
// double dx=0;
// long count=0;
// while( !passedSection(x, isReverseDirection) ){
// if( !grid.isDataAvailable(x, t) ) return -1; // exit !!!
// double vNew = grid.getSpeedResult(x,t);
// dx = vNew*dt;
// x += (isReverseDirection) ? -dx : dx;
// double dv = (t==t0)? 0 : vNew-vOld ;
// double acc = dv/dt; // !!!
// vOld = vNew;
// t += dt;
// double fuelFlow = this.calcMinFuelFlow(vNew, acc);
// //aber fuer v=0, a=0 gibt es 10000 l/h als "error" -> quick hack
// //setze im stillstand 1 l/h an !!!
// //dieser fall kommt in ASM daten quasi nicht vor, aber trotzdem hier
//
// //fuelAccum += (instFuelCons_m3>0.002) ? dt*instFuelCons_m3/10. : dt*1000*instFuelCons_m3;
//
// trajAccumFlow_l += fuelFlow*dt; // in liter
// trajAccumFlow100 += 1000*fuelFlow/vNew;
// count++;
// }
// trajAccumFlow100 = 1e5*trajAccumFlow100/count;
// return (t-t0);
// }
//
// private boolean passedSection(double x, boolean isReverse){
// return( (isReverse)? (x <= xStart) : (x >= xEnd) );
// }
//
// }
