package flame.unit;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import flame.bullets.*;
import flame.effects.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class ApathySentryUnit extends UnitEntity{
    float moveX, moveY, moveTime;
    Teamc target;
    float reload = 0f;
    boolean active;
    ApathyIUnit owner;

    float healDelay = 0f;
    float healFade = 0f;

    void moveSentry(float wx, float wy){
        moveX = wx;
        moveY = wy;
        moveTime = 0f;
    }

    @Override
    public boolean serialize(){
        return false;
    }

    @Override
    public void update(){
        super.update();
        
        if(owner == null || !owner.isValid()){
            destroy();
            return;
        }

        float lx = (moveX - x) * 0.25f * moveTime;
        float ly = (moveY - y) * 0.25f * moveTime;
        moveTime = Mathf.clamp(moveTime + Time.delta / 20f);
        x += lx;
        y += ly;

        if(active){
            if(healDelay > 0){
                rotation = Angles.moveToward(rotation, angleTo(owner), 15f);
                healDelay -= Time.delta;
                healFade = Mathf.clamp(healFade + Time.delta / 12f);
                if(healDelay <= 0f){
                    owner.heal(owner.maxHealth / 20f);
                    reload = 0f;
                }
            }else{
                if(Units.invalidateTarget(target, team, x, y, FlameBullets.sentryLaser.range - 50f)){
                    target = null;
                }

                if(target != null){
                    rotation = Angles.moveToward(rotation, angleTo(target), 15f);
                    if(reload > 0){
                        reload -= Time.delta;
                        if(reload <= 0f){
                            Tmp.v1.trns(rotation, hitSize / 1.5f).add(x, y);
                            Sounds.laser.at(Tmp.v1);
                            FlameBullets.sentryLaser.create(this, Tmp.v1.x, Tmp.v1.y, rotation);
                        }
                    }
                }else{
                    rotation = Angles.moveToward(rotation, angleTo(owner), 15f);
                }
            }
        }
    }

    @Override
    public float clipSize(){
        return 2000f;
    }

    @Override
    public void destroy(){
        super.destroy();
        BloodSplatter.explosion(20, x, y, hitSize / 2, 80f, 35f);
    }

    @Override
    public void draw(){
        float z = Layer.flyingUnitLow;
        TextureRegion r = type.region;
        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));

        type.drawSoftShadow(this);
        //Draw.color(Pal.shadow);
        //Draw.rect(r, x + UnitType.shadowTX, y + UnitType.shadowTY, r.width * Draw.scl * 0.5f, r.height * Draw.scl * 0.5f, rotation - 90f);
        if(healDelay > 0){
            Draw.color(Pal.heal);
            Lines.stroke(3f * healFade * (1f + Mathf.absin(1f, 0.5f)));
            Lines.line(x, y, owner.x, owner.y);
        }

        Draw.z(z);
        Draw.color();
        type.applyColor(this);
        Draw.rect(r, x, y, r.width * Draw.scl * 0.5f, r.height * Draw.scl * 0.5f, rotation - 90f);
        Draw.color();
        Draw.mixcol();
    }
}
