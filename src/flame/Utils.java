package flame;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.struct.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.*;

import java.util.*;

public class Utils{
    public static Rect r = new Rect(), r2 = new Rect();
    static Vec2 v2 = new Vec2(), v3 = new Vec2(), v4 = new Vec2();
    static BasicPool<Hit> hpool = new BasicPool<>(Hit::new);
    static Seq<Hit> hseq = new Seq<>();
    static IntSet collided = new IntSet(), collided2 = new IntSet();
    static float ll = 0f;

    public static Vec2 v = new Vec2(), vv = new Vec2();
    public static Rand rand = new Rand(), rand2 = new Rand();

    public static Seq<Building> buildings = new Seq<>();

    public static float biasSlope(float fin, float bias){
        return (fin < bias ? (fin / bias) : 1f - (fin - bias) / (1f - bias));
    }

    public static float inRayCastCircle(float x, float y, float[] in, Sized target){
        float amount = 0f;
        float hsize = target.hitSize() / 2f;
        int collision = 0;
        int isize = in.length;

        float dst = Mathf.dst(x, y, target.getX(), target.getY());
        float ang = Angles.angle(x, y, target.getX(), target.getY());
        float angSize = Mathf.angle(dst, hsize);

        int idx1 = (int)(((ang - angSize) / 360f) * isize + 0.5f);
        int idx2 = (int)(((ang + angSize) / 360f) * isize + 0.5f);

        for(int i = idx1; i <= idx2; i++){
            int mi = Mathf.mod(i, isize);
            float range = in[mi];

            if((dst - hsize) < range){
                amount += Mathf.clamp((range - (dst - hsize)) / hsize);
                //collision++;
            }
            collision++;
        }

        return collision > 0 ? (amount / collision) : 0f;
    }

    public static void rayCastCircle(float x, float y, float radius, Boolf<Tile> stop, Cons<Tile> ambient, Cons<Tile> edge, Cons<Building> hit, float[] out){
        Arrays.fill(out, radius);

        int res = out.length;
        collided.clear();
        collided2.clear();
        buildings.clear();
        for(int i = 0; i < res; i++){
            final int fi = i;
            float ang = (i / (float)res) * 360f;
            v2.trns(ang, radius).add(x, y);
            float vx = v2.x, vy = v2.y;
            int tx1 = (int)(x / Vars.tilesize), ty1 = (int)(y / Vars.tilesize);
            int tx2 = (int)(vx / Vars.tilesize), ty2 = (int)(vy / Vars.tilesize);

            World.raycastEach(tx1, ty1, tx2, ty2, (rx, ry) -> {
                Tile tile = Vars.world.tile(rx, ry);
                boolean collide = false;

                if(tile != null && !tile.block().isAir() && stop.get(tile)){
                    //r2.setCentered(rx * Vars.tilesize, ry * Vars.tilesize, Vars.tilesize * 2f).grow(0.01f);
                    tile.getBounds(r2);
                    r2.grow(0.1f);
                    Vec2 inter = intersectRect(x, y, vx, vy, r2);
                    if(inter != null){
                        if(tile.build != null && collided.add(tile.build.id)){
                            buildings.add(tile.build);
                        }

                        float dst = Mathf.dst(x, y, inter.x, inter.y);
                        out[fi] = dst;
                        collide = true;
                    }else{
                        for(Point2 d : Geometry.d8){
                            Tile nt = Vars.world.tile(tile.x + d.x, tile.y + d.y);

                            if(nt != null && !nt.block().isAir() && stop.get(nt)){
                                nt.getBounds(r2);
                                r2.grow(0.1f);
                                Vec2 inter2 = intersectRect(x, y, vx, vy, r2);
                                if(inter2 != null){
                                    if(tile.build != null && collided.add(tile.build.id)){
                                        buildings.add(tile.build);
                                    }

                                    float dst = Mathf.dst(x, y, inter2.x, inter2.y);
                                    out[fi] = dst;
                                    collide = true;
                                }
                            }
                        }
                    }
                }

                if(tile != null && collided2.add(tile.pos())){
                    ambient.get(tile);
                    if(collide){
                        edge.get(tile);
                    }
                }

                return collide;
            });
        }
        for(Building b : buildings){
            hit.get(b);
        }
        buildings.clear();
        /*
        float tx = x / Vars.tilesize, ty = y / Vars.tilesize, tr = radius / Vars.tilesize;

        int worldHeight = Vars.world.height(), worldWidth = Vars.world.width();
        int res = out.length;

        int minX = Math.max((int)((x - radius) / Vars.tilesize), 0), minY = Math.max((int)((y - radius) / Vars.tilesize), 0);
        int maxX = Math.min((int)((x + radius) / Vars.tilesize) + 1, worldWidth), maxY = Math.min((int)((y + radius) / Vars.tilesize) + 1, worldHeight);

        tiles.clear();
        for(int ix = minX; ix < maxX; ix++){
            for(int iy = minY; iy < maxY; iy++){
                Tile t = Vars.world.tile(ix, iy);
                if(t != null && Mathf.within(tx, ty, t.x, t.y, tr) && !t.block().isAir() && stop.get(t)){
                    tiles.add(t);
                }
            }
        }
        tiles.sort(t -> t.dst2(tx, ty));
        for(Tile t : tiles){
            //int idx = (int)((Angles.angle(tx, ty, t.x, t.y) / 360f) * res);
            int idx = (int)((Angles.angle(tx, ty, t.x, t.y) / 360f) * res - 0.5f);

            float range1 = out[idx % res];
            float dst = Mathf.dst(tx, ty, t.x, t.y) * Vars.tilesize;

            if(dst < range1 + Vars.tilesize / 1.5f && collided.add(t.pos())){
                edge.get(t);
                Building bl = t.build;
                if(bl != null && collided2.add(t.pos())){
                    hit.get(bl);
                }
            }
            if(dst > (range1 + Vars.tilesize * 8f)){
                continue;
            }

            t.getBounds(r);
            r.grow(0.1f);
            //r.grow(Vars.tilesize);
            //r.set(t.x * Vars.tilesize, t.y * Vars.tilesize, );

            for(int s : sides){
                int nidx = Mathf.mod(idx + s, res);
                float nangle = (nidx / (float)res) * 360f;

                //v.trns(nangle, radius).add(x, y);
                float nsx = Mathf.cosDeg(nangle) * radius + x, nsy = Mathf.sinDeg(nangle) * radius + y;
                //float nsx = v.x, nsy = v.y;
                Vec2 inter2 = intersectRect(x, y, nsx, nsy, r);
                if(inter2 != null){
                    float len = Mathf.dst(x, y, inter2.x, inter2.y);
                    out[nidx] = Math.min(out[nidx], len);
                }else{
                    for(Point2 d : Geometry.d8){
                        //r2.setCentered(t.x + d.x, t.y + d.y, Vars.tilesize);
                        Tile nt = Vars.world.tile(t.x + d.x, t.y + d.y);
                        if(nt != null && !nt.block().isAir() && stop.get(nt)){
                            //float range2 = out[nidx];
                            nt.getBounds(r2);
                            r2.grow(0.1f);

                            Vec2 inter3 = intersectRect(x, y, nsx, nsy, r2);
                            if(inter3 != null){
                                float len = Mathf.dst(x, y, inter3.x, inter3.y);
                                out[nidx] = Math.min(out[nidx], len);
                            }
                        }
                    }
                }
            }
        }

        for(int ix = minX; ix < maxX; ix++){
            for(int iy = minY; iy < maxY; iy++){
                Tile t = Vars.world.tile(ix, iy);
                if(t != null && Mathf.within(tx, ty, t.x, t.y, tr)){
                    int idx = (int)((Angles.angle(tx, ty, t.x, t.y) / 360f) * res) % res;
                    float range = out[idx];
                    //float dst = Mathf.dst(x, y, t.x, t.y);
                    if(Mathf.within(x, y, t.x * Vars.tilesize, t.y * Vars.tilesize, range + Vars.tilesize / 2f)){
                        ambient.get(t);
                    }
                }
            }
        }

        tiles.clear();
        */
    }

    public static void scanEnemies(Team team, float x, float y, float radius, boolean targetAir, boolean targetGround, Cons<Teamc> cons){
        r.setCentered(x, y, radius * 2f);
        Groups.unit.intersect(r.x, r.y, r.width, r.height, u -> {
            if(u.team != team && Mathf.within(x, y, u.x, u.y, radius + u.hitSize / 2f) && u.checkTarget(targetAir, targetGround)){
                cons.get(u);
            }
        });

        if(targetGround){
            buildings.clear();
            for(TeamData data : Vars.state.teams.active){
                if(data.team != team && data.buildingTree != null){
                    data.buildingTree.intersect(r, b -> {
                        if(Mathf.within(x, y, b.x, b.y, radius + b.hitSize() / 2f)){
                            //cons.get(b);
                            buildings.add(b);
                        }
                    });
                }
            }
            for(Building b : buildings){
                cons.get(b);
            }

            buildings.clear();
        }
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
                        Hit h = hpool.obtain();
                        h.entity = t;
                        h.x = x;
                        h.y = y;
                        hseq.add(h);
                    });
                }
                if(data.buildingTree != null){
                    intersectLine(data.buildingTree, width, x1, y1, x2, y2, (t, x, y) -> {
                        if(within != null && !within.get(t)) return;
                        Hit h = hpool.obtain();
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

            //hpool.free(hit);
        }
        //hpool.clear();
        return ll;
    }

    public static boolean circleContainsRect(float x, float y, float radius, Rect rect){
        int count = 0;
        for(int i = 0; i < 4; i++){
            int mod = i % 2;
            int i2 = i / 2;
            float rx1 = (rect.x + rect.width * mod);
            float ry1 = (rect.y + rect.height * i2);

            if(Mathf.within(x, y, rx1, ry1, radius)){
                count++;
            }
        }

        return count == 4;
    }

    public static <T extends QuadTreeObject> void scanQuadTree(QuadTree<T> tree, QuadTreeHandler within, Cons<T> cons){
        if(within.get(tree.bounds, true)){
            for(T t : tree.objects){
                t.hitbox(r2);
                if(within.get(r2, false)){
                    cons.get(t);
                }
            }

            if(!tree.leaf){
                scanQuadTree(tree.botLeft, within, cons);
                scanQuadTree(tree.botRight, within, cons);
                scanQuadTree(tree.topLeft, within, cons);
                scanQuadTree(tree.topRight, within, cons);
            }
        }
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
                /*
                Vec2 v = Geometry.raycastRect(x1, y1, x2, y2, r2);
                if(v != null){
                    float size = Math.max(r2.width, r2.height);
                    float mx = r2.x + r2.width / 2, my = r2.y + r2.height / 2;
                    float scl = (size - width) / size;
                    v.sub(mx, my).scl(scl).add(mx, my);

                    cons.get(t, v.x, v.y);
                }
                 */
                float cx = r2.x + r2.width / 2f, cy = r2.y + r2.height / 2f;
                float cr = Math.max(r2.width, r2.height);

                Vec2 v = intersectCircle(x1, y1, x2, y2, cx, cy, cr / 2f);
                if(v != null){
                    float scl = (cr - width) / cr;
                    v.sub(cx, cy).scl(scl).add(cx, cy);

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

    /** code taken from BadWrong_ on the gamemaker subreddit */
    public static Vec2 intersectCircle(float x1, float y1, float x2, float y2, float cx, float cy, float cr){
        if(!Intersector.nearestSegmentPoint(x1, y1, x2, y2, cx, cy, v4).within(cx, cy, cr)) return null;
        
        cx = x1 - cx;
        cy = y1 - cy;

        float vx = x2 - x1,
                vy = y2 - y1,
                a = vx * vx + vy * vy,
                b = 2 * (vx * cx + vy * cy),
                c = cx * cx + cy * cy - cr * cr,
                det = b * b - 4 * a * c;

        if(a <= Mathf.FLOAT_ROUNDING_ERROR || det < 0){
            return null;
        }else if(det == 0f){
            float t = -b / (2 * a);
            float ix = x1 + t * vx;
            float iy = y1 + t * vy;

            return v4.set(ix, iy);
        }else{
            det = Mathf.sqrt(det);
            float t1 = (-b - det) / (2 * a);

            return v4.set(x1 + t1 * vx, y1 + t1 * vy);
        }
    }

    public static Vec2 intersectRect(float x1, float y1, float x2, float y2, Rect rect){
        boolean intersected = false;

        float nearX = 0f, nearY = 0f;
        float lastDst = 0f;

        for(int i = 0; i < 4; i++){
            int mod = i % 2;
            float rx1 = i < 2 ? (rect.x + rect.width * mod) : rect.x;
            float rx2 = i < 2 ? (rect.x + rect.width * mod) : rect.x + rect.width;
            float ry1 = i < 2 ? rect.y : (rect.y + rect.height * mod);
            float ry2 = i < 2 ? rect.y + rect.height : (rect.y + rect.height * mod);

            if(Intersector.intersectSegments(x1, y1, x2, y2, rx1, ry1, rx2, ry2, vv)){
                float dst = Mathf.dst2(x1, y1, vv.x, vv.y);
                if(!intersected || dst < lastDst){
                    nearX = vv.x;
                    nearY = vv.y;
                    lastDst = dst;
                }

                intersected = true;
            }
        }

        if(rect.contains(x1, y1)){
            nearX = x1;
            nearY = y1;
            intersected = true;
        }

        return intersected ? v2.set(nearX, nearY) : null;
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

    public interface QuadTreeHandler{
        boolean get(Rect rect, boolean tree);
    }
}
