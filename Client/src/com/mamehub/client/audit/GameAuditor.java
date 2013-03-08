package com.mamehub.client.audit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.Utils;
import com.mamehub.thrift.FileInfo;
import com.mamehub.thrift.FileNameLocationPair;
import com.mamehub.thrift.RomInfo;

public class GameAuditor implements Runnable {
	final Logger logger = LoggerFactory.getLogger(GameAuditor.class);

	public static interface AuditHandler {
		public void auditFinished(GameAuditor gameAuditor);

		public void auditError(Exception e);

		public void updateAuditStatus(String string);
	}

	private AuditHandler handler;
	public ConcurrentMap<String, FileInfo> scanData = null;
	public static boolean abort = false;
	public boolean runScanner;
	public Thread gameAuditorThread;
	private HashScanner hashScanner;
	private ConcurrentMap<String, ArrayList<FileNameLocationPair>> hashEntryMap = null;
	private ConcurrentMap<String, String> chdMap = null;
	ConcurrentMap<String, RomInfo> mameRoms = getMameRomInfoMap();
	ConcurrentMap<String, RomInfo> messRoms = getMessRomInfoMap();
	private File indexDir;

	Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40); // The standard analyzer does stemming, so don't use it.
	
	public GameAuditor(AuditHandler ah) throws IOException {
		GameAuditor.abort = false;
		this.handler = ah;

		runScanner = true;
		scanData = Utils.getAuditDatabaseEngine().getOrCreateHashMap(
				FileInfo.class, "RomHash");
		hashEntryMap = Utils.getAuditDatabaseEngine()
				.getOrCreatePrimitiveHashMap("HashEntryMap2");
		chdMap = Utils.getAuditDatabaseEngine().getOrCreatePrimitiveHashMap(
				"ChdMap");

		if (!scanData.isEmpty() && !hashEntryMap.isEmpty()) {
			runScanner = false;
		}

		startAudit(runScanner);
	}

	public boolean startAudit(boolean runScanner) {
		this.runScanner = runScanner;

		if (gameAuditorThread != null && gameAuditorThread.isAlive()) {
			// Cancel previous request
			cancelAudit();
		}
		GameAuditor.abort = false;

		gameAuditorThread = new Thread(this);
		gameAuditorThread.start();
		return true;
	}

	@Override
	public void run() {
		try {
			if (runScanner) {
				scan();
				audit();
			}
			updateCache();
		} catch (Exception e) {
			GameAuditor.abort = true;
			if (handler != null) {
				handler.auditError(new IOException(e));
			}
			return;
		}
		// Replace the existing db with the new one
		if (handler != null && !GameAuditor.abort) {
			handler.auditFinished(this);
		}
	}

	private void scan() throws Exception {
		List<File> paths = new IniParser(new File("mame.ini")).getRomPaths();
		hashScanner = new HashScanner(handler, hashEntryMap, chdMap);
		hashScanner.scan(paths);
		hashScanner = null;
		if (GameAuditor.abort) {
			return;
		}

		/*
		 * logger.info("Creating new reverse map");
		 * handler.updateAuditStatus("AUDIT: Creating new reverse-map"); final
		 * ExecutorService reverseMapThreadPool =
		 * Executors.newFixedThreadPool(8); reverseMapProcessedCount=0; final
		 * int total = scanData.size(); for(final Map.Entry<String, FileInfo>
		 * entry : scanData.entrySet()) { if(GameAuditor.abort){
		 * reverseMapThreadPool.shutdown(); // Wait until all threads are finish
		 * while (!reverseMapThreadPool.isTerminated()) { try {
		 * Thread.sleep(10); } catch (InterruptedException e) { // Bail return;
		 * } }
		 * 
		 * hashEntryMap.clear(); Utils.getAuditDatabaseEngine().commit();
		 * chdMap.clear(); Utils.getAuditDatabaseEngine().commit(); return; }
		 * 
		 * if(entry.getValue().bad) { continue; }
		 * 
		 * reverseMapThreadPool.execute(new Runnable() {
		 * 
		 * @Override public void run() { if(entry.getValue().chdname != null) {
		 * chdMap.put(entry.getValue().chdname, entry.getKey()); } else {
		 * 
		 * if(entry.getValue().contentsCrc32 == null) {
		 * addToHashEntryMap(hashEntryMap, entry.getValue().crc32,
		 * entry.getKey()); } else { for(Map.Entry<String, String> entry2 :
		 * entry.getValue().contentsCrc32.entrySet()) {
		 * addToHashEntryMap(hashEntryMap, entry2.getValue(), entry.getKey()); }
		 * } }
		 * 
		 * synchronized(reverseMapProcessedCount) { reverseMapProcessedCount++;
		 * if(reverseMapProcessedCount%100==0) {
		 * Utils.getAuditDatabaseEngine().commit();
		 * handler.updateAuditStatus("AUDIT: Creating new reverse-map (" +
		 * reverseMapProcessedCount + " of " + total + ")"); } } }
		 * 
		 * }); } reverseMapThreadPool.shutdown(); // Wait until all threads are
		 * finish while (!reverseMapThreadPool.isTerminated()) { try {
		 * Thread.sleep(10); } catch (InterruptedException e) { // Bail
		 * GameAuditor.abort = true; return; } }
		 */
	}

	private void audit() {
		try {
			handler.updateAuditStatus("AUDIT: Parsing MAME Roms");
			logger.info("Parsing MAME roms");
			new RomParser("../hash/mameROMs.xml.gz").process(hashEntryMap,
					chdMap, mameRoms, false, false, false);
			Utils.getAuditDatabaseEngine().commit();

			handler.updateAuditStatus("AUDIT: Parsing MESS Roms");
			// MESS Systems can't come from CHDs, so put a null here
			new RomParser("../hash/messROMs.xml.gz").process(hashEntryMap,
					null, messRoms, true, true, true);
			Utils.getAuditDatabaseEngine().commit();

			Set<String> systemsToRemove = new HashSet<String>();
			for (String system : messRoms.keySet()) {
				if (GameAuditor.abort) {
					return;
				}
				if (messRoms.get(system).isSetMissingReason()) {
					// Don't parse carts if missing bios
					continue;
				}
				File file = new File("../hash/" + system + ".hsi");
				if (file.exists()) {
					Map<String, RomInfo> systemCarts = getSystemRomInfoMap(system);
					handler.updateAuditStatus("AUDIT: Parsing carts for "
							+ system);
					new CartParser(system, file).process(hashEntryMap,
							systemCarts, chdMap);
					Utils.getAuditDatabaseEngine().commit();
				} else {
					systemsToRemove.add(system);
				}
			}
			for (String systemToRemove : systemsToRemove) {
				messRoms.remove(systemToRemove);
			}
			Utils.getAuditDatabaseEngine().commit();

		} catch (Exception e) {
			// Delete the database just in case
			try {
				Utils.getAuditDatabaseEngine().close();
				Utils.getAuditDatabaseEngine().wipeDatabase();
			} catch (IOException e1) {
				// We couldn't delete the database, this is probably bad but not
				// sure what else we can do at this point.
			}
			if (handler != null) {
				handler.auditError(new IOException(e));
			}
			return;
		}
	}

	private void updateCache() throws IOException {
		handler.updateAuditStatus("AUDIT: Updating cache");
		Map<String, TreeMap<String, String>> romSystemNameIdMap = Utils
				.getAuditDatabaseEngine().getOrCreatePrimitiveHashMap(
						"RomSystemNameIdMap");
		Utils.stagedClear(romSystemNameIdMap, Utils.getAuditDatabaseEngine());

		indexDir = new File("AuditIndex" + Utils.AUDIT_DATABASE_VERSION);
		indexDir.mkdir();
		Directory dir = FSDirectory.open(indexDir);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);

		// Create a new index in the directory, removing any
		// previously indexed documents:
		iwc.setOpenMode(OpenMode.CREATE);

		// Optional: for better indexing performance, if you
		// are indexing many documents, increase the RAM
		// buffer. But if you do this, increase the max heap
		// size to the JVM (eg add -Xmx512m or -Xmx1g):
		//
		// iwc.setRAMBufferSizeMB(256.0);

		IndexWriter writer = new IndexWriter(dir, iwc);

		{
			TreeMap<String, String> romNameIdMap = new TreeMap<String, String>();
			for (RomInfo ri : mameRoms.values()) {
				// System.out.println("Adding " + ri);
				romNameIdMap.put(ri.description, ri.romName);
			}
			// System.out.println("Saving " + "Arcade");
			romSystemNameIdMap.put("arcade", romNameIdMap);
			Utils.getAuditDatabaseEngine().commit();
		}

		for (String system : messRoms.keySet()) {
			File file = new File("../hash/" + system + ".hsi");
			if (file.exists()) {
				TreeMap<String, String> romNameIdMap = new TreeMap<String, String>();
				Map<String, RomInfo> systemCarts = getSystemRomInfoMap(system);
				for (RomInfo ri : systemCarts.values()) {
					// System.out.println("Adding " + ri);
					romNameIdMap.put(ri.romName, ri.romName);
				}
				// System.out.println("Saving " + system);
				romSystemNameIdMap.put(system, romNameIdMap);
				Utils.getAuditDatabaseEngine().commit();
			}
		}
		
		for (String system : romSystemNameIdMap.keySet()) {
			for (Map.Entry<String,String> entry : romSystemNameIdMap.get(system).entrySet()) {
				Document doc = new Document();
				
				doc.add(new StringField("Machine", system, Field.Store.YES));
				doc.add(new StringField("gameDescription", entry.getKey(), Field.Store.YES));
				doc.add(new TextField("gameDescriptionLowerCase", entry.getKey().toLowerCase().replace("'", ""), Field.Store.YES));
				doc.add(new StringField("gameName", entry.getValue(), Field.Store.YES));
				
				writer.updateDocument(new Term("gameName", entry.getValue()), doc);
			}
		}

		// NOTE: if you want to maximize search performance,
		// you can optionally call forceMerge here. This can be
		// a terribly costly operation, so generally it's only
		// worth it when your index is relatively static (ie
		// you're done adding documents to it):
		//
		writer.forceMerge(1);

		writer.close();
	}

	public String getSystemForRom(String romId) {
		for (Map.Entry<String, RomInfo> entry : mameRoms.entrySet()) {
			if (entry.getKey().equals(romId)) {
				return "Arcade";
			}
		}

		for (String system : messRoms.keySet()) {
			File file = new File("../hash/" + system + ".hsi");
			if (file.exists()) {
				Map<String, RomInfo> systemCarts = getSystemRomInfoMap(system);
				for (Map.Entry<String, RomInfo> entry : systemCarts.entrySet()) {
					if (entry.getKey().equals(romId)) {
						return system;
					}
				}
			}
		}

		return null;
	}

	public ConcurrentMap<String, RomInfo> getSystemRomInfoMap(String system) {
		return Utils.getAuditDatabaseEngine().getOrCreateHashMap(RomInfo.class,
				system + "Roms");
	}

	public ConcurrentMap<String, RomInfo> getMameRomInfoMap() {
		return Utils.getAuditDatabaseEngine().getOrCreateHashMap(RomInfo.class,
				"MAMERoms");
	}

	public ConcurrentMap<String, RomInfo> getMessRomInfoMap() {
		return Utils.getAuditDatabaseEngine().getOrCreateHashMap(RomInfo.class,
				"MESSRoms");
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		GameAuditor gameAuditor = new GameAuditor(null);
		new Thread(gameAuditor).start();
	}

	public boolean isAuditing() {
		// If there is no thread, the thread is dead, or we are scanning hashes,
		// then we are NOT auditing
		return !(gameAuditorThread == null
				|| gameAuditorThread.isAlive() == false || hashScanner != null);
	}

	public void cancelAudit() {
		GameAuditor.abort = true;
		while (gameAuditorThread != null && gameAuditorThread.isAlive()) {
			try {
				gameAuditorThread.join();
			} catch (InterruptedException e) {
			}
		}
		GameAuditor.abort = false;
	}

	public static class RomQueryResult {
		public float score;
		public RomInfo romInfo;

		public RomQueryResult(float score, RomInfo romInfo) {
			this.score = score;
			this.romInfo = romInfo;
		}
	}

	public List<RomQueryResult> queryRoms(String inputQuery, Map<String, Set<String>> cloudRoms) {
		try {
			Directory dir = FSDirectory.open(indexDir);
			IndexReader reader = DirectoryReader.open(dir);
			IndexSearcher searcher = new IndexSearcher(reader);
			
			QueryParser parser = new QueryParser(Version.LUCENE_40, "gameDescriptionLowerCase", analyzer);
			
			List<RomQueryResult> queryResult = new ArrayList<RomQueryResult>();
			
			// Do some initial subs
			inputQuery = inputQuery.trim().replaceAll("\\s+"," ");
			if (!inputQuery.endsWith("\"")) {
				inputQuery += "*";
			}
			
			// Convert to lower case
			inputQuery = inputQuery.toLowerCase();

			// Escape special characters: + - && || ! ( ) { } [ ] ^ " ~ * ? : \
			inputQuery = inputQuery.replace("\\", "\\\\").replace(":", "\\:");
			
			// If the person put "machine:", actually put the colon back in and capitalize it
			inputQuery = inputQuery.replace("machine\\:", "Machine:");
			
			// Remove periods and apostrophes
			inputQuery = inputQuery.replace("'", "").replace(".", "");
			logger.info("Starting query: " + inputQuery);
			
			Query query;
			try {
				query = parser.parse(inputQuery);
			} catch(ParseException ex) {
				//TODO: Try to replace more special characters and requery
				return queryResult;
			}
			TopDocs results = searcher.search(query, 10000);
			//System.out.println("GOT " + results.scoreDocs.length + " RESULTS");
			for(ScoreDoc scoreDoc : results.scoreDocs) {
				//System.out.println(scoreDoc.score + ": " + searcher.doc(scoreDoc.doc).getField("gameDescription").stringValue());
				//System.out.println(searcher.doc(scoreDoc.doc).getField("Machine").stringValue());
				//System.out.println(searcher.doc(scoreDoc.doc).getField("gameName").stringValue());
				String system = searcher.doc(scoreDoc.doc).getField("Machine").stringValue();
				String romid = searcher.doc(scoreDoc.doc).getField("gameName").stringValue();
				RomInfo romInfo = null;
				if (system.equals("arcade")) {
					romInfo = getMameRomInfoMap().get(romid);
				} else {
					romInfo = getSystemRomInfoMap(system).get(romid);
				}
				//System.out.println(system + " : " + romid + " : " + romInfo + " " + cloudRoms.containsKey(romInfo.system));
				if(romInfo.missingReason != null && (cloudRoms==null || !cloudRoms.containsKey(romInfo.system) || !cloudRoms.get(romInfo.system).contains(romInfo.romName))) {
					continue;
				}
				//System.out.println("GOT ROM: " + romInfo);
				queryResult.add(new RomQueryResult(
						scoreDoc.score,
						romInfo
						));
				if(queryResult.size()>=1000) {
					break;
				}
			}
			logger.info("Finished query");
			
			
			/*
			query = query.toLowerCase();
			// System.out.println("CHECKING " + system + " " + query);
			for (Map.Entry<String, TreeMap<String, String>> parentEntry : romSystemNameIdMap
					.entrySet()) {
				for (Map.Entry<String, String> entry : parentEntry.getValue()
						.entrySet()) {
					// NOTE: Lower score is better
					int score = StringUtils.getLevenshteinDistance(query, entry
							.getKey().toLowerCase())
							- (entry.getKey().length());
					if (entry.getKey().toLowerCase().contains(query)) {
						score -= 1e6;
					}
					if (entry.getKey().toLowerCase().startsWith(query)) {
						score -= 1e6;
					}
					// System.out.println("ID " + entry.getValue() + " NAME " +
					// entry.getKey());
					queryResult.add(new RomQueryResult(score, entry.getValue(),
							entry.getKey()));
					// System.out.println(queryResult.size());
				}
			}
			*/
			
			return queryResult;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
