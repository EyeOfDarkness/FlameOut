package flame.unit.empathy;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.Interp.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.audio.*;
import flame.effects.*;
import flame.graphics.*;
import mindustry.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.util.*;

public class EndAttack extends AttackAI{
    static Vec2[] ringPos = {new Vec2(44.25f, 18f), new Vec2(30.5f, 0f), new Vec2(19.5f, 7.5f)};
    static float[] ringMoveSpeed = {2f, 4f, 8f}, ringReloadTimes = {12f, 8f, 5f};
    static float[][] rotations3D = {
            {0.0625f, 1f, 0.125f},
            {0f, 0.25f, 1f},
            {1f, 1f, 0.1f}
    };
    static Interp swing = new SwingOut(5f);
    static Seq<Building> tmpBuildings = new Seq<>();

    float time = 0f;
    float[] ringRotations = new float[3], ringRotations3D = new float[3 * 3];
    float[] ringReloads = new float[3 * 8];
    //x, y, time
    float[] eyeLasers = new float[3 * 8 * 3];
    int[] ringIdxs = new int[3];

    Seq<Teamc> nearests = new Seq<>(true, 3 * 8 + 1);
    FloatSeq nearestScore = new FloatSeq(true, 3 * 8 + 1);
    FloatSeq crosses = new FloatSeq();
    float targetingUpdate = 0f;
    boolean flash = false;

    boolean ending = false;
    float endTime = 0f, endScanTimer = 0f;

    EndAttack(){
        super();
        addSoundConstant(FlameSounds.screams);
    }

    @Override
    void init(){
        Arrays.fill(ringRotations, 0f);
        Arrays.fill(ringRotations3D, 0f);
        Arrays.fill(ringReloads, 0f);
        flash = false;
        time = 0f;
    }

    @Override
    float weight(){
        return -1f;
        //return super.weight();
    }

    @Override
    boolean canTeleport(){
        return false;
    }

    @Override
    void update(){
        updateTargeting();

        time += Time.delta;
        float duration = 15f * 60;
        //float duration = 7.5f * 60;

        if(time < duration){
            float speed = Mathf.pow(Mathf.clamp(time / 700f) * 3f, 0.5f);
            float speed2 = Mathf.pow(Mathf.clamp(time / duration), 0.5f);
            float out = 1f - Interp.pow2.apply(Mathf.clamp((time - 10f * 60f) / (4.5f * 60)));
            
            for(int i = 0; i < 3; i++){
                ringRotations[i] += ringMoveSpeed[i] * speed * Time.delta * 0.5f * out;
                //ringRotations2[i] += speed2 * Time.delta * 2f;
                if(time < 12 * 60f){
                    for(int j = 0; j < 3; j++){
                        int k = i * 3 + j;
                        ringRotations3D[k] += speed2 * Time.delta * 2f * (rotations3D[i][j]);
                        ringRotations3D[k] %= 360f;
                    }
                }else{
                    float f = Mathf.clamp((time - 12 * 60f) / (3f * 60));

                    for(int j = 0; j < 3; j++){
                        int k = i * 3 + j;
                        float rs = rotations3D[i][j];
                        if(ringRotations3D[k] > 0f){
                            float anr = 45f;
                            float rr = (1f - Mathf.clamp((ringRotations3D[k] - (360 - anr)) / anr)) * 0.99f + 0.01f;
                            ringRotations3D[k] += speed2 * Time.delta * 2f * (rs != 0f ? Mathf.lerp(rs, Mathf.sqrt(1f / rs), f) : 0f) * rr;
                            if(ringRotations3D[k] >= 360f){
                                ringRotations3D[k] = 0f;
                            }
                        }
                        //ringRotations3D[k] %= 360f;
                    }
                }
            }
        }else if(!flash){
            for(int i = 0; i < 3; i++){
                TextureRegion reg = EmpathyRegions.endAPI[i];
                for(int j = 0; j < 8; j++){
                    //endFlash
                    Vec2 p = eyePos3D(i, j, reg.width * Draw.scl);
                    FlameFX.endFlash.at(p.x, p.y, 1f, unit);
                }
            }
            FlameFX.endFlash.at(unit.x, unit.y, 3f, unit);
            FlameSounds.empathyCharge.play(1f);
            flash = true;
            //ending = true;
        }

        if(time > duration + 20f){
            if(!ending){
                crosses.clear();
                int size = Math.min((int)((Vars.world.width() * Vars.world.height()) / ((200 * 200) / 8f)), 1000);
                for(int i = 0; i < size; i++){
                    crosses.add(Mathf.random(Vars.world.unitWidth()), Mathf.random(Vars.world.unitHeight()), Mathf.random(360), Mathf.random(1.7f, 4.25f));
                    crosses.add(0f);
                }

                ending = true;
            }
            //time = 0f;
            //flash = false;
        }
        if(ending){
            updateEnd();
        }

        updateRings();
    }
    void updateEnd(){
        endTime += Time.delta;
        
        float[] ar = crosses.items;
        int s = crosses.size;
        
        float size = Mathf.pow(Mathf.clamp(endTime / (1.75f * 60f)), 5) * 10000f;
        float oldSize = Mathf.pow(Mathf.clamp((endTime - Time.delta) / (1.75f * 60f)), 5) * 10000f;

        if(endScanTimer <= 0f && endTime < (1.75f * 60f)){
            for(Unit u : Groups.unit){
                if(u.team != unit.team && unit.within(u, size)){
                    EmpathyDamage.damageUnit(u, u.maxHealth + 1000f, true, () -> FlameFX.endDeath.at(u.x, u.y, u.hitSize / 2));
                }
            }
            tmpBuildings.clear();
            for(TeamData data : Vars.state.teams.present){
                if(data.team != unit.team && !data.buildings.isEmpty()){
                    for(Building b : data.buildings){
                        if(unit.within(b, size) && !(b instanceof CoreBuild)) tmpBuildings.add(b);
                    }
                }
            }
            for(Building b : tmpBuildings){
                EmpathyDamage.damageBuilding(b, b.maxHealth + 1000f, true, () -> FlameFX.endDeath.at(b.x, b.y, b.hitSize() / 2));
            }

            tmpBuildings.clear();
            endScanTimer = 3;
        }
        if(endTime < (1.75f * 60f)){
            for(TeamData data : Vars.state.teams.present){
                if(data.team != unit.team) data.plans.clear();
            }

            double area = (size * size * Math.PI) - (oldSize * oldSize * Math.PI);
            int amount = Math.min((int)(area / (100f * 100f * Mathf.pi)), 250);
            for(int i = 0; i < amount; i++){
                float r = Mathf.random(360);
                Vec2 v = Tmp.v2.trns(r, Mathf.random(oldSize, size)).add(unit.x, unit.y);
                //Rect rect = Tmp.r1;
                //Vars.world.getQuadBounds(rect);
                float wid = Vars.world.unitWidth(), hei = Vars.world.unitHeight();
                if(v.x > 0 && v.y > 0 && v.x < wid && v.y < hei){
                    FlameFX.endSplash.at(v.x, v.y, r);
                }
            }
        }
        endScanTimer -= Time.delta;
        
        PassiveSoundLoop sound = sounds.get(0);
        float pdst = Mathf.dst(unit.x, unit.y, Core.camera.position.x, Core.camera.position.y);
        //Tmp.v4.set(Core.camera.position).sub(unit.x, unit.y).setLength(size).add(unit.x, unit.y);

        //float pan = sound.calcPan(Tmp.v4.x, Tmp.v4.y);
        float sfin = 1f - Mathf.curve(Mathf.clamp(endTime / (7f * 60f)), 0.5f, 1f);
        //float fall = (1f - Mathf.clamp(Math.abs(pdst - size) / 100000f)) * sfin * Mathf.clamp((size - 100f) / 100f);
        float fall = (1f - Mathf.clamp(pdst > size ? ((pdst - size) / 800f) : (-(pdst - size) / 100000f))) * sfin * Mathf.clamp((size - 100f) / 100f);
        if(fall > 0.0001f){
            sound.play(unit.x, unit.y, fall, 0f, true);
        }
        
        for(int i = 0; i < s; i += 5){
            float x = ar[i];
            float y = ar[i + 1];
            
            if(unit.within(x, y, size)){
                ar[i + 4] = Mathf.clamp(ar[i + 4] + Time.delta / 5f);
            }
        }

        if(endTime >= 10 * 60f){
            time = 0f;
            flash = false;
            endTime = 0f;
            ending = false;

            unit.randAI(true, unit.health < 50);
        }
    }

    @Override
    boolean updateMovementAI(){
        float duration = 15f * 60;
        return !ending && time < (duration - 20f);
    }

    @Override
    boolean canKnockback(){
        return !ending;
    }

    void updateRings(){
        float duration = 15f * 60;
        
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 8; j++){
                int idx = (i * 8 + j);
                int idx2 = idx * 3;

                eyeLasers[idx2 + 2] -= Time.delta;
            }
        }
        if(!nearests.isEmpty() && time < (duration - 30f)){
            for(int i = 0; i < 3; i++){
                if(ringReloads[i] <= 0f){
                    int j = ringIdxs[i];
                    int idx = (i * 8 + j);
                    int idx2 = idx * 3;
                    //Teamc tar = nearests.items[idx % nearests.size];
                    Teamc tar = nearests.get(idx % nearests.size);

                    if(tar != null){
                        if(tar instanceof Unit u){
                            float d = Mathf.pow(u.health / 100000000f, 2) * u.maxHealth;
                            EmpathyDamage.damageUnit(u, 10000f + d, true, null);
                        }else if(tar instanceof Building b){
                            float d = Mathf.pow(b.health / 100000000f, 2) * b.maxHealth;
                            EmpathyDamage.damageBuilding(b, 10000f + d, true, null);
                        }

                        eyeLasers[idx2] = tar.x();
                        eyeLasers[idx2 + 1] = tar.y();
                        eyeLasers[idx2 + 2] = 30f;
                    }

                    TextureRegion reg = EmpathyRegions.endAPI[i];
                    Vec2 p = eyePos3D(i, j, reg.width * Draw.scl);
                    FlameSounds.empathySmallEnd.at(p.x, p.y);

                    ringReloads[i] = ringReloadTimes[i];
                    ringIdxs[i] = Mathf.mod(ringIdxs[i] + ((i % 2) == 0 ? 1 : -1), 8);
                }
            }
        }
        for(int i = 0; i < 3; i++){
            ringReloads[i] -= Time.delta;
        }
    }

    void updateTargeting(){
        if((targetingUpdate -= Time.delta) <= 0f){
            nearests.clear();
            nearestScore.clear();
            for(TeamData data : Vars.state.teams.present){
                if(data.team != unit.team){
                    for(Unit u : data.units){
                        float dst = u.dst(unit);
                        if(nearestScore.isEmpty()){
                            nearests.add(u);
                            nearestScore.add(dst);
                        }else{
                            for(int i = nearests.size - 1; i >= 0; i--){
                                float sdst = nearestScore.items[i];
                                if(dst <= sdst){
                                    nearests.insert(i + 1, u);
                                    nearestScore.insert(i + 1, dst);

                                    if(nearestScore.size > (3 * 8)){
                                        nearests.remove(0);
                                        nearestScore.removeIndex(0);
                                    }

                                    break;
                                }
                            }
                        }
                    }
                    for(Building b : data.buildings){
                        float dst = b.dst(unit);
                        if(nearestScore.isEmpty()){
                            nearests.add(b);
                            nearestScore.add(dst);
                        }else{
                            for(int i = nearests.size - 1; i >= 0; i--){
                                float sdst = nearestScore.items[i];
                                if(dst <= sdst){
                                    nearests.insert(i + 1, b);
                                    nearestScore.insert(i + 1, dst);

                                    if(nearestScore.size > (3 * 8)){
                                        nearests.remove(0);
                                        nearestScore.removeIndex(0);
                                    }

                                    break;
                                }
                            }
                        }
                    }
                }
            }
            /*
            if(!nearestScore.isEmpty() && nearestScore.size < (3 * 8)){
                int len = (3 * 8) - nearestScore.size;
                int isize = nearestScore.size;

                for(int i = 0; i < len; i++){
                    Teamc t = nearests.items[i % isize];
                    nearests.add(t);
                    nearestScore.add(0);
                }
            }
            */

            targetingUpdate = 5f;
        }
    }

    void drawEnd(){
        float size = Mathf.pow(Mathf.clamp(endTime / (1.75f * 60f)), 5) * 10000f;
        float fout = Mathf.clamp((10f * 60 - endTime) / 10f);
        float sfout = Mathf.clamp((10f * 60 - endTime) / 30f);
        float z = Draw.z();
        Draw.z(Layer.blockOver);
        Draw.color(Color.red);
        Draw.blend(GraphicUtils.multiply);

        if(fout >= 0.999f){
            Fill.poly(unit.x, unit.y, 64, size);
        }else{
            Camera camera = Core.camera;
            Fill.rect(camera.position.x, camera.position.y, camera.width, camera.height * fout);
        }

        Draw.blend(Blending.additive);
        Draw.color(FlamePal.empathyDark);

        Draw.z(Layer.flyingUnit);
        float[] ar = crosses.items;
        int s = crosses.size;
        TextureRegion flash = EmpathyRegions.flash;

        Rect r = Tmp.r1;
        Core.camera.bounds(r);
        for(int i = 0; i < s; i += 5){
            float x = ar[i];
            float y = ar[i + 1];
            float rot = ar[i + 2];
            float scl = ar[i + 3] * fout;
            float fin = ar[i + 4];

            Tmp.r2.setCentered(x, y, flash.width * scl);
            if(r.overlaps(Tmp.r2)){
                float dst = Mathf.dst(unit.x, unit.y, x, y);
                if(dst < size){
                    //float f = Interp.swingOut.apply(Mathf.clamp((size - dst) / 30f));
                    //float c = Math.max(Mathf.pow(dst / 10000f, 4f) * 10000f * 25f, 15f);
                    //float c = Math.max(15f, dst / 3f);
                    float f = swing.apply(fin);
                    Draw.rect(flash, x, y, flash.width * scl * f, flash.height * scl * f, rot);
                }
            }
        }

        Draw.color();
        float f = swing.apply(Mathf.clamp((endTime - 30f) / 15f));
        float sin = 1f + Mathf.absin(2f, 0.1f);
        for(int i = 0; i < 4; i++){
            Drawf.tri(unit.x, unit.y, 15f * sfout, 280f * f * sfout * sin, i * 90);
        }
        Lines.stroke(3f * Mathf.clamp((endTime - 30f) / 20f) * sfout * sin);
        Lines.circle(unit.x, unit.y, 75f);

        Draw.blend();
        Draw.color();
        Draw.z(z);
    }

    @Override
    void draw(){
        if(ending) drawEnd();
        //Draw.color(FlamePal.empathyAdd);
        Color darkC = Tmp.c1.set(FlamePal.empathyDark).lerp(FlamePal.red, Mathf.absin(2f, 1f));
        Color col1 = Tmp.c2.set(FlamePal.empathy).lerp(FlamePal.red, Mathf.absin(2f, 1f));

        float fin = Mathf.clamp(time / 40f);
        Draw.blend(Blending.additive);
        Draw.color(darkC, fin * 0.5f);
        
        TextureRegion mag = EmpathyRegions.magicCircle;
        Draw.rect(mag, unit.x, unit.y, mag.width * Draw.scl * 2f, mag.height * Draw.scl * 2f);
        
        for(int i = 0; i < 3; i++){
            Draw.color(darkC, fin);
            TextureRegion reg = EmpathyRegions.endAPI[i];
            int k = i * 3;

            GraphicUtils.circle3D(reg, unit.x, unit.y, ringRotations3D[k], ringRotations3D[k + 1], ringRotations3D[k + 2], reg.width * Draw.scl, ringRotations[i], 64);
            Draw.color(Color.white);
            for(int j = 0; j < 8; j++){
                Vec2 p = eyePos(i, j).scl(2f);
                Tmp.v2.set(p).setLength(reg.width * Draw.scl);
                float ll = p.len() / Tmp.v2.len();

                //v2.trns(ang1 + angle, size);
                //v.set(v2.x, v2.y, 0f).rotate(Vec3.Y, ry).rotate(Vec3.X, rx).rotate(Vec3.Z, rz);
                //float sz1 = 700f / (700f - v.z);
                //v.x *= sz1;
                //v.y *= sz1;
                Vec3 v3 = Tmp.v31.set(Tmp.v2.x, Tmp.v2.y, 0f).rotate(Vec3.Y, ringRotations3D[k + 1]).rotate(Vec3.X, ringRotations3D[k]).rotate(Vec3.Z, ringRotations3D[k + 2]);
                float sz1 = 700f / (700f - v3.z);
                v3.x *= sz1;
                v3.y *= sz1;

                Fill.circle(v3.x * ll + unit.x, v3.y * ll + unit.y, 2f * fin);
            }
        }
        Draw.blend();

        //Draw.color(FlamePal.empathy);
        float z = Draw.z();
        for(int i = 0; i < 3; i++){
            TextureRegion reg = EmpathyRegions.endAPI[i];
            float size = reg.width * Draw.scl;

            for(int j = 0; j < 8; j++){
                int idx = (i * 8 + j) * 3;
                float x = eyeLasers[idx];
                float y = eyeLasers[idx + 1];
                float time = eyeLasers[idx + 2];
                if(time <= 0.0001f) continue;

                Vec2 p = eyePos3D(i, j, size);
                float f = time / 30f;
                float s = 5.25f * f;

                Draw.z(z);
                Draw.color(col1);
                Lines.stroke(s);
                Lines.line(p.x, p.y, x, y, false);
                Fill.circle(p.x, p.y, s);
                Fill.circle(x, y, s);
                Draw.z(z + 0.01f);
                Draw.color(Color.white);
                Lines.stroke(s * 0.5f);
                Lines.line(p.x, p.y, x, y, false);
                Fill.circle(p.x, p.y, s * 0.5f);
                Fill.circle(x, y, s * 0.5f);
            }
        }
        Draw.z(z);
        Draw.color();
    }

    @Override
    boolean shouldDraw(){
        return unit.activeAttack == this;
    }

    Vec2 eyePos3D(int ring, int idx, float size){
        Vec2 p = eyePos(ring, idx).scl(2f);
        Tmp.v2.set(p).setLength(size);
        float ll = p.len() / Tmp.v2.len();
        int k = ring * 3;
        //.rotate(Vec3.Y, ringRotations3D[k + 1]).rotate(Vec3.X, ringRotations3D[k]).rotate(Vec3.Z, ringRotations3D[k + 2])

        Vec3 v3 = Tmp.v31.set(Tmp.v2.x, Tmp.v2.y, 0f).rotate(Vec3.Y, ringRotations3D[k + 1]).rotate(Vec3.X, ringRotations3D[k]).rotate(Vec3.Z, ringRotations3D[k + 2]);
        float sz1 = 700f / (700f - v3.z);
        v3.x *= sz1;
        v3.y *= sz1;

        return Tmp.v3.set(v3.x * ll + unit.x, v3.y * ll + unit.y);
    }

    Vec2 eyePos(int ring, int idx){
        //.add(unit.x, unit.y)
        if(ringPos[ring].y == 0){
            return Tmp.v1.trns(ringRotations[ring] + idx * 45f, ringPos[ring].x);
        }

        int rot = (idx / 2);
        float side = Mathf.signs[idx % 2] * ringPos[ring].y;
        return Tmp.v1.trns(ringRotations[ring] + rot * 90f, ringPos[ring].x, side);
    }
}
