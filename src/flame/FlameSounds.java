package flame;

import arc.audio.*;
import mindustry.*;

public class FlameSounds{
    public static Sound apathyDeath, apathyDeathCry, apathyBleed, laserCharge, bigCharge, laserSmall, laserBig, aoeShoot, transform, largeTransform, clang, idle,
        empathyParry, empathyBigLaser, empathyCharge, empathySquareCharge, empathySquareShoot, empathyHologramActive, empathySmallEnd,
        empathyRendSlash, empathyRendSwing, empathyDash, empathyDash2, empathyRico, empathyBlackHole, empathyShine, empathyTeleport,
        portalOrder, portalChaos, screams;

    static void load(){
        apathyBleed = Vars.tree.loadSound("apathy-bleed");
        apathyDeath = Vars.tree.loadSound("apathy-death");
        apathyDeathCry = Vars.tree.loadSound("apathy-death-cry");

        laserCharge = Vars.tree.loadSound("apathy-laser-charge");
        bigCharge = Vars.tree.loadSound("apathy-big-charge");
        laserSmall = Vars.tree.loadSound("apathy-laser-small");
        laserBig = Vars.tree.loadSound("apathy-laser-large");
        aoeShoot = Vars.tree.loadSound("apathy-aoe-shoot");
        transform = Vars.tree.loadSound("apathy-transform");
        largeTransform = Vars.tree.loadSound("apathy-transform-large");
        clang = Vars.tree.loadSound("apathy-base");
        idle = Vars.tree.loadSound("apathy-idle");

        empathyParry = Vars.tree.loadSound("empathy-parry");
        empathyBigLaser = Vars.tree.loadSound("empathy-big-laser");
        empathyCharge = Vars.tree.loadSound("empathy-charge");
        empathySquareCharge = Vars.tree.loadSound("empathy-square-charge");
        empathySquareShoot = Vars.tree.loadSound("empathy-square-shoot");
        empathyHologramActive = Vars.tree.loadSound("empathy-hologram-active");
        empathySmallEnd = Vars.tree.loadSound("empathy-end-small");

        empathyRendSlash = Vars.tree.loadSound("empathy-rend-slash");
        empathyRendSwing = Vars.tree.loadSound("empathy-rend-swing");
        empathyDash = Vars.tree.loadSound("empathy-dash");
        empathyDash2 = Vars.tree.loadSound("empathy-dash-altb");
        empathyRico = Vars.tree.loadSound("empathy-ricochet");
        empathyBlackHole = Vars.tree.loadSound("empathy-blackhole");
        empathyShine = Vars.tree.loadSound("empathy-shine");
        empathyTeleport = Vars.tree.loadSound("empathy-teleport");

        portalOrder = Vars.tree.loadSound("portal-order");
        portalChaos = Vars.tree.loadSound("portal-chaos");

        screams = Vars.tree.loadSound("screams-of-the-damned");
    }
}
