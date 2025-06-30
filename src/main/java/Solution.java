import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class Solution {
    static final int MAX_COLUMNS = 30;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java -Xmx1G -jar project.jar <input_file_path>");
            return;
        }

        long startTime = System.nanoTime();
        String inputFile = args[0];

        List<ParsedLine> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(Paths.get(inputFile))), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!isValidLine(line)) continue;

                line = line.replace("\"", "");

                String[] parts = line.split(";", -1);

                lines.add(new ParsedLine(line, parts));
            }
        }

        int n = lines.size();
        DSU dsu = new DSU(n);

        for (int col = 0; col < MAX_COLUMNS; col++) {
            Map<String, List<Integer>> valueMap = new HashMap<>();

            for (int idx = 0; idx < n; idx++) {
                String[] parts = lines.get(idx).parts;
                if (col >= parts.length) continue;

                String val = parts[col].trim();
                if (val.isEmpty()) continue;

                valueMap.computeIfAbsent(val, k -> new ArrayList<>()).add(idx);
            }

            for (List<Integer> group : valueMap.values()) {
                int root = group.get(0);
                for (int i = 1; i < group.size(); i++) {
                    dsu.union(root, group.get(i));
                }
            }

        }

        Map<Integer, List<String>> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = dsu.find(i);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(lines.get(i).original);
        }

        List<List<String>> filteredGroups = new ArrayList<>();
        for (List<String> group : groups.values()) {
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
        System.out.printf("Кол-во групп с более, чем 1 элементом: %d\n", filteredGroups.size());
        System.out.printf("Время выполнения: %.3f секунд\n", durationSeconds);
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
