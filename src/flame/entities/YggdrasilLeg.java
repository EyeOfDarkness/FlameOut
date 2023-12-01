package flame.entities;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.unit.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class YggdrasilLeg{
    public float sourceX, sourceY, targetX, targetY, targetAngleDistance, targetAngle;
    public float lastRX, lastRY;
    public Vec2 joint = new Vec2(), base = new Vec2();
    public float length, walkLength;
    public int group;
    public boolean side;
    public int textureIdx1, textureIdx2;

    static Vec2 v1 = new Vec2(), v2 = new Vec2();

    public void set(Unit unit){
        float len = Mathf.random(60f, 220f);
        v1.trns(Mathf.random(360f), len);
        length = len;
        walkLength = len;

        targetX = v1.x;
        targetY = v1.y;
        lastRX = v1.x;
        lastRY = v1.y;
        targetAngleDistance = Mathf.clamp((Angles.angleDist(90f, v1.angle()) - 90f) / 90f);

        side = targetX < 0;
        if(targetY < 0){
            side = !side;
        }

        v2.trns(unit.rotation - 90f, targetX, targetY).add(unit);
        sourceX = v2.x;
        sourceY = v2.y;
        base.set(sourceX, sourceY);

        textureIdx1 = Mathf.random(0, 2);
        textureIdx2 = Mathf.random(0, 2);
        
        targetAngle = Angles.angle(unit.x, unit.y, v2.x, v2.y) - unit.rotation;

        v2.set(base).sub(unit).scl(0.5f).add(unit);
        joint.set(v2);
    }

    public float getProgress(Unit unit){
        //v2.trns(unit.rotation - 90f, targetX, targetY).add(unit);
        //float adst = Angles.angleDist((targetAngle + unit.rotation), Angles.angle(unit.x, unit.y, base.x, base.y)) / ((220f / length) * 15f);

        v2.trns(unit.rotation - 90f, lastRX, lastRY).add(unit);

        //return Mathf.clamp((Mathf.dst(unit.x, unit.y, base.x, base.y) - walkLength) / (35f));
        //return Mathf.clamp(Math.max((Mathf.dst(unit.x, unit.y, base.x, base.y) - walkLength) / (35f), adst * adst));
        return (v2.dst(sourceX, sourceY) / ((220f / length) * 90f));
    }

    public void end(Unit unit){
        sourceX = base.x;
        sourceY = base.y;

        walkLength = Mathf.dst(unit.x, unit.y, sourceX, sourceY);

        v1.trns(-(unit.rotation - 90f), sourceX - unit.x, sourceY - unit.y);

        lastRX = v1.x;
        lastRY = v1.y;
    }

    public void updateMovement(Unit unit, float progress){
        //v1.trns(unit.rotation - 90f, targetX, targetY + 120f * targetAngleDistance * progress).add(unit);
        v1.trns(unit.rotation - 90f, targetX, targetY).add(unit);
        /*
        if(targetAngleDistance > 0){
            float len = Mathf.clamp(unit.deltaLen() / unit.type.speed);
            v2.trns(unit.rotation, (length / 1.25f)).scl(targetAngleDistance * len);
            v1.add(v2);
        }
        */
        float len = Mathf.clamp((unit.deltaLen() / unit.type.speed) * Time.delta);
        float angV = unit.deltaAngle();
        v2.trns(angV, (length / 1.5f)).scl(len * (targetAngleDistance * 0.5f + 0.5f));
        v1.add(v2);

        v2.set(sourceX, sourceY).lerp(v1, Interp.pow2.apply(progress)).sub(unit).clamp(length / 1.4f, 99999f).add(unit);

        base.lerpDelta(v2, progress);
        
        //return base.within(v2, 2f);
    }

    public void updateIK(Unit unit){
        float dst = Mathf.dst(unit.x, unit.y, base.x, base.y);
        if(dst > length){
            float px = (base.x - unit.x) / 2f + unit.x;
            float py = (base.y - unit.y) / 2f + unit.y;
            joint.lerpDelta(px, py, 0.25f);
        }else{
            InverseKinematics.solve(length / 2f, length / 2f, v1.set(base).sub(unit.x, unit.y), side, v2);
            v2.add(unit.x, unit.y);
            float bdst = Mathf.clamp(1f - base.dst(joint) / (length / 2f));
            
            joint.lerpDelta(v2.x, v2.y, 0.25f + bdst * 0.2f);
        }
    }

    public void draw(Unit unit){
        //unit.type.applyColor(unit);
        YggdrasilUnitType type = (YggdrasilUnitType)unit.type;
        TextureRegion end = type.legBaseRegions[textureIdx2], leg = type.legRegions[textureIdx1];

        int flips = side ? 1 : -1;
        Lines.stroke(leg.height * Draw.scl * flips);
        Lines.line(leg, unit.x, unit.y, joint.x, joint.y, false);

        float scl = (end.width / (float)leg.width);
        float dx = (joint.x - base.x) * scl;
        float dy = (joint.y - base.y) * scl;

        Lines.stroke(end.height * Draw.scl * flips);
        Lines.line(end, dx + base.x, dy + base.y, base.x, base.y, false);
    }
}
