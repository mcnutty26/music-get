package mcnutty.music.get;

public class QueueItem {
	
	public String disk_name;
	public String real_name;
	public String ip;
	
	public QueueItem(String disk_name, String real_name, String ip) {
		this.disk_name = disk_name;
		this.real_name = real_name;
		this.ip = ip;
	}
	
	public QueueItem() {
		this.disk_name = null;
		this.real_name = null;
		this.ip = null;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
	        return false;
	    }
	    if (getClass() != obj.getClass()) {
	        return false;
	    }
	    final QueueItem item = (QueueItem) obj;
	    if (disk_name != item.disk_name) {
	    	return false;
	    }
	    if (real_name != item.real_name) {
	    	return false;
	    }
	    if (ip != item.ip) {
	    	return false;
	    }
	    return true;
	}

}
