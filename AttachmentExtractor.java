package com.poc.AttachmentExtractor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;
import javax.mail.util.ByteArrayDataSource;


public class AttachmentExtractorDemo {
	private final MimeMessage mimeMessage;
	private String plainContent;
	private String htmlContent;
	private final List<DataSource> attachmentList;
	private final Map<String, DataSource> cidMap;
	private boolean isMultiPart;

	
	public AttachmentExtractorDemo(final MimeMessage message) {
		attachmentList = new ArrayList<DataSource>();
		cidMap = new HashMap<String, DataSource>();
		this.mimeMessage = message;
		this.isMultiPart = false;
	}

	public AttachmentExtractorDemo parse() throws Exception {
		this.parse(null, mimeMessage);
		return this;
	}

	public List<javax.mail.Address> getTo() throws Exception {
		final javax.mail.Address[] recipients = this.mimeMessage.getRecipients(Message.RecipientType.TO);
		return recipients != null ? Arrays.asList(recipients) : new ArrayList<javax.mail.Address>();
	}

	public List<javax.mail.Address> getCc() throws Exception {
		final javax.mail.Address[] recipients = this.mimeMessage.getRecipients(Message.RecipientType.CC);
		return recipients != null ? Arrays.asList(recipients) : new ArrayList<javax.mail.Address>();
	}

	public List<javax.mail.Address> getBcc() throws Exception {
		final javax.mail.Address[] recipients = this.mimeMessage.getRecipients(Message.RecipientType.BCC);
		return recipients != null ? Arrays.asList(recipients) : new ArrayList<javax.mail.Address>();
	}

	public String getFrom() throws Exception {
		final javax.mail.Address[] addresses = this.mimeMessage.getFrom();
		if (addresses == null || addresses.length == 0) {
			return null;
		}
		return ((InternetAddress) addresses[0]).getAddress();
	}

	public String getReplyTo() throws Exception {
		final javax.mail.Address[] addresses = this.mimeMessage.getReplyTo();
		if (addresses == null || addresses.length == 0) {
			return null;
		}
		return ((InternetAddress) addresses[0]).getAddress();
	}


	protected void parse(final Multipart parent, final MimePart part) throws MessagingException, IOException {
		if (isMimeType(part, "text/plain") && plainContent == null
				&& !Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
			plainContent = (String) part.getContent();
		} else {
			if (isMimeType(part, "text/html") && htmlContent == null
					&& !Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
				htmlContent = (String) part.getContent();
			} else {
				if (isMimeType(part, "multipart/*")) {
					this.isMultiPart = true;
					final Multipart mp = (Multipart) part.getContent();
					final int count = mp.getCount();

					// iterate over all MimeBodyPart

					for (int i = 0; i < count; i++) {
						parse(mp, (MimeBodyPart) mp.getBodyPart(i));
					}
				} else {
					final String cid = stripContentId(part.getContentID());
					final DataSource ds = createDataSource(parent, part);
					if (cid != null) {
						this.cidMap.put(cid, ds);
					}
					this.attachmentList.add(ds);
				}
			}
		}
	}

	private String stripContentId(final String contentId) {
		if (contentId == null) {
			return null;
		}
		return contentId.trim().replaceAll("[\\<\\>]", "");
	}


	private boolean isMimeType(final MimePart part, final String mimeType) throws MessagingException, IOException {

		try {
			final ContentType ct = new ContentType(part.getDataHandler().getContentType());
			return ct.match(mimeType);
		} catch (final ParseException ex) {
			return part.getContentType().equalsIgnoreCase(mimeType);
		}
	}

	protected DataSource createDataSource(final Multipart parent, final MimePart part)
			throws MessagingException, IOException {
		final DataHandler dataHandler = part.getDataHandler();
		final DataSource dataSource = dataHandler.getDataSource();
		final String contentType = getBaseMimeType(dataSource.getContentType());
		final byte[] content = this.getContent(dataSource.getInputStream());
		final ByteArrayDataSource result = new ByteArrayDataSource(content, contentType);
		final String dataSourceName = getDataSourceName(part, dataSource);

		result.setName(dataSourceName);
		return result;
	}

	public MimeMessage getMimeMessage() {
		return mimeMessage;
	}

	public boolean isMultipart() {
		return isMultiPart;
	}

	public String getPlainContent() {
		return plainContent;
	}

	/** @return Returns the attachmentList. */
	public List<DataSource> getAttachmentList() {
		return attachmentList;
	}

	public Collection<String> getContentIds() {
		return Collections.unmodifiableSet(cidMap.keySet());
	}

	/** @return Returns the htmlContent if any */
	public String getHtmlContent() {
		return htmlContent;
	}

	/** @return true if a plain content is available */
	public boolean hasPlainContent() {
		return this.plainContent != null;
	}

	/** @return true if HTML content is available */
	public boolean hasHtmlContent() {
		return this.htmlContent != null;
	}

	/** @return true if attachments are available */
	public boolean hasAttachments() {
		return this.attachmentList.size() > 0;
	}

	public DataSource findAttachmentByName(final String name) {
		DataSource dataSource;

		for (int i = 0; i < getAttachmentList().size(); i++) {
			dataSource = getAttachmentList().get(i);
			if (name.equalsIgnoreCase(dataSource.getName())) {
				return dataSource;
			}
		}

		return null;
	}

	public DataSource findAttachmentByCid(final String cid) {
		final DataSource dataSource = cidMap.get(cid);
		return dataSource;
	}

	protected String getDataSourceName(final Part part, final DataSource dataSource)
			throws MessagingException, UnsupportedEncodingException {
		String result = dataSource.getName();

		if (result == null || result.length() == 0) {
			result = part.getFileName();
		}

		if (result != null && result.length() > 0) {
			result = MimeUtility.decodeText(result);
		} else {
			result = null;
		}

		return result;
	}

	private byte[] getContent(final InputStream is) throws IOException {
		int ch;
		byte[] result;

		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final BufferedInputStream isReader = new BufferedInputStream(is);
		final BufferedOutputStream osWriter = new BufferedOutputStream(os);

		while ((ch = isReader.read()) != -1) {
			osWriter.write(ch);
		}

		osWriter.flush();
		result = os.toByteArray();
		osWriter.close();

		return result;
	}

	private String getBaseMimeType(final String fullMimeType) {
		final int pos = fullMimeType.indexOf(';');
		if (pos >= 0) {
			return fullMimeType.substring(0, pos);
		}
		return fullMimeType;
	}
	
	public static void main(String[] args) throws Exception {
		String path = "C:\\Users\\ASUS\\Downloads\\original_msg2.eml";
		InputStream messageInputStream = new FileInputStream(new File(path));
		MimeMessage message = new MimeMessage(null, messageInputStream);
		AttachmentExtractorDemo messageParser = new AttachmentExtractorDemo(message);
		messageParser.parse();
		for(DataSource source : messageParser.getAttachmentList()) {
			File targetFile = new File(source.getName());
		    Files.copy(source.getInputStream(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		
	}
}
