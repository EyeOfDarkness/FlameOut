package flame.unit.empathy;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static arc.math.Interp.*;

public class ShineAttack extends AttackAI{
    float time = 0f;
    float collisionTime = 0f;
    int side = 1;

    ShineAttack(){
        super();
        addSoundConstant(FlameSounds.empathyShine);
    }

    @Override
    float weight(){
        return unit.nearestTarget > 290 || unit.useLethal() ? -1 : super.weight();
    }

    @Override
    boolean canTeleport(){
        return false;
    }

    @Override
    float effectiveDistance(){
        return 200f;
    }

    @Override
    void draw(){
        float duration = quickSwap ? 4f * 60f : 10f * 60f;
        float z = Draw.z();
        float dur = 1.5f * 60f;
        float out = Mathf.clamp(((duration) - time) / 120f);

        float m1 = (time % dur) / dur;
        float m2 = ((time - dur / 2) % dur) / dur;
        float f1 = pow2.apply(Utils.biasSlope(Mathf.clamp(m1), 0.2f));
        float f2 = pow2.apply(Utils.biasSlope(Mathf.clamp(m2), 0.2f));

        Draw.color(FlamePal.empathyAdd);
        Draw.blend(Blending.additive);
        Draw.z(Layer.flyingUnit - 1f);
        for(int i = 0; i < 8; i++){
            float ang = ((360f / 8) * i + 90) + time / 20 * side;
            drawLaser(f1, ang, out);
        }
        for(int i = 0; i < 8; i++){
            float ang = ((360f / 8) * i + (180f / 8) + 90) + time / 20 * side;
            drawLaser(f2, ang, out);
        }
        Draw.color();
        Draw.blend();
        Draw.z(z);
    }

    float laserWidth(float f){
        //Interp i = f < 0.25f ? circleOut : pow2;
        //Interp i = f < 0.25f ? circleOut : a -> pow2.apply(a * a);
        Interp i = f < 0.25f ? circleOut : a -> (a *= a) <= 0.5f ? Mathf.pow(a * 2, 1.5f) * 0.5f : (1 - Mathf.pow(1 - (a - 0.5f) * 2f, 1.5f)) * 0.5f + 0.5f;
        return i.apply(Utils.biasSlope(f, 0.25f));
    }

    void updateLaser(float width, float angle, float out){
        float len = (450f * out + width * 60);
        float wid = width * 20 * pow4Out.apply(out);
        float damage = 700f;

        Vec2 end = Tmp.v1.trns(angle, len);
        float ex = end.x + unit.x, ey = end.y + unit.y;
        Utils.hitLaser(unit.team, wid, unit.x, unit.y, ex, ey, h -> {
            Vec2 n = Intersector.nearestSegmentPoint(unit.x, unit.y, ex, ey, h.x(), h.y(), Tmp.v2);
            float lwid = laserWidth(Mathf.dst(unit.x, unit.y, n.x, n.y) / len) * wid;
            return n.within(h.x(), h.y(), lwid + (h instanceof Sized hh ? hh.hitSize() / 2 : 0f));
        }, h -> false, (h, x, y) -> {
            Fx.hitBulletColor.at(x, y, angle, FlamePal.empathy);

            boolean lethal = h.maxHealth() < damage * 2f;
            float tdam = Math.max(damage, h.maxHealth() / 300f);
            if(h instanceof Unit u){
                EmpathyDamage.damageUnit(u, tdam, lethal, null);
            }else if(h instanceof Building b){
                EmpathyDamage.damageBuilding(b, damage, lethal, null);
            }
        });
    }
    void drawLaser(float width, float angle, float out){
        //Lines.stroke(width * 15 * pow4Out.apply(out));
        //Lines.lineAngle(unit.x, unit.y, angle, 400f * out + width * 40);
        float len = (450f * out + width * 60);
        float wid = width * 20 * pow4Out.apply(out);
        float x = unit.x, y = unit.y;
        for(int i = 0; i < 40; i++){
            float f1 = i / 40f;
            float f2 = (i + 1) / 40f;
            Tmp.v1.trns(angle, len * f1);
            float bx1 = Tmp.v1.x, by1 = Tmp.v1.y;
            Tmp.v1.trns(angle, len * f2);
            float bx2 = Tmp.v1.x, by2 = Tmp.v1.y;

            Vec2 v1 = Tmp.v1.trns(angle, 0, laserWidth(f1) * wid);
            Vec2 v2 = Tmp.v2.trns(angle, 0, laserWidth(f2) * wid);
            Fill.polyBegin();

            Fill.polyPoint(v1.x + x + bx1, v1.y + y + by1);
            Fill.polyPoint(-v1.x + x + bx1, -v1.y + y + by1);
            Fill.polyPoint(-v2.x + x + bx2, -v2.y + y + by2);
            Fill.polyPoint(v2.x + x + bx2, v2.y + y + by2);

            Fill.polyEnd();
        }
    }

    @Override
    boolean shouldDraw(){
        return unit.activeAttack == this;
    }

    @Override
    void update(){
        float duration = quickSwap ? 4f * 60f : 10f * 60f;
        time += Time.delta;
        float out = Mathf.clamp(((duration) - time) / 120f);
        float fin = Mathf.clamp(time / 60f);

        if(out * fin > 0.001f){
            sounds.get(0).play(unit.x, unit.y, fin * out * 0.5f, true);
        }else{
            sounds.get(0).stop();
        }

        if(collisionTime <= 0f){
            float dur = 1.5f * 60f;

            float m1 = (time % dur) / dur;
            float m2 = ((time - dur / 2) % dur) / dur;
            float f1 = pow2.apply(Utils.biasSlope(Mathf.clamp(m1), 0.2f));
            float f2 = pow2.apply(Utils.biasSlope(Mathf.clamp(m2), 0.2f));

            if(m1 > 0.05f && m1 < 0.5f){
                for(int i = 0; i < 8; i++){
                    float ang = ((360f / 8) * i + 90) + time / 20 * side;
                    updateLaser(f1, ang, out);
                }
            }
            if(m2 > 0.05f && m2 < 0.5f){
                for(int i = 0; i < 8; i++){
                    float ang = ((360f / 8) * i + (180f / 8) + 90) + time / 20 * side;
                    updateLaser(f2, ang, out);
                }
            }

            collisionTime = 5f;
        }
        collisionTime -= Time.delta;
        if(time < duration){
            float m = Mathf.lerp(1f, 0.1f, out);
            unit.move(5, 3, m, m);
        }

        if(time > duration + 60f){
            time = 0f;
            side *= -1;
            unit.randAI(true, unit.health < 50);
        }
    }
}
