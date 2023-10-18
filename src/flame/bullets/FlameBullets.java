package flame.bullets;

import mindustry.entities.bullet.*;

public class FlameBullets{
    public static BulletType smallLaser, sweep, aoe, bigLaser, pin, tracker, sword, test;

    public static void load(){
        smallLaser = new ApathySmallLaserBulletType();
        sweep = new ApathySweepLaserBulletType();
        aoe = new ApathyAoEBulletType();
        bigLaser = new ApathyBigLaserBulletType();

        pin = new EmpathyPinBulletType();
        tracker = new EmpathyTrackerBulletType();
        sword = new EmpathySwordBulletType();

        test = new TestBulletType();
    }
}
