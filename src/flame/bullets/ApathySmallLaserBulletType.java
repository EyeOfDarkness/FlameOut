package flame.bullets;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import flame.unit.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class ApathySmallLaserBulletType extends BulletType{
    protected final static Seq<Healthc> tseq = new Seq<>();

    float length = 1900f;
    float width = 8f;

    public static float inEnd = 8f;

    Color[] colors = {FlamePal.primary.cpy().a(0.5f), FlamePal.primary, FlamePal.primary.cpy().mul(1.5f), Color.white};
    float[] widths = {1.65f, 1f, 0.66f, 0.5f};

    public ApathySmallLaserBulletType(){
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

        //lifetime = 5f * 60f;
        lifetime = inEnd + 20f;

        despawnEffect = Fx.none;
        shootEffect = Fx.none;
        hitEffect = FlameFX.bigLaserHitSpark;

        status = StatusEffects.disarmed;
        statusDuration = 60f;

        damage = 8900f;

        drawSize = 2600f;

        knockback = 40f;
    }

    void handleData(ApathyData d, Healthc target){
        // && !(d.wasDead && !d.target.dead())
        if(d.target == null || (d.target.dead() || !d.target.isAdded() || d.resetTimer <= 0f)){
            if(d.target != null && (d.target.dead() || !d.target.isAdded())){
                //d.ai.strongLaserScore = 0f;
                
                //Log.info("ended");
                //if(d.target instanceof Building bl && target instanceof Building tb && (bl.tile == tb.tile)) Log.info("invalid");
                if(!(d.target instanceof Building bl && target instanceof Building tb && (bl.tile == tb.tile))) d.ai.strongLaserScore = 0f;
                //if(d.target instanceof Building bl && (bl.tile.build != null && (bl.tile.build != bl && bl.block == bl.tile.block()))) Log.info("invalid");
                //if(!(d.target instanceof Building bl && (bl.tile.build != null && (bl.tile.build != bl && bl.block == bl.tile.block())))) d.ai.strongLaserScore = 0f;
                
                d.target = null;
            }

            if(!target.dead()){
                d.target = target;
                d.lastHealth = target.health();
                d.resetTimer = 15f;
                //d.wasDead = false;
            }
            //Log.info("awgvshgsefse" + d.target);
            return;
        }
        if(target == d.target){
            float mul = target instanceof Unit u ? u.healthMultiplier : (target instanceof Building bl ? damage / Math.min(damage, bl.handleDamage(damage)) : 1f);
            //float damageDelta = (target.health() - d.lastHealth) + Math.max(damage, (target.maxHealth() * mul) / 70f);
            float damageDelta = Math.max(damage, (target.maxHealth() * mul) / 70f) / Math.max(0.000001f, (d.lastHealth - target.health())) + target.health() / (4f * 60f);
            //Log.info("test: " + damageDelta);
            if(damageDelta > 0){
                d.ai.strongLaserScore += damageDelta / 2f;
            }
            //Log.info("test: " + d.ai.strongLaserScore);

            d.lastHealth = target.health();
            d.resetTimer = 15f;
        }
    }

    @Override
    public void update(Bullet b){
        if(b.timer(0, 5f)){
            b.fdata = length;
            tseq.clear();

            Object td = b.data;
            if(td != null && !(td instanceof ApathyData)){
                td = null;
                if(b.owner instanceof ApathyIUnit au && au.controller() instanceof ApathyIAI ai){
                    ai.strongLaserScore += 1000000;
                }
            }
            Object d = td;
            //Log.info("data: " + d);

            Vec2 v = Tmp.v1.trns(b.rotation(), length).add(b);
            float vx = v.x;
            float vy = v.y;
            b.fdata = Utils.hitLaser(b.team, width, b.x, b.y, vx, vy, null, t -> t.health() > damage * 20f, (t, x, y) -> {
                hit(b, x, y);
                /*
                if(t instanceof Hitboxc u){
                    hitEntity(b, u, -1f);
                }
                 */
                if(d != null) handleData((ApathyData)d, t);

                float mul = t instanceof Unit u ? u.healthMultiplier : (t instanceof Building bl ? damage / Math.min(damage, bl.handleDamage(damage)) : 1f);
                if(t instanceof Unit unit){
                    //hitEntity(b, u, u.health);
                    Tmp.v3.set(unit).sub(b).nor().scl((knockback + Math.max((length - b.dst(unit)) / (80f * (1.4615384f)), 0f)) * 80f);
                    if(impact) Tmp.v3.setAngle(b.rotation() + (knockback < 0 ? 180f : 0f));
                    unit.impulse(Tmp.v3);
                    //unit.vel.limit(24f / (1 - unit.drag));
                    unit.vel.limit(Mathf.pow(24f, 1 + unit.drag));
                    unit.apply(status, statusDuration);
                }
                t.damagePierce(Math.max(b.damage, (t.maxHealth() * mul) / 70f));

                //if(d != null && (t.dead() || t.health() <= 0 || (t instanceof Teamc tm && tm.team() == Team.derelict))) ((ApathyData)d).wasDead = true;
            });
            if(d != null){
                ApathyData ad = (ApathyData)d;
                if(ad.resetTimer > 0){
                    ad.resetTimer -= Time.delta;
                    if(ad.resetTimer <= 0f){
                        ad.target = null;
                        ad.lastHealth = 0f;
                    }
                }
                if(ad.target != null && (ad.target.dead() || !ad.target.isAdded())){
                    if(!(ad.target instanceof Building bl && bl.tile.build != bl)) ad.ai.strongLaserScore = 0f;
                    ad.target = null;
                    //Log.info("TEstwergtawg");
                }
            }
        }
    }

    @Override
    public void draw(Bullet b){
        float fc = Mathf.clamp(b.time / inEnd);
        float mwidth = Mathf.clamp((b.lifetime - b.time) / 16f) * width * 1.25f;
        float length = fc * b.fdata;
        Vec2 v = Utils.v.trns(b.rotation(), length).add(b);

        float sin = (Mathf.absin(5f, 0.1f) + 1f);

        //Draw.color(Color.white);
        //GraphicUtils.drawShockWave(b.x, b.y, 75f, 0f, -b.rotation() - 90f, 200f, 4f, 12);

        for(int i = 0; i < 4; i++){
            Draw.color(colors[i]);
            Lines.stroke(widths[i] * mwidth * sin);
            Lines.line(b.x, b.y, v.x, v.y, false);
            //Drawf.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, Lines.getStroke(), cwidth * 2f + width / 2f, b.rotation());
            Drawf.tri(v.x, v.y, Lines.getStroke(), mwidth * 3 + width / 1.5f, b.rotation());
            Drawf.tri(v.x, v.y, Lines.getStroke(), mwidth * 2 + width / 2f, b.rotation() + 180f);

            for(int s : Mathf.signs){
                Drawf.tri(b.x, b.y, Lines.getStroke() / 2f, (mwidth * widths[i] * 2 + 120f) * sin * fc, b.rotation() + 90f * s);
            }

            Fill.circle(b.x, b.y, mwidth * 0.7f * widths[i] * sin);
        }
        Draw.reset();
        v = Utils.v.trns(b.rotation(), length + width / 2f).add(b);
        Drawf.light(b.x, b.y, v.x, v.y, mwidth * 2.75f, colors[0], 0.6f);
    }

    @Override
    public void drawLight(Bullet b){

    }

    public static class ApathyData{
        float lastHealth = -1f, resetTimer = 0f;
        Healthc target;
        public ApathyIAI ai;
    }
}
