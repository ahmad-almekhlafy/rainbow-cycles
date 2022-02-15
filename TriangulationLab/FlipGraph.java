import java.io.*;
import java.util.*;

class WorkerThread extends Thread {
	FlipGraph flipGraph;
	Graph startNode;
	List<List<Arc>> foundCycles;
	int r;
	String outputFile;
	static int runningThreadsCounter = 0;
	WorkerThread(FlipGraph flipGraph, Graph startPoint, int r, List<List<Arc>> foundCycles, String outputFile) {
		this.startNode = startPoint;
		this.flipGraph = flipGraph;
		this.r = r;
		this.foundCycles = foundCycles;
		this.outputFile = outputFile;
	}

	private boolean areCyclesInverse(List<Arc> a, List<Arc> b) {
		// Cycle 1: a -> b -> c -> d -> e -> a
		// Inverse Cycle: a <- e <- d <- c <- b <- a
		
		if (a.size() != b.size())
			return false;

		for (int i = 0; i < a.size(); i++) {
			if (!a.get(i).v1.equals(b.get(b.size() - 1 - i).v1) && !a.get(i).v2.equals(b.get(b.size() - 1 - i).v2)) {
				return false;
			}
		}
		return true;
	}


	private void addCycle(List<Arc> newCycle) {
		
		// Make first arc the smallest arc. This way, where a cycle begins is always defined.
		Arc firstArc = null;
		for (Arc arc : newCycle) {
			if (firstArc == null || arc.hashCode() < firstArc.hashCode()) 
				firstArc = arc;
		}
		Collections.rotate(newCycle, -newCycle.indexOf(firstArc));
		
		// Check whether the same cycle, inverted however, had been found already. If yes, return without adding anything.
		for (List<Arc> cycle : foundCycles) {
			if (areCyclesInverse(cycle, newCycle)) 
				return;
		}
		
		if (!foundCycles.contains(newCycle)) {
			foundCycles.add(newCycle);
			System.out.println("Cycle number " + foundCycles.size() + " found.");
			
			if(outputFile.isEmpty()) return;
			
			PrintWriter outputWriter = null;
			try {
				outputWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
				for (int i = 0; i < newCycle.size(); i++) {
					Graph iGraph = newCycle.get(i).v2;
					Edge iDiagonal = newCycle.get(i).diagonal;
					
					// A label of the diagonal appearing in this graph
					String label = "Text(\"e_{" + flipGraph.diagonalToIdx.get(iDiagonal)+ "}\",";
					label += iGraph.nodes.get(0).getGeogebraCode(i * 3+ 0.1, 0 + 3 * foundCycles.size()-0.1) + ")";
					
					outputWriter.println(label);
					outputWriter.println(iGraph.getGeogebraCode(i * 3, 0 + 3 * foundCycles.size()));
					
				}
				outputWriter.println();
				outputWriter.close();
				
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}

	@Override
	public void run() {
		System.out.println("Started: Thread" +  ++WorkerThread.runningThreadsCounter);
		
		Stack<Arc> arcStack = new Stack<>();
		int[] timesFound = new int[flipGraph.diagonalToIdx.keySet().size()];

		Map<Graph, List<Arc>> remainingArcs = new HashMap<>();
		for (Graph g : flipGraph.nodes) {
			remainingArcs.put(g, new ArrayList<>(flipGraph.nodeToOutgoingArcs.get(g)));
		}

		Graph currentNode = startNode;
		List<Arc> currentRemainingArcs = remainingArcs.get(currentNode);
		while(true) {
			if(!currentRemainingArcs.isEmpty()) {
				Arc currentArc = currentRemainingArcs.remove(0);
				
				// if currentArc leads to a visited node that's not the first node in the cycle, move to a next arc.
				if(currentArc.v2.id != startNode.id) {
					boolean nodeAlreadyVisited = false;
					for(Arc arc: arcStack) {
						if(arc.v2.id == currentArc.v2.id) {
							nodeAlreadyVisited = true;
							break;
						}
					}
					if(nodeAlreadyVisited) continue;
				}
				
				int appearingDiagonalIdx = flipGraph.diagonalToIdx.get(currentArc.diagonal);
				if(timesFound[appearingDiagonalIdx] < r) {
					
					timesFound[appearingDiagonalIdx]++;
					arcStack.push(currentArc);
					
					// if we came back to first node in the cycle, and the cycle is complete, we have found a rainbow cycle
					if(currentArc.v2.id == startNode.id) {
						if(arcStack.size() == flipGraph.diagonalToIdx.keySet().size()*r) {
							synchronized (flipGraph) {
								List<Arc> foundCycle = new ArrayList<>(arcStack);
								addCycle(foundCycle);
								return;
							}
						}
						
						// Go back one step, and keep searching for other rainbow cycles
						timesFound[appearingDiagonalIdx]--;
						arcStack.pop();
						
					} else {
						// newCurrentNode get a fresh batch of arcs. 
						// This allows it to be used in exploring different cycles. 
						// What's done upon exploring cycle A shouldn't affect exploration of cycle B. 
						currentNode = currentArc.v2;
						remainingArcs.get(currentNode).clear();
						remainingArcs.get(currentNode).addAll(flipGraph.nodeToOutgoingArcs.get(currentNode));
						currentRemainingArcs = remainingArcs.get(currentNode);
					}
				}
			} else {
				// All arcs of startNode were used (arcStack is empty in this case). Nothing to be done anymore. 
				if(currentNode.id == startNode.id) {
					System.out.println("Finished:" +  WorkerThread.runningThreadsCounter--);
					return;
				}
				
				// No? Then go back one step.
				int diagonalIdx = flipGraph.diagonalToIdx.get(arcStack.pop().diagonal);
				timesFound[diagonalIdx]--;
				currentNode = arcStack.isEmpty()? startNode: arcStack.peek().v2;
				currentRemainingArcs = remainingArcs.get(currentNode);
			}	
		}
	}
}

class Arc {
	Graph v1, v2;
	Edge diagonal;

	Arc(Graph v1, Graph v2, Edge diagonal) {
		this.v1 = v1;
		this.v2 = v2;
		this.diagonal = diagonal;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((v1 == null) ? 0 : v1.hashCode());
		result = prime * result + ((v2 == null) ? 0 : v2.hashCode() * 10);
		result = prime * result + ((diagonal == null) ? 0 : diagonal.hashCode());
		return result;
	}

	
	@Override
	public boolean equals(Object obj) {
		Arc other = (Arc) obj;
		if (this == other
				|| (this.v1.equals(other.v1) && this.v2.equals(other.v2) && this.diagonal.equals(other.diagonal))) {
			return true;
		}

		return false;
	}

}

public class FlipGraph {
	List<Graph> nodes = new ArrayList<>();
	List<Arc> arcs = new ArrayList<>();
	Map<Edge, Integer> diagonalToIdx = new HashMap<>();
	Map<Graph, Set<Arc>> nodeToOutgoingArcs = new HashMap<>();
	Map<Edge, Set<Graph>> diagonalToNodesContainingIt = new HashMap<>();

	public void addGraph(Graph g) {
		if (!nodes.isEmpty()) {
			for (Graph v : nodes) {
				List<Edge> a = new ArrayList<>(v.edges);
				List<Edge> b = new ArrayList<>(g.edges);

				// Check whether difference is only one edge
				a.removeAll(b);
				if (a.size() != 1)
					continue;
				Edge d1 = a.remove(0);

				a.addAll(v.edges);
				b.removeAll(a);
				if (b.size() != 1)
					continue;
				Edge d2 = b.remove(0);

				// Check whether both edges constitute a flip (d2 intersects only d1 in G1)
				if (!segmentsIntersect(d1, d2))
					continue;

				boolean notFlip = false;
				for (Edge e : v.edges) {
					if (e.equals(d1))
						continue;
					if (segmentsIntersect(d2, e))
						notFlip = true;
				}
				if (notFlip)
					continue;

				// Save corresponding diagonals along with their indicies
				if (!diagonalToIdx.containsKey(d1))
					diagonalToIdx.put(d1, diagonalToIdx.size());
				if (!diagonalToIdx.containsKey(d2))
					diagonalToIdx.put(d2, diagonalToIdx.size());
				
				// Create and save arcs
				Arc a1 = new Arc(v, g, d2);
				Arc a2 = new Arc(g, v, d1);
				arcs.add(a1);
				arcs.add(a2);

				// Add both arcs to a Node->OutgoingArcs Map to use later for backtracking
				if (!nodeToOutgoingArcs.containsKey(a1.v1)) {
					Set<Arc> outgoingArcs = new HashSet<>();
					outgoingArcs.add(a1);
					nodeToOutgoingArcs.put(a1.v1, outgoingArcs);
				} else {
					nodeToOutgoingArcs.get(a1.v1).add(a1);
				}

				if (!nodeToOutgoingArcs.containsKey(a2.v1)) {
					Set<Arc> outgoingArcs = new HashSet<>();
					outgoingArcs.add(a2);
					nodeToOutgoingArcs.put(a2.v1, outgoingArcs);
				} else {
					nodeToOutgoingArcs.get(a2.v1).add(a2);
				}
				
				// Fill a diagonal->ContainingNodes Map to use later for backtracking
				Set<Graph> nodesContainingD1 = diagonalToNodesContainingIt.get(d1) == null? new HashSet<>(): diagonalToNodesContainingIt.get(d1);
				nodesContainingD1.add(v);
				diagonalToNodesContainingIt.put(d1, nodesContainingD1);
				
				Set<Graph> nodesContainingD2 = diagonalToNodesContainingIt.get(d2) == null? new HashSet<>(): diagonalToNodesContainingIt.get(d2);
				nodesContainingD2.add(g);
				diagonalToNodesContainingIt.put(d2, nodesContainingD2);
			}
		}	
		
		nodes.add(g);
	}

	// See: https://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect
	private int getOrientation(Node v1, Node v2, Node v3) {
		double val = (v2.y - v1.y) * (v3.x - v2.x) - (v2.x - v1.x) * (v3.y - v2.y);

		if (val == 0) {
			return 0;
		} else if (val < 0) {
			return 1;
		} else {
			return 2;
		}
	}

	private boolean pointOnSegment(Node x, Node p, Node r) {
		if (x.x <= Math.max(p.x, r.x) && x.x >= Math.min(p.x, r.x) && x.y <= Math.max(p.y, r.y)
				&& x.y >= Math.min(p.y, r.y)) {
			return true;
		} else {
			return false;
		}
	}

	boolean segmentsIntersect(Edge e1, Edge e2) {
		if ((e1.contains(e2.v1) || e1.contains(e2.v2)) && !e1.equals(e2))
			return false;

		int o1 = getOrientation(e1.v1, e1.v2, e2.v1);
		int o2 = getOrientation(e1.v1, e1.v2, e2.v2);
		int o3 = getOrientation(e2.v1, e2.v2, e1.v1);
		int o4 = getOrientation(e2.v1, e2.v2, e1.v2);

		if (o1 != o2 && o3 != o4)
			return true;

		if (o1 == 0 && pointOnSegment(e1.v1, e2.v1, e1.v2))
			return true;

		if (o2 == 0 && pointOnSegment(e1.v1, e2.v2, e1.v2))
			return true;

		if (o3 == 0 && pointOnSegment(e2.v1, e1.v1, e2.v2))
			return true;

		if (o4 == 0 && pointOnSegment(e2.v1, e1.v2, e2.v2))
			return true;

		return false;
	}


	public List<List<Arc>> findRainbowCycle(int r, String outputFile) {
		// All graphs containing a diagonals: All cycles must contain one of these graphs. 
		// We get a smallest set of graphs, that all contains the same diagonal.
		// We use them as starting points.
		Set<Graph> possibleStartPoints = new HashSet<>();
		for(Edge diagonal: diagonalToNodesContainingIt.keySet()) {
			if(possibleStartPoints.isEmpty() || possibleStartPoints.size() > diagonalToNodesContainingIt.get(diagonal).size()) {
				possibleStartPoints = diagonalToNodesContainingIt.get(diagonal);
			}
		}
		
		List<List<Arc>> foundCycles = new ArrayList<List<Arc>>();
		List<Thread> workerThreadList = new ArrayList<>();
		for(Graph startingPoint: possibleStartPoints) {
			Thread workerThread  = new WorkerThread(this, startingPoint, r, foundCycles, outputFile);
			workerThreadList.add(workerThread);
		}
		
		for(Thread workerThread: workerThreadList) {
			workerThread.start();
		}
		
		for(Thread workerThread: workerThreadList) {
			try {
				workerThread.join();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return foundCycles;
	}
	
	
	public boolean isRainbowCycle(List<Graph> cycle, int r) {
		
		for (int i = 0; i < cycle.size(); i++) {
			for (int j = i+1; j < cycle.size(); j++) {
				Set<Edge> iSet = new HashSet<>(cycle.get(i).edges);
				Set<Edge> jSet = new HashSet<>(cycle.get(j).edges);
				if(iSet.equals(jSet)) 
					return false;
			}
		}
		
		Map<Edge, Integer> appearanceTimes = new HashMap<>();
		for (int i = 0; i < cycle.size(); i++) {
			int j = i+1;
			if(j == cycle.size()) 
				j = 0;
			
			List<Edge> a = new ArrayList<>(cycle.get(i).edges);
			List<Edge> b = new ArrayList<>(cycle.get(j).edges);

			// Check whether difference is only one edge
			a.removeAll(b);
			if (a.size() != 1)
				return false;
			Edge d1 = a.remove(0);

			a.addAll(cycle.get(i).edges);
			b.removeAll(a);
			if (b.size() != 1)
				return false;
			Edge d2 = b.remove(0);

			// Check whether both edges constitute a flip (d2 intersects only d1 in G1)
			if (!segmentsIntersect(d1, d2)) {
				return false;
			}

			boolean notFlip = false;
			for (Edge e : cycle.get(i).edges) {
				if (e.equals(d1))
					continue;
				if (segmentsIntersect(d2, e))
					notFlip = true;
			}
			if (notFlip)
				return false;

			if (!appearanceTimes.containsKey(d2)) {
				appearanceTimes.put(d2, 1);
			} else {
				if(appearanceTimes.get(d2) == r) {
					return false;
				} else {
					appearanceTimes.put(d2, appearanceTimes.get(d2)+1);
				}
			}
		}
		
		for(Edge d: appearanceTimes.keySet()) {
			if(appearanceTimes.get(d) != r) 
				return false;
		}
		
		return true;
		
	}
	
}
