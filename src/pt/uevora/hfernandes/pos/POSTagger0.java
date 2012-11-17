package pt.uevora.hfernandes.pos;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import pt.uevora.hfernandes.objects.Comment;
import pt.uevora.hfernandes.objects.Corpus;
import pt.uevora.hfernandes.objects.Sentence;
import pt.uevora.hfernandes.parsers.LXPOSTagParser;


/**
 * LXPOSTagger
 * 
 * @author hfernandes
 *
 */
public class POSTagger0 {

	/**
	 * This reads a plain file, was messing paragraphs and end phrases making it impossible to match ID's
	 * later on... :(
	 * @param input
	 * @param outputPlain
	 * @param outputXMl
	 * @throws Exception
	 */
	@Deprecated
	private static void LXPOSTagger(String input, String outputPlain, String outputXMl) throws Exception{
		DefaultExecutor executor = new DefaultExecutor();

		File run = new File("bin/run-pos.sh");
		File inputFile = new File(input);
		File tagger = new File("bin/LXPOSTagger/run-Tagger.sh");
		
		CommandLine cmdLine = new CommandLine(run.getAbsoluteFile());
		cmdLine.addArgument(inputFile.getAbsolutePath());
		cmdLine.addArgument(tagger.getAbsolutePath());
		
		OutputStream fout = new FileOutputStream(outputPlain);
		PumpStreamHandler streamHandler = new PumpStreamHandler(fout);
		executor.setStreamHandler(streamHandler);
		
		System.out.println("Tagging "+input+"...");
		executor.execute(cmdLine);
		System.out.println("Done. Generated "+ outputPlain);
		
		System.out.println("Generating XML from "+outputPlain+"...");
		File resultPlain = new File(outputPlain);
		LXPOSTagParser parser = new LXPOSTagParser();
        
		Corpus corpus = parser.parse(resultPlain);
	        
        Serializer serializer = new Persister();
		File resultXML = new File(outputXMl);

		serializer.write(corpus, resultXML);
		System.out.println("Done. Generated "+ outputXMl);
	}

	/**
	 * Start from a xml file!
	 * @param input
	 * @param outputPlain
	 * @param outputXMl
	 * @throws Exception
	 */
	private static void LXPOSTagger2(String input, String output) throws Exception{
		// read input file
		Serializer serializer2 = new Persister();
		File inputFile = new File(input);
		Corpus corpus = serializer2.read(Corpus.class, inputFile);
		
		DefaultExecutor executor = new DefaultExecutor();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		CommandLine cmdLine;
		File run = new File("bin/lxpos.sh");
		File tagger = new File("bin/LXPOSTagger/run-Tagger.sh");
		File tmp = new File("output/tmp.txt");
		FileOutputStream tmpOS;
		PumpStreamHandler streamHandler = new PumpStreamHandler(baos);
		executor.setStreamHandler(streamHandler);
		tmp.deleteOnExit();
		
		System.out.println("Tagging "+input+"...");
		
		String taggedValue;
		for (Comment comment : corpus.getComments()) {
		   for (Sentence sentence : comment.getSentences()) {
			   tmpOS = new FileOutputStream(tmp);
			   tmpOS.write(sentence.getValue().getBytes());
			   			   
			   cmdLine = new CommandLine(run.getAbsoluteFile());
			   cmdLine.addArgument(tmp.getAbsolutePath());
			   cmdLine.addArgument(tagger.getAbsolutePath());
			   executor.execute(cmdLine);
			   
			   taggedValue = StringUtils.trim(baos.toString());

			   sentence.setPosValue(taggedValue);
			   LXPOSTagParser.processSentence(sentence); // process the sentence tokens
			   baos.reset();
			   tmpOS.close();
			   
		   }
		   
		}
		
		
		Serializer serializer = new Persister();
		File result = new File(output);

		serializer.write(corpus, result);
		System.out.println("Done. Generated " + output);
	}
	
	public static void main(String args[]) throws Exception{
		String INPUT = "resources/SentiCorpus-PT_clean5.xml";
		String OUTPUT = "output/SentiCorpus-PT_pos_0.xml";
		
		LXPOSTagger2(INPUT, OUTPUT);
	}
	
}