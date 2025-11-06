package com.zevrant.services.pojo;

public class ProxmoxVolume {
    private String format;
    private long size;
    private String content;
    private long ctime;
    private String volid;
    private String volumeName;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public String getVolid() {
        return volid;
    }

    public void setVolid(String volid) {
        this.volid = volid;
    }

    String getVolumeName() {
        return volumeName
    }

    void setVolumeName(String volumeName) {
        this.volumeName = volumeName
    }
}
