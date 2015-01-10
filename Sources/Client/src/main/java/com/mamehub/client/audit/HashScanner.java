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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.CRC32;
import java.util.zip.ZipException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.Utils;
import com.mamehub.client.audit.GameAuditor.AuditHandler;
import com.mamehub.client.utility.SevenZHeaderReader;
import com.mamehub.thrift.FileInfo;
import com.mamehub.thrift.RomHashEntryValue;

public class HashScanner {
    final Logger logger = LoggerFactory.getLogger(HashScanner.class);

    private AuditHandler handler;
    public Integer numProcessed = 0;
    final ExecutorService threadPool = Executors.newFixedThreadPool(16);
    int total = 0;
    private ConcurrentMap<String, ArrayList<RomHashEntryValue>> hashEntryMap;
    private ConcurrentMap<String, String> chdMap;
    private ConcurrentMap<String, Boolean> filesScanned;

    private ConcurrentLinkedQueue<Future<File>> hasherFutures = new ConcurrentLinkedQueue<Future<File>>();

    private List<String> systemNames;

    public HashScanner(GameAuditor.AuditHandler handler,
            ConcurrentMap<String, ArrayList<RomHashEntryValue>> hashEntryMap,
            ConcurrentMap<String, String> chdMap,
            ConcurrentMap<String, Boolean> filesScanned,
            List<String> systemNames) {
        this.handler = handler;
        this.hashEntryMap = hashEntryMap;
        this.chdMap = chdMap;
        this.filesScanned = filesScanned;
        this.systemNames = systemNames;
    }

    public class Hasher implements Callable<File> {
        private File file;
        private String systemName;

        public Hasher(File file, String systemName) {
            this.file = file;
            this.systemName = systemName;
        }

        @Override
        public File call() {
            Thread t = Thread.currentThread();
            t.setPriority(Thread.MIN_PRIORITY);

            synchronized (numProcessed) {
                numProcessed++;
                if (numProcessed % 100 == 0) {
                    handler.updateAuditStatus("AUDIT ( " + numProcessed + " / "
                            + total + " ): Hashing: " + file.getName());
                }
                if (numProcessed % 5000 == 0) {
                    Utils.getAuditDatabaseEngine().commit();
                }
            }

            try {
                if (file.getName().endsWith(".chd")) {
                    // Skip CHDs
                    return null;
                } else {
                    if (file.getName().endsWith(".zip")) {
                        FileInfo zipFileInfo = new FileInfo(
                                file.getAbsolutePath(), false, null,
                                file.length(), new HashMap<String, String>(),
                                null, systemName);

                        ZipFile zf = null;
                        try {
                            zf = new ZipFile(file, Charsets.UTF_8.name(), false);
                            for (Enumeration<ZipArchiveEntry> e = zf
                                    .getEntries(); e.hasMoreElements();) {
                                ZipArchiveEntry ze = e.nextElement();
                                String name = ze.getName();

                                String crc32 = longToHex(ze.getCrc());
                                zipFileInfo.contentsCrc32.put(name, crc32);
                            }

                            for (Map.Entry<String, String> entry2 : zipFileInfo.contentsCrc32
                                    .entrySet()) {
                                addToHashEntryMap(hashEntryMap,
                                        entry2.getValue(),
                                        new RomHashEntryValue(entry2.getKey(),
                                                file.getAbsolutePath(),
                                                systemName));
                            }
                        } catch (ZipException ze) {
                            FileInfo badFileInfo = new FileInfo().setId(
                                    file.getAbsolutePath()).setBad(true);
                        } catch (Exception e) {
                            logger.info("GOT AN EXCEPTION", e);
                            return null;
                        } finally {
                            if (zf != null) {
                                zf.close();
                            }
                        }
                    } else if (file.getName().endsWith(".7z")) {
                        FileInfo zipFileInfo = new FileInfo(
                                file.getAbsolutePath(), false, null,
                                file.length(), new HashMap<String, String>(),
                                null, systemName);

                        SevenZHeaderReader zf = null;
                        try {
                            zf = new SevenZHeaderReader(file);
                            while (true) {
                                SevenZArchiveEntry entry = zf.getNextEntry();
                                if (entry == null) {
                                    break;
                                }
                                String name = entry.getName();
                                String crc32 = longToHex(entry.getCrc() & 0xffffffffL);
                                zipFileInfo.contentsCrc32.put(name, crc32);
                                System.out.println(name + " " + crc32);
                            }

                            for (Map.Entry<String, String> entry2 : zipFileInfo.contentsCrc32
                                    .entrySet()) {
                                addToHashEntryMap(hashEntryMap,
                                        entry2.getValue(),
                                        new RomHashEntryValue(entry2.getKey(),
                                                file.getAbsolutePath(),
                                                systemName));
                            }
                        } catch (Exception e) {
                            logger.info("GOT AN EXCEPTION", e);
                            return null;
                        } finally {
                            if (zf != null) {
                                zf.close();
                            }
                        }
                    } else {
                        FileInfo newFileInfo;
                        try {
                            String crc = crc32Sum(new FileInputStream(file));
                            // logger.info(file.getAbsolutePath() + " : " +
                            // file.getName() + " : " + crc);
                            newFileInfo = new FileInfo(file.getAbsolutePath(),
                                    false,
                                    // shaSum(new FileInputStream(file)),
                                    crc, file.length(), null, null, systemName);
                            addToHashEntryMap(
                                    hashEntryMap,
                                    newFileInfo.crc32,
                                    new RomHashEntryValue(file
                                            .getAbsolutePath(), file
                                            .getAbsolutePath(), systemName));
                        } catch (Exception e) {
                            logger.info("GOT AN EXCEPTION");
                            e.printStackTrace();
                            return null;
                        }
                    }
                }
            } catch (Exception e) {
                handler.auditError(e);
            }

            return file;
        }

        private void addToHashEntryMap(
                Map<String, ArrayList<RomHashEntryValue>> hashEntryMap,
                String hash, RomHashEntryValue entry) {
            // logger.info("Adding: " + sha1 + " -> " + entry);
            if (!hashEntryMap.containsKey(hash)) {
                ArrayList<RomHashEntryValue> al = new ArrayList<RomHashEntryValue>();
                al.add(entry);
                hashEntryMap.put(hash, al);
            } else {
                ArrayList<RomHashEntryValue> entries = new ArrayList<RomHashEntryValue>(
                        hashEntryMap.get(hash));
                if (!entries.contains(hash)) {
                    entries.add(entry);
                }
                hashEntryMap.put(hash, entries);
            }
        }

        public String crc32Sum(InputStream is) throws IOException {
            byte[] data = new byte[1 * 1024 * 1024];
            CRC32 crc32 = new CRC32();

            int nRead;

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                crc32.update(data, 0, nRead);
            }
            is.close();

            return longToHex(crc32.getValue());
        }

        public String shaSum(InputStream is) throws IOException {
            MessageDigest md = null;
            byte[] shaData = new byte[1 * 1024 * 1024];
            if (md == null) {
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
                if (arrayToHexFormatter != null) {
                    arrayToHexFormatter.close();
                }
            }
        }
    }

    public void scan(List<File> paths) throws IOException {
        handler.updateAuditStatus("AUDIT: Clearing old reverse-map");
        logger.info("Clearing old reverse-map");

        boolean deletedFiles = false;
        for (String filename : filesScanned.keySet()) {
            if (!new File(filename).exists()) {
                deletedFiles = true;
            }
        }

        if (deletedFiles) {
            Utils.stagedClear(hashEntryMap, Utils.getAuditDatabaseEngine());
            Utils.stagedClear(chdMap, Utils.getAuditDatabaseEngine());
            Utils.stagedClear(filesScanned, Utils.getAuditDatabaseEngine());
        }

        if (GameAuditor.abort) {
            return;
        }

        logger.info("Starting scan..");

        for (File path : paths) {
            walk(path, null);
            if (GameAuditor.abort) {
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
            if (GameAuditor.abort) {
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
        if (crc < 0) {
            throw new RuntimeException("OOPS");
        }
        Formatter arrayToHexFormatter = null;
        try {
            arrayToHexFormatter = new Formatter();
            arrayToHexFormatter.format("%02x%02x%02x%02x", ((crc >> 24) % 256),
                    ((crc >> 16) % 256), ((crc >> 8) % 256), crc % 256);
            return arrayToHexFormatter.toString();
        } finally {
            if (arrayToHexFormatter != null) {
                arrayToHexFormatter.close();
            }
        }
    }

    public void walk(File root, String machineName) throws IOException {

        File[] list = root.listFiles();
        handler.updateAuditStatus("AUDIT: Found " + list.length + " files in "
                + root.getName() + "...");

        for (File f : list) {
            if (GameAuditor.abort) {
                return;
            }
            if (f.isDirectory()) {
                // We need to go one level deeper to find chds and mess software
                for (File innerF : f.listFiles()) {
                    if (filesScanned.containsKey(innerF.getAbsolutePath())) {
                        // Skip already scanned files
                        continue;
                    }
                    if (innerF.getName().endsWith(".chd")) {
                        // Get SHA-1
                        byte sha1[] = new byte[20];
                        FileInputStream fis = new FileInputStream(innerF);
                        try {
                            if (fis.skip(84) != 84) {
                                logger.error("BAD CHD: "
                                        + innerF.getAbsolutePath());
                            } else if (fis.read(sha1) != 20) {
                                logger.error("BAD CHD: "
                                        + innerF.getAbsolutePath());
                            } else {
                                String hexString = Hex.encodeHexString(sha1);
                                // System.out.println("CHD: " + innerF.getName()
                                // + " " +
                                // hexString);
                                chdMap.put(hexString, innerF.getAbsolutePath());
                                filesScanned
                                        .put(innerF.getAbsolutePath(), true);
                            }
                        } finally {
                            fis.close();
                        }
                        break;
                    }
                }

                if (machineName == null) {
                    if (systemNames.contains(f.getName())) {
                        walk(f, f.getName());
                    }
                }
            } else {
                if (filesScanned.containsKey(f.getAbsolutePath())) {
                    // Skip already scanned files
                    continue;
                }
                Hasher h = new Hasher(f, machineName);
                hasherFutures.add(threadPool.submit(h));
                while (hasherFutures.size() > 100) {
                    try {
                        File processedFile = hasherFutures.poll().get();
                        if (processedFile != null) {
                            filesScanned.put(processedFile.getAbsolutePath(),
                                    true);
                        }
                        Thread.sleep(0);
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }
                total++;
            }
        }
    }

    /*
     * public static byte[] fileToBytes(File file) throws IOException {
     * RandomAccessFile f = new RandomAccessFile(file, "r"); byte[] b = new
     * byte[(int)f.length()]; f.read(b); return b; }
     * 
     * public static String shaSum(byte[] convertme) throws IOException{ if(md
     * == null) { try { md = MessageDigest.getInstance("SHA-1"); } catch
     * (NoSuchAlgorithmException e) { throw new IOException(e); } } return
     * byteArray2Hex(md.digest(convertme)); }
     */
}
