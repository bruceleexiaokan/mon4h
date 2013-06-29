import org.junit.Test;




public class test {

	private String t = "|12|34";
	
	@Test
	public void T() {
		
		String[] s = t.split("|");
		
		for( String ss : s ) { 
			System.out.println(ss);
		}
	}
	
}
