package flame.bullets;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.graphics.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;

public class EmpathyTrackerBulletType extends BulletType{
    public EmpathyTrackerBulletType(){
        super(4f, 200f);
        lifetime = 4f * 60f;
        pierce = pierceArmor = pierceBuilding = true;
        trailLength = 10;
        trailWidth = 3f;
        trailColor = FlamePal.empathy;
        drag = 0.005f;
        collides = collidesTiles = false;
        hitEffect = Fx.hitBulletColor;
        hitColor = FlamePal.empathy;
        keepVelocity = false;
    }

    @Override
    public void update(Bullet b){
        super.update(b);
        Rect r = Tmp.r3;
        b.hitbox(r);
        float dam = 170f;
        
        if(b.timer(0, 15)){
            b.collided.clear();
        }

        Groups.unit.intersect(r.x, r.y, r.width, r.height, u -> {
            if(u.team != b.team && !b.collided.contains(u.id)){
                hit(b, b.x, b.y);

                boolean lethal = u.maxHealth < dam * 3f;
                float tdam = Math.max(dam, u.maxHealth / 1200f);
                EmpathyDamage.damageUnit(u, tdam, lethal, null);
                b.collided.add(u.id);
            }
        });
        Building build = Vars.world.buildWorld(b.x, b.y);
        if(build != null && build.team != b.team && !b.collided.contains(build.id)){
            hit(b, b.x, b.y);

            boolean lethal = build.maxHealth < dam * 3f;
            float tdam = Math.max(dam, build.maxHealth / 1200f);
            EmpathyDamage.damageBuilding(build, tdam, lethal, null);
            b.collided.add(build.id);
        }
    }

    @Override
    public void updateHoming(Bullet b){
        if(b.data instanceof Healthc target){
            float fin = Mathf.clamp((b.time - b.fdata) / 20f);
            float rotSlope = Mathf.slope(Mathf.clamp(((b.time - b.fdata) / 40f) % 4));
            Vec2 move = Tmp.v1.set(target.x(), target.y()).sub(b.x, b.y).scl(1f / 200f).add(b.vel.x / 4f, b.vel.y / 4f).limit(3f).scl(fin);
            b.vel.add(move).limit(15f);
            if(rotSlope > 0){
                b.vel.setAngle(Mathf.slerpDelta(b.vel.angle(), b.angleTo(target), 0.3f * rotSlope));
            }
            if(!target.isValid()){
                b.data = null;
            }
        }
    }

    @Override
    public void draw(Bullet b){
        drawTrail(b);
        Draw.color(trailColor);
        GraphicUtils.diamond(b.x, b.y, trailWidth, trailWidth * 3, b.rotation());
        Draw.color();
    }
}
