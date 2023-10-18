package flame.bullets;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class ApathySweepLaserBulletType extends BulletType{
    float length = 1800f;
    float width = 8f;

    Color[] colors = {FlamePal.primary.cpy().a(0.5f), FlamePal.primary, Color.white};
    float[] widths = {1.65f, 1f, 0.66f};

    static IntSet set = new IntSet();

    public ApathySweepLaserBulletType(){
        speed = 0f;
        collides = collidesTiles = false;
        absorbable = false;
        hittable = false;
        keepVelocity = false;

        removeAfterPierce = false;
        pierce = true;
        pierceArmor = true;
        pierceCap = -1;
        impact = true;

        lifetime = 3f * 60f;

        despawnEffect = Fx.none;
        shootEffect = Fx.none;
        hitEffect = FlameFX.bigLaserHitSpark;

        status = StatusEffects.disarmed;
        statusDuration = 4f * 60f;

        damage = 6000f;

        drawSize = 3600f;
    }

    public void hitEntity(Bullet b, Hitboxc entity, float health){
        if(entity instanceof Healthc h){
            h.damagePierce(Math.max(b.damage, h.maxHealth() / 120f));
        }

        if(entity instanceof Unit unit){
            Tmp.v3.set(unit).sub(b).nor().scl(knockback * 80f);
            if(impact) Tmp.v3.setAngle(b.rotation() + (knockback < 0 ? 180f : 0f));
            unit.impulse(Tmp.v3);
            unit.apply(status, statusDuration);
        }
    }

    @Override
    public void update(Bullet b){
        if(b.timer(0, 5f)){
            set.clear();
            float swid = ((2 * Mathf.PI * length) * (5f / 360f)) / width;
            //float ewid = 1f + Mathf.clamp(Angles.angleDist(b.rotation(), b.fdata) / 5f);
            float ewid = Mathf.lerp(1f, swid, Mathf.clamp(Angles.angleDist(b.rotation(), b.fdata) / 5f));

            laser(b, b.rotation(), ewid);
            /*
            int dst = (int)(Angles.angleDist(b.rotation(), b.fdata) / 5f);
            for(int i = 1; i < dst; i++){
                float f = i / (dst - 1f);
                float r = Mathf.slerp(b.rotation(), b.fdata, f);
                laser(b, r, ewid);
            }
             */
            float mid = Mathf.slerp(b.rotation(), b.fdata, 0.5f);
            float adst = Angles.angleDist(b.rotation(), b.fdata);

            scanCone(b, mid, adst);

            b.fdata = b.rotation();
        }
    }

    @SuppressWarnings("unchecked")
    void scanCone(Bullet b, float rot, float dst){
        for(TeamData data : Vars.state.teams.active){
            if(data.team != b.team && data.unitTree != null){
                Utils.scanCone(data.unitTree, b.x, b.y, rot, length, dst / 2, u -> {
                    if((u.isFlying() || u instanceof TimedKillUnit) && !set.contains(u.id)){
                        Tmp.v1.set(b.x, b.y).sub(u.x, u.y).setLength(u.hitSize / 2).add(u.x, u.y);
                        if(!Float.isNaN(Tmp.v1.x)){
                            hit(b, Tmp.v1.x, Tmp.v1.y);
                        }
                        hitEntity(b, u, u.health);

                        set.add(u.id);
                    }
                });
            }
        }
        Utils.scanCone((QuadTree<Bullet>)(Groups.bullet.tree()), b.x, b.y, rot, length, dst / 2, d -> {
            if(d.team != b.team && !set.contains(d.id)){
                BulletType dtype = d.type;

                d.hit = false;
                d.remove();
                dtype.hitEffect.at(d.x, d.y, d.rotation(), dtype.hitColor);
                dtype.hitSound.at(d.x, d.y, dtype.hitSoundPitch, dtype.hitSoundVolume);
                Effect.shake(dtype.hitShake, dtype.hitShake, d);

                set.add(d.id);
            }
        });
    }

    @SuppressWarnings("unchecked")
    void laser(Bullet b, float rotation, float speed){
        float mwidth = Mathf.curve(b.fin(), 0f, 0.05f) * Mathf.curve(b.fout(), 0, 0.15f) * width * speed;
        Vec2 v = Utils.v.trns(rotation, length - mwidth / 2).add(b);
        for(TeamData data : Vars.state.teams.active){
            if(data.team != b.team && data.unitTree != null){
                Utils.intersectLine(data.unitTree, mwidth, b.x, b.y, v.x, v.y, (u, x, y) -> {
                    if((u.isFlying() || u instanceof TimedKillUnit) && !set.contains(u.id)){
                        hit(b, x, y);
                        hitEntity(b, u, u.health);

                        set.add(u.id);
                    }
                });
            }
        }

        Utils.intersectLine((QuadTree<Bullet>)(Groups.bullet.tree()), mwidth, b.x, b.y, v.x, v.y, (d, x, y) -> {
            //
            if(d.team != b.team && !set.contains(d.id)){
                BulletType dtype = d.type;

                d.hit = false;
                d.remove();
                dtype.hitEffect.at(d.x, d.y, d.rotation(), dtype.hitColor);
                dtype.hitSound.at(d.x, d.y, dtype.hitSoundPitch, dtype.hitSoundVolume);
                Effect.shake(dtype.hitShake, dtype.hitShake, d);

                set.add(d.id);
            }
        });
    }

    @Override
    public void draw(Bullet b){
        //
        float mwidth = Mathf.curve(b.fin(), 0f, 0.05f) * Mathf.curve(b.fout(), 0, 0.15f) * width;
        Utils.v.trns(b.rotation(), 20f).add(b);
        
        float bx = Utils.v.x;
        float by = Utils.v.y;
        
        Vec2 v = Utils.v.trns(b.rotation(), length).add(b);

        for(int i = 0; i < 3; i++){
            Draw.color(colors[i]);
            Lines.stroke(widths[i] * mwidth * (Mathf.absin(5f, 0.1f) + 1f));
            Lines.line(bx, by, v.x, v.y, false);
            //Drawf.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, Lines.getStroke(), cwidth * 2f + width / 2f, b.rotation());
            Drawf.tri(v.x, v.y, Lines.getStroke(), mwidth * 2 + width / 2f, b.rotation());
            Fill.circle(bx, by, mwidth * 0.7f * widths[i] * (Mathf.absin(5f, 0.1f) + 1f));
        }
        Draw.reset();
        v = Utils.v.trns(b.rotation(), length + width / 2f).add(b);
        Drawf.light(bx, by, v.x, v.y, mwidth * 2f, colors[0], 0.6f);
    }

    @Override
    public void drawLight(Bullet b){

    }
}
