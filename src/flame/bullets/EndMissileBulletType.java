package flame.bullets;

import arc.math.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import flame.unit.empathy.*;
import flame.unit.weapons.EndLauncherWeapon.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;

public class EndMissileBulletType extends BasicBulletType{
    public EndMissileBulletType(){
        super(14f, 1000f, "missile-large");
        backColor = trailColor = hitColor = FlamePal.red;
        frontColor = FlamePal.red.cpy().mul(2f);

        shrinkY = 0f;
        width = 9f;
        height = 22f;

        trailLength = 9;
        trailWidth = 3f;

        lifetime = 90f;

        despawnHit = true;

        hitEffect = Fx.none;
        despawnEffect = Fx.none;
    }

    @Override
    public void updateHoming(Bullet b){
        if(!(b.data instanceof EndLauncherData data)) return;
        data.ret -= Time.delta;
        if(data.ret <= 0f){
            Teamc target = Units.closestTarget(b.team, b.x, b.y, 520f, u -> !data.mount.targets.containsKey(u), bl -> !data.mount.targets.containsKey(bl));
            if(target == null) target = Units.closestTarget(b.team, b.x, b.y, 520f, u -> true, bl -> true);

            if(target instanceof Building bl){
                b.aimTile = bl.tile;
            }else{
                b.aimTile = null;
            }

            if(data.current != null && target != null){
                data.mount.removeTarget(data.current);
            }
            data.current = target;
            data.ret = target == null ? 5f : 17f;

            if(data.current != null){
                data.mount.addTarget(target);
            }
        }
        if(data.current != null && Units.invalidateTarget(data.current, b.team, b.x, b.y, 530f)){
            data.mount.removeTarget(data.current);
            data.current = null;
        }

        if(data.current != null){
            b.aimX = data.current.x();
            b.aimY = data.current.y();
        }else{
            b.aimX = data.mount.aimX;
            b.aimY = data.mount.aimY;
        }

        float ang = Angles.angle(b.x, b.y, b.aimX, b.aimY);

        b.vel.setAngle(Angles.moveToward(b.rotation(), ang, (10f + b.fin() * 8f) * (1f + Mathf.absin(b.time, 4f, 0.5f)) * Interp.pow2.apply(Mathf.clamp(b.time / 30f)) * Time.delta));
    }

    @Override
    public void hit(Bullet b, float x, float y){
        super.hit(b, x, y);

        FlameFX.desMissileHit.at(x, y, b.rotation());

        Utils.scanEnemies(b.team, x, y, 40f, true, true, t -> {
            float dam = Mathf.chance(0.333f) ? 1000f : 200f;
            if(t instanceof Unit u){
                EmpathyDamage.damageUnit(u, dam + u.maxHealth / 110f, true, () -> SpecialDeathEffects.get(u.type).deathUnit(u, x, y, Angles.angle(x, y, u.x, u.y)));
            }else if(t instanceof Building bl){
                EmpathyDamage.damageBuilding(bl, dam + bl.maxHealth / 110f, true, () -> SpecialDeathEffects.get(bl.block).deathBuilding(bl, x, y, Angles.angle(x, y, bl.x, bl.y)));
            }
        });
    }

    @Override
    public void removed(Bullet b){
        super.removed(b);

        if(!(b.data instanceof EndLauncherData data)) return;
        if(data.current != null) data.mount.removeTarget(data.current);
    }
}
