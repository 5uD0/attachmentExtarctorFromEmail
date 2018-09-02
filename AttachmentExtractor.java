import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
public class AttachmentExtractor {
	
	/*
	 * Parse .eml file and extract and remove attachments from the .eml file
	 * Author---Sudip Banerjee 
	 * 
	 * */

	public static void main(String[] args) {
		String path = "File Path";
		FileOutputStream outStream = null;
		try {
			File emlFile = new File("test.eml");
			outStream = new FileOutputStream(emlFile);
			String data = new String(Files.readAllBytes(Paths.get(path+"\\test.eml")));
			String[] mailData = data.split("</html>");
			outStream.write((mailData[0]+"</html>").getBytes());
			String[] attachments = mailData[1].split("Content-Disposition: attachment;");
			for(int i=1;i<=attachments.length-1;i++) {
				String[] attachmentData = attachments[i].split("\r\n\r\n");
				String fileName = attachmentData[0].split("=")[1].replaceAll("^\"|\"$", "");
				String[] encodedAttachment = attachmentData[1].split("--");
				String encodedAttachmentData = encodedAttachment[0];
				encodedAttachmentData.replace("\n", "");
				encodedAttachmentData.replace("\r", "");
				encodedAttachmentData.trim();
				byte[] valueDecoded = Base64.getMimeDecoder().decode(encodedAttachmentData); 
				File outputFile = new File(fileName);
				outStream = new FileOutputStream(outputFile);
				outStream.write(valueDecoded);
				outStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
