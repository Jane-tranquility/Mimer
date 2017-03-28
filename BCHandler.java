/*--------------------------------------------------------
1. Jing Li / Feb 19, 2017

2. Java version used: Version 8 Update 60 (build 1.8.0_60-b27) 

3. Precise command-line compilation examples / instructions:
> sh BCHandlerCompile.sh

4. Precise examples / instructions to run this program:
In separate shell windows:
> sh BCHandlerRun.sh

5. List of files needed for running the program.
> BCHandler.java
> BCHandlerCompile.sh
> BCHandlerRun.sh
> shim.sh
> mimer-data.xyz

6. I used Applescript to build an application to associate shim.sh with my personal .xyz file
----------------------------------------------------------*/

import java.io.*;  // Get the Input Output libraries
import java.net.*; // Get the Java networking libraries
import java.util.*;  //get the utilities libraries
import com.thoughtworks.xstream.XStream;  //get the xstram libraries
import com.thoughtworks.xstream.io.xml.DomDriver; //get the sml libraries

/*
 * define a new class myDataArray, containing array of Strings
 * @num_lines: number of strings in the array
 * @lined: String array, maximum number of string is 10
*/
class myDataArray {
  int num_lines = 0;                   //define the number of strings in the array
  String[] lines = new String[8];      //String array, maximum number of string is 8
}

/*
 * define the BCHandler class, which would be invoked from shim.sh file with a file name as the first argument
 * and send the contents of the file to MyWebServer through host name and port number 2570
 * it serves as the client side of the back channel communication with the server
*/
public class BCHandler{
	private static String XMLfileName = "C:\\temp\\mimer.output";   //define the path for the output
  private static PrintWriter      toXmlOutputFile;     //define a PrintWriter to write the messages to a temporary file
  private static File             xmlFile;         //create a file
  private static BufferedReader   fromMimeDataFile; //create a BufferedReader

  public static void main(String args[]) {
    String serverName;       //define a string serverName
    int i = 0;
    myDataArray da = new myDataArray();   //define da as an String array using class myDataArray, the content is from the file of the first argument
    XStream xstream = new XStream();
    String[] testLines = new String[4];  
    myDataArray daTest = new myDataArray();  //define daTest as an String array using class myDataArray, the content is the deserialized data from the xml messages
    if (args.length < 1) serverName = "localhost"; //if doesn't give any server name, use localhost
    else serverName = args[0];                     //otherwise use the server name
    	
    System.out.println("Jing Li's back channel Client.\n");
    System.out.println("Using server: " + serverName + ", Port: 2540 / 2570");       //print out some info on the console

   		try {
     		System.out.println("Executing the java application.");
     		System.out.flush();
      	Properties p = new Properties(System.getProperties());   //get properties
      
      	String argOne = p.getProperty("firstarg");   //get the first arg which is the file name
      	System.out.println("First var is: " + argOne);  //print the file name out
      
      	fromMimeDataFile = new BufferedReader(new FileReader(argOne));   //read from the file
      		
      	while(((da.lines[i++] = fromMimeDataFile.readLine())!= null) && i < 8){   // Only allows for eight lines of data in input file plus safety:
				  System.out.println("Data is: " + da.lines[i-1]);     //print out each line out on the console
      	}
      	da.num_lines = i-1;   //to count which line is it, whether it is off the limit
      		
      	String xml = xstream.toXML(da);   //marshal the da to xml format
      	sendToBC(xml, serverName);        //use the method defined below to send the xml messages to the server through back channel communication

      	System.out.println("\n\nHere is the XML version:");
	  		System.out.print(xml);    //print out the xml format of the file on the console

	  		daTest = (myDataArray) xstream.fromXML(xml); // deserialize data from the xml format

	  		System.out.println("\n\nHere is the deserialized data: ");
	  		for(int k=0; k < daTest.num_lines; k++){System.out.println(daTest.lines[k]);}   //print out each line of the deserialized data on the client console
	  		System.out.println("\n");


      	xmlFile = new File(XMLfileName);     //create a new file according to the path name
      	if (xmlFile.exists() == true && xmlFile.delete() == false){     //if the file exists and cannot be deleted, throw IOException
				  throw (IOException) new IOException("XML file delete failed.");
      	}
      	xmlFile = new File(XMLfileName);       //if the file can't be created, throw an IOException 
      	if (xmlFile.createNewFile() == false){
				  throw (IOException) new IOException("XML file creation failed.");
      	}else{
				toXmlOutputFile = new PrintWriter(new BufferedWriter(new FileWriter(XMLfileName)));
				toXmlOutputFile.println("First arg to Handler is: " + argOne + "\n");
				toXmlOutputFile.println(xml);        //write the xml format of the file to the new file
				toXmlOutputFile.close();
      	}
    	}catch (Throwable e) {
      		e.printStackTrace();
    	}
  	}

    /*
     * the sendToBC method creates a socket connecting with the server at port 2570
     * and send the xml messages to the server
    */
  	static void sendToBC (String sendData, String serverName){
    	Socket sock;
    	BufferedReader fromServer;
    	PrintStream toServer;
    	String textFromServer;
    	try{
     		sock = new Socket(serverName, 2570);  //create the connection with the server at port 2570
     		toServer   = new PrintStream(sock.getOutputStream());// Will be blocking until we get ACK from server that data sent
      	fromServer = new  BufferedReader(new InputStreamReader(sock.getInputStream()));  //get messages from server
      
      	toServer.println(sendData);   //send the xml messages to the server at port 2570
      	toServer.println("end_of_xml"); //send the end of xml
      	toServer.flush(); 
      		
      	System.out.println("Blocking on acknowledgment from Server... ");   // Read two or three lines of response from the server,
     		textFromServer = fromServer.readLine();                             // and block while synchronously waiting:
      	if (textFromServer != null){System.out.println(textFromServer);}
      	sock.close();
    	} catch (IOException x) {
      		System.out.println ("Socket error.");
      		x.printStackTrace ();
    	}
  	}
}