// package de.akesting.utils;
//
// import java.io.PrintWriter;
//
// import org.jdom.Element;
//
// // TODO
// // Boolean abfrage ob isEnabled() .. um Format.Aufrufe zu sparen!!!
// // FileOutput und XML Steuerung optional ...
// // Minimum an Output zulassen: Logger.out == immer, Logger.log==optional,
// // Logger.err immer
//
// public class Logger {
//
// // default configuration:
//
// private static boolean log2Console = true;
// private static boolean logErrorOnly = false;
// private static boolean log2File = false;
//
// private static PrintWriter fstrLog = null;
// private static final String endingLogging = "log";
//
// private Logger() {
// }
//
// public static void configure(String projectName, Element elem) {
// /*
// * if(XmlUtils.containsAttribute(elem, XmlElements.LoggerLog2Console)){
// * log2Console = XmlUtils.getBooleanValue(elem, XmlElements.LoggerLog2Console);
// * }
// *
// * if(XmlUtils.containsAttribute(elem, XmlElements.LoggerLogErrorOnly)){
// * logErrorOnly = XmlUtils.getBooleanValue(elem, XmlElements.LoggerLogErrorOnly);
// * }
// *
// * if(XmlUtils.containsAttribute(elem, XmlElements.LoggerLog2File)){
// * log2File = XmlUtils.getBooleanValue(elem, XmlElements.LoggerLog2File);
// * }
// *
// * if(log2File){
// * String filename = projectName+"."+endingLogging;
// * // open file handle
// * try{
// * fstrLog = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));
// * } catch (java.io.IOException e) {
// * Logger.err("Error  " + "Cannot open file " + filename);
// * e.printStackTrace();
// * }
// * }
// */
// }
//
// public static boolean isLogging2Console() {
// return log2Console;
// }
//
// public static void setLogging2Console(boolean isOn) {
// log2Console = isOn;
// }
//
// public static boolean logIsEnabled() {
// return ((log2Console || log2File) && !logErrorOnly);
// }
//
// // print out in any case:
// public static void out(String msg) {
// System.out.println("<OUT> " + msg);
// if (fstrLog != null) {
// fstrLog.println("<OUT> " + msg);
// fstrLog.flush();
// }
// }
//
// public static void log(String msg) {
// if (logErrorOnly)
// return;
// if (log2Console)
// System.out.println("<LOG> " + msg);
// if (fstrLog != null) {
// fstrLog.println("<LOG> " + msg);
// fstrLog.flush();
// }
// }
//
// public static void err(String msg) {
// if (fstrLog != null) {
// fstrLog.println("<ERROR> " + msg);
// fstrLog.flush();
// }
// if (log2Console || fstrLog == null) {
// System.err.println("<ERROR> " + msg);
// }
// }
//
// public static void err(boolean withExit, String msg) {
// err(msg);
// System.exit(0);
// }
// }
