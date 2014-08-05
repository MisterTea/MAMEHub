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
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

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
		public T deserialize(DataInput in, int input_size) throws IOException {
			int size = in.readInt();
			byte[] b = new byte[size];
			in.readFully(b);
			ByteArrayInputStream bais = new ByteArrayInputStream(b);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Object o;
			try {
				o = ois.readObject();
			} catch (ClassNotFoundException e) {
				throw new IOException(e);
			}
			ois.close();
			bais.close();
			return (T)o;
		}

    @Override
    public int fixedSize() {
      return -1;
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
		public T deserialize(DataInput in, int input_size) throws IOException {
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

    @Override
    public int fixedSize() {
      return -1;
    }
		
	}

	private final static Logger logger = Logger.getLogger(ClientDatabaseEngine.class.getName());

	private File dbDirectory;
	private String dbFileName;

	public DB database;

	private boolean inMemory;

    private boolean compression;

	public ClientDatabaseEngine(String baseDbDirectory, String dbName, boolean wipe, boolean inMemory, boolean compression) throws IOException {
		super();
		this.inMemory = inMemory;
		this.compression = compression;
		logger.info("CREATING NEW DATABASE ENGINE");
		/** create (or open existing) database using builder pattern*/
		dbDirectory = new File(baseDbDirectory + "/" + dbName);
		dbFileName = dbDirectory + "/" + "db";
		logger.info(" db dir: " + dbDirectory + " db file" + dbFileName);
		if (wipe) {
			wipeDatabase();
		} else {
			createDatabase(compression);
		}
	}
	
	private void createDatabase(boolean compression) {
		dbDirectory.mkdirs();
		DBMaker dbMaker = null;
		if(inMemory) {
			dbMaker = DBMaker.newMemoryDirectDB();
		} else {
			dbMaker = DBMaker.newFileDB(new File(dbFileName));
		}
		if (compression) {
	        dbMaker.compressionEnable();
		}
		//dbMaker.closeOnExit();
		//dbMaker.disableTransactions();
		// dbMaker.cacheSoftRefEnable();
		//dbMaker.disableCache();
		//dbMaker.enableHardCache();
		// dbMaker.asyncWriteDisable();
		database = dbMaker.make();
	}
	
	public synchronized void wipeDatabase() throws IOException {
		System.out.println("WIPING OLD DATABASE");
		if (database != null) {
			database.close();
		}
		if (!inMemory) {
			// Wipe the old database
			FileUtils.deleteDirectory(dbDirectory);
		}
		createDatabase(compression);
	}
	
	@SuppressWarnings("rawtypes")
	public synchronized <T extends TBase> ConcurrentMap<String, T> getOrCreateMap(Class<T> inClass, String className) {
		ConcurrentMap<String, T> dbMap = null;
		if(dbMap == null) {
			dbMap = database.getTreeMap(className);
			if (dbMap == null) {
				dbMap = database.createTreeMap(className).nodeSize(120).counterEnable().valueSerializer(new ThriftSerializer<T>(inClass)).makeStringMap();
			}
		}
		return dbMap;
	}
	
	public synchronized <T> ConcurrentMap<String, T> getOrCreatePrimitiveMap(String className) {
		ConcurrentMap<String, T> dbMap = null;
		if(dbMap == null) {
			dbMap = database.getTreeMap(className);
			if (dbMap == null) {
        dbMap = database.createTreeMap(className).nodeSize(120).counterEnable().makeStringMap();
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
		database.compact();
	}

	public synchronized void commit() {
		database.commit();
	}
}
