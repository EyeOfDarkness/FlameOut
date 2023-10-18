package flame.unit.empathy;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.Pool.*;
import flame.*;
import flame.Utils.*;
import flame.effects.*;
import flame.entities.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;

public class RicochetAttack extends AttackAI{
    float reload = 0f;
    float forgetTime = 0f;

    static BasicPool<BulletData> bdpool = new BasicPool<>(BulletData::new);
    static Teamc tt;
    static float td;

    @Override
    void updatePassive(){
        reload -= Time.delta;
    }

    @Override
    void reset(){
        reload = 0f;
    }

    @Override
    float weight(){
        return reload > 0 || unit.nearbyBullets < 8 ? -1f : 50f + unit.nearbyBullets;
    }
    
    @Override
    boolean teleSwapCompatible(){
        return true;
    }

    @Override
    void update(){
        if(unit.getTarget() != null && unit.within(unit.getTarget(), 700f)){
            if(reload <= 0f){
                tt = null;
                td = 0f;
                float rad = 700f;
                Rect r = Tmp.r1.setCentered(unit.x, unit.y, rad * 2, rad * 2);
                Groups.bullet.intersect(r.x, r.y, r.width, r.height, b -> {
                    if(b.team != unit.team && b.within(unit, rad) && b.type.speed > 0.01f && b.time < (b.lifetime - 5f)){
                        float dst = FlameOutSFX.inst.getBulletDps(b.type) - b.dst(unit) / 1000f;
                        if(tt == null || dst > td){
                            tt = b;
                            td = dst;
                        }
                    }
                });
                Groups.unit.intersect(r.x, r.y, r.width, r.height, u -> {
                    if(u.team != unit.team && u.within(unit, rad) && (u instanceof TimedKillc || u.controller() instanceof MissileAI)){
                        float dst = FlameOutSFX.inst.getUnitDps(u.type) - u.dst(unit) / 1000f;
                        if(tt == null || dst > td){
                            tt = u;
                            td = dst;
                        }
                    }
                });
                if(tt != null && unit.getTarget() instanceof Sized s){
                    RicoEntity e = new RicoEntity();
                    e.x = unit.x;
                    e.y = unit.y;
                    e.team = unit.team;
                    e.target = s;
                    e.targetBullet = tt;
                    e.add();
                    //reload = 2.5f * 60f;
                    reload = 6f * 60f;

                    Sounds.bolt.at(unit.x, unit.y);
                    unit.randAI(true, unit.health < 50);
                }
            }
        }
        if(unit.nearbyBullets <= 0){
            forgetTime += Time.delta;
            if(forgetTime > 30f){
                forgetTime = 0f;
                unit.randAI(true, unit.health < 50);
            }
        }else{
            forgetTime = 0f;
        }
    }

    @SuppressWarnings("unchecked")
    static class RicoEntity extends BasicEntity{
        float x, y;
        Teamc targetBullet;
        Sized target;
        Team team;
        float delay = 0f;
        int bounces = 0;
        int direct = 0;
        boolean bounceBack = false;

        Seq<BulletData> bullets = new Seq<>();

        static Bullet tmpBullet = new MockBullet();

        @Override
        public void update(){
            if(target == null || !((Entityc)target).isAdded()){
                Teamc t = Units.closestTarget(team, x, y, 700f);
                if(t instanceof Sized){
                    target = (Sized)t;
                }else{
                    remove();
                    return;
                }
            }
            if(targetBullet == null && delay <= 0f){
                tt = null;
                td = 0f;

                float srcDst = target.dst(x, y);
                float ang = Angles.angle(x, y, target.getX(), target.getY());
                float sang = ang + (bounceBack ? 180f : 0f);
                float sx = bounceBack ? target.getX() : x;
                float sy = bounceBack ? target.getY() : y;
                float spread = bounceBack ? 45f : Mathf.angle(srcDst, target.hitSize() / 2.5f);
                float scanDst = bounceBack ? 500f : (srcDst + 300f);

                if(direct <= 0){
                    Utils.scanCone((QuadTree<Bullet>)Groups.bullet.tree(), sx, sy, sang, scanDst, spread, true, b -> {
                        if(b.team != team && b.type.speed > 0.01f && (bounceBack || b.dst(x, y) > srcDst)){
                            float dst = b.dst(sx, sy);
                            if(tt == null || dst > td){
                                tt = b;
                                td = dst;
                            }
                        }
                    });
                    Utils.scanCone((QuadTree<Unit>)Groups.unit.tree(), sx, sy, sang, scanDst, spread, true, u -> {
                        if(u.team != team && (u instanceof TimedKillc || u.controller() instanceof MissileAI) && (bounceBack || u.dst(x, y) > srcDst)){
                            float dst = u.dst(sx, sy);
                            if(tt == null || dst > td){
                                tt = u;
                                td = dst;
                            }
                        }
                    });
                    if(tt != null) direct = Mathf.random(2, 5);
                }
                if(tt == null){
                    float rad = 350f;
                    Rect r = Tmp.r1.setCentered(x, y, rad * 2, rad * 2);
                    Groups.bullet.intersect(r.x, r.y, r.width, r.height, b -> {
                        if(b.team != team && b.type.speed > 0.01f && b.within(x, y, rad) && b.dst(x, y) > 30f && b.dst(target.getX(), target.getY()) <= srcDst + 16f){
                            float dst = b.dst(x, y);
                            if(tt == null || dst < td){
                                tt = b;
                                td = dst;
                            }
                        }
                    });
                    Groups.unit.intersect(r.x, r.y, r.width, r.height, u -> {
                        if(u.team != team && u.within(x, y, rad) && (u instanceof TimedKillc || u.controller() instanceof MissileAI) && u.dst(target.getX(), target.getY()) <= srcDst + 16f){
                            float dst = u.dst(x, y);
                            if(tt == null || dst < td){
                                tt = u;
                                td = dst;
                            }
                        }
                    });
                }

                //TODO ricochet between units
                targetBullet = tt;
                bounceBack = false;
                if(targetBullet == null){
                    /*
                    float dst = srcDst + 300f;

                    FlameFX.empathyRico.at(x, y, ang, dst);
                    Tmp.v1.trns(ang, dst).add(x, y);
                    scan(x, y, Tmp.v1.x, Tmp.v1.y);

                    remove();
                    */
                    
                    if(shouldBounceBack(ang + 180f)){
                        float dst = srcDst - (target.hitSize() / 2.5f);
                        
                        FlameFX.empathyRico.at(x, y, ang, dst);
                        Tmp.v1.trns(ang, dst).add(x, y);
                        scan(x, y, Tmp.v1.x, Tmp.v1.y);
                        x = Tmp.v1.x;
                        y = Tmp.v1.y;
                        FlameFX.empathyParry.at(x, y);
                        FlameSounds.empathyRico.at(x, y);
                        
                        bounceBack = true;
                        direct = 0;
                        delay = 5f;
                    }else{
                        float dst = srcDst + 300f;

                        FlameFX.empathyRico.at(x, y, ang, dst);
                        Tmp.v1.trns(ang, dst).add(x, y);
                        scan(x, y, Tmp.v1.x, Tmp.v1.y);

                        remove();
                    }
                    return;
                }
            }
            if(targetBullet != null){
                float dst = targetBullet.dst(x, y);
                float ang = Angles.angle(x, y, targetBullet.getX(), targetBullet.getY());

                FlameFX.empathyRico.at(x, y, ang, dst);
                if(targetBullet instanceof Bullet b){
                    BulletData bd = bdpool.obtain();
                    bd.type = b.type;
                    bd.data = b.data;
                    bullets.add(bd);
                }else if(targetBullet instanceof Unit u){
                    for(Weapon w : u.type.weapons){
                        if(w.shootOnDeath || w.bullet.killShooter){
                            BulletData bd = bdpool.obtain();
                            bd.type = w.bullet;
                            bullets.add(bd);
                        }
                    }
                }
                float lx = x, ly = y;

                x = targetBullet.x();
                y = targetBullet.y();

                scan(lx, ly, x, y);
                delay = 5f;

                //EmpathyDamage.annihilate(targetBullet, false);
                targetBullet.team(team);
                EmpathyDamage.annihilate(targetBullet, false);
                FlameFX.empathyParry.at(x, y);
                FlameSounds.empathyRico.at(x, y);

                targetBullet = null;
                bounces++;
                direct--;
            }
            delay -= Time.delta;
        }
        
        boolean shouldBounceBack(float angle){
            float sx = target.getX();
            float sy = target.getY();
            
            tt = null;
            
            Utils.scanCone((QuadTree<Bullet>)Groups.bullet.tree(), sx, sy, angle, 500f, 45f, true, b -> {
                if(b.team != team && b.type.speed > 0.01f && b.time < (b.lifetime - 16f)){
                    tt = b;
                }
            });
            Utils.scanCone((QuadTree<Unit>)Groups.unit.tree(), sx, sy, angle, 500f, 45f, true, u -> {
                if(u.team != team && ((u instanceof TimedKillc tk && tk.time() < (tk.lifetime() - 16f)) || (!(u instanceof TimedKillc) && u.controller() instanceof MissileAI))){
                    tt = u;
                }
            });
            
            return tt != null;
        }

        void scan(float x1, float y1, float x2, float y2){
            float ed = bounces * 50f;
            Utils.hitLaser(team, 3f, x1, y1, x2, y2, null, h -> false, (h, x, y) -> {
                handleBullets(h, x, y);
                if(h instanceof Unit u){
                    EmpathyDamage.damageUnit(u, Math.max(200f + ed, u.maxHealth / 1000f), true, null);
                }else if(h instanceof Building b){
                    EmpathyDamage.damageBuilding(b, Math.max(200f + ed, b.maxHealth / 1000f), true, null);
                }
            });
        }
        void handleBullets(Healthc h, float x, float y){
            for(BulletData b : bullets){
                BulletType type = b.type;
                Bullet tb = tmpBullet;
                tb.type = b.type;
                tb.data = b.data;
                tb.team = team;
                tb.time = 0f;
                tb.x = x;
                tb.y = y;
                tb.aimX = x;
                tb.aimY = y;
                tb.hitSize = b.type.hitSize;

                try{
                    type.hitEntity(tb, (Hitboxc)h, h.health());
                    type.hit(tb, x, y);
                }catch(Exception e){
                    Log.err(e);
                }
            }
            bullets.clear();
        }

        @Override
        protected void removeGroup(){
            super.removeGroup();
            for(BulletData b : bullets){
                bdpool.free(b);
            }
        }
    }

    static class BulletData implements Poolable{
        BulletType type;
        Object data;

        @Override
        public void reset(){
            type = null;
            data = null;
        }
    }
}
