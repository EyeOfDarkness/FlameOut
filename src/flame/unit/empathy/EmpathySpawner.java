package flame.unit.empathy;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.graphics.*;
import flame.unit.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.graphics.*;

import static arc.math.Interp.*;

public class EmpathySpawner{
    float time = 0f;
    float x, y;
    float reactivateTime = 0f;
    boolean active = true, disabled = false;
    boolean spawned = false, shouldSpawn = true;
    int timeScl = 1;

    float health = -1;
    int countDown = 0;

    final static Vec2 v = new Vec2(), v2 = new Vec2();
    final static FloatSeq polySeq = new FloatSeq();
    final static Seq<Team> randTeam = new Seq<>();
    final static float duration = 15f * 60f;

    void update(){
        if(reactivateTime > 0 && !disabled){
            reactivateTime -= Time.delta;
            if(reactivateTime <= 0f){
                active = true;
                spawned = false;
                time = 0f;
                x = Vars.player.unit().x;
                y = Vars.player.unit().y;
            }
        }
        if(active){
            time += Time.delta * timeScl;

            if(time >= duration - 15 && !spawned){
                spawnEmpathy();
                spawned = true;
            }

            if(time >= duration){
                active = false;
            }
        }
    }
    private void spawnEmpathy(){
        EmpathyUnit unit = new EmpathyUnit();
        for(Team t : Team.all){
            if(Vars.player.team() != t && t != Team.derelict){
                for(TeamData data : Vars.state.teams.present){
                    if(data.team != t){
                        randTeam.add(t);
                    }
                }
            }
        }

        unit.team = randTeam.random();

        unit.team.data().unitCap = 99999;

        unit.setType(FlameUnitTypes.empathy);
        unit.elevation = 1f;
        unit.heal();

        if(health > 0){
            unit.health = health;
        }

        unit.x = x;
        unit.y = y;
        unit.rotation = 90f;

        unit.add();

        if(countDown != 0){
            unit.setCountDown(countDown);
        }
    }

    public static void progressiveStar(float x, float y, float rad, float rot, int count, int stellation, float fin){
        int c = Mathf.ceil(count * fin);
        for(int i = 0; i < c; i++){
            //360f * (1f / count) * i * stellation + rotation;
            float r1 = 360f * (1f / count) * stellation * i + rot;
            float r2 = 360f * (1f / count) * stellation * (i + 1f) + rot;

            float rx1 = Mathf.cosDeg(r1) * rad + x, ry1 = Mathf.sinDeg(r1) * rad + y;
            float rx2 = Mathf.cosDeg(r2) * rad + x, ry2 = Mathf.sinDeg(r2) * rad + y;

            if(fin >= 1f || (i < (c - 1))){
                Lines.line(rx1, ry1, rx2, ry2);
            }else{
                float f = fin < 1f ? (fin * count) % 1f : 1f;
                float lx = Mathf.lerp(rx1, rx2, f), ly = Mathf.lerp(ry1, ry2, f);
                Lines.line(rx1, ry1, lx, ly);
            }
        }
    }

    public static void progressiveCircle(float x, float y, float rad, float rot, float fin){
        if(fin < 0.9999f){
            if(fin < 0.001f) return;
            int r = Lines.circleVertices(rad * fin);
            //if(r <= 0) return;
            Lines.beginLine();
            for(int i = 0; i < r; i++){
                float ang = (360f / (r - 1)) * fin * i + rot;
                float sx = Mathf.cosDeg(ang) * rad, sy = Mathf.sinDeg(ang) * rad;
                Lines.linePoint(sx + x, sy + y);
            }
            Lines.endLine();
        }else{
            Lines.circle(x, y, rad);
        }
    }

    void eclipse(float x, float y, float rot, float rad, float f){
        float af = Math.abs(f);
        if(af < 0.001f) return;
        float f2 = (af - 0.5f) * 2f;

        for(int i = 0; i < 48; i++){
            float r1 = (180f / 48f) * i;
            float r2 = (180f / 48f) * (i + 1);

            float rx1 = Mathf.sinDeg(r1) * rad, ry1 = Mathf.cosDeg(r1) * rad;
            float rx2 = Mathf.sinDeg(r2) * rad, ry2 = Mathf.cosDeg(r2) * rad;
            float sx1 = rx1 * f2, sx2 = rx2 * f2;

            polySeq.clear();
            for(int s = 0; s < 2; s++){
                int sign = s == 0 ? -1 : 1;
                int off = (s + (f > 0 ? 1 : 0)) % 2;
                float ox1 = (off == 0 ? sx1 : rx1);
                float ox2 = (off == 0 ? sx2 : rx2);

                v.set(ox1 * sign, ry1).rotate(rot).add(x, y);
                v2.set(ox2 * sign, ry2).rotate(rot).add(x, y);

                if(s == 0){
                    polySeq.add(v.x, v.y, v2.x, v2.y);
                }else{
                    polySeq.add(v2.x, v2.y, v.x, v.y);
                }
            }
            Fill.poly(polySeq);
        }
    }

    void draw(){
        float lz = Draw.z();
        Draw.z(Layer.flyingUnit + 4f);

        float fin1 = Mathf.clamp(time / 120f);
        float fin2 = Mathf.clamp((time - 60f) / 120f);
        float fin3 = Mathf.clamp((time - 120f) / 120f);
        float fin4 = Mathf.clamp((time - 120f) / (4f * 60f));
        float fin5 = Mathf.clamp((time - 180f) / 120f);
        float fin6 = Mathf.clamp((time - 180f) / 180f);

        float fin2c = pow2.apply(fin2);
        float fin3c = pow2.apply(fin3);
        float fin4c = pow2.apply(fin4);
        float fin5c = pow2.apply(fin5);
        float fin6c = pow2.apply(fin6);

        float fout1 = pow2.apply(Mathf.clamp((duration - time) / 100f));
        //float fout1 = 1f;

        TextureRegion r1 = EmpathyRegions.magicCircle;

        Draw.color(FlamePal.empathy, fin1 * fout1);
        Draw.rect(r1, x, y, r1.width * Draw.scl * 2f, r1.height * Draw.scl * 2f);

        Draw.color(FlamePal.empathy);
        //Lines.stroke(2f * fout1);
        if(fin2 > 0.0001f){
            float ang = time / 2f;
            Lines.stroke(2f * fout1);
            progressiveCircle(x, y, 200f * fout1, ang - 90f, fin2c);
            Lines.stroke(1.5f * fout1);
            progressiveStar(x, y, 200f * fout1, ang + 180, 11, 2, fin2c);
            
            Lines.stroke(fout1 * fin2c);
            Lines.circle(x, y, 185f);
            
            Lines.stroke(2f * fout1);

            v.trns(ang, 200f * fout1).add(x, y);
            progressiveCircle(v.x, v.y, 90f, ang - 180f, fin2c);
            //progressiveStar(v.x, v.y, 50f, (ang + 180) - ang / 2f, 7, fin2c);
            progressiveStar(v.x, v.y, 90f, -ang / 1.5f, 7, 2, fin2c);
            Lines.stroke(fout1 * fin2c);
            Lines.circle(v.x, v.y, 80f);
            
            Lines.stroke(2f * fout1);
            v.trns(ang + 180f, 200f * fout1).add(x, y);
            progressiveCircle(v.x, v.y, 50f, ang, fin2c);
            
            v2.trns(-ang * 1.5f, 50f).add(v.x, v.y);
            progressiveCircle(v2.x, v2.y, 20f, ang * 3f, fin3c);
            progressiveStar(v2.x, v2.y, 20f, ang * 3f + 180, 5, 2, fin2c);
            
            Lines.stroke(fout1 * fin2c);
            Lines.circle(v.x, v.y, 45f);
        }
        if(fin3c > 0.0001f){
            float ang = time / 2f;
            Lines.stroke(fout1 * 2.5f);
            progressiveStar(x, y, 200f, ang + 180f, 4, 1, fin3c);
            Lines.stroke(fout1);
            progressiveCircle(x, y, 225f, ang, fin3c);
            Lines.stroke(fout1 * 2f);
            progressiveCircle(x, y, 250f, ang + 135f, fin3c);
            
            Lines.stroke(2f * fout1);
            
            progressiveCircle(x, y, 150f, Mathf.mod(-ang, 360f), fin3c);
            progressiveCircle(x, y, 130f, Mathf.mod(-ang + 180f, 360f), fin3c);
            
            Lines.stroke(fin3c * fout1);
            int seg = 3;
            for(int i = 0; i < seg; i++){
                float fr = ((360f / seg) / seg) * i * fin4c;
                Lines.poly(x, y, seg, 250f, ang + fr);
            }
            Lines.stroke(fin3c * fout1 * 2f);
            Lines.circle(x, y, 95f);
            for(int i = 0; i < 8; i++){
                float fr = (360f / 8) * i - ang / 1.5f;
                v.trns(fr, 95f).add(x, y);
                Draw.color(FlamePal.empathy);
                Lines.circle(v.x, v.y, 20f * fin4c);
                Draw.color(FlamePal.empathy, 0.333f * fout1);
                Fill.circle(v.x, v.y, 20f * fin4c);
                //eclipse(v.x, v.y, 0f, 20f * fin4c, 1f - (i / 8f));
            }
        }
        if(fin5 > 0.0001f){
            //float ang = -time / 3f;
            Draw.color(FlamePal.empathy);
            Lines.stroke(0.75f * fin5c * fout1);
            Lines.circle(x, y, 290f * fout1);
            float fout1cc = pow2Out.apply(fout1);
            
            for(int i = 0; i < 7; i++){
                int sg = (i == 0 || i == 6) ? 1 : 2;
                float fr = (i / 6f) * 180f;
                for(int s = 0; s < sg; s++){
                    float sn = s == 0 ? 1 : -1;
                    float fr2 = fr * sn + 180f * (time / 240f);
                    
                    v.trns(fr2 + 90f, 290f * fout1).add(x, y);
                    Lines.circle(v.x, v.y, 25f * fin5c * fout1cc);
                    //eclipse(v.x, v.y, fr2, 30f * fin5 * fout1, (1f - (i / 6f)) * sn * -1);
                    eclipse(v.x, v.y, fr2, 25f * fin5c * fout1cc, Mathf.mod(((1f - (i / 6f)) * sn * -1) + 1f + time / 240f, 2) - 1f);
                    //v.trns(fr2 + 90f, 280f).add(x, y);
                }
            }
            
            float ang = -time / 3f;
            float ang2 = -time / 5f;
            
            if(fin6c > 0.0001f){
                Lines.stroke(2f * fout1);
                progressiveCircle(x, y, 390f, ang, fin6c);
                Draw.color(FlamePal.empathy, 0.5f);
                progressiveCircle(x, y, 400f, -ang * 1.5f, fin6c);
                
                Draw.color(FlamePal.empathy);
                v.trns(ang * 0.75f, 390f).add(x, y);
                progressiveCircle(v.x, v.y, 35f * fout1cc, -ang, fin6c);
                Lines.stroke(fout1);
                progressiveCircle(v.x, v.y, 30f * fin6c * fout1cc, ang, fin6c);
                //eclipse(v.x, v.y, fr2, 25f * fin5c * fout1cc, Mathf.mod(((1f - (i / 6f)) * sn * -1) + 1f + time / 240f, 2) - 1f);
                eclipse(v.x, v.y, ang * 0.75f + 90f, 28f * fin6c * fout1cc, Mathf.mod(time / 80f, 2f) - 1f);
                
                float fin7 = Mathf.clamp((time - 190f) / 190f);
                float fin7c = pow2.apply(fin7);
                
                Lines.stroke(2f * fout1);
                progressiveCircle(x, y, 590f, ang2 * 2f, fin7c);
                Draw.color(FlamePal.empathy, 0.5f);
                progressiveCircle(x, y, 595f, -ang2, fin7c);
                
                Draw.color(FlamePal.empathy);
                v.trns(ang2 * 0.75f + 120f, 590f).add(x, y);
                float vx1 = v.x, vy1 = v.y;
                progressiveCircle(v.x, v.y, 60f * fout1cc, -ang2, fin7c);
                
                progressiveCircle(v.x, v.y, 180f * fout1cc, ang2 * 2f, fin7c);
                
                Lines.stroke(fout1);
                progressiveCircle(v.x, v.y, 55f * fin7c * fout1cc, ang2, fin7c);
                //eclipse(v.x, v.y, ang * 0.75f + 90f, 32f * fin6c * fout1cc, Mathf.mod(time / 80f, 2f) - 1f);
                eclipse(v.x, v.y, ang2 * 0.75f + 120f + 90f, 52f * fin7c * fout1cc, Mathf.mod(time / 100f, 2f) - 1f);
                
                Lines.stroke(2f * fout1);
                float ang3 = (time / 2f) * 0.75f;
                v.trns(ang3, 180f).add(vx1, vy1);
                progressiveCircle(v.x, v.y, 20f * fout1cc, -ang3 * 2f, fin7c);
                Lines.stroke(fout1);
                progressiveCircle(v.x, v.y, 15f * fin7c * fout1cc, ang3, fin7c);
                eclipse(v.x, v.y, ang3 + 90f, 12f * fin7c * fout1cc, Mathf.mod(time / 70f, 2f) - 1f);
                
                Lines.stroke(fout1 * 1.5f);
                Draw.color(FlamePal.empathy, 0.5f);
                
                float fin8 = Mathf.clamp((time - 250f) / 120f);
                float fin8c = pow2.apply(fin8);
                //progressiveStar(v.x, v.y, 90f, -ang / 1.5f, 7, 2, fin2c);
                progressiveStar(x, y, 590f, 90f, 9, 2, fin8c);
                Lines.stroke(fout1 * 2.25f);
                progressiveStar(x, y, 590f, 90f, 3, 1, fin8c);
            }
            //Lines.circle(x, y, 340f);
        }

        float i3rdRad = time * 4f;
        float i3rdScl = 512f * 3f + time * 3f;
        float fout2 = Mathf.clamp(((12f * 60) - time) / 120f);

        float invD = duration - 8f * 60f;
        float sfin = Mathf.clamp(((time - invD) / (duration - invD)));
        //float sfin = 1f - Mathf.clamp((duration - time - 3f * 60) / (3f * 60f - 15f));
        float sfout = Mathf.clamp((duration - time) / 15f);
        if(sfin > 0 && sfin < 1){
            Draw.color();
            Draw.z(Layer.flyingUnit + 5f);
            for(int i = 0; i < 4; i++){
                Drawf.tri(x, y, 35f * (pow3In.apply(sfout)) * pow2.apply(sfin * sfin), 560f * pow2.apply(sfin * sfin) * (1f + Mathf.absin(1f, 0.1f)), i * 90);
            }
        }

        FlameShaders.thirdImpactShader.draw(Layer.flyingUnit + 3f, x, y, i3rdRad, i3rdScl, fout2);

        Draw.color();
        Draw.z(lz);
        //Draw.flush();
    }
}
