package com.quseit.service;

/**
 * 创建一个下载信息的实体类
 */
public class DownloadInfo {
	private int threadId;// 下载器id
	private long startPos;// 开始点
	private long endPos;// 结束点
	private long compeleteSize;// 完成度
	private String url;// 下载器网络标识
	private String path;// 下载器网络标识
	private String orgLink;
	private int quality;
	private int stat;
	private String title;
	private String artist;
	private String album;
	private int service_stat;
	private String service_json;

	public DownloadInfo(int threadId, long startPos, long endPos,
			long compeleteSize, String url, String path, String orgLink,
			int quality, int stat, String title, String artist, String album) {
		this.threadId = threadId;
		this.startPos = startPos;
		this.endPos = endPos;
		this.compeleteSize = compeleteSize;
		this.url = url;
		this.path = path;
		this.orgLink = orgLink;
		this.quality = quality;
		this.stat = stat;
		this.title = title;
		this.artist = artist;
		this.album = album;

	}
	
	public DownloadInfo(int threadId, long startPos, long endPos,
		long compeleteSize, String url, String path, String orgLink,
		int quality, int stat, String title, String artist, String album,int service_stat) {
		this.threadId = threadId;
		this.startPos = startPos;
		this.endPos = endPos;
		this.compeleteSize = compeleteSize;
		this.url = url;
		this.path = path;
		this.orgLink = orgLink;
		this.quality = quality;
		this.stat = stat;
		this.title = title;
		this.artist = artist;
		this.album = album;
		this.service_stat=service_stat;
		
	}
	
	public DownloadInfo(int threadId, long startPos, long endPos,
			long compeleteSize, String url, String path, String orgLink,
			int quality, int stat, String title, String artist, String album,int service_stat,String service_json) {
		this.threadId = threadId;
		this.startPos = startPos;
		this.endPos = endPos;
		this.compeleteSize = compeleteSize;
		this.url = url;
		this.path = path;
		this.orgLink = orgLink;
		this.quality = quality;
		this.stat = stat;
		this.title = title;
		this.artist = artist;
		this.album = album;
		this.service_stat=service_stat;
		this.service_json=service_json;
		
	}

	public DownloadInfo(int service_stat,String service_json,int stat){
		this.service_stat=service_stat;
		this.service_json=service_json;
		this.stat = stat;
	}
	
	public DownloadInfo() {
	}

	public String getTitle() {
		return title;
	}

	public String getArtist() {
		return artist;
	}

	public String getAlbum() {
		return album;
	}

	public int getStat() {
		return stat;
	}

	public int getQuality() {
		return quality;
	}

	public void setQuality(int quality) {
		this.quality = quality;
	}

	public String getOrgLink() {
		return orgLink;
	}

	public void setOrgLink(String orgLink) {
		this.orgLink = orgLink;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getThreadId() {
		return threadId;
	}

	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}

	public long getStartPos() {
		return startPos;
	}

	public void setStartPos(int startPos) {
		this.startPos = startPos;
	}

	public long getEndPos() {
		return endPos;
	}

	public void setEndPos(int endPos) {
		this.endPos = endPos;
	}

	public long getCompeleteSize() {
		return compeleteSize;
	}

	public void setCompeleteSize(int compeleteSize) {
		this.compeleteSize = compeleteSize;
	}
    public int getService_stat() {
		return service_stat;
	}
	public void setService_stat(int service_stat) {
		this.service_stat = service_stat;
	}
	public String getService_json() {
		return service_json;
	}
	public void setService_json(String service_json) {
		this.service_json = service_json;
	}
	@Override
	public String toString() {
		return "DownloadInfo [threadId=" + threadId + ", startPos=" + startPos
				+ ", endPos=" + endPos + ", compeleteSize=" + compeleteSize
				+ ", title=" + title + ",artist=" + artist + ",album=" + album
				+ ",url=" + url + ", stat=" + stat + ",orglink=" + orgLink
				+ "]";
	}
}
