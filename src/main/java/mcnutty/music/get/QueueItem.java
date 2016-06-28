//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

class QueueItem {

    String disk_name;
    String real_name;
    String ip;

    QueueItem(String disk_name, String real_name, String ip) {
        this.disk_name = disk_name;
        this.real_name = real_name;
        this.ip = ip;
    }

    QueueItem() {
        this.disk_name = null;
        this.real_name = null;
        this.ip = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueueItem queueItem = (QueueItem) o;

        if (disk_name != null ? !disk_name.equals(queueItem.disk_name) : queueItem.disk_name != null) return false;
        if (real_name != null ? !real_name.equals(queueItem.real_name) : queueItem.real_name != null) return false;
        return ip != null ? ip.equals(queueItem.ip) : queueItem.ip == null;

    }

    @Override
    public int hashCode() {
        int result = disk_name != null ? disk_name.hashCode() : 0;
        result = 31 * result + (real_name != null ? real_name.hashCode() : 0);
        result = 31 * result + (ip != null ? ip.hashCode() : 0);
        return result;
    }

}
