package nachos.userprog;

import java.util.LinkedList;
import java.util.List;

class FileDescriptorManager {
	private List<Integer> freeDescriptors;
	private List<FileEntry> usedDescriptors;
	
	public FileDescriptorManager() {
		// TODO Implement this.  Responsible for setting up stdin and stdout.
	}
	
	/**
	 * addFile() adds the given file name to the list of file descriptors.  This should be the only 
	 * way a file is ever added to the file descriptors.
	 * @param fileName is the name of the file to add
	 * @param create indicates whether the file should be created if it does not already exist
	 * @return the file descriptor number
	 */
	public int addFile(String fileName, boolean create) {
		// TODO Implement this.
		return -1;
	}
	
	/**
	 * closeFile() closes the file associated with the given file name.  This, along with exit(), should 
	 * be the only two ways a  file is ever removed from the file descriptors.
	 * @param fileName is the name of the file to remove
	 */
	public void closeFile(String fileName) {
		// TODO Implement this.
	}
	
	/**
	 * getFile() retrieves a file from the file manager.
	 * @param fileName is the name of the file to be retrieved.
	 * @return
	 */
	public Object getFile(String fileName) {
		// TODO Implement this.
		return null;
	}
	
	/**
	 * exit() helps with exiting the process by ensuring that each file in the file manager is 
	 * closed properly.
	 */
	public void exit() {
		// TODO Implement this.
	}
	
	private class FileEntry {
		private int fileDescriptor = Constants.INVALID_DESCRIPTOR;
		private String fileName;
		private Object filePointer;
		
		public FileEntry(int descr, String fileName, Object filePointer) {
			// TODO Implement this.
		}
	}
}
