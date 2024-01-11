package flame.bullets;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import flame.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import java.util.*;

public class EndFlameBulletType extends BulletType{
    Color[] colors = {Color.white, FlamePal.red, FlamePal.red.cpy().mul(0.75f), Color.gray};
    Color[] smokeColors = {FlamePal.red.cpy().mul(0.75f), Color.darkGray, Color.gray};
    float particleSpread = 7f, particleSizeScl = 18f;
    int particleAmount = 11;
    static final Color tc = new Color(), tc2 = new Color();

    public EndFlameBulletType(){
        super(12f, 30f);
        pierce = pierceBuilding = true;
        pierceCap = -1;
        lifetime = 60f;
        despawnEffect = Fx.none;
        status = StatusEffects.melting;
        statusDuration = 60f * 15f;
        hitSize = 14f;
        collidesAir = false;
        keepVelocity = false;
        hittable = reflectable = absorbable = false;

        incendAmount = 1;
        incendChance = 0.9f;

        float r = calculateRange();
        shootEffect = new Effect(lifetime + 15f, r * 2f, e -> {
            Draw.color(tc.lerp(colors, e.fin()));
            tc2.set(tc).shiftSaturation(0.77f);
            float qfin = Mathf.clamp(e.time / 5f);

            Angles.randLenVectors(e.id, particleAmount, e.finpow() * (r + 15f), e.rotation, particleSpread, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.65f + e.fout() * qfin * particleSizeScl);
                Drawf.light(e.x + x, e.y + y, (0.65f + e.fout(Interp.pow4Out) * particleSizeScl) * 4f, tc2, 0.5f * e.fout(Interp.pow2Out));
            });
        }).layer(Layer.bullet - 0.001f);
        smokeEffect = new Effect(lifetime * 3f, r * 2.25f, e -> {
            Draw.color(tc.lerp(smokeColors, e.fin()));

            float slope = (0.5f - Math.abs(e.fin(Interp.pow2InInverse) - 0.5f)) * 2f;

            Angles.randLenVectors(e.id, particleAmount, e.fin(Interp.pow5Out) * ((r * 1.125f) + 15f), e.rotation, particleSpread, (x, y) -> {
                Fill.circle(e.x + x, e.y + y, 0.65f + slope * particleSizeScl);
                Fill.circle(e.x + (x / 2f), e.y + (y / 2f), 0.5f + slope * (particleSizeScl / 2f));
            });
        }).followParent(false).layer(Layer.flyingUnitLow - 0.002f);

        Color[] hitColor = Arrays.copyOf(colors, colors.length - 1);
        hitEffect = new Effect(14f, e -> {
            Draw.color(tc.lerp(hitColor, e.fin()));
            Lines.stroke(0.5f + e.fout());

            Angles.randLenVectors(e.id, particleAmount / 3, e.fin() * 15f, e.rotation, 50f, (x, y) -> {
                float ang = Mathf.angle(x, y);
                Lines.lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
            });
        });
    }

    @Override
    public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
        super.hitTile(b, build, x, y, initialHealth, direct);
        if(Mathf.chance(0.9f)) build.enabled = false;
    }
}
