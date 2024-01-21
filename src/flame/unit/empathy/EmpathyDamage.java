package flame.unit.empathy;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import flame.effects.*;
import flame.special.*;
import flame.unit.*;
import mindustry.*;
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.Turret.*;
import mindustry.world.blocks.storage.CoreBlock.*;

public class EmpathyDamage{
    final static Seq<AbsoluteDamage<?>> damages = new Seq<>();
    final static IntMap<AbsoluteDamage<?>> damageMap = new IntMap<>();
    final static Seq<EmpathyHolder> units = new Seq<>();
    final static IntMap<EmpathyHolder> empathyMap = new IntMap<>();
    final static IntSet exclude = new IntSet();
    final static Seq<Unit> excludeSeq = new Seq<>(), queueExcludeRemoval = new Seq<>();

    final static Seq<Entityc> toRemove = new Seq<>();
    static int[][] addArr = new int[2][0];
    static int staggerIdx = 0;
    static IntSet addedSet = new IntSet(256);
    private static float scanTimer = 0f;
    private static boolean activeAdd = true;

    private static EmpathySpawner spawner = null;
    private final static Seq<EmpathyDeath> deaths = new Seq<>();

    public static void reset(){
        damageMap.clear();
        damages.clear();
        toRemove.clear();

        addedSet.clear();
        deaths.clear();

        if(spawner != null){
            spawner.time = 0f;
            spawner.active = false;
            spawner.shouldSpawn = true;

            if(!units.isEmpty()){
                EmpathyUnit u = units.first().unit;
                spawner.health = u.getTrueHealth();
                spawner.countDown = u.getCountDown();
            }
        }
        
        units.clear();
        empathyMap.clear();
        exclude.clear();
        excludeSeq.clear();
        queueExcludeRemoval.clear();
    }
    public static void worldLoad(){
        if(spawner != null && spawner.shouldSpawn){
            spawner.reactivateTime = 5f * 60f;
            spawner.timeScl++;
            spawner.shouldSpawn = false;
        }
    }

    public static void initContent(){
        //addArr = new int[2];
        ContentLoader con = Vars.content;
        Seq<UnitType> units = con.units();
        Seq<Block> blocks = con.blocks();

        addArr[0] = new int[units.size];
        addArr[1] = new int[blocks.size];
    }

    public static void exclude(Unit unit){
        if(!SpecialMain.validEmpathySpawn()) return;
        if(exclude.add(unit.id)){
            excludeSeq.add(unit);
        }
    }
    public static void removeExclude(Unit unit){
        //exclude.remove(unit.id);
        //excludeSeq.remove(unit);
        queueExcludeRemoval.add(unit);
    }
    public static boolean containsExclude(int id){
        return exclude.contains(id);
    }

    static void clearExclude(){
        exclude.clear();
        excludeSeq.clear();
        queueExcludeRemoval.clear();
    }

    static void empathyDeath(float x, float y, float rotation){
        EmpathyDeath d = new EmpathyDeath();
        d.x = x;
        d.y = y;
        d.rotation = rotation;

        if(spawner != null){
            spawner.disabled = true;
        }

        deaths.add(d);
    }

    public static void spawnEmpathy(float x, float y){
        if(spawner == null && SpecialMain.validEmpathySpawn()){
            EmpathySpawner s = new EmpathySpawner();
            s.x = x;
            s.y = y;
            s.active = true;
            s.shouldSpawn = true;
            spawner = s;
        }
    }

    public static void draw(){
        if(spawner != null){
            if(spawner.active){
                //Log.info(spawner.time);
                spawner.draw();
            }
        }
        if(!deaths.isEmpty()){
            for(EmpathyDeath d : deaths){
                d.draw();
            }
        }

        for(EmpathyHolder u : units){
            if(u.removeCount >= 5){
                u.unit.draw();
            }
        }
    }

    public static void update(){
        if(spawner != null && (spawner.active || spawner.reactivateTime > 0f)){
            spawner.update();
        }
        if(FlameUnitTypes.empathy != null){
            if(spawner == null || !spawner.spawned){
                FlameUnitTypes.empathy.hidden = true;
            }else if(FlameUnitTypes.empathy.hidden){
                FlameUnitTypes.empathy.hidden = false;
            }
        }
        if(!deaths.isEmpty()){
            deaths.removeAll(d -> {
                d.update();
                return d.time >= EmpathyDeath.duration;
            });
        }

        if((scanTimer -= Time.delta) <= 0f){
            scanTimer = 15f;
            addedSet.clear();
            for(EmpathyHolder u : units){
                u.added = false;
            }

            for(Entityc e : Groups.all){
                for(EmpathyHolder u : units){
                    if(u.unit == e) u.added = true;
                }
                if(damageMap.containsKey(e.id())){
                    AbsoluteDamage<?> ad = damageMap.get(e.id());
                    if(ad.dead){
                        ad.addCount++;
                        ad.maximizeAdditions();
                        ad.dead = false;
                        if(ad.addCount >= 4) ad.purgatory = true;
                    }
                }
                addedSet.add(e.id());
            }

            for(EmpathyHolder u : units){
                if(!u.added){
                    u.removeCount++;
                    switch(u.removeCount){
                        case 1, 2 -> u.unit.add();
                        case 3, 4 -> {
                            activeAdd = false;
                            EmpathyUnit en = u.unit.duplicate();
                            empathyMap.remove(u.unit.id);
                            empathyMap.put(en.id, u);
                            u.unit = en;
                            activeAdd = true;
                        }
                    }
                }
            }
            int dsize = Groups.draw.size();
            for(int i = (staggerIdx % 3); i < dsize; i += 3){
                Drawc d = Groups.draw.index(i);
                if(!addedSet.contains(d.id())){
                    Groups.all.add(d);
                }
            }
            staggerIdx++;
        }

        if(units.size > 0){
            for(EmpathyHolder u : units){
                if(u.removeCount >= 5){
                    u.unit.update();
                }
            }

            for(TeamData data : Vars.state.teams.present){
                data.plans.clear();
            }
        }

        damages.removeAll(ad -> {
            ad.update();
            int ac = addArr[ad.getType()][ad.getId()];
            if(ad.reAdded() && (ac != -1)){
                ad.forgetTime = 0f;
            }else{
                ad.forgetTime += Time.delta;
            }
            float time = ac != -1 ? 15f * 60f : 60f * 4f;
            boolean re = ad.forgetTime > time && !ad.purgatory;
            if(re){
                if(ad.addCount <= 0){
                    addArr[ad.getType()][ad.getId()] = -1;
                }
                damageMap.remove(ad.id);
            }
            return re;
        });
        if(!queueExcludeRemoval.isEmpty()){
            for(Unit u : queueExcludeRemoval){
                exclude.remove(u.id);
                excludeSeq.remove(u);
            }
            queueExcludeRemoval.clear();
        }
    }

    static boolean targeted(Teamc t){
        return units.contains(eu -> eu.unit.getTarget() == t);
    }

    public static boolean isNaNInfinite(float ...fields){
        for(float v : fields){
            if(Float.isNaN(v) || Float.isInfinite(v) || v >= Float.MAX_VALUE) return true;
        }
        return false;
    }

    static void addEmpathy(EmpathyUnit unit){
        if(activeAdd && !empathyMap.containsKey(unit.id)){
            EmpathyHolder h = new EmpathyHolder();
            h.unit = unit;
            units.add(h);
            empathyMap.put(unit.id, h);
        }
    }
    static void removeEmpathy(EmpathyUnit unit){
        if(!activeAdd) return;
        units.remove(h -> h.unit == unit);
        empathyMap.remove(unit.id);
    }
    static void onDuplicate(EmpathyUnit last, EmpathyUnit latest){
        if(!activeAdd) return;
        EmpathyHolder h = empathyMap.get(last.id);
        if(h == null) h = new EmpathyHolder();
        empathyMap.remove(last.id);
        h.unit = latest;
        empathyMap.put(latest.id, h);
    }

    public static float getHealth(Teamc h){
        AbsoluteDamage<?> ad = damageMap.get(h.id());
        if(ad != null){
            return ad.getHealthFract();
        }
        return Float.NaN;
    }

    public static void nanLock(Unit unit, float x, float y){
        AbsoluteDamage<?> ad = damageMap.get(unit.id);
        if(ad != null){
            if(ad instanceof UnitAbsoluteDamage ud && ud.nanTime <= 0f){
                ud.nanTime = 5f;
                ud.nanX = x;
                ud.nanY = y;
            }
        }
    }

    public static void damageUnitKnockback(Unit unit, float damage, float vx, float vy, Runnable onDeath){
        if(exclude.contains(unit.id) || empathyMap.containsKey(unit.id)){
            unit.damagePierce(damage);
            unit.impulse(vx, vy);
            return;
        }

        AbsoluteDamage<?> ad = damageMap.get(unit.id);
        if(ad != null){
            ad.damage(damage, true, onDeath);
            ad.forgetTime = 0f;

            if(ad instanceof UnitAbsoluteDamage ud){
                ud.vx = vx;
                ud.vy = vy;

                if(ud.knockbackTime <= 0f && unit instanceof Legsc leg){
                    ud.legMove = leg.totalLength();
                }

                ud.knockbackTime = 60f;
            }
        }else{
            UnitAbsoluteDamage ud = new UnitAbsoluteDamage();
            ud.entity = unit;
            ud.id = unit.id;
            ud.health = !isNaNInfinite(unit.health) ? unit.health : 10000f;
            ud.vx = vx;
            ud.vy = vy;
            ud.knockbackTime = 60f;

            if(unit instanceof Legsc leg){
                ud.legMove = leg.totalLength();
            }

            ud.damage(damage, true, onDeath);

            damageMap.put(unit.id, ud);
            damages.add(ud);
        }
    }

    public static void damageUnit(Unit unit, float damage, boolean lethal, Runnable onDeath){
        if(exclude.contains(unit.id) || empathyMap.containsKey(unit.id)){
            unit.damagePierce(damage);
            return;
        }
        AbsoluteDamage<?> ad = damageMap.get(unit.id);
        if(ad != null){
            ad.damage(damage, lethal, onDeath);
            ad.forgetTime = 0f;
        }else{
            UnitAbsoluteDamage ud = new UnitAbsoluteDamage();
            ud.entity = unit;
            ud.id = unit.id;
            //ud.health = !(Float.isNaN(unit.health) || Float.isInfinite(unit.health)) ? unit.health : 10000f;
            ud.health = !isNaNInfinite(unit.health) ? unit.health : 10000f;

            ud.damage(damage, lethal, onDeath);

            damageMap.put(unit.id, ud);
            damages.add(ud);
        }
    }
    public static void damageBuilding(Building build, float damage, boolean lethal, Runnable onDeath){
        if(build instanceof CoreBuild && !targeted(build)) return;

        damageBuildingRaw(build, damage, lethal, onDeath);
    }
    public static void damageBuildingRaw(Building build, float damage, boolean lethal, Runnable onDeath){
        AbsoluteDamage<?> ad = damageMap.get(build.id);
        if(ad != null){
            ad.damage(damage, lethal, onDeath);
            ad.forgetTime = 0f;
        }else{
            BuildingAbsoluteDamage bd = new BuildingAbsoluteDamage();
            bd.entity = build;
            bd.id = build.id;
            bd.health = !isNaNInfinite(build.health) ? build.health : 10000f;

            bd.damage(damage, lethal, onDeath);

            damageMap.put(build.id, bd);
            damages.add(bd);
        }
    }

    static void annihilate(Entityc entity, boolean setNaN){
        Groups.all.remove(entity);
        if(entity instanceof Drawc d) Groups.draw.remove(d);
        if(entity instanceof Syncc s) Groups.sync.remove(s);

        if(entity instanceof Unit unit){
            try{
                ReflectUtils.findField(unit.getClass(), "added").setBoolean(unit, false);
            }catch(Exception e){
                Log.err(e);
            }

            if(setNaN){
                unit.x = unit.y = unit.rotation = Float.NaN;
                unit.vel.x = unit.vel.y = Float.NaN;
                for(WeaponMount mount : unit.mounts){
                    mount.reload = Float.NaN;
                }
            }

            unit.team.data().updateCount(unit.type, -1);
            unit.controller().removed(unit);

            Groups.unit.remove(unit);

            for(WeaponMount mount : unit.mounts){
                if(mount.bullet != null){
                    mount.bullet.time = mount.bullet.lifetime;
                    mount.bullet = null;
                }
                if(mount.sound != null){
                    mount.sound.stop();
                }
            }
        }
        if(entity instanceof Building building){
            Groups.build.remove(building);
            building.tile.remove();
            if(setNaN){
                building.x = building.y = Float.NaN;
            }
            if(building instanceof TurretBuild tb){
                tb.ammo.clear();
                if(setNaN){
                    tb.rotation = Float.NaN;
                    tb.reloadCounter = Float.NaN;
                }
            }

            //if(building.sound != null) building.sound.stop();
            try{
                ReflectUtils.findField(building.getClass(), "added").setBoolean(building, false);

                SoundLoop sl = (SoundLoop)ReflectUtils.findField(building.getClass(), "sound").get(building);
                if(sl != null){
                    sl.stop();
                }
            }catch(Exception e){
                Log.err(e);
            }
        }
        if(entity instanceof Bullet bullet){
            Groups.bullet.remove(bullet);

            try{
                ReflectUtils.findField(bullet.getClass(), "added").setBoolean(bullet, false);
            }catch(Exception e){
                Log.err(e);
            }
        }
        if(entity instanceof Pool.Poolable p) Groups.queueFree(p);
    }

    static void handleAdditions(int start, Entityc exclude, Entityc exclude2, Seq<Building> proxy){
        toRemove.clear();
        int size = Groups.all.size();
        for(int i = start; i < size; i++){
            Entityc e = Groups.all.index(i);
            if(e != exclude && e != exclude2 && (proxy == null || !proxy.contains(b -> e == b)) && !(e instanceof EffectState)) toRemove.add(e);
        }
        
        for(Entityc e : toRemove){
            annihilate(e, false);
        }
        //Log.info("addition handled:" + toRemove.toString());
        toRemove.clear();
    }

    static class EmpathyHolder{
        EmpathyUnit unit;
        int removeCount = 0;
        boolean added = false;
    }

    static class AbsoluteDamage<T extends Healthc>{
        T entity;
        float health;
        int addCount, id;
        float forgetTime, slowdown;
        boolean purgatory, dead;

        Runnable lastRunnable;
        float deathRunTime = 0f;

        void damage(float damage, boolean lethal, Runnable deathEffect){
            if(isNaNInfinite(damage)) damage = health * 10f;
            if(lethal){
                health -= damage;
                if(health > 0) entity.damage(damage);
            }else{
                float minDamage = 5f;
                health = Math.max(minDamage, health - damage);
                if(health <= minDamage){
                    slowdown = Math.max(slowdown, damage);
                }
                //entity.damagePierce(damage);
                //if(entity.health() < 0.1f) entity.health(0.1f);
                entity.damagePierce(Math.min(damage, Math.max(0, entity.health() - minDamage)));
            }
            if(deathEffect != null){
                lastRunnable = deathEffect;
                deathRunTime = 4f;
            }else if(lastRunnable == null){
                setDefaultDeath();
            }
            /*
            if(health <= 0f && !dead){
                dead = true;
                if(deathEffect != null) deathEffect.run();
                removeEntity();
            }
            */
        }

        float getHealthFract(){
            return 1f;
        }
        float getHealth(){
            return 0f;
        }
        void setHealth(){
        }

        int getType(){
            return -1;
        }
        int getId(){
            return -1;
        }
        void maximizeAdditions(){
            addArr[getType()][getId()] = Math.max(addCount, addArr[getType()][getId()]);
        }
        int getAddCount(){
            return addArr[getType()][getId()];
        }

        void setDefaultDeath(){
            lastRunnable = () -> {
                if(entity instanceof Unit u){
                    SpecialDeathEffects eff = SpecialDeathEffects.get(u.type);

                    if(eff.explosionEffect == Fx.none){
                        u.type.deathExplosionEffect.at(u.x, u.y, u.bounds() / 2f / 8f);
                    }else{
                        eff.explosionEffect.at(u.x, u.y, u.hitSize / 2f);
                    }
                    float shake = u.hitSize / 3f;

                    Effect.shake(shake, shake, u);
                    u.type.deathSound.at(u);
                }else if(entity instanceof Building b){
                    SpecialDeathEffects eff = SpecialDeathEffects.get(b.block);

                    b.block.destroySound.at(b);

                    if(eff.explosionEffect == Fx.none){
                        Fx.dynamicExplosion.at(b.x, b.y, (Vars.tilesize * b.block.size) / 2f / 8f);
                    }else{
                        eff.explosionEffect.at(b.x, b.y, b.hitSize() / 2f);
                    }

                    float shake = b.hitSize() / 3f;
                    Effect.shake(shake, shake, b);
                }
            };
        }

        void update(){
            if(getHealth() > health + 10f) forgetTime = 0f;

            health = Math.min(health, getHealth());
            setHealth();

            if(health <= 0f && !dead){
                dead = true;
                if(lastRunnable != null){
                    lastRunnable.run();
                    lastRunnable = null;
                }
                removeEntity();
            }
            
            if(deathRunTime > 0f){
                deathRunTime -= Time.delta;
                if(deathRunTime <= 0f){
                    setDefaultDeath();
                }
            }

            if(purgatory){
                //addCount = Math.max(addCount, 4);
                removeEntity();
            }
            if(slowdown > 0) slowdown -= Time.delta;
        }

        boolean reAdded(){
            return entity.isAdded();
        }
        void removeEntity(){
            int ac = getAddCount();
            boolean setNaN = ac >= 3;
            if(ac <= 0){
                Entityc lastEntity = Groups.all.index(Groups.all.size() - 1);
                int idx = Groups.all.size();
                boolean wasAdded = entity.isAdded();
                entity.remove();
                if((entity instanceof Building bl)){
                    Events.fire(new BlockDestroyEvent(bl.tile));
                    if(bl.tile != Vars.emptyTile) bl.tile.remove();
                    //idx = (Groups.all.size());
                }
                if(!entity.isAdded() && wasAdded){
                    idx--;
                }
                Entityc newEntity = Groups.all.index(Groups.all.size() - 1);
                int newCount = Groups.all.size();

                //TODO fix
                if(newCount > idx && lastEntity != newEntity){
                    handleAdditions(idx, entity, lastEntity, (entity instanceof Building bu) ? bu.proximity : null);
                }
                if(reAdded()){
                    addCount++;
                    maximizeAdditions();
                    removeEntity();
                }
            }else{
                annihilate(entity, setNaN);
            }
        }
    }
    static class UnitAbsoluteDamage extends AbsoluteDamage<Unit>{
        float vx, vy;
        float knockbackTime;
        float legMove = -1f;
        float nanTime = 0f, nanX, nanY;

        void updateNaN(){
            Unit u = entity;
            boolean isNaN = false;
            if(isNaNInfinite(entity.x, entity.y, entity.rotation)){
                u.x = u.lastX = nanX;
                u.y = u.lastY = nanY;
                u.deltaX = u.deltaY = 0;
                if(Float.isNaN(u.rotation) || Float.isInfinite(u.rotation)) u.rotation = Mathf.random(360f);
                u.vel.setZero();
                u.hitSize = Math.max(1f, u.hitSize);
                u.drag = Math.max(u.drag, 0.0001f);
                isNaN = true;
            }
            for(WeaponMount m : u.mounts){
                if(isNaN |= EmpathyDamage.isNaNInfinite(m.rotation, m.targetRotation, m.aimX, m.aimY)){
                    m.rotation = m.targetRotation = m.weapon.baseRotation;
                    m.aimX = nanX;
                    m.aimY = nanY;
                    m.bullet = null;
                }
            }
            if(isNaN) nanTime = 5f;
        }

        @Override
        void update(){
            super.update();

            if(nanTime > 0 || isNaNInfinite(entity.x, entity.y, entity.rotation)){
                int ac = getAddCount();
                boolean setNaN = ac >= 3;
                if(!setNaN){
                    updateNaN();
                }else{
                    removeEntity();
                }
            }

            if(knockbackTime > 0){
                float f = Mathf.clamp(knockbackTime / 20f) * Time.delta;
                updateLegs();
                if(nanTime > 0){
                    nanX += vx * f;
                    nanY += vy * f;
                }
                entity.x += vx * f;
                entity.y += vy * f;
            }
            nanTime -= Time.delta;

            if(slowdown > 0 || knockbackTime > 0){
                float f = Mathf.clamp((slowdown + knockbackTime) / 30f);
                for(WeaponMount m : entity.mounts){
                    //m.reload -= (m.reload * f);
                    m.reload = Math.min(m.weapon.reload, m.reload + (m.weapon.reload - m.reload) * f);
                }
            }
            float lkt = knockbackTime;
            knockbackTime = Math.max(knockbackTime - Time.delta, 0f);
            if(lkt > 0 && knockbackTime <= 0 && entity instanceof Legsc leg){
                leg.totalLength(legMove);
            }
        }

        void updateLegs(){
            if(entity instanceof Legsc leg){
                float limit = entity.type.legLength * 1.25f;

                leg.totalLength(-leg.moveSpace() * 1.25f);
                int i = 0;
                for(Leg l : leg.legs()){
                    //l.base.sub(entity).clampLength(0f, limit).add(entity);
                    Vec2 base = leg.legOffset(Tmp.v5, i).add(entity.x, entity.y);

                    Tmp.v1.set(l.base).sub(base).clampLength(entity.type.legMinLength, limit).add(base);
                    l.base.lerpDelta(Tmp.v1, 0.8f);

                    Tmp.v1.set(l.base).sub(base).clampLength(entity.type.legMinLength/2f, limit/2f).add(base);
                    l.joint.lerpDelta(Tmp.v1, 0.8f);
                    i++;
                }
            }
        }

        @Override
        float getHealthFract(){
            if(isNaNInfinite(entity.maxHealth)) return 0f;
            return health / entity.maxHealth;
        }

        @Override
        float getHealth(){
            float h = entity.health;
            return isNaNInfinite(h) ? 1000 : h;
        }
        @Override
        void setHealth(){
            entity.healTime = 0f;
            entity.health = Math.min(entity.health, health);
        }

        @Override
        int getType(){
            return 0;
        }

        @Override
        int getId(){
            return entity.type.id;
        }
    }
    static class BuildingAbsoluteDamage extends AbsoluteDamage<Building>{
        @Override
        void update(){
            super.update();
            if(slowdown > 0){
                float f = Mathf.clamp(slowdown / 30f);
                if(entity instanceof TurretBuild tb){
                    tb.reloadCounter -= tb.reloadCounter * f;
                }
            }
        }

        @Override
        float getHealthFract(){
            if(isNaNInfinite(entity.maxHealth)) return 0f;
            return health / entity.maxHealth;
        }
        @Override
        float getHealth(){
            float h = entity.health;
            return isNaNInfinite(h) ? 1000 : h;
        }
        @Override
        void setHealth(){
            entity.lastHealTime = 0f;
            entity.health = Math.min(entity.health, health);
        }

        @Override
        boolean reAdded(){
            return entity.tile.block() == entity.block;
        }

        @Override
        int getType(){
            return 1;
        }

        @Override
        int getId(){
            return entity.block.id;
        }
    }
}
