import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.example.Main;

public class testWhite4 {

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

    public static Graph<String, DefaultWeightedEdge> convertToJgraphT(
            Map<String, Map<String, Integer>> adjGraph) {
        Graph<String, DefaultWeightedEdge> jgraph = new DefaultDirectedWeightedGraph<>(
                DefaultWeightedEdge.class);
        for (Map.Entry<String, Map<String, Integer>> fromEntry : adjGraph.entrySet()) {
            String from = fromEntry.getKey();
            Map<String, Integer> edges = fromEntry.getValue();
            jgraph.addVertex(from);
            for (Map.Entry<String, Integer> entry : edges.entrySet()) {
                String to = entry.getKey();
                int weight = entry.getValue();
                jgraph.addVertex(to);
                DefaultWeightedEdge edge = jgraph.addEdge(from, to);
                if (edge != null) {
                    jgraph.setEdgeWeight(edge, weight);
                }
            }
        }
        return jgraph;
    }

    @Test
    public void testShortestPath_invalidInput() throws IOException {

        // 模拟非法输入 "analyzed the data"（超过两个单词） + exit
        String simulatedInput = "analyzed the data\nexit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
        System.setIn(inputStream);
        // 捕获标准输出
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
        // 执行方法
        Main.calcShortestPath(convertToJgraphT(buildDirectedGraph(readFile())), "shortest.dot", "shortest.png");
        // 恢复标准输出
        System.setOut(originalOut);
        // 获取程序输出
        String output = outputStream.toString(StandardCharsets.UTF_8);
        System.out.println("case4 actual output:\n" + output);
        // 核心断言判断：应提示输入格式错误
        assertTrue(output.contains("输入格式有误！请输入一个或两个英文单词。"), "应提示输入格式错误！");
        assertTrue(output.contains("已退出最短路径查询。"), "应有退出提示");
    }
}