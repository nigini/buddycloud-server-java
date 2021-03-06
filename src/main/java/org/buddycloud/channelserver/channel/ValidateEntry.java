package org.buddycloud.channelserver.channel;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

public class ValidateEntry {

	private static Logger LOGGER = Logger.getLogger(ValidateEntry.class);
	
	private Element entry;
   
	private String errorMsg = "";
	private String inReplyTo;
	private Element meta;
	private Element media;
	
	Map<String, String>params = new HashMap<String, String>();
	
	private Element geoloc;

	public ValidateEntry(Element entry) {
		this.entry = entry;
	}
	
	public String getErrorMsg() {
		return this.errorMsg;
	}
	
	/**
	 * This is a big hackety-hack.
	 */
	public boolean isValid()
	{
		if (this.entry == null) {
			this.errorMsg = "Dude, the entry is missing.";
			return false;
		}
		
		/*
            <entry xmlns="http://www.w3.org/2005/Atom">
    		   <id>1</id>
    		   <title>Status update.</title>
    		   <content>This is my new status!</content>
    		   <updated>2011-06-18T09:08:57Z</updated>
    		   <author>
    		      <name>tuomas@koski.com</name>
    		   </author>
            </entry>
		 */
		
		Element id = this.entry.element("id");
		if(id == null || id.getText().isEmpty()) {
			LOGGER.debug("ID of the entry was missing. We add a default one to it: 1");
			this.entry.addElement("id").setText("1");
		}

		Element title = this.entry.element("title");
		if(title == null) {
			LOGGER.debug("Title of the entry was missing. We add a default one to it: 'Post'.");
			title = this.entry.addElement("title");
			title.setText("Post");
		}
		this.params.put("title", title.getText());
		
		Element content = this.entry.element("content");
		if(content == null) {
			this.errorMsg = "Mandatory element content is missing.";
			return false;
		}
		this.params.put("content", content.getText());
		
		Element updated = this.entry.element("updated");
		if(updated == null) {
			
			String updateTime = Conf.formatDate(new Date());
			
			LOGGER.debug("Update of the entry was missing. We add a default one to it: '" + updateTime + "'.");
			this.entry.addElement("updated").setText(updateTime);
		}
		
		this.geoloc = this.entry.element("geoloc");
		
		Element reply = this.entry.element("in-reply-to");
		if (null != reply) {
			inReplyTo = reply.attributeValue("ref").toString();
			if (true == inReplyTo.matches(",+")) {
				String[] tokens = inReplyTo.split(",");
				inReplyTo       = tokens[2];
			}
		}
		Element meta = this.entry.element("meta");
		if (null != meta) {
			this.meta = meta;
		}
		
		Element media = this.entry.element("media");
		if (null != media) {
			this.media = media;
		}
		
		return true;
	}
	
	public Element createBcCompatible(String bareJID, String channelServerJID, String node) {
		
		Element entry = new DOMElement("entry", new org.dom4j.Namespace("", "http://www.w3.org/2005/Atom"));
		entry.add(new org.dom4j.Namespace("activity", "http://activitystrea.ms/spec/1.0/"));
		
		/**
		 * We are going to build this now.
		 * 
		   <entry xmlns="http://www.w3.org/2005/Atom" xmlns:activity="http://activitystrea.ms/spec/1.0/">
             <id>tag:channels.koski.com,/user/tuomas@koski.com/status,1dfs5-6s8e-zerf-4494</id>
             <title>Status update.</title>
             <content>This is my new status!</content>
             <updated>2011-08-03T09:58:57Z</updated>
             <author>
                <name>tuomas@koski.com</name>
                <uri>acct:tuomas@koski.com</uri>
                <activity:object-type>person</activity:object-type>
             </author>
             <activity:verb>post</activity:verb>
             <activity:object>
                <activity:object-type>note</activity:object-type>
             </activity:object>
             <meta>
                ... additional meta ...
             </meta>
             <media>
                <item id="mediaId" channel="channel@example.com"/>
             </media>
          </entry>
		 */
		String id       = UUID.randomUUID().toString();
		String postType = "note";
		
		entry.addElement("id")
		     .setText("tag:" + channelServerJID + "," + node + "," + id);
		
		entry.addElement("title")
			 .setText(this.params.get("title"));
		
		entry.addElement("content")
		     .setText(this.params.get("content"));
		
		String leData = Conf.formatDate(new Date());
		
		entry.addElement("published")
		     .setText(leData);
		
		entry.addElement("updated")
		     .setText(leData);
		
		Element author = entry.addElement("author");
		
		author.addElement("name")
			  .setText(bareJID);
		
		author.addElement("uri")
		      .setText("acct:" + bareJID);
	
		author.addElement("activity:object-type")
	          .setText("person");

		if (this.geoloc != null) {
			entry.add(this.geoloc.createCopy());
		}
		if (this.inReplyTo != null) {
			Element reply = entry.addElement("in-reply-to");
			reply.addNamespace("",  "http://purl.org/syndication/thread/1.0");
			reply.addAttribute("ref", inReplyTo);
			postType = "comment";
		}
		
		this.geoloc = this.entry.element("geoloc");
		
		entry.addElement("activity:verb")
	         .setText("post");
		
		Element activity_object = entry.addElement("activity:object");
		activity_object.addElement("activity:object-type")
		               .setText(postType);
		
		if (null != meta) {
			entry.add(meta.createCopy());
		}
		
		if (null != media) {
			entry.add(media.createCopy());
		}
		
		return entry;
	}
	
}
