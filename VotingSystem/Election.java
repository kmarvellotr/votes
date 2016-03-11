import java.io.*;
import java.util.*;


/** Election is the base class for all elections. It also contains the main program that
 ** runs when the program is started
 **/
abstract public class Election {
   boolean done = false;  // when true, break out of processing loop
   
   class Candidate {
	   String name;
	   String party;
	int voteCount;
	Candidate(String name, String party) {
		this.name = name;
		this.party = party;
		voteCount = 0;
	}
   }
   class Voter {
	String name;
	String party;
	boolean voted;
	Voter(String name, String party) {
		this.name = name;
		this.party = party;
		voted = false;
	}
   }
   
   protected Hashtable candidates = new Hashtable();
   protected Hashtable voters = new Hashtable();
   StringBuffer OfficialRecord = new StringBuffer();
   
   void log(String s) {	
      OfficialRecord.append(s + '\n');
      System.out.println(s);
   }
	
   public static void main(String[] args) {
       // Instantiate an election
       Election e = null;
          if (args.length > 0) {
             if (args[0].compareTo("General") == 0) { 
	        e = new GeneralElection();
	     } else if (args[0].compareTo("Primary") == 0) {
	        e = new PrimaryElection();
	     }
	   } 
       if (e == null) {
          System.out.println("Specify General or Primary");
	  return;
       }
       // Open input file
       FileReader in;
       String filename = "election.dat";
       try {
          in = new FileReader(filename);
       } catch (IOException ex) {
	 e.log("Cannot open " + filename);
	 return;
       } 
       // Processing
       try {
	 e.process(in);
         in.close();
       } catch (IOException ex) {
	 e.log("Something Bad Happened");
       } 
   } // main
   
   /** 
    ** Main processing loop, reads and processes messages
    **/
   void process(FileReader rdr) throws IOException {
      BufferedReader b = new BufferedReader(rdr);
      String msg;
      MessageFactory MF = new MessageFactory();
      while (!done && b.ready()) {
	msg = b.readLine();
	// In a real system, we would probably use multithreading
	// and place each message in a queue.
	Message m = MF.create(msg);
	if (m != null) {
	   try {
	      m.perform(this);
	   } catch (ElectionException ex) {
	      log(ex.getLogMessage());
	   }
	}
      }
   }
   
   /** verification is abstract, so it must be defined in a derived class **/
   abstract void verify(String cand, String voter) throws ElectionException;
   
   /**  Record a vote **/
   void vote(String cand, String voter) throws ElectionException {
      verify(cand, voter);
      Candidate c = getCandidate(cand);
      c.voteCount += 1;
      Voter v = getVoter(voter);
      v.voted = true;
      log(voter + " voted for " + cand);
   }
   
   /** Register a voter **/
   void register(String voter, String party) {
     Voter v = (Voter) voters.get(voter);
     if (v != null) {
        v.party = party;
	log(voter + " affiliation changed to " + party);
     } else {
        voters.put(voter, new Voter(voter, party));
        log("Register voter " + voter+ " as a " + party);
     }
   }
   
   /** Register a candidate **/
   void candidate(String cand, String party) {
     Candidate c = (Candidate) candidates.get(cand);
     if (c != null) {
       c.party = party;
       log(cand + " affiliation changed to " + party);
       return;
     } else {
       candidates.put(cand, new Candidate(cand, party));
       log("Register candidate " + cand + " as a " + party);
     }
   }
   
   /** List all of the candidates, PrimaryElection overrides **/
   void list(String voter) throws ElectionException {
	   Enumeration it = candidates.elements();
	   log("Candidate list for " + voter);
	   int counter = 0;
	   while(it.hasMoreElements()) {
	     Candidate c = (Candidate) it.nextElement();
    	     log("  " + ++counter + c.name);
	   }
   }
   
   /** Report the vote count for each candidate **/
   void tally() throws ElectionException {
	   log("Tally");
	   Enumeration it = candidates.elements();
	   while(it.hasMoreElements()) {
		Candidate c = (Candidate) it.nextElement();
		log("  " + c.name + " (" + c.party + ") " + c.voteCount);
	   }
   }
   
   /** Reset all of the vote counts to 0 and voters as not voted **/
   void reset() {
	   log("Reset");

	   Enumeration ic = candidates.elements();
	   while(ic.hasMoreElements()) {
		Candidate c = (Candidate) ic.nextElement();
		c.voteCount = 0;
	   }
	   
	   Enumeration iv = voters.elements();
	   while(iv.hasMoreElements()) {
		Voter v = (Voter) iv.nextElement();
		v.voted = false;
	   }
	   
   }
   
   void dump() {
	   System.out.println("\n\n----------------Start of Dump-------------------");
	   System.out.println(OfficialRecord);
	   System.out.println("---------------End of Dump------------------------");
   }
   
   void exit(){
	   done = true;
	   log("Exit");
   }
   
   /** Retrieve a voter object
    **/
   Voter getVoter(String voter) throws NotRegisteredException {
      Voter v = (Voter) voters.get(voter);
      if (v == null) {
	 throw new NotRegisteredException(voter);
      }
      return v;
   }
   
   /** Retrieve a candidate object
   **/
   Candidate getCandidate(String name) throws NotACandidateException {
      Candidate c = (Candidate) candidates.get(name);
      if (c == null) {
	 throw new NotACandidateException(name);
      }
      return c;
   }
   
   /** Return true if candidate and voter parties match
    **/
   boolean sameParty(Candidate c, Voter v) {
      return (c.party.compareTo(v.party) == 0);
   }
 	   
}; // Election

/*****************************************************************************
 MESSAGE CLASSES
 *****************************************************************************/
 
/** The Messsage class is the abstract base class for all messages. Each drived
 ** message type must define a perform method that operates on an election
 **/
abstract class Message {
   String type;
   /** perform implements tasks associated with the specific message
    ** @param e is the election for which the message is intended
    ** @throws ElectionException if the tasks cannot be performed
    **/
   abstract void perform(Election e) throws ElectionException;
   
   /** parse separates message arguments into individual strings
    ** Each argument is of the form <argument>, where the argument is delimited
    ** by angle brackets. parse reads the actual arguments into an array 
    ** provided by the caller. The number of arguments read is the lesser of
    ** the number of elements provided in the message and the number of slots
    ** in the receiving array (args).
    ** @param msg is the actual message, which is a single string
    ** @param args is an array of strings into which the actual arguments are 
    **        placed. 
    **/
   void parse(String msg, String[] args) {
      StringBuffer b = new StringBuffer();
      type = msg.substring(0,4);
      int count = 0;
      int start = msg.indexOf('<');
      int end = msg.indexOf('>');
      while (count < args.length && start < end && start >= 0) {
	args[count++] = msg.substring(start + 1, end);
        start = msg.indexOf('<', end);
        end = msg.indexOf('>', start);
      } 
   } // Parse
};// Message

/** Request to record a vote **/
	class VoteMessage extends Message 
	{
		String[] args = new String[2]; // Expects two arguments, candidate and voter.
			VoteMessage(String s) 
			{
				parse(s, args);
			}
   // Record the vote
		void perform(Election e) throws ElectionException 
		{
			e.vote(args[0], args[1]);
		}
	};

/** Request to register a new voter **/
	class RegisterMessage extends Message 
	{
		String[] args = new String[2]; // Expects two arguments, voter and party
			RegisterMessage(String s) 
			{
				parse(s, args);
			}
   // Register the voter
		void perform(Election e) throws ElectionException 
		{
			e.register(args[0], args[1]);
		}
	}; //RegisterMessage

/** Request to register a candidate **/
class CandidateMessage extends Message {
   String[] args = new String[2];
   CandidateMessage(String s) {
     parse(s, args);
   }
   // Register the candidate
   void perform(Election e) throws ElectionException {
     e.candidate(args[0], args[1]);
   }
}; // End CandidateMessage

/** Request to list the candidates available to a particular voter **/
class ListMessage extends Message {
   String[] args = new String[1]; // Expects one argument, a voter
   ListMessage(String s) {
      parse(s, args);
   }
   // Display the list
   void perform(Election e) throws ElectionException {
     e.list(args[0]);
   }
};

/** Request that the program - and the election - terminate 
 **/
class ExitMessage extends Message {
   void perform(Election e) throws ElectionException {
     e.exit();
   }
};

/** Request a listing of all of the transactions for the election 
 **/
	class DumpMessage extends Message 
	{
		void perform(Election e) throws ElectionException 
		{
			e.dump();
		}
	};

/** Request that the election be restarted 
 **/
	class ResetMessage extends Message  
	{
		void perform(Election e) throws ElectionException 
		{
			e.reset();
		}
	};

/** Request a tally of all of the candidates and the number of
 ** votes they have received 
 **/
	class TallyMessage extends Message 
	{
		void perform(Election e) throws ElectionException 
		{
			e.tally();
		}
	};

/** Example of a Factory -- this contains only one static method, create, which
 ** generates a new message object of the appropriate type from a text 
 ** message. Each message has the form described in the assignment
 ** where the first four characters define the message type, the fifth 
 ** character is blank and the remainder of the message string contains
 ** arguments delimited by angle brackets
 **/
class MessageFactory {
   /** This method, create, examines the first few characters of the message
    ** to determine the type of the message, then creates a new instance of 
    ** the appropriate message type.
    ** @param msg is text, which could be read from a file or could be received 
    ** from a network of voting machines and terminals
    ** @return is an object derived from the class Method, or null if the type of 
    ** the message is unknown
    **/
   public static Message create(String msg) 
   {
	   Message result = null;
	   char type = msg.charAt(0);
	   // Note, if this were real code we would check the complete message id.
	   switch (type) {
	   case 'D': // dump
	        result = new DumpMessage();
		break;
	case 'E': // Exit
		result = new ExitMessage();
		break;
        case 'V': // VOTE
		result = new VoteMessage(msg);
		break;
	case 'R': // RGST or REST
	        if (msg.charAt(1) == 'G') {
		  result = new RegisterMessage(msg);
	        } else {
		  result = new ResetMessage();
		}
		break;
	case 'C': // CAND
		result = new CandidateMessage(msg);
		break;
	case 'L': // LIST
		result = new ListMessage(msg);
		break;
	case 'T': // TLLY
		result = new TallyMessage();
		break;
     } // switch
     return result;
   } // create
}; // MessageFactory

/** GeneralElection
 ** Voters can vote for any candidate
 **/
class GeneralElection extends Election  {
   GeneralElection() {
     log("General Election");
   }
   
   /**
    ** verify only that the voter has not already voted
    @param candidate is not used
    @param voter is the name of a voter
    **/
   void verify(String candidate, String voter)throws ElectionException {
     Voter v = getVoter(voter);
     if (v.voted) { // check that voter hasn't already voted
       throw new AlreadyVotedException(voter);
     }
   }
};

/** PrimaryElection
 ** Voters can only vote for candidates in the same party
 **/
class PrimaryElection extends Election {
   PrimaryElection() {
     log("Primary Election");
   }
   
   /** Verify that the voter has not already voted and that the candidate
    ** for whom the voter is voting has the same party affiliation as the
    ** voter
    ** @param candidate is the name of the candidate for whom the voter
    ** is voting
    ** @param voter is the name of the voter
    ** @throws AlreadyVotedException if the voter has already voted in
    ** this election.
    ** @throws WrongPartyException if the voter and candidate do not
    ** belong to the same party.
    **/
   void verify(String candidate, String voter) throws ElectionException  {
     // check that voter hasn't already voted
     Voter v = getVoter(voter);
     if (v.voted) {
       throw new AlreadyVotedException(voter);
     }
     // check that party affiliations are the same
     Candidate c = getCandidate(candidate);
     if (!sameParty(c, v)) {
       throw new WrongPartyException(voter, c.party);
     }
   }
   
   /** List only the candidates for the voters own party **/
   void list(String voter) throws ElectionException {
      Enumeration it = candidates.elements();
      Voter v = getVoter(voter);
      log("List for " + voter);
      int counter = 0;
      while(it.hasMoreElements()) {
	 Candidate c = (Candidate) it.nextElement();
	 if (sameParty(c, v)) {
	    log("  " + ++counter + c.name);
	 }
      }
   }
};

/** 
 ** Exceptions for various unexpected situations
 ** This is obviously overkill, but it illustrates the use of an exception hierarchy
 **/
abstract class ElectionException extends Exception {
  abstract String getLogMessage();
};

/** AlreadyVotedException indicates that a voter is attempting to vote
 ** more than once in an election
 **/
class AlreadyVotedException extends ElectionException {
  String voter;
  AlreadyVotedException(String voter) {this.voter = voter;}
  String getLogMessage() {return voter + " already voted";} 
};

/** WrongPartyException indicates that a voter is voting for a candidate
 ** in another party in a primary election
 **/
	class WrongPartyException extends ElectionException 
	{
			String voter;
			String party;
			
			WrongPartyException(String voter, String party) 
			{
				this.voter = voter;
				this.party = party;
			}
				String getLogMessage() 
			{
					return voter + " cannot vote for a " + party;
			}
	};

/** NotRegisteredException indicates that the specified name is not
 ** that of any registered voter 
 **/
class NotRegisteredException extends ElectionException {
  String name;
  NotRegisteredException(String name) {this.name = name;}
  String getLogMessage() {return name + " is not registered";}
};

/** NotACandidateException indicates that a request has been made
 ** to vote for someone who is not recognized as a registered candidate
 **/
class NotACandidateException extends ElectionException {
  String name;
  NotACandidateException(String name) {this.name = name;}
  String getLogMessage() {return name + " is not a candidate";}
};