import java.util.*;
import java.util.stream.Collectors;

class WorkerThread extends Thread {
	FlipGraph flipGraph;
	Set<Integer> startNode;
	List<List<Arc>> foundCycles;
	int r;
	static int runningThreadsCounter = 0;
	
	WorkerThread(FlipGraph flipGraph, Set<Integer> startPoint, int r, List<List<Arc>> foundCycles) {
		this.startNode = startPoint;
		this.flipGraph = flipGraph;
		this.r = r;
		this.foundCycles = foundCycles;
	}
	
	@Override
	public void run() {
		System.out.println("Started:" +  ++WorkerThread.runningThreadsCounter);
		
		Stack<Arc> arcStack = new Stack<>();
		int[] timesFound = new int[flipGraph.edges.size()];

		Map<Set<Integer>, List<Arc>> remainingArcs = new HashMap<>();
		for (Set<Integer> g : flipGraph.nodes) {
			remainingArcs.put(g, new ArrayList<>(flipGraph.nodeToOutgoingArcs.get(g)));
		}

		Set<Integer> currentNode = startNode;
		List<Arc> currentRemainingArcs = remainingArcs.get(currentNode);
		while(true) {
			if(!currentRemainingArcs.isEmpty()) {
				Arc currentArc = currentRemainingArcs.remove(0);
				
				// if currentArc leads to a visited node that's not the first node in the cycle, move to a next arc.
				if(currentArc.endNode != startNode) {
					boolean nodeAlreadyVisited = false;
					for(Arc arc: arcStack) {
						if(arc.endNode == currentArc.endNode) {
							nodeAlreadyVisited = true;
							break;
						}
					}
					if(nodeAlreadyVisited) continue;
				}
				
				int appearingDiagonalIdx = flipGraph.edges.indexOf(currentArc.edge);
				if(timesFound[appearingDiagonalIdx] < r) {
					
					timesFound[appearingDiagonalIdx]++;
					arcStack.push(currentArc);
					
					// if we came back to first node in the cycle, and the cycle is complete, we have found a rainbow cycle
					if(currentArc.endNode == startNode) {
						if(arcStack.size() == flipGraph.edges.size()*r) {
							synchronized (flipGraph) {
								List<Arc> foundCycle = new ArrayList<>(arcStack);
								List<Set<Integer>> toPrint = new ArrayList<>();
								for(Arc a: foundCycle) {
									toPrint.add(a.startNode);
								}
								System.out.println(toPrint);
								foundCycles.add(foundCycle);
							}
						}
						
						// Go back one step, and keep searching for other rainbow cycles
						timesFound[appearingDiagonalIdx]--;
						arcStack.pop();
						
					} else {
						// newCurrentNode get a fresh batch of arcs. 
						// This allows it to be used in exploring different cycles. 
						// What's done upon exploring cycle A shouldn't affect exploration of cycle B. 
						currentNode = currentArc.endNode;
						remainingArcs.get(currentNode).clear();
						remainingArcs.get(currentNode).addAll(flipGraph.nodeToOutgoingArcs.get(currentNode));
						currentRemainingArcs = remainingArcs.get(currentNode);
					}
				}
			} else {
				// All arcs of startNode were used (arcStack is empty in this case). Nothing to be done anymore. 
				if(currentNode == startNode) {
					System.out.println("Finished:" +  WorkerThread.runningThreadsCounter--);
					return;
				}
				
				// No? Then go back one step.
				int diagonalIdx = flipGraph.edges.indexOf(arcStack.pop().edge);
				timesFound[diagonalIdx]--;
				currentNode = arcStack.isEmpty()? startNode: arcStack.peek().endNode;
				currentRemainingArcs = remainingArcs.get(currentNode);
			}	
		}
	}
}

class Arc{
	
	Set<Integer> startNode, endNode;
	Set<Integer> edge = new HashSet<>();
	
	public Arc(Set<Integer> startNode, Set<Integer> endNode) throws Exception {
		this.startNode = startNode;
		this.endNode = endNode;
		
		int appearing = 0, disappearing = 0;
		for(int x: startNode) {
			if(!endNode.contains(x)) disappearing = x;
		}
		
		for(int x: endNode) {
			if(!startNode.contains(x)) appearing = x;
		}
		
		if(appearing != 0 && disappearing != 0) {
			edge.add(appearing);
			edge.add(disappearing);
		} else {
			throw new Exception();
		}
	}

	@Override
	public String toString() {
		return startNode + " -> "+ endNode;
	}
}

public class FlipGraph {
	List<Set<Integer>> nodes;
	List<Set<Integer>> edges;
	List<Arc> arcs;
	Map<Set<Integer>, Set<Arc>> nodeToOutgoingArcs = new HashMap<>();

	private List<Set<Integer>> generateCombinations(int n, int r) {
		List<Set<Integer>> combinations = new ArrayList<>();
	    Integer[] combination = new Integer[r];

	    // initialize with lowest lexicographic combination
	    for (int i = 0; i < r; i++) {
	        combination[i] = i;
	    }

	    while (combination[r - 1] < n) {
	    	Integer [] combinationMod = combination.clone();
	    	for (int i = 0; i < combinationMod.length; i++) {
	    		combinationMod[i] = combinationMod[i]+1;
			}
	    	
	        combinations.add(new HashSet(Arrays.asList(combinationMod)));

	         // generate next combination in lexicographic order
	        int t = r - 1;
	        while (t != 0 && combination[t] == n - r + t) {
	            t--;
	        }
	        combination[t]++;
	        for (int i = t + 1; i < r; i++) {
	            combination[i] = combination[i - 1] + 1;
	        }
	    }

	    return combinations;
	}
	
	private List<Arc> generateArcs(List<Set<Integer>> nodes) throws Exception{
		
		List<Arc> arcs = new ArrayList<>();
		for (int i = 0; i < nodes.size(); i++) {
			for (int j = 0; j < nodes.size(); j++) {
				Set<Integer> result = nodes.get(i).stream()
						  .distinct()
						  .filter(nodes.get(j)::contains)
						  .collect(Collectors.toSet());
				if(result.size() == nodes.get(0).size()-1) {
					arcs.add(new Arc(nodes.get(i), nodes.get(j)));
				}
			}
		}
		
		return arcs;
	}
	
	public FlipGraph(int n, int k) throws Exception {
		nodes = generateCombinations(n, k);
		arcs = generateArcs(nodes);
		edges = generateCombinations(n, 2);
		
		for(Set<Integer> node: nodes) {
			nodeToOutgoingArcs.put(node, new HashSet<>());
		}
		
		for(Arc arc: arcs) {
			nodeToOutgoingArcs.get(arc.startNode).add(arc);
		}
		
	}
	
	
	public List<List<Arc>> findRainbowCycle(int r) {
		List<List<Arc>> foundCycles = new ArrayList<List<Arc>>();
		
		List<Thread> workerThreadList = new ArrayList<>();
		// Five different start nodes
		for(int i=1; i<=5; i++) {
			workerThreadList.add(new WorkerThread(this, nodes.get((nodes.size()/5)*i), r, foundCycles));
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
	
	// Adoboted from the findRainbowCycle method. Probably needs some cleaning.
	public void findRainbowBlock(int n, int k, int r) throws Exception {
		List<Set<Integer>> nodes = generateCombinations(n, k);
		List<Arc> arcs = generateArcs(nodes);
		Map<Set<Integer>, Set<Arc>> nodeToOutgoingArcs = new HashMap<>();
		
		for(Set<Integer> node: nodes) {
			nodeToOutgoingArcs.put(node, new HashSet<>());
		}
		
		for(Arc arc: arcs) {
			nodeToOutgoingArcs.get(arc.startNode).add(arc);
		}
		
		Stack<Arc> arcStack = new Stack<>();
		int[] distancesTimesFound = new int[n/2];

		Map<Set<Integer>, List<Arc>> remainingArcs = new HashMap<>();
		for (Set<Integer> g : nodes) {
			remainingArcs.put(g, new ArrayList<>(nodeToOutgoingArcs.get(g)));
		}
		
		// startNode is 1,2,..,k-1,n
		Set<Integer> startNode = null;
		for(Set<Integer> set: nodes) {
			boolean thisIsIt = true;
			if(!set.contains(n)) {
				thisIsIt = false;
				continue;
			}
			for (int i = 1; i < k; i++) {
				if(!set.contains(i)) thisIsIt = false;
			}
			if(thisIsIt) startNode = set;
		}
		
		Set<Integer> currentNode = startNode;
		List<Arc> currentRemainingArcs = remainingArcs.get(currentNode);
		while(true) {
			if(!currentRemainingArcs.isEmpty()) {
				Arc currentArc = currentRemainingArcs.remove(0);
				
				boolean nodeAlreadyVisited = false;
				for(Arc arc: arcStack) {
					if(arc.endNode == currentArc.endNode || currentArc.endNode == startNode) {
						nodeAlreadyVisited = true;
						break;
					}
				}
				
				if(nodeAlreadyVisited) continue;
					
				int[] edge = currentArc.edge.stream().mapToInt(Integer::intValue).toArray();
				int distanceFound = shorterDirection(edge[0], edge[1], n);
				if(distancesTimesFound[distanceFound-1] < r) {
					
					distancesTimesFound[distanceFound-1]++;
					arcStack.push(currentArc);
					
					boolean allDistancesFound = true;
					for (int i = 0; i < distancesTimesFound.length-1; i++) {
						if(distancesTimesFound[i] != r) allDistancesFound = false;
					}
					
					// Takes into accound whether n is odd or even. This probably need some cleaning.
					if(distancesTimesFound[distancesTimesFound.length-1] != (n%2==0 || r==1? 1: 2))
						allDistancesFound = false;
					
					if(allDistancesFound) {
						boolean foundPotentialBlock = true;
						
						// Check whether first node in second block is 1,2,...,k
						for (int i = 1; i <= k; i++) {
							if(!currentArc.endNode.contains(i)) foundPotentialBlock = false;
						}
						
						if(foundPotentialBlock) {
							List<Arc> foundCycle = new ArrayList<>(arcStack);
							
							List<Set<Integer>> toPrint = new ArrayList<>();
							for(Arc a: foundCycle) {
								toPrint.add(a.startNode);
							}
							
							if(isRainbowBlock(toPrint, n, k, r))
								System.out.println(toPrint);
						}
						
						// Go back one step, and keep searching for other rainbow cycles
						distancesTimesFound[distanceFound-1]--;
						arcStack.pop();
						
					} else {
						currentNode = currentArc.endNode;
						remainingArcs.get(currentNode).clear();
						remainingArcs.get(currentNode).addAll(nodeToOutgoingArcs.get(currentNode));
						currentRemainingArcs = remainingArcs.get(currentNode);
					}
				}
			} else {
				if(arcStack.isEmpty()) {
					return;
				}
				
				int[] edge = arcStack.pop().edge.stream()
                        .mapToInt(Integer::intValue)
                        .toArray();
				int distanceFound = shorterDirection(edge[0], edge[1], n);
				distancesTimesFound[distanceFound-1]--;
				currentNode = arcStack.isEmpty()? startNode: arcStack.peek().endNode;
				currentRemainingArcs = remainingArcs.get(currentNode);
			}	
		}
	}
	
	
	public static int shorterDirection(int crt, int next ,int mod) {
		  int toRight = (next - crt + mod) % mod;
		  int toLeft = (crt - next + mod) % mod;
		  return toLeft < toRight ? toLeft : toRight;
	}
	
	public boolean isRainbowBlock(List<Set<Integer>> block, int n, int k, int r) {
		// Create edges
		List<String> edges = new ArrayList<>();
		for (int i = 1; i < n; i++) {
			for (int j = i+1; j <=n; j++) {
				edges.add(""+i+","+j);
			}
		}
		
		
		List<Set<Integer>> cycle = new ArrayList<>(block);
		
		for (int i=1; i<n; i++) {
			for(Set<Integer> set: block) {
				Set<Integer> newSet = new HashSet<>();
				for(Integer num: set) {
					Integer numPlus = (num+i) % n;
					if(numPlus == 0) numPlus = n;
					newSet.add(numPlus);
				}
				cycle.add(newSet);
			}
		}
		
		// Check if each edge is used the correct number of times
		Map<String, Integer> appearanceNumbers = new HashMap<>();
		for(String e: edges) {
			appearanceNumbers.put(e, 0);
		}
		
		for(int i=0; i< cycle.size(); i++) {
			int j = i+1;
			if(j == cycle.size()) j = 0;
			
			List<Integer> tmp1 = new ArrayList<>(cycle.get(i));
			List<Integer> tmp2 = new ArrayList<>(cycle.get(j));
			tmp1.removeAll(cycle.get(j));
			tmp2.removeAll(cycle.get(i));
			
			String edgeUsed = tmp1.get(0)+","+tmp2.get(0);
			String edgeUsedReversed = tmp2.get(0)+","+tmp1.get(0);
			
			
			if(appearanceNumbers.containsKey(edgeUsed)) {
				appearanceNumbers.put(edgeUsed, appearanceNumbers.get(edgeUsed)+1);	
			} else {
				appearanceNumbers.put(edgeUsedReversed, appearanceNumbers.get(edgeUsedReversed)+1);	
			}
			
		}
		
		for(Integer appNum: appearanceNumbers.values()) {
			if(appNum != r) return false;
		}
		
		// Check if size of a set is not correct
		for(Set<Integer> set: cycle) {
			if(set.size() != k) 
				return false;
		}
		
		// Check if cycle contains duplicates
		for (int i = 1; i < cycle.size(); i++) {
			for (int j = i+1; j < cycle.size(); j++) {
				if (cycle.get(i).equals(cycle.get(j))) {
					return false;
				}
			}
		}
		
		
		return true;
	}
	
}
