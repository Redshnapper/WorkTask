import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Solution {
    static final int MAX_COLUMNS = 3;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java -Xmx1G -jar project.jar <input_file_path.7z>");
            return;
        }

        long startTime = System.nanoTime();
        String inputFile = args[0];
        File file = new File(inputFile);

        int lineCount = 0;
        List<Map<String, List<Integer>>> columnMaps = new ArrayList<>();
        for (int i = 0; i < MAX_COLUMNS; i++) columnMaps.add(new HashMap<>());

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             SeekableByteChannel channel = raf.getChannel();
             SevenZFile sevenZFile = SevenZFile.builder().
                     setSeekableByteChannel(channel).
                     get()) {

            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".csv")) {
                    InputStream entryStream = new SevenZFileInputStream(sevenZFile);
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(entryStream, StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!isValidLine(line)) continue;

                            line = line.replace("\"", "");
                            String[] parts = line.split(";", -1);
                            for (int col = 0; col < Math.min(parts.length, MAX_COLUMNS); col++) {
                                String val = parts[col].trim();
                                if (val.isEmpty()) continue;
                                columnMaps.get(col).computeIfAbsent(val, k -> new ArrayList<>()).add(lineCount);
                            }
                            lineCount++;
                        }
                    }
                }
            }
        }

        DSU dsu = new DSU(lineCount);
        for (Map<String, List<Integer>> colMap : columnMaps) {
            for (List<Integer> list : colMap.values()) {
                if (list.size() < 2) continue;
                int root = list.get(0);
                for (int i = 1; i < list.size(); i++) {
                    dsu.union(root, list.get(i));
                }
            }
        }

        Map<Integer, List<String>> groupMap = new HashMap<>();
        int currentLine = 0;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             SeekableByteChannel channel = raf.getChannel();
             SevenZFile sevenZFile = SevenZFile.builder().
                     setSeekableByteChannel(channel).
                     get()) {

            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".csv")) {
                    InputStream entryStream = new SevenZFileInputStream(sevenZFile);
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(entryStream, StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!isValidLine(line)) {
                                currentLine++;
                                continue;
                            }

                            line = line.replace("\"", "");
                            int groupId = dsu.find(currentLine);
                            groupMap.computeIfAbsent(groupId, k -> new ArrayList<>()).add(line);
                            currentLine++;
                        }
                    }
                }
            }
        }

        List<List<String>> filteredGroups = new ArrayList<>();
        for (List<String> group : groupMap.values()) {
            if (group.size() > 1) {
                filteredGroups.add(group);
            }
        }

        filteredGroups.sort((a, b) -> b.size() - a.size());

        try (PrintWriter writer = new PrintWriter("out.txt", StandardCharsets.UTF_8)) {
            writer.println(filteredGroups.size());
            int groupNum = 1;
            for (List<String> group : filteredGroups) {
                writer.println("Группа " + groupNum++);
                for (String str : group) {
                    writer.println(str);
                }
                writer.println();
            }
        }

        double durationSeconds = (System.nanoTime() - startTime) / 1e9;

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        System.out.printf("Кол-во групп с более, чем 1 элементом: %d\n", filteredGroups.size());
        System.out.printf("Время выполнения: %.3f секунд\n", durationSeconds);
        System.out.printf("Всего памяти: %.2f MB\n", totalMemory / (1024.0 * 1024));
        System.out.printf("Использовано памяти: %.2f MB\n", usedMemory / (1024.0 * 1024));
    }

    static boolean isValidLine(String line) {
        String[] parts = line.split(";", -1);
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            if (!(part.startsWith("\"") && part.endsWith("\""))) return false;
            String inner = part.substring(1, part.length() - 1);
            if (inner.contains("\"")) return false;
        }
        return true;
    }
}
