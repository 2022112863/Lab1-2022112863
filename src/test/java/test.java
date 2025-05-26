import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import org.example.Main;

public class test {

    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;

    public String readFile() {
        String filename = "Easy Test.txt";  // 指定文件名，相对路径
        StringBuilder result = new StringBuilder();
        try {
            Path baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
            Path filePath = baseDir.resolve(filename).normalize();
            if (!filePath.startsWith(baseDir)) {
                throw new SecurityException("禁止访问基准目录之外的路径: " + filePath);
            }
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                int ch;
                while ((ch = reader.read()) != -1) {
                    char c = (char) ch;
                    result.append(Character.isLetter(c) ? Character.toLowerCase(c) : ' ');
                }
            }
        } catch (IOException e) {
            System.err.println("无法打开文件: " + e.getMessage());
            return null;
        }
        return result.toString();
    }

    public Map<String, Map<String, Integer>> buildDirectedGraph(String text) {
        Map<String, Map<String, Integer>> graph = new HashMap<>();
        String[] words = text.trim().split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            String from = words[i];
            String to = words[i + 1];
            if (from.isEmpty() || to.isEmpty()) {
                continue;
            }
            graph.putIfAbsent(from, new HashMap<>());
            Map<String, Integer> edges = graph.get(from);
            edges.put(to, edges.getOrDefault(to, 0) + 1);
        }
        return graph;
    }

    @BeforeEach
    public void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    public void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    // 测试用例1：scientist analyzed -> 有桥接词 carefully
    @Test
    public void testBridgeWordsExist() {
        String input = "scientist analyzed\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        Main.queryBridgeWords(buildDirectedGraph(readFile()));
        String output = outContent.toString(StandardCharsets.UTF_8);
        originalOut.println("case1 expected output:The bridge words from scientist to analyzed are: carefully.\n");
        originalOut.println("case1 actual output:\n" + output);
        assertTrue(output.contains("The bridge words from scientist to analyzed are: carefully."));
    }
}
