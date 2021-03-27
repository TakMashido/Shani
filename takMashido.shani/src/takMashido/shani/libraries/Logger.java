package takMashido.shani.libraries;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * Simple class for managing multiple PrintStream's designated for logging. All streams are designated by name.
 * If trying to get non existing PrintStream it's created pointing to [name].log file in default directory.
 * Can also add existing PrintStream to the pool.
 *
 * @author TakMashido
 */
public class Logger {
	/**Where to create log files*/
	private static File defaultLogDirectory=new File(".");
	
	/**Contain all registered logging print streams*/
	private static HashMap<String, PrintStream> streams=new HashMap<>();
	
	private static String streamOpenMessage=null;
	
	/**
	 * Set default log files directory for new log files creation.
	 * @param file Where to create new files. It has to be writable directory.
	 * @throws IOException When given File object is not valid -> points to file not directory or isn't writable. Or when error occurred during creation if not existing previously.
	 */
	public static void setDefaultLogDirectory(File file) throws IOException {
		if(!file.isDirectory()){
			System.err.println("Can only set directory as default log directory.");
			throw new IOException("Can only set directory as default log directory.");
		}
		
		if(!file.exists())
			file.createNewFile();
		
		if(!file.canWrite()){
			System.err.println("Can only set writable directory as log files target.");
			throw new IOException("Can only set writable directory as log files target.");
		}
		
		defaultLogDirectory=file;
	}
	
	/**
	 * Set message to print into stream just after adding it to pool. Useful for finding boundaries of logs from different executions.
	 * @param message
	 */
	public static void setStreamOpenMessage(String message){
		streamOpenMessage=message;
	}
	
	/**
	 * Add PrintStream to the pool. Use if you want stream pointing to custom stream. Not to dedfault file.
	 * @param stream PrintStream to push into the pool.
	 * @param name Name of given log stream.
	 */
	public static void addStream(PrintStream stream, String name){
		streams.put(name,stream);
		
		if(streamOpenMessage!=null)
			stream.println(streamOpenMessage);
	}
	/**
	 * Get logging PrintStream. If not existing creates new file in default logs directory and returns buffered print stream pointing to that directory.
	 * Equivalent to {@code Logger.getStream(channelName,true)}.
	 * @param channelName Name of log's channel to get.
	 * @return PrintStream being log channel for given name.
	 */
	public static PrintStream getStream(String channelName){
		return getStream(channelName, true);
	}
	/**
	 * Get logging PrintStream. If not existing creates new file in default logs directory and returns buffered print stream pointing to that directory.
	 * @param channelName Name of log's channel to get.
	 * @param buffer If setup buffering during new PrintStream creation.
	 * @return PrintStream being log channel for given name.
	 */
	public static PrintStream getStream(String channelName, boolean buffer){
		PrintStream ret=streams.get(channelName);
		
		if(ret!=null)
			return ret;
		
		File logFile=new File(defaultLogDirectory,channelName+".log");
		if(!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				System.err.printf("Logger can't create new log file \"%s.log\" in \"%s\" directory.\n",channelName,defaultLogDirectory.getAbsolutePath());
				
				ret=new PrintStream(OutputStream.nullOutputStream());
			}
		}
		
		if(ret==null){
			try {
				if(buffer)
					ret=new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile,true)));
				else
					ret=new PrintStream(new FileOutputStream(logFile,true));
				
				if(streamOpenMessage!=null)
					ret.println(streamOpenMessage);
			} catch (FileNotFoundException e) {
				String errorMessage="File was checked for creation but code somehow managed to go in there.";
				assert false:errorMessage;
				
				System.err.println(errorMessage);
				e.printStackTrace();
				
				ret=new PrintStream(OutputStream.nullOutputStream());
			}
		}
		
		assert ret!=null:"Stream has to be set in this point. Even with something pointing into nullOutputStream";
		
		streams.put(channelName,ret);
		
		
		return ret;
	}
	
	/**Flush all streams*/
	public static void flush(){
		for(PrintStream stream:streams.values())
			stream.flush();
	}
}
