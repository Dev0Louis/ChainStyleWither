package dev.louis.chainstylewither.config;

public interface Config {
    Config DEFAULT = new DefaultConfig();

    boolean isEnableBounceBlueWitherSkull();

    boolean isEnableMoreInertialBlueWitherSkull();

    boolean isEnableShootMoreBlueWitherSkull();

    boolean isEnableSpinAndWhiteSummon();

    boolean isEnableExplodeByHalfHealth();

    boolean isEnableExplodeByDie();

    boolean isEnableChargeAttack();

    boolean isEnableALotHealth();

    boolean isEnableMaintainWeakenedState();
}
