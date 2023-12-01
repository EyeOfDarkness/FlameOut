package flame.entities;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import flame.*;
import flame.effects.*;
import flame.unit.empathy.*;
import mindustry.game.*;
import mindustry.gen.*;

public class EmpathyLightning extends DrawEntity implements Poolable{
    //x1, y1, x2, y2
    FloatSeq lines = new FloatSeq();
    float drawSize = 0f;
    float time = 0f;

    static FloatSeq points = new FloatSeq(), toAdd = new FloatSeq();
    static IntSet collided = new IntSet();
    static Vec2 v = new Vec2();

    public static void create(Team team, float x, float y, float rotation, int length){
        EmpathyLightning entity = Pools.obtain(EmpathyLightning.class, EmpathyLightning::new);

        points.clear();
        toAdd.clear();
        collided.clear();

        //points.add(x, y, rotation);
        //float[] p = points.items;
        float mdst = 0f;

        /*
        for(int i = 0; i < length; i++){
            float[] p = points.items;
            
            for(int j = 0; j < points.size; j += 3){
                int split = Mathf.chance(0.1f) ? 2 : 1;

                for(int k = 0; k < split; k++){
                    float x1 = p[j], y1 = p[j + 1];
                    float rot = p[j + 2] + Mathf.range(20f);

                    v.trns(rot, 50f).add(x1, y1);

                    entity.lines.add(x1, y1, v.x, v.y);
                    mdst = Math.max(Mathf.dst(x, y, v.x, v.y), mdst);

                    Utils.hitLaser(team, 2f, x1, y1, v.x, v.y, null, h -> false, (h, hx, hy) -> {
                        if(collided.add(h.id())){
                            FlameFX.empathyLightningHit.at(hx, hy, rot);
                            float dam = 1500f;
                            if(h instanceof Unit u){
                                EmpathyDamage.damageUnit(u, Math.max(dam, u.maxHealth / 220f), u.maxHealth < dam * 3f, null);
                            }else if(h instanceof Building b){
                                EmpathyDamage.damageBuilding(b, Math.max(dam, b.maxHealth / 220f), b.maxHealth < dam * 3f, null);
                            }
                        }
                    });

                    toAdd.add(v.x, v.y, rot);
                }
            }
            points.clear();
            points.addAll(toAdd);
            toAdd.clear();
        }
         */
        points.add(x, y, rotation, length);
        int i = 0;
        while(i < 200){
            float[] p = points.items;

            for(int j = 0; j < points.size; j += 4){
                int count = (int)p[3];
                if(count > 0){
                    int split = Mathf.chance(0.1f) ? 2 : 1;
                    for(int k = 0; k < split; k++){
                        float x1 = p[j], y1 = p[j + 1];
                        float rot = p[j + 2] + Mathf.range(20f);

                        v.trns(rot, 50f).add(x1, y1);

                        entity.lines.add(x1, y1, v.x, v.y);
                        mdst = Math.max(Mathf.dst(x, y, v.x, v.y), mdst);

                        Utils.hitLaser(team, 2f, x1, y1, v.x, v.y, null, h -> false, (h, hx, hy) -> {
                            if(collided.add(h.id())){
                                FlameFX.empathyLightningHit.at(hx, hy, rot);
                                float dam = 1500f;
                                if(h instanceof Unit u){
                                    EmpathyDamage.damageUnit(u, Math.max(dam, u.maxHealth / 220f), u.maxHealth < dam * 3f, null);
                                }else if(h instanceof Building b){
                                    EmpathyDamage.damageBuilding(b, Math.max(dam, b.maxHealth / 220f), b.maxHealth < dam * 3f, null);
                                }
                            }
                        });

                        toAdd.add(v.x, v.y, rot, (count - 1) + (split != 1 ? Mathf.random(-4, 1) : 0));
                    }
                }
            }
            if(toAdd.isEmpty()) break;
            points.clear();
            points.addAll(toAdd);
            toAdd.clear();

            i++;
        }

        entity.x = x;
        entity.y = y;
        entity.drawSize = mdst * 2f;
        entity.add();
    }

    @Override
    public void update(){
        time += Time.delta;
        if(time > 30f){
            remove();
        }
    }

    @Override
    public float clipSize(){
        return drawSize;
    }

    @Override
    public void draw(){
        float fin = Mathf.clamp(time / 30f);
        float fout = 1f - fin;
        float[] ar = lines.items;
        int len = lines.size;

        Draw.color(Color.white, FlamePal.empathy, fin);
        Lines.stroke(5f * fout);

        for(int i = 0; i < len; i += 4){
            float x1 = ar[i], y1 = ar[i + 1];
            float x2 = ar[i + 2], y2 = ar[i + 3];

            Lines.line(x1, y1, x2, y2, false);
        }

        Draw.reset();
    }

    @Override
    public void reset(){
        lines.clear();
        drawSize = 0f;
        time = 0f;
    }

    @Override
    protected void removeGroup(){
        super.removeGroup();
        Groups.queueFree(this);
    }
}
