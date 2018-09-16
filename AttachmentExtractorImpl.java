package com.poc.AttachmentExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class AttachmentExtractor {

	public static void main(String[] args) throws Exception {
		String path = "C:\\Users\\ASUS\\Downloads\\original_msg2.eml";
		InputStream messageInputStream = new FileInputStream(new File(path));
		MimeMessage message = new MimeMessage(null, messageInputStream);
		AttachmentExtractorDemo messageParser = new AttachmentExtractorDemo(message);
		messageParser.setNewMessage(message);
		messageParser.parse();
		for(DataSource source : messageParser.getAttachmentList()) {
			File targetFile = new File(source.getName());
		    Files.copy(source.getInputStream(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		    InputStream attachmentStream = source.getInputStream();
		    byte[] bytes = new byte[attachmentStream.available()];
		    attachmentStream.read(bytes);
		    byte[] valueDecoded = Base64.getDecoder().decode(bytes);
		    for(byte b:valueDecoded) {
		    	System.out.print(b);
		    }
		    Files.write(Paths.get("C:\\Users\\ASUS\\eclipse-workspace\\codegladiator2018\\ExtractEmail\\sudip.dat"), bytes);
		}
		 MimeMultipart obj = (MimeMultipart) message.getContent();
		 MimeMultipart obj1 = new MimeMultipart();
		 System.out.println(obj.getCount());
		 System.out.println(obj.getBodyPart(obj.getCount()-2).getDisposition());
		 for(int i=0;i<obj.getCount();i++) {
			 if(!("attachment".equals(obj.getBodyPart(i).getDisposition()))) {
				 obj1.addBodyPart(obj.getBodyPart(i));
			 }
		 }
		 message.setContent(obj1);
		 message.saveChanges();
		 System.out.println(new AttachmentExtractorDemo(message).getAttachmentList().size());
		 File eml = new File("C:\\Users\\ASUS\\eclipse-workspace\\codegladiator2018\\ExtractEmail\\originaleml.eml");
		 OutputStream fos = new FileOutputStream(eml);
		 message.writeTo(fos);
		 //fos.write(emlData);
		 fos.close();
	}
	
}
