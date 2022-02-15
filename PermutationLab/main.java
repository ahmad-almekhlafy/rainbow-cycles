import java.util.*;

public class main {

	public static boolean isRainbowCycle(List<String> list, int r, int n) {
		// Create the edges
		List<String> edges = new ArrayList<>();
		for (int i = 1; i < n; i++) {
			for (int j = i+1; j <=n; j++) {
				edges.add(i+""+j);
			}
		}
		
		// Check if the list contains any duplicates
		for (int i = 1; i < list.size(); i++) {
			for (int j = i+1; j < list.size(); j++) {
				if (list.get(i).equals(list.get(j))) {
					return false;
				}
			}
		}
		
		// Cheack if each edge is used the correct number of times
		Map<String, Integer> appearanceNumbers = new HashMap<>();
		for(String e: edges) {
			appearanceNumbers.put(e, 0);
		}
		
		for(int i=0; i< list.size(); i++) {
			int j = i+1;
			if(j == list.size()) j = 0;
			
			String diff = getTransposition(list.get(i), list.get(j), n);
			appearanceNumbers.put(diff, appearanceNumbers.get(diff)+1);	
		}
		
		for(String e: edges) {
			if(appearanceNumbers.get(e) != r) {
				return false;
			}
		}

		return true;
	}
	
	public static List<String> get2RainbowCycle(int n){
		List<String> inductionBase = Arrays.asList("123","321","231","213","312","132");
		if(n == 3) return inductionBase;
		if(n < 3) return null;
		
		List<String> output = List.copyOf(inductionBase);
		
		for(int i=3; i<n; i++) {
			output = applyInductionStep(output, i);
		}
		
		return output;
		
	}
	public static List<String> applyInductionStep(List<String> list, int n){
		// Create the edges
		List<String> edges = new ArrayList<>();
		for (int i = 1; i < n; i++) {
			for (int j = i+1; j <=n; j++) {
				edges.add(""+i+j);
			}
		}
		
		// Add n+1 at the end of each permutation (only in Block A if n is odd)
		// If n is odd, replace n with n+1 in each permutation of Block B and add n at the end of each such permutation
		List<String> newCycle = new ArrayList<>();
		for(int i=0; i< list.size(); i++) {
			if(n%2==0 || i <=list.size()/2) {
				newCycle.add(list.get(i) + (n+1));
			}  else {
				String mod = list.get(i);
				mod = mod.replace(Character.forDigit(n, 10), Character.forDigit(n+1, 10));
				mod += n;
				newCycle.add(mod);
			}
		}
		
		
		// Add the new permutations
		for(int i=0; i< newCycle.size(); i++) {
			int j = i+1;
			if(j == newCycle.size()) j = 0;
			
			// Replace t_i with the triple t_i(n+1)
			String diff = getTransposition(newCycle.get(i), newCycle.get(j), n);
			for(int k=1; k<= n/2; k++) {
				// if two consequtive permutations are connected with t_i
				if(diff.equals(""+(2*k-1)+(2*k))) { 
					String permBeforeInsertionPlace = newCycle.get(i);
					
					String firstTransposition = "";
					firstTransposition += diff.charAt(0);
					firstTransposition += Character.forDigit(n+1, 10);
					
					String firstNewPerm = applyTransposition(permBeforeInsertionPlace, firstTransposition);
					String secondNewPerm = applyTransposition(firstNewPerm, diff);
					
					newCycle.add(i+1, firstNewPerm);
					newCycle.add(i+2, secondNewPerm);
					i+=2;
				}
			}
		}
		
		// Apply (n,n+1) on last permutation of block A if n is odd 
		if(n%2 == 1) {
			String lastPermBlockA = newCycle.get(newCycle.size()/2);
			String transposition  = ""+n;
			transposition  +=n+1;
			newCycle.add(newCycle.size()/2+1, applyTransposition(lastPermBlockA, transposition));
			
			newCycle.add(applyTransposition(newCycle.get(newCycle.size()-1), getTransposition(list.get(0), list.get(list.size()-1), n) ));
		}
		
		return newCycle;
	}
	
	public static String getTransposition(String perm1, String perm2, int n) {
		char[] s1 = perm1.toCharArray();
		char[] s2 = perm2.toCharArray();
		String diff = "";
		for(int k=0; k<n ;k++) {
			if(s1[k] != s2[k]) {
				diff += (k+1);
			}
		}
		return diff;
	}
	
	public static String applyTransposition(String inputPerm, String transposition) {
		char[] permAsArray = inputPerm.toCharArray();
		char tmp = permAsArray[Character.getNumericValue(transposition.charAt(1)-1)];
		permAsArray[Character.getNumericValue(transposition.charAt(1))-1] = permAsArray[Character.getNumericValue(transposition.charAt(0))-1];
		permAsArray[Character.getNumericValue(transposition.charAt(0))-1] = tmp;
		
		return new String(permAsArray);
	}
	
	
	public static void main(String[] args) {	
		List<String> rainbowCycle = get2RainbowCycle(4);
		if(isRainbowCycle(rainbowCycle, 2, 4)){
			System.out.println("A 2-rainbow cycle for n=9 was successfully produced.");
			System.out.println("Block A:");
			for (int i = 0; i < rainbowCycle.size()/2; i++) {
				System.out.println(rainbowCycle.get(i));
			}
			
			System.out.println("Block B:");
			for (int i = rainbowCycle.size()/2; i < rainbowCycle.size(); i++) {
				System.out.println(rainbowCycle.get(i));
			}
		}

	}

}
