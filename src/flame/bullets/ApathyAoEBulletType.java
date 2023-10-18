package flame.bullets;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.Pool.*;
import flame.*;
import flame.Utils.*;
import flame.effects.*;
import flame.graphics.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

import static mindustry.Vars.*;

public class ApathyAoEBulletType extends BulletType{
    float length = 1200f;
    TextureRegion region;

    static BasicPool<ExplosionPoint> exPool = new BasicPool<>(ExplosionPoint::new);
    static QuadTree<ExplosionPoint> exTree;
    static int s = 0;

    public ApathyAoEBulletType(){
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

        lifetime = 8f;

        despawnEffect = Fx.none;
        shootEffect = Fx.none;

        damage = 5000f;

        drawSize = 2400f;

        //instantDisappear = true;
    }

    public static void loadStatic(){
        Events.on(EventType.WorldLoadEvent.class, e -> {
            //Groups.resize(-finalWorldBounds, -finalWorldBounds, tiles.width * tilesize + finalWorldBounds * 2, tiles.height * tilesize + finalWorldBounds * 2);

            exTree = new QuadTree<>(new Rect(-finalWorldBounds, -finalWorldBounds, world.width() * tilesize + finalWorldBounds * 2, world.height() * tilesize + finalWorldBounds * 2));
        });
    }

    @Override
    public void load(){
        super.load();
        region = Core.atlas.find("flameout-flash");
    }

    @Override
    public void init(Bullet b){
        //
        Seq<ExplosionPoint> data = new Seq<>();
        b.data = data;

        exTree.clear();

        for(TeamData td : state.teams.present){
            if(td.team != b.team){
                //
                if(td.unitTree != null){
                    Utils.scanCone(td.unitTree, b.x, b.y, b.rotation(), length, 17f, u -> {
                        s = 0;
                        Rect r = Tmp.r1.setCentered(u.x, u.y, 40f, 40f);
                        exTree.intersect(r, e -> s++);

                        if(u.isGrounded() && (s <= 0 || Mathf.chance(0.05f / s))){
                            ExplosionPoint p = exPool.obtain();
                            Vec2 v = Tmp.v1.rnd(40f * Mathf.random());
                            p.x = u.x + v.x;
                            p.y = u.y + v.y;
                            p.size = Mathf.random(0.75f, 2f);
                            data.add(p);
                            exTree.insert(p);
                        }
                    });
                }
                if(td.buildingTree != null){
                    Utils.scanCone(td.buildingTree, b.x, b.y, b.rotation(), length, 17f, t -> {
                        s = 0;
                        Rect r = Tmp.r1.setCentered(t.x, t.y, 40f, 40f);
                        exTree.intersect(r, e -> s++);

                        if(s <= 0 || Mathf.chance(0.05f / s)){
                            ExplosionPoint p = exPool.obtain();
                            Vec2 v = Tmp.v1.rnd(40f * Mathf.random());
                            p.x = t.x + v.x;
                            p.y = t.y + v.y;
                            p.size = Mathf.random(0.75f, 1.25f);
                            data.add(p);
                            exTree.insert(p);
                        }
                    });
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void draw(Bullet b){
        if(b.data instanceof Seq){
            Seq<ExplosionPoint> data = (Seq<ExplosionPoint>)b.data;
            float spark = (1f + Mathf.absin(b.fin(Interp.pow2In), 1f / 50f, 1.5f)) * b.fin();
            Draw.color(FlamePal.primary);
            Draw.blend(Blending.additive);

            Drawf.tri(b.x, b.y, 40f * b.fout(), length, b.rotation());
            Drawf.tri(b.x, b.y, 40f * b.fout(), 55f, b.rotation() + 180f);

            for(ExplosionPoint ex : data){
                float s = spark * ex.size;
                float ang = Angles.angle(b.x, b.y, ex.x, ex.y);
                Draw.color(FlamePal.primary);
                GraphicUtils.tri(ex.x, ex.y, b.x, b.y, (1f + spark / 2f) * 4f * b.fout(), ang);
                Draw.color();

                Draw.rect(region, ex.x, ex.y, region.width * Draw.scl * s, region.height * Draw.scl * s);
            }
            Draw.blend();
            Draw.color();
        }
    }

    @Override
    public void drawLight(Bullet b){

    }

    void explode(Bullet b, float x, float y, float size, int count){
        //Damage.damage();
        float s = size * 120f;
        float exDamage = (damage * 10f) / (1 + (count - 1) * 8f);
        //float exDamage = 200000f * Mathf.clamp(1f - count / 15f);
        float exScl = Math.max(1f, 5f * (1f - count / 10f));
        Damage.damage(b.team, x, y, s, damage + exDamage, true, true, true, true, null);
        Rect r = Utils.r;
        r.setCentered(x, y, s * 2);

        for(TeamData data : state.teams.present){
            if(data.team != b.team){
                if(data.unitTree != null){
                    data.unitTree.intersect(r, u -> {
                        if(u.within(x, y, s + u.hitSize / 2f) && u.isGrounded()){
                            float dst = 1f - Interp.pow3In.apply(Mathf.clamp(u.dst(x, y) / (s + u.hitSize / 2f)));

                            u.damagePierce(dst * (u.maxHealth / 35f) * exScl);
                            u.apply(StatusEffects.disarmed, 2f * 60f);
                        }
                    });
                }
                if(data.turretTree != null){
                    data.turretTree.intersect(r, t -> {
                        if(t.within(x, y, s + t.hitSize() / 2f)){
                            float dst = 1f - Interp.pow3In.apply(Mathf.clamp(t.dst(x, y) / (s + t.hitSize() / 2f)));

                            t.damagePierce(dst * (t.maxHealth / 35f) * exScl);
                            if(t instanceof TurretBuild tr){
                                tr.ammo.clear();
                            }
                        }
                    });
                }
            }
        }

        FlameFX.aoeExplosion2.at(x, y, s);
        Sounds.largeExplosion.at(x, y, Mathf.random(0.9f, 1.1f));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removed(Bullet b){
        if(b.data instanceof Seq){
            Seq<ExplosionPoint> data = (Seq<ExplosionPoint>)b.data;
            int size = data.size;
            for(ExplosionPoint exp : data){
                explode(b, exp.x, exp.y, exp.size, size);
                exPool.free(exp);
            }
        }
    }

    static class ExplosionPoint implements QuadTreeObject, Poolable{
        float x = 0;
        float y = 0;
        float size = 0f;

        @Override
        public void hitbox(Rect out){
            out.setCentered(x, y, 90f * size);
        }

        @Override
        public void reset(){
            x = 0f;
            y = 0f;
            size = 0f;
        }
    }
}
