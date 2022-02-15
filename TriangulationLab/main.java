import java.io.*;
import java.util.*;


public class main {
	public static void main(String[] args) throws CloneNotSupportedException, FileNotFoundException{	
			
		List<Graph> graphs = new ArrayList<>();
		FileInputStream inputFile = new FileInputStream(args[0]);
		Scanner fileScanner = new Scanner(inputFile);
		List<Node> nodes = new ArrayList<>();
		while (fileScanner.hasNextLine()) {
	
			String nextLine = fileScanner.nextLine();
			Scanner lineScanner = new Scanner(nextLine);
			lineScanner.useDelimiter("[^[\\-]?\\d+[\\.\\d+]?]+");
			if (nextLine.startsWith("V")) {
				while (lineScanner.hasNext()) {
					nodes.add(new Node(Double.valueOf(lineScanner.next()), Double.valueOf(lineScanner.next())));
				}
	
			} else {
				Graph nextGraph = new Graph();
				for (Node v : nodes) {
					nextGraph.nodes.add(new Node(v.x, v.y));
				}
	
				while (lineScanner.hasNextInt()) {
					nextGraph.edges.add(new Edge(nextGraph.nodes.get(lineScanner.nextInt()),
							nextGraph.nodes.get(lineScanner.nextInt())));
				}
	
				graphs.add(nextGraph);
			}
			
			lineScanner.close();
		}
		fileScanner.close();
	
		FlipGraph f = new FlipGraph();
		for (Graph g : graphs) {
			f.addGraph(g);
		}
		
		f.findRainbowCycle(Integer.valueOf(args[1]), args[2]);
	}
}

