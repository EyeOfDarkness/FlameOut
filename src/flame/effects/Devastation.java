package flame.effects;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import flame.*;
import flame.entities.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class Devastation extends DrawEntity implements Poolable{
    TextureRegion main = new TextureRegion();
    public float rotation, width, height;
    public float vx, vy, vr;
    public float health;
    public float z, color;
    public float time, lifetime;
    public float contagiousChance = 0f;
    public float slowDownAmount = 1f;
    public Effect explosion = Fx.none;
    public Team team = Team.derelict;
    public boolean collides = true;

    float collisionDelay = 0f;
    float contagiousTime = 0f;

    public static Devastation create(){
        return Pools.obtain(Devastation.class, Devastation::new);
    }

    @Override
    public void update(){
        x += vx * Time.delta;
        y += vy * Time.delta;
        rotation += vr * Time.delta;

        Teamc teamc = Units.closestTarget(team, x, y, 8f, Flyingc::isGrounded);
        Tile tile = Vars.world.tileWorld(x, y);

        if(collides && collisionDelay <= 0f && teamc instanceof Healthc healthc){
            float scl = 0.01f + Mathf.sqrt(vx * vx + vy * vy) / 5f;
            float lh = healthc.health();
            float lh2 = health;

            healthc.damage(health * scl);
            health -= lh * scl;
            //health -= healthc.health() * scl;

            float ratio = Mathf.clamp((lh / lh2) * scl) * slowDownAmount;

            vx *= 1f - ratio;
            vy *= 1f - ratio;
            vr *= 1f - ratio;

            if(contagiousChance > 0 && Mathf.dst2(vx, vy) > 4f && (!healthc.isValid() && !healthc.isAdded())){
                if(Mathf.chance(contagiousChance)){
                    SpecialDeathEffects eff = teamc instanceof Building bl ? SpecialDeathEffects.get(bl.block) : (teamc instanceof Unit un ? SpecialDeathEffects.get(un.type) : SpecialDeathEffects.def);
                    Runnable r = teamc instanceof Building bl ? bl::draw : (teamc instanceof Drawc draw ? draw::draw : () -> {});

                    float healthBase = Math.min(width, height);
                    float healthBase2 = teamc instanceof Building bl ? bl.block.size * Vars.tilesize : (teamc instanceof Unit u ? Math.min(u.type.region.width, u.type.region.height) * Draw.scl : 1f);
                    FlameOut.devasBatch.baseZ = Layer.block;
                    FlameOut.devasBatch.switchBatch(r, dev -> {
                        float dim = Math.min(dev.width, dev.height);

                        dev.lifetime = lifetime * Mathf.random(0.8f, 1f);
                        dev.health = (dim / healthBase2) * healthc.maxHealth() * 0.6f;
                        dev.team = team;
                        dev.explosion = eff.explosionEffect != Fx.none ? eff.explosionEffect : FlameFX.fragmentExplosion;
                        dev.collides = Mathf.chance(0.75f);
                        dev.contagiousChance = contagiousChance;
                        dev.slowDownAmount = slowDownAmount;

                        Vec2 v = Utils.vv.set(vx, vy).rotate(Mathf.range(15f)).scl(Mathf.random(0.9f, 1.05f));

                        dev.vx = v.x * (healthBase / dim);
                        dev.vy = v.y * (healthBase / dim);
                        dev.vr = Mathf.range(Math.abs(vr));
                        dev.collisionDelay = 10f;
                    });
                    contagiousTime = 25f;
                }
            }
            FlameFX.fragmentExplosionSpark.at(x, y, Math.min(width, height) / 5f);

            /*
            if(split){
                FlameFX.fragmentExplosionSmoke.at(x, y, Math.min(width, height) / 5f);
            }else{
                explosion.at(x, y, Math.min(width, height) / 4f);
            }
            */
        }
        if(collisionDelay > 0) collisionDelay -= Time.delta;
        if(contagiousTime > 0) contagiousTime -= Time.delta;
        if(tile != null && tile.block().isStatic() && tile.block().solid){
            health = 0f;
        }

        time += Time.delta;
        if(time > lifetime || health <= 0f || (vx * vx + vy * vy) < 0.0001f){
            if(contagiousTime <= 0f){
                explosion.at(x, y, Math.min(width, height) / 2f);
            }else{
                //FlameFX.fragmentExplosionSmoke.at(x, y, Math.min(width, height) / 2f);
                FlameFX.fragmentExplosionSpark.at(x, y, Math.min(width, height) / 2f);
            }

            remove();
        }
    }

    @Override
    public void draw(){
        //Draw.color();
        Draw.color(color);
        Draw.z(z);
        Draw.rect(main, x, y, width, height, rotation);
    }

    @Override
    public float clipSize(){
        return Math.max(width, height) * 2.5f;
    }

    public void set(TextureRegion tex, float x, float y, float width, float height, float rotation){
        //main = tex;
        main.set(tex);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
    }

    @Override
    public void reset(){
        //main = null;
        main.set(Core.atlas.white());
        rotation = width = height = health = vx = vy = vr = 0f;
        z = color = 0f;
        time = lifetime = 0f;
        contagiousChance = 0f;
        slowDownAmount = 1f;
        explosion = Fx.none;
        team = Team.derelict;
        collides = true;
        collisionDelay = contagiousTime = 0f;
    }

    @Override
    protected void removeGroup(){
        super.removeGroup();
        Groups.queueFree(this);
    }
}
