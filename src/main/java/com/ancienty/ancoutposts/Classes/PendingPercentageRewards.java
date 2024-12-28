package com.ancienty.ancoutposts.Classes;

public class PendingPercentageRewards {

    private int totalIncreasedDamage;
    private int totalReducedDamage;

    public PendingPercentageRewards(int totalIncreasedDamage, int totalReducedDamage) {
        this.totalIncreasedDamage = totalIncreasedDamage;
        this.totalReducedDamage = totalReducedDamage;
    }

    public int getTotalIncreasedDamage() {
        return totalIncreasedDamage;
    }

    public int getTotalReducedDamage() {
        return totalReducedDamage;
    }
}
