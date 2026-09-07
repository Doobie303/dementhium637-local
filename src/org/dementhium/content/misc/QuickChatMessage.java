package org.dementhium.content.misc;

import org.dementhium.model.mask.ChatMessage;

public class QuickChatMessage extends ChatMessage {

	private int fileId;
	
	public QuickChatMessage(int fileId, byte[] data) {
		super(0x8000, data == null ? 0 : data.length, data == null ? null : new String(data)); //???
		this.fileId = fileId;
	}
	
	public QuickChatMessage(int fileId, String message) {
		super(0x8000, message.length(), new String(message)); //???
		this.fileId = fileId;
	}

	public int getFileId() {
		return fileId;
	}

	public void setFileId(int fileId) {
		this.fileId = fileId;
	}


}

