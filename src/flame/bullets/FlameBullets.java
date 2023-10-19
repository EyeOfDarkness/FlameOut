package flame.bullets;

import arc.graphics.*;
import mindustry.entities.bullet.*;

public class FlameBullets{
    public static BulletType smallLaser, sweep, aoe, bigLaser, sentryLaser, pin, tracker, sword, test;

    public static void load(){
        smallLaser = new ApathySmallLaserBulletType();
        sweep = new ApathySweepLaserBulletType();
        aoe = new ApathyAoEBulletType();
        bigLaser = new ApathyBigLaserBulletType();

        sentryLaser = new LaserBulletType(900f){{
            length = 1400f;
            colors = new Color[]{Color.white};
            width = 5f;
        }};

        pin = new EmpathyPinBulletType();
        tracker = new EmpathyTrackerBulletType();
        sword = new EmpathySwordBulletType();

        test = new TestBulletType();
    }
}
