package dev.louis.chainstylewither.config;

public class DefaultConfig implements Config {
    @Override
    public boolean isEnableBounceBlueWitherSkull() {
        return true;
    }

    @Override
    public boolean isEnableMoreInertialBlueWitherSkull() {
        return true;
    }

    @Override
    public boolean isEnableShootMoreBlueWitherSkull() {
        return true;
    }

    @Override
    public boolean isEnableSpinAndWhiteSummon() {
        return true;
    }

    @Override
    public boolean isEnableExplodeByHalfHealth() {
        return true;
    }

    @Override
    public boolean isEnableExplodeByDie() {
        return true;
    }

    @Override
    public boolean isEnableChargeAttack() {
        return true;
    }

    @Override
    public boolean isEnableALotHealth() {
        return true;
    }

    @Override
    public boolean isEnableMaintainWeakenedState() {
        return true;
    }
}
