package flame.bullets;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import flame.unit.empathy.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;

public class TestBulletType extends BulletType{
    static boolean disintegrate = true;

    //TODO remove
    TestBulletType(){
        collides = collidesGround = false;
        instantDisappear = true;
        lifetime = 1f;
    }

    @Override
    public void init(Bullet b){
        Vec2 v = Tmp.v1.trns(b.rotation(), 900f).add(b.x, b.y);
        float vx = v.x, vy = v.y;
        float bx = b.x, by = b.y;
        Utils.hitLaser(b.team, 50f, b.x, b.y, v.x, v.y, null, h -> false, (h, x, y) -> {
            if(h instanceof Unit u){
                EmpathyDamage.damageUnit(u, u.maxHealth + 1000f, true, () -> {
                    SpecialDeathEffects sd = SpecialDeathEffects.get(u.type);
                    if(disintegrate){
                        //sd.disintegrateUnit(u, bx, by, vx, vy, 100f);
                        Carve.generate(u.x, u.y, b.rotation(), (u.hitSize / 2f) / 1.5f, u::draw);
                    }else{
                        Tmp.v2.trns(Angles.angle(vx, vy, bx, by), u.hitSize / 2f).add(u);
                        sd.deathUnit(u, Tmp.v2.x, Tmp.v2.y, Angles.angle(bx, by, vx, vy));
                    }
                });
            }
        });
    }
}
