/*--------------------------------------------------------
1. Jing Li / Feb 19, 2017

2. Java version used: Version 8 Update 60 (build 1.8.0_60-b27) 

3. Precise command-line compilation examples / instructions:
> sh MyWebServerCompile.sh

4. Precise examples / instructions to run this program:
In separate shell windows:
> sh MyWebServerRun.sh

5. List of files needed for running the program.
> MyWebServer.java
> MyWebServerCompile.sh
> MyWebServerRun.sh

----------------------------------------------------------*/

import java.io.*;                 //import everything in package java.io
import java.net.*;                //import everything in package java.net
import java.util.*;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class MyWebServer {

	public static void main(String[] args) throws IOException {
		
		int q_len=6;       //the number of queues can get to wait in operating system
		int port=2540;     //the port number is 2540
		Socket sock;

		BCLooper AL = new BCLooper(); //define an instance of runnable BCLooper class, the definition of the class is as belowed
		Thread t = new Thread(AL); // opens up a side thread listening to port 2570 for xml file
		t.start();  // start the thread

		ServerSocket servsock=new ServerSocket(port, q_len);   //create a socket conneting with the port number
		System.out.println("Jing Li's server starts to listen at port 2540.\n");
		while (true){
			sock=servsock.accept();              //socket is waiting to be connected with the client      
			new Worker(sock).start();              //use the Worker thread to do the work     
		}

	}

}
/*
 * The BCLooper class is used to run a new thread to listen at port 2570
 * which is used for the BCHandler to pass the file content back to the server
*/
class BCLooper implements Runnable {
    public static boolean adminControlSwitch = true;   //define a control variable to control whether the new thread is working or not
  
    public void run(){ // running the loop in the new thread
    	System.out.println("In BC Looper thread, waiting for 2570 connections");
    
    	int q_len = 6; // Number of requests for OpSys to queue 
    	int port = 2570;  // Listen here for Back Channel Connections
    	Socket sock;
    
    	try{
    		ServerSocket servsock = new ServerSocket(port, q_len);     //create a socket listening for the back channel communications
       		while (adminControlSwitch) {     //if the admin control of the thread is open, keeping listening to the clients
				sock = servsock.accept();    //block the tread while listening to the new connections
				new BCWorker (sock).start(); //if connected with client, start a new BCWorker thread
        	}
    	}catch (IOException ioe) {System.out.println(ioe);}    //catch the IOException
  	}
}

/*
 * define a new class myDataArray, containing array of Strings
 * @num_lines: number of strings in the array
 * @lined: String array, maximum number of string is 10
*/
class myDataArray {
    int num_lines = 0;       //define the number of strings in the array
    String[] lines = new String[10];   //String array, maximum number of string is 10
}

/*
 * BCWorking extends the class Thread, and it would get running when there's a back channel communication
*/
class BCWorker extends Thread {
    private Socket sock;                  //define a Socket to connect with clients
    private int i;                        
    BCWorker (Socket s){sock = s;}        //construction for the class
    PrintStream out = null;               //define an PrintStream for sending messages back to the clients
    BufferedReader in = null;             //define a BufferedReader for getting messages from clients

    String[] xmlLines = new String[15];   //define an String array containing 15 items
    String[] testLines = new String[10];  //define an String array containing 10 items
    String xml;
    String temp;
    XStream xstream = new XStream();      //define an XStream for xml messages
    final String newLine = System.getProperty("line.separator");  //get the newline separator
    myDataArray da = new myDataArray();    //define da as myDataArray instance
     
    public void run(){    //the main run method
    	System.out.println("Called BC worker.");
      	try{
			in =  new BufferedReader(new InputStreamReader(sock.getInputStream()));    //to read in messages from the back channal clients
			out = new PrintStream(sock.getOutputStream()); // to send messages back to back channel client
			i = 0; 
			xml = "";
			while(true){
	  			temp = in.readLine();   //read line by line from the clients
	  			if (temp.indexOf("end_of_xml") > -1) break;    //if it's the end od the messages, break out from the while loop
	  			else xml = xml + temp + newLine; // if it's not the end of the messages, add the line which is read in to the xml string
			}
			System.out.println("The XML marshaled data:");
			System.out.println(xml); //print out the whole xml messages on the console
			out.println("Acknowledging Back Channel Data Receipt"); // send the receip messages back to the client
			out.flush(); 
			sock.close();  //close the socket
	
        	da = (myDataArray) xstream.fromXML(xml); // deserialize / unmarshal data from the xml
			System.out.println("Here is the restored data: ");
			for(i = 0; i < da.num_lines; i++){     
	  			System.out.println(da.lines[i]);  //print out the deserialized data on the console
			}
      	}catch (IOException ioe){
      	} // end run
    }
}


/*
 * The Worker thread would get http protocol message from the browser 
 * and return information back to the browser accordingly
 * -the static file, which ends with .txt or .html, also works when the requst is asking for a directory
 * -send a dynamic html file back to the browser including file names on the same directory with the server
 * -would accept FORM input from the user and send relative messages back 
*/
class Worker extends Thread{   
	Socket sock;               
	Worker (Socket s){sock=s;} 
	
  
	public void run(){
		PrintStream out=null;            
		BufferedReader in=null;       
		try{
			in=new BufferedReader(new InputStreamReader(sock.getInputStream()));         
			out=new PrintStream(sock.getOutputStream());  
			String name;                       //the message coming from the socket
			String[] nameList;
			String fileName;          
			name=in.readLine();       //read the first line of the message coming from the browser  
			String fileType;          //define the filetype  

			//If the message coming from the browser is starts with GET methos for the HTTP protocol, would do the following
			//otherwise send back an error message.          
			if (name!=null&&!name.isEmpty()&&name.startsWith("GET")){
				nameList=name.split(" ");           //get a string array separated by " "
				String addr=nameList[1];            //get the middle part which contains the path name

				//If the browser is asking for the current directory which the server is working on, send a dynamic html page back to the browser 
				//which lists all the file name on the browser by hot link references to their contents
				if (addr.equals("/")){
					fileType="text/html";       //set up the type to be html file
					out.print("HTTP/1.1 200 OK\r\n" +"Content-Type: " +fileType +"\r\n"+"Content-Length: " +"2000"+"\r\n"+"Date: " + new Date() + "\r\n\r\n" ); //send http header back to the browser
					out.print("<html><head>\r\n</head>\r\n<body>\r\n");   //add head for the html
					out.print("<h1>Index of my directory</h1>\r\n");      //add head of the body for the html
					File dir=new File("./");                             //create an abstract file using it's path
					File[] listOfFiles=dir.listFiles();                  //create an array of File which contains all the File or directory under the current directory
					for ( int i = 0 ; i < listOfFiles.length ; i ++ ) {
						String linkName;
						
      					if ( listOfFiles[i].isDirectory ( ) ) {                   //if the file is a directory, get the path and add a slash after it
      						linkName=listOfFiles[i].getPath()+"/";
      						System.out.println(linkName);
							out.print ( "<a href= "+linkName+">"+listOfFiles[i].getPath().substring(2)+"</a><br>\r\n") ;  //send back the name of the directory using hot link reference
      					}else if ( listOfFiles[i].isFile ( ) ){
      						linkName=listOfFiles[i].getPath();                   //if it's a file, get the path 
							out.print (  "<a href="+linkName+">"+listOfFiles[i].getPath().substring(2)+"</a><br>\r\n") ;  //send back the name of the file using hot link reference
   						}
					}
					out.print("</body></html>");  //end up the html file

				//if the browser is asking for a directory, send a dynamic html page back to the browser 
				//which lists all the file name under that directory by hot link references to their contents
				}else if(addr.endsWith("/")){
					addr=addr.substring(1);
					fileType="text/html";           //set up the type to be html file
					out.print("HTTP/1.1 200 OK\r\n" +"Content-Type: " +fileType +"\r\n"+"Content-Length: " +"2000"+"\r\n"+"Date: " + new Date() + "\r\n\r\n" );   //send http header back to the browser
					out.print("<html><head>\r\n</head>\r\n<body>\r\n");    //add head for the html
					File dir=new File(addr);        //create an abstract file using it's path
					int len=addr.length();          //calculate the path's length
					out.print("<h1>Index of my "+addr+" directory</h1>\r\n");          //add head of the body for the html
					File[] listOfFiles=dir.listFiles();                                //create an array of File which contains all the File or directory under the current directory
					for ( int i = 0 ; i < listOfFiles.length ; i ++ ) {
						String linkName;
						
      					if ( listOfFiles[i].isDirectory ( ) ) {
      						linkName=listOfFiles[i].getPath().substring(len)+"/";          //if it is a directory, get the path, deleting the current name and add a slash after it
							out.print ( "<a href= "+linkName+">"+listOfFiles[i].getPath().substring(len)+"</a><br>\r\n") ;    //send back the name of the directory using hot link reference
      					}else if ( listOfFiles[i].isFile ( ) ){
      						linkName=listOfFiles[i].getPath().substring(len);               //if it's a file, get the path , delete the current name from it 
							out.print (  "<a href="+linkName+">"+listOfFiles[i].getPath().substring(len)+"</a><br>\r\n") ;    //send back the name of the file using hot link reference
   						}
					}
					out.print("</body></html>");   //end up the html file

				//if the browser is asking for a txt or html file, get the content of the file,
				//and send it back to the browser.
				}else if (addr.endsWith("txt")||addr.endsWith("html")||addr.endsWith("xyz")){
					fileName=addr.substring(1);            //extract "/" from the path name to get the file name
					if (fileName.endsWith(".html")){
						fileType="text/html";              //if the file is a html file, set up fileType to be html
					}else if (fileName.endsWith(".txt")){
						fileType="text/plain";             //if the file is a txt file, set up fileType to be plain
					}else{
						fileType="application/xyz";     //if the file is a .xyz file, set up the file content to xyz
					}
				
					//try to find the file with the same name as asked for, 
					//if can find such file in the current directory, send to to the browser,
					//otherwise give an error message 
					try{
						File f=new File(fileName);        //create an abstract file using it's path
						InputStream file=new FileInputStream(f);         //create InputStream
						out.print("HTTP/1.1 200 OK\r\n" +"Content-Type: " +fileType + "\r\n"+"Content-Length: " +f.length()+"\r\n"+"Date: " + new Date() + "\r\n\r\n" ); //send http header back to the browser
						
						try {
            				byte[] buffer = new byte[1000];   //set the buffer size
            				while (file.available()>0) 
               					out.write(buffer, 0, file.read(buffer));  //write the message to the browser from the buffer read
        				} catch (IOException e) { System.out.println(e); }
        				out.print("</body></html>");                  //end up the html file
					}catch(FileNotFoundException e){
						out.println("File not found error!");
					}

				//if we get a FORM message from the browser, extract the message we want,
				//and do some calculation, send the result back	
				}else if (addr.contains("fake-cgi")){
					String[] parameters=addr.split("\\?")[1].split("&");     //try to extract the stream coming in by ? and &
					String person=parameters[0].split("=")[1];               //get the information for person
					int num1=Integer.parseInt(parameters[1].split("=")[1]);  //get the num1
					int num2=Integer.parseInt(parameters[2].split("=")[1]);  //get the num2
					int sum=num1+num2;     //add num1 and num2, get the sum of them
					fileType="text/html";  //set up the type to be html file
					out.print("HTTP/1.1 200 OK\r\n" +"Content-Type: " +fileType +"\r\n"+"Content-Length: " +"100"+"\r\n"+"Date: " + new Date() + "\r\n\r\n" );//send http header back to the browser
					out.print("<html><head>\r\n</head>\r\n<body>\r\n");
					out.print("<h1>Dear "+person+", the sum of "+num1+" and "+ num2+ " is "+sum+". </h1><br>\r\n");  //send back the infromation we get after calculation
					out.print("</body></html>");   //end up the html file
				}else{
					out.println("Request can't be resolved!");
				}
				
			}else{
				out.println("Bad request, the browser sent a request this web server doesn't understand!");
			}
			
			sock.close();         
		}catch(IOException ioe){
			System.out.println(ioe);              
		}
	}
}
