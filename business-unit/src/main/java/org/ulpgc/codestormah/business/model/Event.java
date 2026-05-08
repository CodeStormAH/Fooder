package org.ulpgc.codestormah.business.model;

public class Event {
    private String ts;
    private String ss;
    private String topic;
    private Product payload;

    public Event() {}

    public String getTs() { return ts; }
    public String getSs() { return ss; }
    public String getTopic() { return topic; }
    public Product getPayload() { return payload; }
}
