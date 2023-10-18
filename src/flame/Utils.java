package flame;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.struct.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;

public class Utils{
    public static Rect r = new Rect(), r2 = new Rect();
    static Vec2 v2 = new Vec2(), v3 = new Vec2();
    static BasicPool<Hit> hpool = new BasicPool<>(Hit::new);
    static Seq<Hit> hseq = new Seq<>();
    static float ll = 0f;

    public static Vec2 v = new Vec2(), vv = new Vec2();
    public static Rand rand = new Rand(), rand2 = new Rand();

    public static float biasSlope(float fin, float bias){
        return (fin < bias ? (fin / bias) : 1f - (fin - bias) / (1f - bias));
    }

    public static float hitLaser(Team team, float width, float x1, float y1, float x2, float y2, Boolf<Healthc> within, Boolf<Healthc> stop, LineHitHandler<Healthc> cons){
        hseq.removeAll(h -> {
            hpool.free(h);
            return true;
        });
        ll = Mathf.dst(x1, y1, x2, y2);
        /*
        for(QuadTree<QuadTreeObject> tree : trees){
            //
            intersectLine(tree, width, x1, y1, x2, y2, (a, x, y) -> {
                Teamc t = (Teamc)a;
                if(within != null && !within.get(t)) return;
                Hit<Teamc> h = hpool.newObject();
                h.entity = t;
                h.x = x;
                h.y = y;
                hseq.add(h);
            });
        }
         */
        for(TeamData data : Vars.state.teams.present){
            if(data.team != team){
                if(data.unitTree != null){
                    intersectLine(data.unitTree, width, x1, y1, x2, y2, (t, x, y) -> {
                        if(within != null && !within.get(t)) return;
                        Hit h = hpool.newObject();
                        h.entity = t;
                        h.x = x;
                        h.y = y;
                        hseq.add(h);
                    });
                }
                if(data.buildingTree != null){
                    intersectLine(data.buildingTree, width, x1, y1, x2, y2, (t, x, y) -> {
                        if(within != null && !within.get(t)) return;
                        Hit h = hpool.newObject();
                        h.entity = t;
                        h.x = x;
                        h.y = y;
                        hseq.add(h);
                    });
                }
            }
        }
        hseq.sort(a -> a.entity.dst2(x1, y1));
        for(Hit hit : hseq){
            Healthc t = hit.entity;

            //cons.get(hit);
            cons.get(t, hit.x, hit.y);
            if(stop.get(t)){
                ll = Mathf.dst(x1, y1, hit.x, hit.y) - (t instanceof Sized s ? s.hitSize() / 4f : 0f);
                break;
            }
        }
        return ll;
    }

    public static <T extends QuadTreeObject> void intersectLine(QuadTree<T> tree, float width, float x1, float y1, float x2, float y2, LineHitHandler<T> cons){
        //intersectLine(tree, width, x1, y1, x2, y2, cons, 12);
        r.set(tree.bounds).grow(width);
        if(Intersector.intersectSegmentRectangle(x1, y1, x2, y2, r)){
            //
            for(T t : tree.objects){
                //cons.get(t);
                t.hitbox(r2);
                //float size = Math.max(r2.width, r2.height);
                r2.grow(width);
                Vec2 v = Geometry.raycastRect(x1, y1, x2, y2, r2);
                if(v != null){
                    /*
                    float scl = (unit.hitSize - unitWidth) / unit.hitSize;
                    vec.sub(unit).scl(scl).add(unit);
                    */
                    float size = Math.max(r2.width, r2.height);
                    float mx = r2.x + r2.width / 2, my = r2.y + r2.height / 2;
                    float scl = (size - width) / size;
                    v.sub(mx, my).scl(scl).add(mx, my);

                    cons.get(t, v.x, v.y);
                }
            }

            if(!tree.leaf){
                intersectLine(tree.botLeft, width, x1, y1, x2, y2, cons);
                intersectLine(tree.botRight, width, x1, y1, x2, y2, cons);
                intersectLine(tree.topLeft, width, x1, y1, x2, y2, cons);
                intersectLine(tree.topRight, width, x1, y1, x2, y2, cons);
            }
        }
    }

    public static <T extends QuadTreeObject> void scanCone(QuadTree<T> tree, float x, float y, float rotation, float length, float spread, Cons<T> cons){
        scanCone(tree, x, y, rotation, length, spread, cons, true, false);
    }
    public static <T extends QuadTreeObject> void scanCone(QuadTree<T> tree, float x, float y, float rotation, float length, float spread, boolean accurate, Cons<T> cons){
        scanCone(tree, x, y, rotation, length, spread, cons, true, accurate);
    }
    public static <T extends QuadTreeObject> void scanCone(QuadTree<T> tree, float x, float y, float rotation, float length, float spread, Cons<T> cons, boolean source, boolean accurate){
        //
        if(source){
            v2.trns(rotation - spread, length).add(x, y);
            v3.trns(rotation + spread, length).add(x, y);
        }
        //r.set(tree.bounds).grow(width);
        Rect r = tree.bounds;
        boolean valid = false;
        if(Intersector.intersectSegmentRectangle(x, y, v2.x, v2.y, r) || Intersector.intersectSegmentRectangle(x, y, v3.x, v3.y, r) || r.contains(x, y)){
            valid = true;
        }
        float lenSqr = length * length;
        if(!valid){
            for(int i = 0; i < 4; i++){
                float mx = (r.x + r.width * (i % 2)) - x;
                float my = (r.y + (i >= 2 ? r.height : 0f)) - y;

                float dst2 = Mathf.dst2(mx, my);
                if(dst2 < lenSqr && Angles.within(rotation, Angles.angle(mx, my), spread)){
                    valid = true;
                    break;
                }
            }
        }
        if(valid){
            for(T t : tree.objects){
                Rect rr = r2;
                t.hitbox(rr);

                float mx = (rr.x + rr.width / 2) - x;
                float my = (rr.y + rr.height / 2) - y;
                float size = (Math.max(rr.width, rr.height) / 2f);
                float bounds = size + length;
                float at = accurate ? Mathf.angle(Mathf.sqrt(mx * mx + my * my), size) : 0f;
                if(mx * mx + my * my < (bounds * bounds) && Angles.within(rotation, Angles.angle(mx, my), spread + at)){
                    cons.get(t);
                }
            }
            if(!tree.leaf){
                scanCone(tree.botLeft, x, y, rotation, length, spread, cons, false, accurate);
                scanCone(tree.botRight, x, y, rotation, length, spread, cons, false, accurate);
                scanCone(tree.topLeft, x, y, rotation, length, spread, cons, false, accurate);
                scanCone(tree.topRight, x, y, rotation, length, spread, cons, false, accurate);
            }
        }
    }

    public static float angleDistSigned(float a, float b){
        a += 360f;
        a %= 360f;
        b += 360f;
        b %= 360f;
        float d = Math.abs(a - b) % 360f;
        int sign = (a - b >= 0f && a - b <= 180f) || (a - b <= -180f && a - b >= -360f) ? 1 : -1;
        return (d > 180f ? 360f - d : d) * sign;
    }

    public static class BasicPool<T> extends Pool<T>{
        Prov<T> prov;

        public BasicPool(Prov<T> f){
            prov = f;
        }

        @Override
        protected T newObject(){
            return prov.get();
        }
    }

    public static class Hit{
        Healthc entity;
        float x;
        float y;
    }

    public interface LineHitHandler<T>{
        void get(T t, float x, float y);
    }
}
