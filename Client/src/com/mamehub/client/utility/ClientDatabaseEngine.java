package com.mamehub.client.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import org.apache.jdbm.DB;
import org.apache.jdbm.DBMaker;
import org.apache.jdbm.Serializer;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

public class ClientDatabaseEngine {
	public static class ObjectSerializer<T> implements Serializer<T>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public void serialize(DataOutput out, T obj) throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.flush();
			oos.close();
			baos.flush();
			baos.close();
			out.writeInt(baos.size());
			out.write(baos.toByteArray());
		}

		@SuppressWarnings("unchecked")
		@Override
		public T deserialize(DataInput in) throws IOException,
				ClassNotFoundException {
			int size = in.readInt();
			byte[] b = new byte[size];
			in.readFully(b);
			ByteArrayInputStream bais = new ByteArrayInputStream(b);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Object o = ois.readObject();
			ois.close();
			bais.close();
			return (T)o;
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	public static class ThriftSerializer<T extends TBase> implements Serializer<T>, Serializable {
		private static final long serialVersionUID = 1L;
		private transient TSerializer serializer;
		private transient TDeserializer deserializer;
		private Class<T> inClass;
		
		public ThriftSerializer(Class<T> inClass) {
			this.inClass = inClass;
		}

		@Override
		public void serialize(DataOutput out, T obj) throws IOException {
			if(serializer == null) {
				TBinaryProtocol.Factory protocolFactory = new TBinaryProtocol.Factory();
				serializer = new TSerializer(protocolFactory);
			}
			
			try {
				byte[] bytes = serializer.serialize(obj);
				out.writeInt(bytes.length);
				out.write(bytes);
			} catch (TException e) {
				throw new IOException(e);
			}
		}

		@Override
		public T deserialize(DataInput in) throws IOException,
				ClassNotFoundException {
			if(deserializer == null) {
				TBinaryProtocol.Factory protocolFactory = new TBinaryProtocol.Factory();
				deserializer = new TDeserializer(protocolFactory);
			}
			
			try {
				T t = inClass.newInstance();
				int length = in.readInt();
				byte b[] = new byte[length];
				in.readFully(b);
				deserializer.deserialize(t, b);
				return t;
			} catch(Exception e) {
				throw new IOException(e);
			}
		}
		
	}

    public static class StringSerializer implements Serializer<String>,Serializable {
		private static final long serialVersionUID = 1L;

		public StringSerializer() {

        }

        public void serialize(DataOutput out, String obj) throws IOException {
            out.writeUTF(obj);
        }

        public String deserialize(DataInput in) throws IOException, ClassNotFoundException {
            return in.readUTF();
        }
    }
	
	private final static Logger logger = Logger.getLogger(ClientDatabaseEngine.class.getName());

	private String dbDirectory;
	private String dbFileName;

	public DB database;

	public ClientDatabaseEngine(String baseDbDirectory, String dbName, boolean wipe, boolean inMemory) throws IOException {
		super();
		logger.info("CREATING NEW DATABASE ENGINE");
		/** create (or open existing) database using builder pattern*/
		dbDirectory = baseDbDirectory + "/" + dbName;
		dbFileName = dbDirectory + "/" + "db";
		logger.info(" db dir: " + dbDirectory + " db file" + dbFileName);
		if (wipe) {
			wipeDatabase();
		}
		new File(dbDirectory).mkdirs();
		DBMaker dbMaker = null;
		if(inMemory) {
			dbMaker = DBMaker.openMemory();
		} else {
			dbMaker = DBMaker.openFile(dbFileName);
		}
		//dbMaker.closeOnExit();
		//dbMaker.disableCache();
		//dbMaker.disableTransactions();
		//dbMaker.enableSoftCache();
		//dbMaker.disableCache();
		//dbMaker.enableHardCache();
		database = dbMaker.make();
	}
	
	public synchronized void wipeDatabase() throws IOException {
		logger.info("WIPING OLD DATABASE");
		if(database != null) {
			for(String s : database.getCollections().keySet()) {
				logger.info("DELETING COLLECTION: " + s);
				database.deleteCollection(s);
			}
		}
		// Wipe the old database
		File folder = new File(dbDirectory);
		File[] listOfFiles = folder.listFiles();
		if(listOfFiles == null) {
			if(!new File(dbDirectory).mkdir()) {
				throw new IOException("Could not make databse directory!");
			}
		} else {

			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					String dirFilename = listOfFiles[i].getPath();
					logger.info(dirFilename);
					if(!new File(dirFilename).delete()) {
						throw new IOException("Could not delete file: " + dirFilename);
					}
					if(new File(dirFilename).delete()) {
						throw new IOException("Could not delete file: " + dirFilename);
					}
				}
			}
		}

		if (new File(dbFileName).delete()) {
			logger.info("WIPED OLD DATABASE");
		}
	}
	
	@SuppressWarnings("rawtypes")
	public synchronized <T extends TBase> ConcurrentMap<String, T> getOrCreateHashMap(Class<T> inClass, String className) {
		ConcurrentMap<String, T> dbMap = null;
		if(dbMap == null) {
			dbMap = database.getHashMap(className);
			if (dbMap == null) {
				dbMap = database.createHashMap(className, new StringSerializer(), new ThriftSerializer<T>(inClass));
			}
		}
		return dbMap;
	}
	
	public synchronized <T> ConcurrentMap<String, T> getOrCreatePrimitiveHashMap(String className) {
		ConcurrentMap<String, T> dbMap = null;
		if(dbMap == null) {
			dbMap = database.getHashMap(className);
			if (dbMap == null) {
				dbMap = database.createHashMap(className);
			}
		}
		return dbMap;
	}
	
	@SuppressWarnings("rawtypes")
	public synchronized <T extends TBase> List<T> getOrCreateLinkedList(Class<T> inClass, String className) {
		List<T> dbMap = null;
		if(dbMap == null) {
			dbMap = database.getLinkedList(className);
			if (dbMap == null) {
				dbMap = database.createLinkedList(className, new ThriftSerializer<T>(inClass));
			}
		}
		return dbMap;
	}
	
	public synchronized void close() {
		if(database != null && !database.isClosed())
			database.close();
		database = null;
	}
	
	public synchronized void defrag() {
		database.defrag(true);
	}

	public synchronized void commit() {
		database.commit();
	}
}
