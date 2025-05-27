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

public class testWhite {

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
    public void testExitInput() throws IOException {

        // 准备模拟输入 "exit"
        String simulatedInput = "exit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
        System.setIn(inputStream);
        // 捕获系统输出
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
        // 调用待测方法
        Main.calcShortestPath(convertToJgraphT(buildDirectedGraph(readFile())), "shortest.dot", "shortest.png");
        // 恢复标准输出
        System.setOut(originalOut);
        // 断言输出中包含退出提示语
        String output = outputStream.toString(StandardCharsets.UTF_8);
        System.out.println("case1 actual output:\n" + output);
        assertTrue(output.contains("已退出最短路径查询。"), "输出应包含退出提示");
    }

    @Test
    public void testShortestPath_wroteToReport() throws IOException {

        // 模拟输入 "wrote report" + exit 以结束交互
        String simulatedInput = "wrote report\nexit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
        System.setIn(inputStream);
        // 捕获输出
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
        // 执行测试方法
        Main.calcShortestPath(convertToJgraphT(buildDirectedGraph(readFile())), "shortest.dot", "shortest.png");
        // 恢复输出流
        System.setOut(originalOut);
        // 获取并打印输出
        String output = outputStream.toString(StandardCharsets.UTF_8);
        System.out.println("case2 actual output:\n" + output);
        // 核心断言判断输出内容
        assertTrue(output.contains("最短路径长度：3.0"), "应输出路径长度 3.0");
        assertTrue(output.contains("wrote -> a -> detailed -> report"), "应输出具体路径");
        assertTrue(output.contains("最短路径图已导出至: shortest.png"), "应输出图导出提示");
        assertTrue(output.contains("已退出最短路径查询。"), "应有退出提示");
    }

    @Test
    public void testShortestPathsFromWrote() throws IOException {
        String simulatedInput = "wrote\nexit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
        System.setIn(inputStream);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
        Main.calcShortestPath(convertToJgraphT(buildDirectedGraph(readFile())), "shortest.dot", "shortest.png");
        System.setOut(originalOut);
        String output = outputStream.toString(StandardCharsets.UTF_8);
        System.out.println("case3 actual output:\n" + output);
        // 检查是否包含所有目标路径和长度
        String[] expectedPaths = {
                "最短路径 wrote -> but ：wrote -> a -> detailed -> report -> with -> the -> team -> but，长度 = 8.0",
                "最短路径 wrote -> the ：wrote -> a -> detailed -> report -> with -> the，长度 = 5.0",
                "最短路径 wrote -> a ：wrote -> a，长度 = 1.0",
                "最短路径 wrote -> detailed ：wrote -> a -> detailed，长度 = 2.0",
                "最短路径 wrote -> shared ：wrote -> a -> detailed -> report -> and -> shared，长度 = 5.0",
                "最短路径 wrote -> data ：wrote -> a -> detailed -> report -> with -> the -> data，长度 = 6.0",
                "最短路径 wrote -> so ：wrote -> a -> detailed -> report -> with -> the -> data -> so，长度 = 7.0",
                "最短路径 wrote -> carefully ：wrote -> a -> detailed -> report -> with -> the -> scientist -> carefully，长度 = 8.0",
                "最短路径 wrote -> analyzed ：wrote -> a -> detailed -> report -> with -> the -> scientist -> analyzed，长度 = 8.0",
                "最短路径 wrote -> scientist ：wrote -> a -> detailed -> report -> with -> the -> scientist，长度 = 7.0",
                "最短路径 wrote -> more ：wrote -> a -> detailed -> report -> with -> the -> team -> requested -> more，长度 = 9.0",
                "最短路径 wrote -> it ：wrote -> a -> detailed -> report -> with -> the -> scientist -> analyzed -> it，长度 = 9.0",
                "最短路径 wrote -> team ：wrote -> a -> detailed -> report -> with -> the -> team，长度 = 7.0",
                "最短路径 wrote -> requested ：wrote -> a -> detailed -> report -> with -> the -> team -> requested，长度 = 8.0",
                "最短路径 wrote -> again ：wrote -> a -> detailed -> report -> with -> the -> scientist -> analyzed -> it -> again，长度 = 10.0",
                "最短路径 wrote -> report ：wrote -> a -> detailed -> report，长度 = 3.0",
                "最短路径 wrote -> with ：wrote -> a -> detailed -> report -> with，长度 = 4.0",
                "最短路径 wrote -> and ：wrote -> a -> detailed -> report -> and，长度 = 4.0"
        };
        for (String path : expectedPaths) {
            assertTrue(output.contains(path), "缺失预期输出: " + path);
        }
        assertTrue(output.contains("已退出最短路径查询。"), "应有退出提示");
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

    @Test
    public void testShortestPath_wordNotFound() throws IOException {

        // 模拟用户输入 "like day"（路径中至少一个词不在图中）+ exit
        String simulatedInput = "like day\nexit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
        System.setIn(inputStream);
        // 捕获控制台输出
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
        // 构建图并执行 calcShortestPath 方法
        Main.calcShortestPath(convertToJgraphT(buildDirectedGraph(readFile())), "shortest.dot", "shortest.png");
        // 恢复控制台输出
        System.setOut(originalOut);
        // 读取输出内容
        String output = outputStream.toString(StandardCharsets.UTF_8);
        System.out.println("case5 actual output:\n" + output);
        // 核心断言：判断是否输出缺失词语的提示
        assertTrue(output.contains("图中缺少单词：like 或 day"), "应提示缺少单词 like 或 day！");
        assertTrue(output.contains("已退出最短路径查询。"), "应有退出提示");
    }
    
    @Test
    public void testShortestPath_likeMissing() throws IOException {
        // 模拟输入 "like"（只输入一个词），然后 exit
        String simulatedInput = "like\nexit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
        System.setIn(inputStream);
        // 捕获输出
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
        // 执行主方法
        Main.calcShortestPath(
                convertToJgraphT(buildDirectedGraph(readFile())),
                "shortest.dot",
                "shortest.png"
        );
        // 恢复原输出流
        System.setOut(originalOut);
        // 获取输出内容
        String output = outputStream.toString(StandardCharsets.UTF_8);
        System.out.println("case6 actual output:\n" + output);
        // 核心断言
        assertTrue(output.contains("图中不存在单词：like"), "应输出图中缺少单词 like 的提示");
    }
}
