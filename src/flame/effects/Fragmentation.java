package flame.effects;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.entities.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

import java.util.*;

/**
 * @author EyeOfDarkness
 */
public class Fragmentation{
    float[] xs, ys;
    int width, height;
    float drawWidth, drawHeight;
    Seq<IntSeq> islands = new Seq<>();
    TextureRegion region;
    public Cons<FragmentEntity> onDeath;
    public Effect trailEffect = FlameFX.debrisSmoke, explosionEffect = FlameFX.fragmentExplosion;
    public Color effectColor = Color.white, drawnColor = Color.white.cpy();
    float shadowElevation = 0f;
    float layer = Layer.flyingUnit;

    static int[] returnArr = new int[3];
    static boolean[] occupied = new boolean[4];
    static FloatSeq fseq = new FloatSeq();
    static IntSeq intSeq1 = new IntSeq(), intSeq2 = new IntSeq();
    static int maxDimension = 150;
    static Vec2 tmpVec = new Vec2();
    static int[] arr = {
            0, 0,
            1, 0,
            1, 1,
            0, 1
    };

    public Fragmentation(TextureRegion region){
        this(region, Math.max(3, (int)((20f / 320) * region.width)), Math.max(3, (int)((20f / 320) * region.height)), Math.max(2, Math.min((int)((6f / (320f * 320f)) * region.width * region.height), 30)));
    }
    
    public Fragmentation(TextureRegion region, int width, int height){
        this(region, width, height, 4);
    }

    public Fragmentation(TextureRegion region, int width, int height, int fragments){
        if(width > maxDimension) width = maxDimension;
        if(height > maxDimension) height = maxDimension;

        xs = new float[width * height];
        ys = new float[width * height];
        this.width = width;
        this.height = height;
        this.region = region;

        drawWidth = width * Draw.scl;
        drawHeight = height * Draw.scl;

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                float fx = x / (width - 1f);
                float fy = y / (height - 1f);
                int idx = x + y * width;

                if(x > 0 && x < (width - 1)){
                    fx += Mathf.range(0.1f, 0.4f) / (width - 1f);
                }
                if(y > 0 && y < (height - 1)){
                    fy += Mathf.range(0.1f, 0.4f) / (height - 1f);
                }

                xs[idx] = fx;
                ys[idx] = fy;
            }
        }
        int size = (width - 1) * (height - 1);

        if(size > occupied.length){
            occupied = new boolean[size];
        }else{
            Arrays.fill(occupied, false);
        }

        intSeq1.clear();
        intSeq2.clear();

        int unoccupied = (width - 1) * (height - 1);

        for(int i = 0; i < fragments; i++){
            IntSeq island = new IntSeq();
            int iter = 0;
            while(iter < 100){
                int rx = Mathf.random(0, Math.max(width - 2, 0));
                int ry = Mathf.random(0, Math.max(height - 2, 0));
                int pos = islandPackPos(rx, ry);

                if(!occupied[pos]){
                    occupied[pos] = true;
                    intSeq1.add(pack(pos, i));
                    island.add(pos);
                    unoccupied--;
                    break;
                }
                iter++;
            }

            islands.add(island);
        }

        while(!intSeq1.isEmpty()){
            int[] items = intSeq1.items;
            int s = intSeq1.size;
            boolean fragType = unoccupied > ((width - 1) * (height - 1)) / 2;
            
            float totalWeight = 0f;
            float minimumWeight = 999999f;
            for(IntSeq is : islands){
                totalWeight += is.size;
                minimumWeight = Math.min(minimumWeight, is.size);
            }

            intSeq2.clear();
            for(int i = 0; i < s; i++){
                int[] data = unpack(items[i]);
                
                int sss = islands.get(data[2]).size;
                float chance = Mathf.clamp((sss - minimumWeight) / (totalWeight - minimumWeight));

                if(fragType){
                    if(Mathf.chance(0.75f * (1f - chance))){
                        for(int j = 0; j < 4; j++){
                            Point2 d4 = Geometry.d4(j);
                            int nx = data[0] + d4.x;
                            int ny = data[1] + d4.y;
                            int pos = islandPackPos(nx, ny);

                            if(inboundsIsland(nx, ny) && !occupied[pos]){
                                occupied[pos] = true;
                                unoccupied--;
                                intSeq2.add(pack(pos, data[2]));
                                islands.get(data[2]).add(pos);
                            }
                        }
                    }else{
                        intSeq2.add(items[i]);
                    }
                }else{
                    for(int j = 0; j < 4; j++){
                        Point2 d4 = Geometry.d4(j);
                        int nx = data[0] + d4.x;
                        int ny = data[1] + d4.y;
                        int pos = islandPackPos(nx, ny);

                        if(inboundsIsland(nx, ny) && !occupied[pos]){
                            occupied[pos] = true;
                            unoccupied--;
                            intSeq2.add(pack(pos, data[2]));
                            islands.get(data[2]).add(pos);
                        }
                    }
                    if(Mathf.chance(0.5f * chance)){
                        Point2 d = Geometry.d8edge[Mathf.random(3)];
                        int nx = data[0] + d.x;
                        int ny = data[1] + d.y;
                        int pos = islandPackPos(nx, ny);
                        if(inboundsIsland(nx, ny) && !occupied[pos]){
                            occupied[pos] = true;
                            unoccupied--;
                            intSeq2.add(pack(pos, data[2]));
                            islands.get(data[2]).add(pos);
                        }
                    }
                }
            }
            intSeq1.clear();
            intSeq1.addAll(intSeq2);
        }
        
        //boolean[] unoccupied = new boolean[(width - 1) * (height - 1)];
    }

    public static Fragmentation generate(float x, float y, float rotation, float layer, float elevation, TextureRegion texture, Cons<FragmentEntity> cons){
        return generate(x, y, rotation, texture.width * Draw.scl, texture.height * Draw.scl, layer, elevation, texture, cons);
    }

    public static Fragmentation generate(float x, float y, float rotation, float drawWidth, float drawHeight, float layer, float elevation, TextureRegion texture, Cons<FragmentEntity> cons){
        //this(region, Math.max(3, (int)((20f / 320) * region.width)), Math.max(3, (int)((20f / 320) * region.height)), Math.max(2, Math.min((int)((6f / (320f * 320f)) * region.width * region.height), 30)));
        int modW = (int)((20f / 320f) * Math.abs(drawWidth / Draw.scl)), modH = (int)((20f / 320f) * Math.abs(drawHeight / Draw.scl)), modC = Math.max(4, Math.min((int)((6f / (320f * 320f)) * Math.abs(drawWidth / Draw.scl) * Math.abs(drawHeight / Draw.scl)), 30));

        Fragmentation frag = new Fragmentation(texture, Math.max(modW, 3), Math.max(modH, 3), modC);
        frag.shadowElevation = elevation;
        frag.layer = layer;
        frag.drawWidth = drawWidth;
        frag.drawHeight = drawHeight;
        int idx = 0;
        for(IntSeq is : frag.islands){
            Vec3 pos = frag.getOffset(is);

            Vec2 wpos = tmpVec.trns(rotation, (pos.x - 0.5f) * frag.drawWidth, (pos.y - 0.5f) * frag.drawHeight).add(x, y);
            FragmentEntity fe = new FragmentEntity();
            fe.main = frag;
            fe.island = idx;
            fe.x = wpos.x;
            fe.y = wpos.y;
            fe.rotation = rotation;
            fe.boundSize = pos.z * Math.min(frag.drawWidth, frag.drawHeight);
            fe.offsetX = pos.x;
            fe.offsetY = pos.y;
            fe.lifetime = 3f * 60f + Mathf.random(25f);
            fe.calculateArea();
            cons.get(fe);

            fe.add();
            idx++;
        }
        return frag;
    }

    Vec3 getOffset(IntSeq island){
        float mx = 0f;
        float my = 0f;
        int count = 0;
        
        float minWidth = 2f, maxWidth = 0f;
        float minHeight = 2f, maxHeight = 0f;

        int size = island.size;
        int[] items = island.items;

        for(int t = 0; t < size; t++){
            int pos = items[t];
            int x = pos % (width - 1);
            int y = pos / (width - 1);

            for(int i = 0; i < 8; i += 2){
                int ox = x + arr[i];
                int oy = y + arr[i + 1];
                int idx = getIdx(ox, oy);
                
                float vx = xs[idx];
                float vy = ys[idx];
                
                minWidth = Math.min(minWidth, vx);
                maxWidth = Math.max(maxWidth, vx);
                
                minHeight = Math.min(minHeight, vx);
                maxHeight = Math.max(maxHeight, vx);

                mx += vx;
                my += vy;
                count++;
            }
        }
        float boundX = maxWidth - minWidth;
        float boundY = maxHeight - minHeight;
        float bounds = Math.min(boundX, boundY);

        return Tmp.v31.set(mx / count, my / count, bounds);
    }

    boolean inboundsIsland(int x, int y){
        return x >= 0 && x < (width - 1) && y >= 0 && y < (height - 1);
    }
    int islandPackPos(int x, int y){
        return (x + y * (width - 1));
    }
    int pack(int pos, int id){
        return pos | (id << 28);
    }
    int[] unpack(int value){
        int pos = value & 268435455;
        int id = value >>> 28;

        returnArr[0] = pos % (width - 1);
        returnArr[1] = pos / (width - 1);
        returnArr[2] = id;

        return returnArr;
    }

    int getIdx(int x, int y){
        return x + y * width;
    }

    public void draw(){
        Rand r = Utils.rand;
        r.setSeed(462 + (int)(Time.time / 120f));
        float lz = Draw.z();
        Draw.z(Layer.flyingUnit);
        TextureRegion region = UnitTypes.eclipse.region;
        //TextureRegion region = Core.atlas.white();

        float mcolr = Draw.getMixColor().toFloatBits();

        for(IntSeq island : islands){
            Draw.color(Tmp.c1.set(r.nextFloat(), r.nextFloat(), 0.5f));
            float color = Tmp.c1.toFloatBits();

            int[] items = island.items;
            int size = island.size;

            for(int t = 0; t < size; t++){
                fseq.clear();
                int x = items[t] % (width - 1);
                int y = items[t] / (width - 1);
                for(int i = 0; i < 8; i += 2){
                    int ox = x + arr[i];
                    int oy = y + arr[i + 1];
                    int idx = getIdx(ox, oy);
                    float wx = 250f + xs[idx] * 50f;
                    float wy = 250f + ys[idx] * 50f;

                    float u = Mathf.lerp(region.u, region.u2, xs[idx]);
                    float v = Mathf.lerp(region.v2, region.v, ys[idx]);

                    fseq.addAll(wx, wy, color, u, v, mcolr);
                }
                Draw.vert(region.texture, fseq.items, 0, fseq.size);
            }
        }
        Draw.z(lz);
        Draw.color();
    }

    public static class FragmentEntity extends DrawEntity{
        Fragmentation main;
        int island;
        boolean impact = false;

        float offsetX = 0f;
        float offsetY = 0f;
        public float boundSize = 0f;
        public float area = 0f;

        public float time = 0, lifetime = 0;

        public float z = 1f;
        public float rotation = 0f;
        public float vx, vy, vz, vr;

        void calculateArea(){
            int size = main.islands.get(island).size;
            area = Mathf.sqrt((((float)size) / ((main.width - 1f) * (main.height - 1f))) * (main.drawWidth * main.drawHeight)) * 1.5f;
            //area = (size / Math.min((main.width - 1f), (main.height - 1f))) * Math.min(main.drawWidth, main.drawHeight);
            //area = Mathf.sqrt((size * (main.drawWidth * main.drawHeight)) / ((main.width - 1f) * (main.height - 1f)));
        }

        @Override
        public void update(){
            x += vx * Time.delta;
            y += vy * Time.delta;
            z += vz * Time.delta;
            rotation += vr * Time.delta;

            float drag = 0.007f * Time.delta;
            
            if(z <= 0f){
                z = 0f;
                drag = 0.2f * Time.delta;

                if(!impact){
                    Tmp.c1.set(Color.black);
                    Tile tile = Vars.world.tileWorld(x, y);
                    if(tile != null){
                        Tmp.c1.set(tile.floor().mapColor).mul(1.1f);
                    }
                    FlameFX.fragmentGroundImpact.at(x, y, boundSize / 2, Tmp.c1);
                    Effect.shake(area / 12f, area / 12f, x, y);

                    impact = true;
                }
            }

            vx *= 1 - drag;
            vy *= 1 - drag;
            vz *= 1 - 0.002f * Time.delta;
            vz -= 0.01f;
            vr *= 1 - drag;

            if(Mathf.chance(0.6f) && z > 0){
                int size = Mathf.clamp((int)((boundSize * boundSize) / 900f), 1, 15);
                for(int i = 0; i < size; i++){
                    Tmp.v1.rnd(Mathf.random(boundSize / 3f)).add(x, y);
                    main.trailEffect.at(Tmp.v1.x, Tmp.v1.y, Mathf.random(5f, 9f), main.effectColor);
                }
            }

            if((time += Time.delta) >= lifetime){
                //if(removed != null) removed.run();
                main.explosionEffect.at(x, y, area / 2, main.effectColor);
                Effect.shake(area / 3f, area / 4f, x, y);
                if(main.onDeath != null) main.onDeath.get(this);
                remove();
            }
        }

        public void drawFragment(float cx, float cy){
            IntSeq is = main.islands.get(island);

            int[] items = is.items;
            int size = is.size;

            TextureRegion region = main.region;
            float[] xs = main.xs;
            float[] ys = main.ys;

            float color = Draw.getColor().toFloatBits();
            float mcolr = Color.clear.toFloatBits();

            float cos = Mathf.cosDeg(rotation);
            float sin = Mathf.sinDeg(rotation);

            for(int t = 0; t < size; t++){
                fseq.clear();
                int x = items[t] % (main.width - 1);
                int y = items[t] / (main.width - 1);
                for(int i = 0; i < 8; i += 2){
                    int ox = x + arr[i];
                    int oy = y + arr[i + 1];
                    int idx = main.getIdx(ox, oy);
                    float wx = (xs[idx] - offsetX) * main.drawWidth;
                    float wy = (ys[idx] - offsetY) * main.drawHeight;
                    //Vec2 vec = Tmp.v1.trns(rotation, wx, wy).add(this.x, this.y);

                    float tx = (wx * cos - wy * sin) + cx;
                    float ty = (wx * sin + wy * cos) + cy;

                    float u = Mathf.lerp(region.u, region.u2, xs[idx]);
                    float v = Mathf.lerp(region.v2, region.v, ys[idx]);

                    fseq.addAll(tx, ty, color, u, v, mcolr);
                }
                Draw.vert(region.texture, fseq.items, 0, fseq.size);
            }
        }

        @Override
        public void draw(){
            float zl = Mathf.clamp(Mathf.lerp(Math.min(Layer.groundUnit, main.layer), main.layer, z), Layer.blockUnder, Layer.flyingUnit + 1f);
            if(zl > Layer.bullet - 0.021f){
                zl = Math.max(Layer.effect + 0.021f, zl);
            }
            Draw.z(Math.min(Layer.darkness, zl - 1f));
            Draw.color(Pal.shadow);

            if((z * main.shadowElevation) > 0.001f){
                //float e = Mathf.clamp(unit.elevation, shadowElevation, 1f) * shadowElevationScl * (1f - unit.drownTime);
                float e = z * main.shadowElevation;
                float sx = x + UnitType.shadowTX * e, sy = y + UnitType.shadowTY * e;
                drawFragment(sx, sy);
            }
            //float z = isPayload ? Draw.z() : unit.elevation > 0.5f ? (lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit) : groundLayer + Mathf.clamp(hitSize / 4000f, 0, 0.01f);

            Draw.color();
            Draw.z(zl);
            Draw.color(main.drawnColor);
            drawFragment(x, y);
            Draw.color();
        }

        @Override
        public float clipSize(){
            int max = Math.max(main.region.width, main.region.height);
            return max * Draw.scl * 1.5f;
        }
    }
}
