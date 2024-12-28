package com.ancienty.ancoutposts.Classes;

public class Reward {
    private String type;
    private Integer percent;
    private String display;
    private String command;
    private boolean isRecurring;
    private Integer interval; // Interval in minutes for recurring rewards

    // Constructor for one-time rewards
    public Reward(String type, Integer percent, String display, String command) {
        this.type = type;
        this.percent = percent;
        this.display = display;
        this.command = command;
        this.isRecurring = false;
        this.interval = null;
    }

    // Constructor for rewards with isRecurring and interval
    public Reward(String type, Integer percent, String display, String command, boolean isRecurring, Integer interval) {
        this.type = type;
        this.percent = percent;
        this.display = display;
        this.command = command;
        this.isRecurring = isRecurring;
        this.interval = interval;
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public Integer getPercent() {
        return percent;
    }

    public String getDisplay() {
        return display;
    }

    public String getCommand() {
        return command;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPercent(Integer percent) {
        this.percent = percent;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }
}
