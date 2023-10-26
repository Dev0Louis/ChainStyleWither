package dev.louis.chainstylewither.entity;

public interface ChainStyleWitherBoss {
    int getWitherDeathTime();

    float getWitherDeathTime(float delta);

    int getBlockBreakingCooldown();

    void setBlockBreakingCooldown(int tick);

    int getChargeCoolDown();

    void setChargeCoolDown(int tick);

    int getClientCharge();

    void setClientCharge(int charge);

    float getClientCharge(float delta);
}
