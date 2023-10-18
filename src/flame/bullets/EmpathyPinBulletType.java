package flame.bullets;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.graphics.*;
import flame.unit.empathy.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;

public class EmpathyPinBulletType extends BulletType{
    TextureRegion region;

    public EmpathyPinBulletType(){
        super(2f, 800f);
        lifetime = 120f;
        collides = collidesTiles = false;
        pierce = true;
        absorbable = false;
        hittable = false;
        keepVelocity = false;
        despawnEffect = Fx.none;
        shootEffect = Fx.none;
        hitEffect = Fx.hitBulletColor;
        hitColor = FlamePal.empathy;
        drawSize = 40f * 1.5f;
    }

    @Override
    public void load(){
        region = Core.atlas.find("flameout-heart-pin");
    }

    @Override
    public void update(Bullet b){
        b.vel.setLength(Mathf.lerp(2f, 16f, Mathf.clamp(b.time / 60f)));
        if(b.timer(0, 5f)){
            float length = 40 / 2f;
            float dam = 700f;
            Vec2 v = Tmp.v1.trns(b.rotation(), length);
            Utils.hitLaser(b.team, 2f, -v.x + b.x, -v.y + b.y, v.x + b.x, v.y + b.y, null, h -> false, (h, x, y) -> {
                hit(b, x, y);
                boolean lethal = h.maxHealth() < dam * 2f;
                float tdam = Math.max(dam, h.maxHealth() / 500f);
                if(h instanceof Unit u){
                    EmpathyDamage.damageUnit(u, tdam, lethal, null);
                }else if(h instanceof Building build){
                    EmpathyDamage.damageBuilding(build, tdam, true, null);
                }
            });
        }
    }

    @Override
    public void draw(Bullet b){
        float out = Mathf.clamp((b.lifetime - b.time) / 16f);
        float out2 = 1 - Mathf.clamp(b.time / 6f);
        Draw.color(FlamePal.empathyAdd);
        Draw.alpha(out);
        Draw.blend(Blending.additive);
        if(out2 > 0.001f){
            GraphicUtils.diamond(b.x, b.y, 5f * out2, 40f, b.rotation());
        }
        Draw.rect(region, b.x, b.y, b.rotation() - 90f);
        Draw.blend();
    }

    @Override
    public void drawLight(Bullet b){

    }
}
