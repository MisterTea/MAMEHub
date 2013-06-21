package com.mamehub.client.audit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.Utils;
import com.mamehub.client.audit.GameAuditor.AuditHandler;
import com.mamehub.thrift.FileInfo;
import com.mamehub.thrift.FileNameLocationPair;

public class HashScanner {
	final Logger logger = LoggerFactory.getLogger(HashScanner.class);

	private AuditHandler handler;
	ConcurrentMap<String, FileInfo> scanData;
	public Integer numProcessed = 0;
	final ExecutorService threadPool = Executors.newFixedThreadPool(8);
	int total=0;
	private ConcurrentMap<String, ArrayList<FileNameLocationPair>> hashEntryMap;
	private ConcurrentMap<String, String> chdMap;

	private ConcurrentLinkedQueue<Future<?>> hasherFutures = new ConcurrentLinkedQueue<Future<?>>();
	
	public HashScanner(GameAuditor.AuditHandler handler, ConcurrentMap<String, ArrayList<FileNameLocationPair>> hashEntryMap, ConcurrentMap<String, String> chdMap) {
		this.handler = handler;
		scanData = Utils.getAuditDatabaseEngine().getOrCreateMap(FileInfo.class, "RomHash");
		this.hashEntryMap = hashEntryMap;
		this.chdMap = chdMap;
	}
	
	public class Hasher implements Runnable {
		private File file;
		private boolean onlyChds;

		public Hasher(File file, boolean onlyChds) {
			this.file = file;
			this.onlyChds = onlyChds;
		}

		@Override
		public void run() {
			synchronized(numProcessed) {
				numProcessed++;
				if(numProcessed%100==0) {
					handler.updateAuditStatus("AUDIT ( "+numProcessed+" / " + total + " ): Hashing: " + file.getName());
				}
				if(numProcessed%5000==0) {
					Utils.getAuditDatabaseEngine().commit();
				}
			}
			
			try {
				FileInfo previousFileInfo = null;
				previousFileInfo = scanData.get(file.getAbsolutePath());
				if(previousFileInfo != null && previousFileInfo.length == file.length()) {
					// Already scanned and same length, no need to re-scan
					//logger.info("Skipping: " + previousFileInfo);
					return;
				}
				//logger.info("HASHING: " + file);
				
				if(file.getName().endsWith(".zip")) {
					if(onlyChds) {
						return;
					}
					
					FileInfo zipFileInfo = new FileInfo(file.getAbsolutePath(), false, null,file.length(), new HashMap<String,String>(), null);
	
					ZipFile zf = null;
					try {
						zf = new ZipFile(file);
						for (Enumeration<? extends ZipEntry> e = zf.entries(); e
								.hasMoreElements();) {
							ZipEntry ze = e.nextElement();
							String name = ze.getName();
	
							String crc32 = longToHex(ze.getCrc());
							/*
							InputStream in = zf.getInputStream(ze);
							String sha1 = shaSum(in);
							in.close();
							*/
							//System.out.println(file.getName() + " " + name + " " + crc32);
							zipFileInfo.contentsCrc32.put(name,crc32);
							//logger.info(file.getAbsolutePath() + " : " + name + " : " + crc32);
	
						}
						
						scanData.put(file.getAbsolutePath(), zipFileInfo);
						for(Map.Entry<String, String> entry2 : zipFileInfo.contentsCrc32.entrySet()) {
							addToHashEntryMap(hashEntryMap, entry2.getValue(), new FileNameLocationPair(entry2.getKey(), file.getAbsolutePath()));
						}
					} catch(ZipException ze) {
						FileInfo badFileInfo = new FileInfo().setId(file.getAbsolutePath()).setBad(true);
						scanData.put(file.getAbsolutePath(), badFileInfo);
					} catch (Exception e) {
						logger.info("GOT AN EXCEPTION", e);
						return;
					} finally {
						if(zf != null) {
							zf.close();
						}
					}
				} else {
					if(file.getName().endsWith(".chd") && onlyChds) {
						FileInfo newFileInfo;
						// The chdname is the filename minus ".chd"
						newFileInfo = new FileInfo().setId(file.getAbsolutePath()).setBad(false).setChdname(file.getName().substring(0,file.getName().length()-4))
								.setLength(file.length());
						scanData.put(file.getAbsolutePath(), newFileInfo);
						chdMap.put(newFileInfo.chdname, file.getAbsolutePath());
					} else {
						FileInfo newFileInfo;
						try {
							String crc = crc32Sum(new FileInputStream(file));
							//logger.info(file.getAbsolutePath() + " : " + file.getName() + " : " + crc);
							newFileInfo = new FileInfo(file.getAbsolutePath(), false,
									//shaSum(new FileInputStream(file)),
									crc,
									file.length(), null, null);
							scanData.put(file.getAbsolutePath(), newFileInfo);
							addToHashEntryMap(hashEntryMap, newFileInfo.crc32, new FileNameLocationPair(file.getAbsolutePath(), file.getAbsolutePath()));
						} catch (Exception e) {
							logger.info("GOT AN EXCEPTION");
							e.printStackTrace();
							return;
						}
					}
				}
			} catch(Exception e) {
				handler.auditError(e);
			}
		}

		private void addToHashEntryMap(Map<String, ArrayList<FileNameLocationPair>> hashEntryMap, String hash, FileNameLocationPair entry) {
			//logger.info("Adding: " + sha1 + " -> " + entry);
			if(!hashEntryMap.containsKey(hash)) {
				ArrayList<FileNameLocationPair> al = new ArrayList<FileNameLocationPair>();
				al.add(entry);
				hashEntryMap.put(hash,al);
			} else {
				ArrayList<FileNameLocationPair> entries = new ArrayList<FileNameLocationPair>(hashEntryMap.get(hash));
				if(!entries.contains(hash)) {
					entries.add(entry);
				}
				hashEntryMap.put(hash, entries);
			}
		}
		
		public String crc32Sum(InputStream is) throws IOException{
			byte[] data = new byte[1*1024*1024];
			CRC32 crc32 = new CRC32();
			
			int nRead;

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				crc32.update(data, 0, nRead);
			}
			is.close();

		    return longToHex(crc32.getValue());
		}
		
		public String shaSum(InputStream is) throws IOException{
		    MessageDigest md = null;
			byte[] shaData = new byte[1*1024*1024];
			if(md == null) {
				try {
					md = MessageDigest.getInstance("SHA-1");
				} catch (NoSuchAlgorithmException e) {
					throw new IOException(e);
				} 
			}
			
			int nRead;

			while ((nRead = is.read(shaData, 0, shaData.length)) != -1) {
			  md.update(shaData, 0, nRead);
			}
			is.close();

		    return byteArray2Hex(md.digest());
		}
		
		private String byteArray2Hex(final byte[] hash) {
			Formatter arrayToHexFormatter = null;
			try {
			    arrayToHexFormatter = new Formatter();
		        for (byte b : hash) {
			        arrayToHexFormatter.format("%02x", b);
			    }
			    return arrayToHexFormatter.toString();
			} finally {
				if(arrayToHexFormatter != null) {
					arrayToHexFormatter.close();
				}
			}
		}
	}
	
	public void scan(List<File> paths) throws IOException {
		handler.updateAuditStatus("AUDIT: Clearing old reverse-map");
		logger.info("Cleaing old reverse-map");

		Set<String> filePathsToRemove = new HashSet<String>();
		for(Map.Entry<String, FileInfo> scanEntry : scanData.entrySet()) {
			File file = new File(scanEntry.getKey());
			if(!file.exists()) {
				logger.info("Can't find " + file.getAbsolutePath() + "... removing.");
				filePathsToRemove.add(file.getAbsolutePath());
			}
		}
		
		// Go through each data structure removing elements corresponding to the deleted files
		{
			Iterator<Map.Entry<String,FileInfo>> iter = scanData.entrySet().iterator();
			while (iter.hasNext()) {
			    Map.Entry<String,FileInfo> entry = iter.next();
			    if(filePathsToRemove.contains(entry.getKey())) {
			    	iter.remove();
					Utils.getAuditDatabaseEngine().commit();
			    }
			}
		}
		{
			Iterator<Map.Entry<String,ArrayList<FileNameLocationPair>>> iter = hashEntryMap.entrySet().iterator();
			int count=0;
			while (iter.hasNext()) {
			    Map.Entry<String,ArrayList<FileNameLocationPair>> entry = iter.next();
			    Iterator<FileNameLocationPair> iter2 = entry.getValue().iterator();
			    boolean removedOne=false;
			    while(iter2.hasNext()) {
			    	FileNameLocationPair fnlp = iter2.next();
			    	if(filePathsToRemove.contains(fnlp.location)) {
			    		removedOne=true;
			    	}
			    }
			    if(!removedOne) {
			    	continue;
			    }
			    if(entry.getValue().isEmpty()) {
			    	iter.remove();
			    } else {
			    	hashEntryMap.put(entry.getKey(), entry.getValue());
			    }
			    count++;
			    if(count%1000==0) {
					Utils.getAuditDatabaseEngine().commit();
			    }
			}
			Utils.getAuditDatabaseEngine().commit();
		}
		{
			Iterator<Map.Entry<String,String>> iter = chdMap.entrySet().iterator();
			while (iter.hasNext()) {
			    Map.Entry<String,String> entry = iter.next();
			    if(filePathsToRemove.contains(entry.getValue())) {
			    	iter.remove();
					Utils.getAuditDatabaseEngine().commit();
			    }
			}
		}
		// TODO: verify that the above code works
		/*
		Utils.stagedClear(scanData, Utils.getAuditDatabaseEngine());
		Utils.stagedClear(hashEntryMap, Utils.getAuditDatabaseEngine());
		Utils.stagedClear(chdMap, Utils.getAuditDatabaseEngine());
		*/
		if(GameAuditor.abort) {
			return;
		}
		
		logger.info("Starting scan..");
		
		for(File path : paths) {
			walk(path, false);
			if(GameAuditor.abort) {
				return;
			}
		}
		
		logger.info("Finished reading file headers.");
		// This will make the executor accept no new threads
	    // and finish all existing threads in the queue
		threadPool.shutdown();
	    // Wait until all threads are finish
	    while (!threadPool.isTerminated()) {
	    	try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// Bail
	    		threadPool.shutdownNow();
				return;
			}
	    	if(GameAuditor.abort) {
	    		threadPool.shutdownNow();
	    		return;
	    	}
	    }

		logger.info("Finished scanning");
		Utils.getAuditDatabaseEngine().commit();
		logger.info("Committed all changes");
		handler.updateAuditStatus("AUDIT: Done Scanning.  Matching ROMs...");
	}
	
	public String longToHex(long crc) {
	    Formatter arrayToHexFormatter = null;
	    try {
	    	arrayToHexFormatter = new Formatter();
	        arrayToHexFormatter.format("%02x%02x%02x%02x", ((crc>>24)%256), ((crc>>16)%256), ((crc>>8)%256), crc%256);
		    return arrayToHexFormatter.toString();
	    } finally {
	    	if(arrayToHexFormatter != null) {
	    		arrayToHexFormatter.close();
	    	}
	    }
	}

	public void walk( File root, boolean onlyChds ) throws IOException {

        File[] list = root.listFiles();
		handler.updateAuditStatus("AUDIT: Found " + list.length + " files in " + root.getName() + "...");

        for ( File f : list ) {
			if(GameAuditor.abort) {
				return;
			}
            if ( f.isDirectory() ) {
            	// We can't recursively scan because MAME does not let us tell it where CHDs are.
            	
            	// We need to go one level deeper to find chds
            	if(!onlyChds)
            		walk( f, true );
            }
            else {
            	Hasher h = new Hasher(f, onlyChds);
        		hasherFutures.add(threadPool.submit(h));
            	while (hasherFutures.size() > 100) {
                	try {
						hasherFutures.poll().get();
					} catch (Exception e) {
						throw new IOException(e);
					}
            	}
    			total++;
            }
        }
    }
	
    /*
	public static byte[] fileToBytes(File file) throws IOException {
		RandomAccessFile f = new RandomAccessFile(file, "r");
		byte[] b = new byte[(int)f.length()];
		f.read(b);
		return b;
	}
	
	public static String shaSum(byte[] convertme) throws IOException{
		if(md == null) {
			try {
				md = MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				throw new IOException(e);
			} 
		}
	    return byteArray2Hex(md.digest(convertme));
	}
	*/
}
