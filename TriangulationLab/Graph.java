import java.util.*;


class Node {
		double x, y;
		
		Node(double x, double y){
			this.x = x;
			this.y = y;
		}
		
		@Override
		public String toString() {
			return "(" + x + ", " + y + ")";
		}
		
		public String getGeogebraCode(double offsetX, double offsetY) {
			return "(" + (x + offsetX) + ", " + (y + offsetY) + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(Math.round(x * 100.0) / 100.0);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits((Math.round(y * 100.0) / 100.0)+5);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			Node other = (Node) obj;
			if (this == other || Math.abs(this.x - other.x)<=1e-6 && Math.abs(this.y - other.y)<=1e-6) {
				return true;
			}
			
			return false;
		}	
}

class Edge {
	Node v1, v2;
	
	Edge(Node v1, Node v2){
		this.v1 = v1;
		this.v2 = v2;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		int firstHash = (v1 == null) ? 0 : v1.hashCode();
		int secondHash = (v1 == null) ? 0 : v2.hashCode();
		if(firstHash > secondHash) {
			result = prime * result + firstHash;
			result = prime * result + secondHash;
		} else {
			result = prime * result + secondHash;
			result = prime * result + firstHash;
		}
		
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		Edge other = (Edge) obj;
		if (this == other || ((this.v1.equals(other.v1) && this.v2.equals(other.v2)) || (this.v1.equals(other.v2) && this.v2.equals(other.v1)))) {
			return true;
		}
		
		return false;
	}


	@Override
	public String toString() {
		return "{" + v1.toString() + ", " + v2.toString() + "}";
	}
	
	public String getGeogebraCode(double offsetX, double offsetY) {
		return "Segment(" + v1.getGeogebraCode(offsetX, offsetY) + "," + v2.getGeogebraCode(offsetX, offsetY) + ")";
	}
	
	public boolean contains(Node p) {
		if(v1.equals(p) || v2.equals(p)) {
			return true;
		}
		return false;
	}
}


public class Graph {
	List<Node> nodes = new ArrayList<>();
	List<Edge> edges = new ArrayList<>();
	public int id;
	static int counter = 0;

	public Graph() {
		this.id = counter++;
	}
	
	@Override
	public String toString() {
		
		String retString = "V = {";
		for(Node v: nodes) {
			retString += v + ", ";
		}
		retString = retString.substring(0, retString.length() - 2) +  "}, E={";
		for(Edge e: edges) {
			retString += "(" + nodes.indexOf(e.v1) + ", " + nodes.indexOf(e.v2) + "), ";
		}
		retString = retString.substring(0, retString.length() - 2) +  "}";
		
		return retString;
		
	}
	
	
	public String getGeogebraCode(double offsetX, double offsetY) {
		String retString = "";
		
		for(Edge e: edges) {
			retString += e.getGeogebraCode(offsetX, offsetY)+"\n";
		}
		for(Node n: nodes) {
			retString += n.getGeogebraCode(offsetX, offsetY)+"\n";
		}
		
		return retString;
	}
	
	public void offset(double x, double y) {
		for(Node n: nodes) {
			n.x += x;
			n.y += y;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((edges == null) ? 0 : edges.hashCode());
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Graph other = (Graph) obj;
		if (edges == null) {
			if (other.edges != null) {
				return false;
			}
		} else if (!edges.equals(other.edges)) {
			return false;
		}
		if (nodes == null) {
			if (other.nodes != null) {
				return false;
			}
		} else if (!nodes.equals(other.nodes)) {
			return false;
		}
		return true;
	}
	
	public boolean isStarTriangulation() {
		Map<Node, Integer> nodeToDegree = new HashMap<>();
		
		for(Node v: nodes) {
			nodeToDegree.put(v, 0);
		}
		
		for(Edge e: edges) {
			nodeToDegree.put(e.v1, nodeToDegree.get(e.v1)+1);
			nodeToDegree.put(e.v2, nodeToDegree.get(e.v2)+1);
		}
		
		for(Node v: nodes) {
			if(nodeToDegree.get(v) == nodes.size()-1) return true;
		}
		
		return false;
		
	}
	
	public boolean isZigZagTriangulation() {
		Map<Node, Integer> nodeToDegree = new HashMap<>();
		
		for(Node v: nodes) {
			nodeToDegree.put(v, 0);
		}
		
		for(Edge e: edges) {
			nodeToDegree.put(e.v1, nodeToDegree.get(e.v1)+1);
			nodeToDegree.put(e.v2, nodeToDegree.get(e.v2)+1);
		}
		
		int nodesWithDeg2 = 0;
		int nodesWithDeg3 = 0;
		int nodesWithDeg4 = 0;
		
		for(Node v: nodes) {
			if(nodeToDegree.get(v) == 2) {
				nodesWithDeg2++;
			} else if (nodeToDegree.get(v) == 3) {
				nodesWithDeg3++;
			} else if (nodeToDegree.get(v) == 4) {
				nodesWithDeg4++;
			} else {
				return false;
			}
		}
		
		if(nodesWithDeg2 == 2 && nodesWithDeg3 == 2 && nodesWithDeg4 == nodes.size() - 4) {
			return true;
		}
			
		return false;
		
	}
}
