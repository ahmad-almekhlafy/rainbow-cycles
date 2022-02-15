public class main {

	public static void main(String[] args) {
		try {
			FlipGraph f = new FlipGraph(7,2);
			//f.findRainbowCycle(1);
			f.findRainbowBlock(7,2,1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
