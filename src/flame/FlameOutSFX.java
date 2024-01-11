package flame;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.Pool.*;
import flame.Utils.*;
import flame.effects.*;
import flame.graphics.*;
import flame.special.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.entities.bullet.*;
import mindustry.entities.pattern.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static mindustry.Vars.*;
import static arc.Core.*;

public class FlameOutSFX implements ApplicationListener{
    public static FlameOutSFX inst;

    private final BasicPool<LockMovement> lockPool = new BasicPool<>(LockMovement::new);
    private final Seq<LockMovement> locks = new Seq<>();
    private final IntMap<LockMovement> lockMap = new IntMap<>();

    private static final Seq<HealPrevention<?>> hpSeq = new Seq<>();
    private static final IntMap<HealPrevention<?>> hpMap = new IntMap<>();
    private static final BasicPool<UnitHealPrevention> uhpPool = new BasicPool<>(UnitHealPrevention::new);
    private static final BasicPool<BuildingHealPrevention> bhpPool = new BasicPool<>(BuildingHealPrevention::new);

    private static float[] bulletDps, unitDps;

    float impFrameTime = 0f;
    Seq<ImpactFrameDrawer> impFrameEntities = new Seq<>();
    BasicPool<ImpactFrameDrawer> impFramePool = new BasicPool<>(ImpactFrameDrawer::new);
    Floatp trueDelta = () -> {
        float result = Core.graphics.getDeltaTime() * 60f;
        return (Float.isNaN(result) || Float.isInfinite(result)) ? 1f : Mathf.clamp(result, 0.0001f, 60f / 10f);
    };
    public FrameBuffer buffer;
    public static float timeDelta = 1f;
    public static float realDelta = 1f;

    FloatSeq blackHoleQueue = new FloatSeq();

    protected FlameOutSFX(){
        if(Vars.platform instanceof ApplicationCore core){
            core.add(this);
        }
        Events.on(EventType.ResetEvent.class, e -> {
            locks.clear();
            lockMap.clear();
            EmpathyDamage.reset();
        });
        Events.on(EventType.ContentInitEvent.class, e -> Core.app.post(() -> {
            Seq<BulletType> bullets = content.bullets();
            Seq<UnitType> units = content.units();
            bulletDps = new float[bullets.size];
            unitDps = new float[units.size];
            for(BulletType b : bullets){
                updateBullet(b);
            }
            for(UnitType u : units){
                updateUnit(u);
            }
            EmpathyDamage.initContent();
        }));
        //SpecialDeathEffects.load();
        Events.run(Trigger.draw, this::draw);
        inst = this;
    }

    public float getUnitDps(UnitType unit){
        if(unit.id >= unitDps.length) return 0f;
        return unitDps[unit.id];
    }
    public float getBulletDps(BulletType bullet){
        if(bullet.id >= bulletDps.length) return 0f;
        return bulletDps[bullet.id];
    }

    float updateUnit(UnitType unit){
        if(unitDps[unit.id] == 0f){
            unitDps[unit.id] = 0.000001f;
            float damage = 0f;
            for(Weapon w : unit.weapons){
                ShootPattern p = w.shoot;
                float d;
                if(!w.shootOnDeath && !w.bullet.killShooter){
                    d = (updateBullet(w.bullet) * p.shots * (w.continuous ? w.bullet.lifetime / 5f : 1f)) / w.reload;
                }else{
                    d = updateBullet(w.bullet) * p.shots;
                }
                damage += d + (Mathf.pow(unit.hitSize, 0.75f) * unit.crashDamageMultiplier);
            }
            unitDps[unit.id] = damage;
        }
        return unitDps[unit.id];
    }
    float updateBullet(BulletType type){
        if(bulletDps[type.id] == 0f){
            //recursion
            bulletDps[type.id] = type.damage;
            float damage = type.damage + type.splashDamage;

            if(type.fragBullet != null) damage += type.fragBullets * updateBullet(type.fragBullet);
            if(type.lightning > 0){
                damage += type.lightning * Mathf.pow(type.lightningLength, 0.75f) * Math.max(type.lightningType != null ? updateBullet(type.lightningType) : 0f, type.lightningDamage);
            }
            if(type.intervalBullet != null){
                damage += (updateBullet(type.intervalBullet) * type.intervalBullets) / type.bulletInterval;
            }
            //if(type.instantDisappear)
            if(type.spawnUnit != null){
                damage += updateUnit(type.spawnUnit);
            }
            if(type.despawnUnit != null){
                damage += updateUnit(type.despawnUnit) * type.despawnUnitCount;
            }
            bulletDps[type.id] = Math.max(0.00001f, damage);
        }
        return bulletDps[type.id];
    }

    void loadHeadless(){
        //blocksTexture = Texture.createEmpty(Tex)
        buffer = new FrameBuffer(2, 2);
    }
    void draw(){
        buffer.resize(graphics.getWidth(), graphics.getHeight());
        EmpathyDamage.draw();

        SpecialMain.draw();

        //drawOrderPortal(700f, 700f, 50f + Mathf.absin(15, 30f), 300f + Mathf.absin(30, 30f));

        if(!blackHoleQueue.isEmpty()){
            FlameShaders.blackholeShader.holes.addAll(blackHoleQueue);
            Draw.draw(Layer.floor - 1f, () -> buffer.begin(Color.clear));
            Draw.draw(Layer.blockOver + 0.1f, () -> {
                buffer.end();
                buffer.blit(FlameShaders.blackholeShader);
                FlameShaders.blackholeShader.holes.clear();
            });
            blackHoleQueue.clear();
        }

        if(impFrameTime > 0f && !impFrameEntities.isEmpty()){
            Draw.draw(Layer.end - 1, () -> {
                ImpactBatch.beginSwap();
                Draw.rect();
                Draw.flush();
                
                ImpactBatch.batch.setWhite(true);
                FlameShaders.harshShadow.clear();
                for(ImpactFrameDrawer impact : impFrameEntities){
                    float x = impact.x;
                    float y = impact.y;
                    FlameShaders.harshShadow.addLight(x, y, impact.intensity);

                    int ints = (int)(impact.intensity) + 8;
                    for(int i = 0; i < ints; i++){
                        float angle = impact.directional ? impact.rotation + (Interp.pow2In.apply(Mathf.random(1f)) * 180f * Mathf.randomSign()) : Mathf.random(360f);
                        float adst = impact.directional ? (Interp.pow2In.apply((180f - Angles.angleDist(impact.rotation, angle)) / 180f) * 0.75f + 0.25f) : 1f;
                        float range = impact.intensity * 110f * Mathf.random(0.5f, 1f) * adst;
                        float width = impact.intensity * 2f * Mathf.random(0.5f, 1f);

                        Drawf.tri(x, y, width, range, angle);
                        Drawf.tri(x, y, width, width * 2f, angle + 180f);
                    }
                }
                ImpactBatch.batch.setWhite(false);
                Draw.flush();
                
                //buffer.resize(graphics.getWidth(), graphics.getHeight());

                //please make block shadows public Anuke.
                boolean last = state.rules.lighting;
                boolean shield = renderer.animateShields;
                state.rules.lighting = false;
                renderer.animateShields = false;
                buffer.begin(Color.clear);
                Draw.shader(FlameShaders.alphaCut);
                ImpactBatch.batch.canChangeShader = false;
                ImpactBatch.batch.useColor = true;

                //reduces fps to -10. breaks rendering
                //renderer.blocks.floor.drawLayer(CacheLayer.walls);
                renderer.blocks.drawBlocks();
                
                ImpactBatch.batch.canChangeShader = true;
                ImpactBatch.batch.useColor = false;
                Draw.shader();
                
                buffer.end();
                buffer.blit(FlameShaders.harshShadow);

                //FlameShaders.harshShadow.addLight(800, 600, 12f);
                //FlameShaders.impactShader.addLight(player.x + 20f, player.y + 110f, 5f);
                //FlameShaders.impactShader.addLight(player.x, player.y, 20f);

                buffer.begin(Color.clear);

                Draw.shader(FlameShaders.alphaCut);
                ImpactBatch.batch.canChangeShader = false;
                ImpactBatch.batch.useColor = true;
                //Draw.rect(UnitTypes.eclipse.region, player.x, player.y);
                //Draw.rect(UnitTypes.eclipse.region, player.x, player.y + 110f);
                //Draw.rect(UnitTypes.eclipse.region, player.x, player.y - 110f, 90f + Time.time);
                Groups.unit.draw(Unitc::draw);

                ImpactBatch.batch.canChangeShader = true;
                ImpactBatch.batch.useColor = false;
                Draw.shader();

                buffer.end();
                buffer.blit(FlameShaders.harshShadow);
                
                state.rules.lighting = last;
                renderer.animateShields = shield;

                ImpactBatch.batch.setWhite(true);
                Draw.flush();
                for(ImpactFrameDrawer impact : impFrameEntities){
                    if(impact.draw != null) impact.draw.draw();
                    if(impact.run != null) impact.run.run();
                }
                Draw.flush();
                ImpactBatch.batch.setWhite(false);

                ImpactBatch.endSwap();
            });
        }
    }

    public void drawOrderPortal(float x, float y, float width, float height){
        Draw.draw(Layer.flyingUnitLow - 0.5f, () -> {
            TextureRegion r = EmpathyRegions.portal;
            FlameShaders.orderShader.region = r;
            FlameShaders.orderShader.width = width;
            FlameShaders.orderShader.height = height;
            FlameShaders.orderShader.srcX = x;
            FlameShaders.orderShader.srcY = y;

            Draw.shader(FlameShaders.orderShader);
            Draw.rect(r, x, y, width, height);
            Draw.shader();
        });
    }
    public void drawChaosPortal(float x, float y, float width, float height){
        Draw.draw(Layer.flyingUnitLow - 0.5f, () -> {
            TextureRegion r = EmpathyRegions.portal;
            FlameShaders.chaosShader.region = r;
            FlameShaders.chaosShader.width = width;
            FlameShaders.chaosShader.height = height;
            FlameShaders.chaosShader.srcX = x;
            FlameShaders.chaosShader.srcY = y;

            //Draw.flush();
            Draw.shader(FlameShaders.chaosShader);
            Draw.rect(r, x, y, width, height);
            //Draw.flush();
            Draw.shader();
        });
    }

    public void blackHole(float x, float y, float strength, float rotation){
        blackHoleQueue.add(x, y, strength, rotation);
    }

    public void impactFrames(float x, float y, float rotation, float intensity, boolean directional, Runnable draw){
        ImpactFrameDrawer i = impFramePool.obtain();
        i.run = draw;
        i.intensity = intensity;
        i.x = x;
        i.y = y;
        i.rotation = rotation;
        i.directional = directional;

        impFrameEntities.add(i);
        impFrameTime = Math.max(impFrameTime, 6f);
    }
    public void impactFrames(Drawc d, float x, float y, float rotation, float intensity, boolean directional){
        ImpactFrameDrawer i = impFramePool.obtain();
        i.draw = d;
        i.intensity = intensity;
        i.x = x;
        i.y = y;
        i.rotation = rotation;
        i.directional = directional;

        impFrameEntities.add(i);
        impFrameTime = Math.max(impFrameTime, 6f);
        //impFrameTime = Math.max(impFrameTime, 120f);
    }

    public void cancelMovementLock(Unit unit){
        LockMovement l = lockMap.get(unit.id);
        if(l != null){
            locks.remove(l);
            lockMap.remove(unit.id);
            lockPool.free(l);
        }
    }

    public void addMovementLock(Unit unit, float duration){
        LockMovement l = lockMap.get(unit.id);
        if(l == null){
            l = lockPool.obtain();
            l.u = unit;
            l.id = unit.id;
            l.x = unit.x;
            l.y = unit.y;
            l.time = duration;
            locks.add(l);
            lockMap.put(unit.id, l);
        }else{
            l.time = Math.max(l.time, duration);
        }
    }

    public void lockHealing(Healthc h, float health, float duration){
        HealPrevention<?> a = hpMap.get(h.id());
        if(a != null){
            a.update(duration, health);
        }else{
            if(h instanceof Unit u){
                UnitHealPrevention uh = uhpPool.obtain();
                uh.set(u, duration, health);
                hpMap.put(h.id(), uh);
                hpSeq.add(uh);
            }else if(h instanceof Building b){
                BuildingHealPrevention bh = bhpPool.obtain();
                bh.set(b, duration, health);
                hpMap.put(h.id(), bh);
                hpSeq.add(bh);
            }
        }
    }

    @Override
    public void update(){
        timeDelta = Math.max(trueDelta.get(), Time.delta);

        SpecialMain.update();

        if(Vars.state.isPaused()) return;
        locks.removeAll(l -> {
            l.u.x = Mathf.lerpDelta(l.u.x, l.x, 0.9f);
            l.u.y = Mathf.lerpDelta(l.u.y, l.y, 0.9f);

            l.time -= Time.delta;
            if(l.time <= 0f){
                lockMap.remove(l.id);
                lockPool.free(l);
            }
            return l.time <= 0f;
        });
        hpSeq.removeAll(hp -> {
            hp.update();
            boolean v = hp.duration <= 0f || !hp.e.dead();
            if(v) hp.remove();
            return v;
        });
        if(impFrameTime > 0){
            impFrameTime -= Time.delta;
            if(impFrameTime <= 0f){
                for(ImpactFrameDrawer imf :impFrameEntities){
                    impFramePool.free(imf);
                }
                impFrameEntities.clear();
            }
        }
        EmpathyDamage.update();
        Severation.updateStatic();
    }

    static class BuildingHealPrevention extends HealPrevention<Building>{
        @Override
        void update(){
            float h = e.health;
            lastHealth = Math.min(lastHealth, h);
            e.health = lastHealth;
            duration -= Time.delta;
        }

        @Override
        void remove(){
            super.remove();
            bhpPool.free(this);
        }
    }
    static class UnitHealPrevention extends HealPrevention<Unit>{
        @Override
        void update(){
            float h = e.health;
            lastHealth = Math.min(lastHealth, h);
            e.health = lastHealth;
            duration -= Time.delta;
        }

        @Override
        void remove(){
            super.remove();
            uhpPool.free(this);
        }
    }
    static abstract class HealPrevention<T extends Healthc>{
        T e;
        float duration;
        float lastHealth;

        abstract void update();
        void update(float duration, float lastHealth){
            this.duration = Math.max(this.duration, duration);
            this.lastHealth = Math.min(this.lastHealth, lastHealth);
        }
        void remove(){
            hpMap.remove(e.id());
        }
        void set(T t, float duration, float lastHealth){
            e = t;
            this.duration = duration;
            this.lastHealth = lastHealth;
        }
    }

    static class LockMovement{
        Unit u;
        int id;
        float x, y;
        float time = 0f;
    }
    static class ImpactFrameDrawer implements Poolable{
        Drawc draw;
        Runnable run;
        float x, y, rotation;
        boolean directional;
        float intensity;

        @Override
        public void reset(){
            draw = null;
            run = null;
            directional = false;
            rotation = intensity = x = y = 0;
        }
    }
}
