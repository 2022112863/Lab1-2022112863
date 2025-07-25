package org.example;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.YenKShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

/**
 * 全部函数.
 */
public class Main {

  private static final SecureRandom rand = new SecureRandom();

  /**
   * 读取文本文件内容.
   */
  public static String readFile(String filename) {
    StringBuilder result = new StringBuilder();
    try {
      // 获取基准路径（项目根目录）
      Path baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
      // 构造并规范化读取路径
      Path filePath = baseDir.resolve(filename).normalize();
      // 路径安全检查，防止越出基准目录
      if (!filePath.startsWith(baseDir)) {
        throw new SecurityException("禁止访问基准目录之外的路径: " + filePath);
      }
      // 使用 UTF-8 编码读取文件
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

  /**
   * 构建邻接表表示的有向图.
   */
  public static Map<String, Map<String, Integer>> buildDirectedGraph(String text) {
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

  /**
   * 在命令行中展示有向图.
   */
  public static void showDirectedGraph(Map<String, Map<String, Integer>> graph) {
    System.out.println("生成的有向图（命令行格式）：");
    for (Map.Entry<String, Map<String, Integer>> fromEntry : graph.entrySet()) {
      String from = fromEntry.getKey();
      Map<String, Integer> edges = fromEntry.getValue();
      for (Map.Entry<String, Integer> toEntry : edges.entrySet()) {
        String to = toEntry.getKey();
        Integer value = toEntry.getValue();
        System.out.println(from + " -> " + to + " [values=" + value + "]");
      }
    }

  }

  /**
   * 使用带权重的图.
   */
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

  /**
   * 查询桥接词（支持多次查询，输入 exit 退出）.
   */
  public static void queryBridgeWords(Map<String, Map<String, Integer>> graph) {
    Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    System.out.println("/******************** 查询桥接词 ********************/");
    System.out.println("请输入两个单词（用空格分隔），或者输入 exit 退出：");
    while (true) {
      System.out.print("> ");
      String line = scanner.nextLine().trim();
      if (line.equalsIgnoreCase("exit")) {
        System.out.println("已退出桥接词查询。");
        break;
      }
      String[] parts = line.split("\\s+");
      if (parts.length != 2) {
        System.out.println("输入格式错误！请确保输入两个英文单词。");
        continue;
      }
      String word1 = parts[0].toLowerCase();
      String word2 = parts[1].toLowerCase();
      // 检查 word1、word2 是否在图中
      if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
        System.out.println("No " + word1 + " or " + word2 + " in the graph!");
        continue;
      }
      List<String> bridgeWords = new ArrayList<>();
      Map<String, Integer> fromEdges = graph.get(word1);
      for (String bridgeCandidate : fromEdges.keySet()) {
        Map<String, Integer> bridgeEdges = graph.get(bridgeCandidate);
        if (bridgeEdges != null && bridgeEdges.containsKey(word2)) {
          bridgeWords.add(bridgeCandidate);
        }
      }
      if (bridgeWords.isEmpty()) {
        System.out.println("No bridge words from " + word1 + " to " + word2 + "!");
      } else {
        System.out.print("The bridge words from " + word1 + " to " + word2 + " are: ");
        for (int i = 0; i < bridgeWords.size(); i++) {
          System.out.print(bridgeWords.get(i));
          if (i < bridgeWords.size() - 2) {
            System.out.print(", ");
          } else if (i == bridgeWords.size() - 2) {
            System.out.print(", and ");
          }
        }
        System.out.println(".");
      }
    }
  }

  /**
   * 导出图像并显示边的权重.
   */
  public static void exportGraph(Graph<String, DefaultWeightedEdge> graph,
                                 String dotFilePath, String pngFilePath) throws IOException {
    DOTExporter<String, DefaultWeightedEdge> exporter = new DOTExporter<>(v -> v);
    exporter.setVertexAttributeProvider(v -> Map.of("label", DefaultAttribute.createAttribute(v)));
    exporter.setEdgeAttributeProvider(e -> {
      double weight = graph.getEdgeWeight(e);
      return Map.of("label", DefaultAttribute.createAttribute(String.valueOf((int) weight)));
    });
    Path basePath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    Path dotFile = basePath.resolve(dotFilePath).normalize();
    Path pngFile = basePath.resolve(pngFilePath).normalize();
    if (!dotFile.startsWith(basePath) || !pngFile.startsWith(basePath)) {
      throw new SecurityException("不允许写出基目录外的路径");
    }
    try (Writer writer = Files.newBufferedWriter(dotFile, StandardCharsets.UTF_8)) {
      exporter.exportGraph(graph, writer);
    }
    Graphviz.fromFile(dotFile.toFile())
            .render(Format.PNG)
            .toFile(pngFile.toFile());
    System.out.println("图像生成完毕: " + pngFilePath);
  }

  /**
   * 生成包含桥接词的新文本.
   */
  public static void generateNewText(Map<String, Map<String, Integer>> graph) {
    Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    System.out.println("/******************** 生成包含桥接词的新文本 ********************/");
    System.out.println("请输入一段文本（至少两个单词，用空格分隔开），或输入 exit 退出：");
    while (true) {
      System.out.print("> ");
      String line = scanner.nextLine().trim();
      if (line.equalsIgnoreCase("exit")) {
        System.out.println("已退出新文本生成。");
        break;
      }
      String[] words = line.toLowerCase().split("\\s+");
      if (words.length < 2) {
        System.out.println("文本过短，无法生成桥接词扩展。\n");
        continue;
      }
      StringBuilder newText = new StringBuilder();
      SecureRandom random = new SecureRandom();
      for (int i = 0; i < words.length - 1; i++) {
        String word1 = words[i];
        String word2 = words[i + 1];
        newText.append(word1).append(" ");
        // 查找桥接词：word1 -> bridge -> word2
        List<String> bridgeWords = new ArrayList<>();
        Map<String, Integer> word1Edges = graph.get(word1);
        if (word1Edges != null) {
          for (String bridge : word1Edges.keySet()) {
            Map<String, Integer> bridgeEdges = graph.get(bridge);
            if (bridgeEdges != null && bridgeEdges.containsKey(word2)) {
              bridgeWords.add(bridge);
            }
          }
        }
        if (!bridgeWords.isEmpty()) {
          String chosenBridge = bridgeWords.get(random.nextInt(bridgeWords.size()));
          newText.append(chosenBridge).append(" ");
        }
      }
      newText.append(words[words.length - 1]);
      System.out.println("生成的新文本：");
      System.out.println(newText.toString());
    }
  }

  /**
   * 查询最短路径（支持1个或2个单词）.
   */
  public static void calcShortestPath(Graph<String, DefaultWeightedEdge> graph,
                                      String dotPath, String pngPath) throws IOException {
    Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    System.out.println("/******************** 最短路径查询 ********************/");
    System.out.println("请输入1个或2个单词（用空格分隔），或输入 exit 退出：");
    while (true) {
      System.out.print("> ");
      String line = scanner.nextLine().trim();
      if (line.equalsIgnoreCase("exit")) {
        System.out.println("已退出最短路径查询。");
        break;
      }
      String[] parts = line.split("\\s+");
      if (parts.length == 1) {
        String source = parts[0];
        if (!graph.containsVertex(source)) {
          System.out.println("图中不存在单词：" + source);
          continue;
        }
        DijkstraShortestPath<String,
                DefaultWeightedEdge> dijkstra = new DijkstraShortestPath<>(graph);
        for (String target : graph.vertexSet()) {
          if (source.equals(target)) {
            continue;
          }
          GraphPath<String, DefaultWeightedEdge> path = dijkstra.getPath(source, target);
          if (path != null) {
            // 使用箭头分隔节点，构造路径的输出
            String pathString = String.join(" -> ", path.getVertexList());
            System.out.println("最短路径 " + source
                    + " -> " + target + " ：" + pathString + "，长度 = " + path.getWeight());
          }
        }
      } else if (parts.length == 2) {
        String source = parts[0];
        String target = parts[1];
        if (!graph.containsVertex(source) || !graph.containsVertex(target)) {
          System.out.println("图中缺少单词：" + source + " 或 " + target);
          continue;
        }
        YenKShortestPath<String, DefaultWeightedEdge> yen = new YenKShortestPath<>(graph);
        List<GraphPath<String, DefaultWeightedEdge>> paths = yen.getPaths(source, target, 10);
        if (paths.isEmpty()) {
          System.out.println("从 " + source + " 到 " + target + " 不可达！");
        } else {
          double minWeight = paths.get(0).getWeight();
          List<GraphPath<String, DefaultWeightedEdge>> shortestPaths = new ArrayList<>();
          for (GraphPath<String, DefaultWeightedEdge> path : paths) {
            if (path.getWeight() == minWeight) {
              shortestPaths.add(path);
            }
          }
          System.out.println("最短路径长度：" + minWeight);
          for (int i = 0; i < shortestPaths.size(); i++) {
            // 获取路径的节点列表并将它们用箭头连接
            String pathString = String.join(" -> ", shortestPaths.get(i).getVertexList());
            System.out.println("路径 " + (i + 1) + ": " + pathString);
          }
          highlightPathsInGraph(graph, shortestPaths, dotPath, pngPath);
          System.out.println("最短路径图已导出至: " + pngPath);
        }
      } else {
        System.out.println("输入格式有误！请输入一个或两个英文单词。");
      }
    }
  }

  /**
   * 显示路径信息.
   */
  public static void highlightPathsInGraph(Graph<String, DefaultWeightedEdge> graph,
                                             List<GraphPath<String, DefaultWeightedEdge>> paths,
                                             String dotFilePath, String pngFilePath
  ) throws IOException {
    // 准备颜色列表（可扩展）
    String[] colors = {"blue", "red", "green", "orange", "purple", "brown", "cyan"};
    Map<DefaultWeightedEdge, String> edgeColorMap = new HashMap<>();
    // 为每条路径分配颜色
    for (int i = 0; i < paths.size(); i++) {
      String color = colors[i % colors.length]; // 循环使用颜色
      for (DefaultWeightedEdge edge : paths.get(i).getEdgeList()) {
        edgeColorMap.put(edge, color);
      }
    }
    DOTExporter<String, DefaultWeightedEdge> exporter = new DOTExporter<>(v -> v);
    exporter.setVertexAttributeProvider(v -> {
      Map<String, Attribute> map = new LinkedHashMap<>();
      map.put("label", DefaultAttribute.createAttribute(v));
      return map;
    });
    exporter.setEdgeAttributeProvider(e -> {
      double weight = graph.getEdgeWeight(e);
      Map<String, Attribute> map = new LinkedHashMap<>();
      map.put("label", DefaultAttribute.createAttribute(String.valueOf((int) weight)));
      // 如果边在路径中，设置其颜色
      if (edgeColorMap.containsKey(e)) {
        map.put("color", DefaultAttribute.createAttribute(edgeColorMap.get(e)));
        map.put("penwidth", DefaultAttribute.createAttribute("2"));
      }
      return map;
    });
    // 获取当前项目根目录作为 baseDir
    Path baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    // 构建规范化路径，防止路径穿越
    Path dotFile = baseDir.resolve(dotFilePath).normalize();
    Path pngFile = baseDir.resolve(pngFilePath).normalize();
    // 安全检查：确保目标路径没有越界
    if (!dotFile.startsWith(baseDir) || !pngFile.startsWith(baseDir)) {
      throw new SecurityException("不允许写出基目录外的路径: " + dotFile + " 或 " + pngFile);
    }
    // 导出 .dot 文件
    try (Writer writer = Files.newBufferedWriter(dotFile, StandardCharsets.UTF_8)) {
      exporter.exportGraph(graph, writer);
    }
    // 渲染为 PNG
    Graphviz.fromFile(dotFile.toFile())
            .render(Format.PNG)
            .toFile(pngFile.toFile());

  }

  /**
   * 计算PageRank.
   */
  public static Map<String, Double> calPageRank(Graph<String, DefaultWeightedEdge> graph,
                                                double dampingFactor, int maxIterations) {
    System.out.println("/******************** 计算PageRank ********************/");
    // 初始化PR值
    Map<String, Double> pageRank = new HashMap<>();
    int numNodes = graph.vertexSet().size();
    // STEP 1：计算每个节点的 入度+出度 之和，并统计总和
    Map<String, Integer> degreeMap = new HashMap<>();
    for (String node : graph.vertexSet()) {
      int in = graph.inDegreeOf(node);
      int out = graph.outDegreeOf(node);
      int sum = in + out;
      degreeMap.put(node, sum);
    }
    // STEP 2：初始化每个节点的PageRank为其度在总度数中的占比
    for (String node : graph.vertexSet()) {
      pageRank.put(node, (double) degreeMap.get(node));
    }
    // PageRank迭代计算
    for (int iteration = 0; iteration < maxIterations; iteration++) {
      Map<String, Double> newPageRank = new HashMap<>();
      // 对每个节点计算新的PageRank
      for (String node : graph.vertexSet()) {
        double newPr = (1.0 - dampingFactor) / numNodes;  // PR值的基础部分
        double sum = 0.0;
        Set<String> inboundNeighbors = graph.incomingEdgesOf(node).stream()
                .map(graph::getEdgeSource)
                .collect(Collectors.toSet());
        // 对于每个指向当前节点的节点，累加其PR值 / 出度
        for (String neighbor : inboundNeighbors) {
          int outDegree = graph.outDegreeOf(neighbor);
          if (outDegree == 0) {
            // 如果出度为0，均分该节点的PR值给其他所有节点
            double distributedPr = pageRank.get(neighbor) / (numNodes - 1);
            for (String otherNode : graph.vertexSet()) {
              if (!otherNode.equals(neighbor)) {
                newPageRank.put(otherNode,
                        newPageRank.getOrDefault(otherNode, 0.0) + distributedPr);
              }
            }
          } else {
            sum += pageRank.get(neighbor) / outDegree;
          }
        }
        newPr += dampingFactor * sum;
        newPageRank.put(node, newPr);
      }
      // 更新PR值
      pageRank = newPageRank;
    }
    return pageRank;
  }

  /**
   * 随机游走.
   */
  public static void randomWalk(Graph<String, DefaultWeightedEdge> graph,
                                String outputFilePath) throws IOException {
    System.out.println("/******************** 随机游走 ********************/");
    List<String> visitedNodes = new ArrayList<>();
    // 随机选择起点
    List<String> vertices = new ArrayList<>(graph.vertexSet());
    if (vertices.isEmpty()) {
      System.out.println("图中无节点，无法进行随机游走！");
      return;
    }
    String current = vertices.get(rand.nextInt(vertices.size()));
    visitedNodes.add(current);
    System.out.println("随机游走起点为: " + current);
    System.out.println("输入 Enter 继续，输入 q 停止游走：");
    Set<DefaultWeightedEdge> visitedEdges = new HashSet<>();
    while (true) {
      Set<DefaultWeightedEdge> outgoing = graph.outgoingEdgesOf(current);
      List<DefaultWeightedEdge> candidates = new ArrayList<>();
      for (DefaultWeightedEdge edge : outgoing) {
        if (!visitedEdges.contains(edge)) {
          candidates.add(edge);
        }
      }
      if (candidates.isEmpty()) {
        System.out.println("节点不存在出边");
        break;
      }
      Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
      // 用户可以随时终止
      String input = scanner.nextLine();
      if (input.equalsIgnoreCase("q")) {
        System.out.println("用户停止随机游走");
        break;
      }
      // 随机选择一条边
      DefaultWeightedEdge edge = candidates.get(rand.nextInt(candidates.size()));
      visitedEdges.add(edge);
      current = graph.getEdgeTarget(edge);
      visitedNodes.add(current);
      System.out.println("-> " + current);
    }
    // 输出到文件
    StringBuilder result = new StringBuilder();
    for (String word : visitedNodes) {
      result.append(word).append(" ");
    }
    System.out.println("随机游走路径： " + result.toString().trim());
    String baseDir = System.getProperty("user.dir");
    Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
    Path outputPath = basePath.resolve(outputFilePath).normalize();
    // 检查路径安全
    if (!outputPath.startsWith(basePath)) {
      throw new SecurityException("不允许写出基目录外的路径: " + outputPath);
    }
    try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
      writer.write(result.toString().trim());
    }
    System.out.println("已写入文件：" + outputFilePath);
  }

  /**
   * 主函数入口.
   */
  public static void main(String[] args) throws IOException {
    String filename;
    if (args.length > 0) {
      filename = args[0];
    } else {
      Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
      System.out.print("请输入文本文件路径：");
      filename = scanner.nextLine();
    }
    String processedText = readFile(filename);
    if (processedText == null) {
      return;
    }
    Map<String, Map<String, Integer>> adjGraph = buildDirectedGraph(processedText);
    Graph<String, DefaultWeightedEdge> jgraph = convertToJgraphT(adjGraph);
    exportGraph(jgraph, "graph.dot", "graph.png");
    // 在命令行中展示图
    showDirectedGraph(adjGraph);
    // 查询桥连接词bridge words
    queryBridgeWords(adjGraph);
    // 根据bridge word生成新文本
    generateNewText(adjGraph);
    // 计算两个单词之间的最短路径
    calcShortestPath(jgraph, "shortest.dot", "shortest.png");
    // 计算PageRank
    double dampingFactor = 0.85;  // 阻尼因子
    int maxIterations = 50;     // 最大迭代次数
    Map<String, Double> pageRank = calPageRank(jgraph, dampingFactor, maxIterations);
    // 输出每个节点的PageRank
    for (Map.Entry<String, Double> entry : pageRank.entrySet()) {
      System.out.printf("%s: %.2f%n", entry.getKey(), entry.getValue());
    }
    // 添加随机游走功能

    randomWalk(jgraph, "random_walk.txt");
  }
}