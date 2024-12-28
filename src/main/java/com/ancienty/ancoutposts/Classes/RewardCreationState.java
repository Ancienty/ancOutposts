package com.ancienty.ancoutposts.Classes;

public class RewardCreationState {
    private int step;
    private String outpostId;
    private String rewardType;
    private Integer percent;
    private String display;
    private String command;
    private boolean isRecurring;
    private Integer interval;

    // Constructor
    public RewardCreationState(String outpostId) {
        this.outpostId = outpostId;
        this.step = 1;
    }

    // Getters and setters
    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getOutpostId() {
        return outpostId;
    }

    public String getRewardType() {
        return rewardType;
    }

    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }

    public Integer getPercent() {
        return percent;
    }

    public void setPercent(Integer percent) {
        this.percent = percent;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }
}
