import java.util.Scanner;

public class Votes {
	public static void main (String[] args){
		
		int age; 
		String name;
		Scanner input = new Scanner(System.in);
		System.out.println("Please enter legal first and last name.");
		name = input.nextLine();
		System.out.println("Please enter your age as of November 3rd, 2016.");
		age = input.nextInt();
		
		if(age >= 18)
		{
			System.out.println(name + ", you are eligible to vote. Please press enter to proceed.");
		}
		else
		{
			System.out.println(name + ", you are not eligible to vote. Have a nice day.");
		}
		
		
	}
}
