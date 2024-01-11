package flame.entities;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.Utils.*;
import flame.unit.empathy.*;
import flame.unit.weapons.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class DesShockWaveEntity extends DrawEntity{
    final static float range = 2000f;
    final static Circle c1 = new Circle();

    float time = 0f;
    Team team = Team.derelict;
    Teamc targeting;

    public static void create(Team team, Teamc target, float x, float y){
        DesShockWaveEntity w = new DesShockWaveEntity();
        w.x = x;
        w.y = y;
        w.team = team;
        w.targeting = target;

        w.add();
    }

    @Override
    public void update(){
        float lrange = time > 0 ? getRange() : 0f;
        time += Time.delta / 120f;
        float crange = getRange();

        for(TeamData data : Vars.state.teams.active){
            if(data.team != team){
                QuadTreeHandler handler = (rect, tree) -> {
                    if(tree){
                        c1.set(x, y, crange);
                        return (lrange <= 0f || !Utils.circleContainsRect(x, y, lrange, rect)) && Intersector.overlaps(c1, rect);
                    }
                    float min = Math.min(rect.width, rect.height) / 2f;
                    float mx = rect.x + rect.width / 2f;
                    float my = rect.y + rect.height / 2f;

                    return Mathf.within(x, y, mx, my, crange + min) && !Mathf.within(x, y, mx, my, lrange - min);
                };

                if(data.unitTree != null){
                    Utils.scanQuadTree(data.unitTree, handler, u -> {
                        if(EndDespondencyWeapon.isStray(u) && u != targeting){
                            EmpathyDamage.damageUnit(u, u.maxHealth + 1000f, true, null);
                        }
                    });
                }
                if(data.turretTree != null){
                    Seq<Building> seq = Utils.buildings.clear();
                    Utils.scanQuadTree(data.turretTree, handler, bl -> {
                        if(bl != targeting){
                            seq.add(bl);
                        }
                    });
                    for(Building bl : seq){
                        EmpathyDamage.damageBuildingRaw(bl, bl.maxHealth + 1000f, true, null);
                    }
                    seq.clear();
                }
            }
        }

        if(time >= 1f){
            remove();
        }
    }

    float getRange(){
        return 100f + Interp.pow2Out.apply(time) * range;
    }

    @Override
    public void draw(){
        float fout = 1f - time;
        Draw.z(Layer.darkness + 0.1f);
        Draw.color(FlamePal.redLight);
        Lines.stroke(30f * fout);
        Lines.circle(x, y, getRange());

        Rand r = Utils.rand;
        r.setSeed(id);
        for(int i = 0; i < 20; i++){
            float rot = r.random(360f);
            Vec2 v = Tmp.v1.trns(rot, getRange()).add(x, y);
            Drawf.tri(v.x, v.y, 30f * fout, 100f + 500f * time, rot + 180f);
        }
        Draw.reset();
    }

    @Override
    public float clipSize(){
        return 99999999f;
    }
}
