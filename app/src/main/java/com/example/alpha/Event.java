package com.example.alpha;

public class Event {
    private String eventTitle;
    private String inventor;
    private myDate date;

    public Event(){ }
    public Event (String eventTitle, String inventor, myDate date){
        this.eventTitle = eventTitle;
        this.inventor = inventor;
        this.date = date;
    }

}
