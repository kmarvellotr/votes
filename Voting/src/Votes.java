import java.util.Scanner;

public class Votes {
	private static String bernie;

	public static void main (String[] args){
		
		int age; 
		String name;
		Scanner input = new Scanner(System.in);
		System.out.println("Please enter legal first and last name.");
		name = input.nextLine();
		System.out.println("Please enter your age as of November 3rd, 2016.");
		age = input.nextInt();
		
		if(name.equals("bernie") || age >= 18 && name.equals("clinton"))
		{
			System.out.println(name + ", you are eligible to vote. Please press enter to proceed.");
			
		}
		else 
		{
			System.out.println(name + ", you are not eligible to vote. Have a nice day.");
		}
		}
		
	}

