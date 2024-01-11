package flame.unit.weapons;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.bullets.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class EndLauncherWeapon extends Weapon{
    //x, y, rotation
    static float[] targets = {-8f, 0f, -45f,
                            12f, 10f, -35f,
                            25f, 20f, -25f,
                            28f, 30f, -15f
                            };

    public EndLauncherWeapon(String name){
        super(name);
        mirror = false;
        alternate = false;
        top = false;
        x = y = 0f;
        shootWarmupSpeed = 0.025f;
        shootCone = 360f;
        minWarmup = 0.99f;
        reload = 60f * 2.5f;

        //shootSound = Sounds.missileSmall;
        shootSound = Sounds.missile;
        mountType = EndLauncherMount::new;

        bullet = new EndMissileBulletType();
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        EndLauncherMount lm = (EndLauncherMount)mount;
        boolean can = unit.canShoot();
        mount.reload = Math.max(mount.reload - Time.delta * unit.reloadMultiplier, 0);
        mount.recoil = Mathf.approachDelta(mount.recoil, 0, unit.reloadMultiplier / recoilTime);

        mount.smoothReload = Mathf.lerpDelta(mount.smoothReload, mount.reload / reload, smoothReloadSpeed);
        mount.charge = mount.charging && shoot.firstShotDelay > 0 ? Mathf.approachDelta(mount.charge, 1, 1 / shoot.firstShotDelay) : 0;

        float warmupTarget = (can && mount.shoot) || (lm.burstCount > 0) ? 1f : 0f;
        if(linearWarmup){
            mount.warmup = Mathf.approachDelta(mount.warmup, warmupTarget, shootWarmupSpeed);
        }else{
            mount.warmup = Mathf.lerpDelta(mount.warmup, warmupTarget, shootWarmupSpeed);
        }

        lm.updateTarget();

        if(mount.shoot && lm.burstCount <= 0 && can && mount.warmup > minWarmup && mount.reload <= 0.0001f){
            mount.reload = reload;
            lm.burstCount = 8 * 6;
            lm.burstTime = 0f;
        }

        if(lm.burstCount > 0){
            mount.reload = reload;

            if(lm.burstTime <= 0f){
                int mc = targets.length / 3;
                int hs = mount.totalShots / 2;
                boolean flip = mount.totalShots % 2 == 1;
                Vec3 pos = getShootPosition(unit, mount, hs % mc, flip);

                shootAlt(unit, lm, pos.x, pos.y, pos.z);

                lm.burstTime = 1f;
                lm.burstCount--;
                mount.totalShots++;
            }
            lm.burstTime -= Time.delta;
        }
    }

    void shootAlt(Unit unit, EndLauncherMount mount, float x, float y, float rot){
        //mount.bullet = bullet.create(unit, shooter, unit.team, bulletX, bulletY, angle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd), lifeScl, null, mover, mount.aimX, mount.aimY);
        Bullet b = bullet.create(unit, unit.team, x, y, rot);
        b.aimX = mount.aimX;
        b.aimY = mount.aimY;
        EndLauncherData data = new EndLauncherData();
        data.mount = mount;
        data.ret = Mathf.random(5f);
        b.data = data;

        shootSound.at(x, y, Mathf.random(soundPitchMin, soundPitchMax));
        bullet.shootEffect.at(x, y, rot, bullet.hitColor, unit);
        Effect.shake(shake, shake, x, y);
    }

    @Override
    public void draw(Unit unit, WeaponMount mount){
    }

    @Override
    public void drawOutline(Unit unit, WeaponMount mount){
        drawBase(unit, mount);
    }

    Vec3 getShootPosition(Unit unit, WeaponMount mount, int idx, boolean flip){
        float x = unit.x, y = unit.y, rot = unit.rotation;
        float ang = -41f;
        float warm = mount.warmup;
        int i = idx * 3;
        int sign = flip ? -1 : 1;
        float tx = targets[i], ty = targets[i + 1], tr = targets[i + 2];
        float srot = rot + (ang + tr * warm) * sign;

        Vec2 v2 = Tmp.v2.trns(unit.rotation - 90f, (60f + tx * warm) * sign, -65f + ty * warm).add(x, y);
        Tmp.v3.trns(srot, 43f * 2f);
        v2.add(Tmp.v3);

        return Tmp.v31.set(v2.x, v2.y, srot);
    }

    void drawBase(Unit unit, WeaponMount mount){
        int count = targets.length;

        float x = unit.x, y = unit.y, rot = unit.rotation;
        float ang = -41f;
        float warm = mount.warmup;
        unit.type.applyColor(unit);
        for(int i = 0; i < count; i += 3){
            float tx = targets[i], ty = targets[i + 1], tr = targets[i + 2];
            float lxs = Draw.xscl;
            for(int sign : Mathf.signs){
                Vec2 v2 = Tmp.v2.trns(unit.rotation - 90f, (60f + tx * warm) * sign, -65f + ty * warm);

                Draw.xscl = sign;
                drawWeapon(x + v2.x, y + v2.y, rot + (ang + tr * warm) * sign);
            }
            Draw.xscl = lxs;
        }
        Draw.reset();
    }
    void drawWeapon(float x, float y, float rotation){
        Vec2 v = Tmp.v1.trns(rotation, 39f).add(x, y);
        TextureRegion reg = region;
        Draw.rect(reg, v.x, v.y, rotation - 90f);
    }

    public static class EndLauncherMount extends WeaponMount{
        public Seq<Teamc> targetSeq = new Seq<>();
        public ObjectFloatMap<Teamc> targets = new ObjectFloatMap<>();
        int burstCount = 0;
        float burstTime = 0f;

        EndLauncherMount(Weapon weapon){
            super(weapon);
        }

        void updateTarget(){
            targetSeq.removeAll(t -> {
                float v = targets.increment(t, 0f, -Time.delta);
                boolean re = v <= 0f || (t instanceof Healthc h && !h.isValid());
                if(re) targets.remove(t, 0f);
                return re;
            });
        }

        public void removeTarget(Teamc target){
            targets.remove(target, 0f);
            targetSeq.remove(target);
        }

        public void addTarget(Teamc target){
            if(!targets.containsKey(target)){
                targetSeq.add(target);
            }
            targets.put(target, 15f);
        }
    }

    public static class EndLauncherData{
        public EndLauncherMount mount;
        public Teamc current;
        public float ret;
    }
}
