package flame.graphics;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;

public class CacheBatch3D extends Batch{
    public FloatSeq data = new FloatSeq();
    public Seq<Texture> textureSeq = new Seq<>();

    public void begin(){
        data.clear();
        textureSeq.clear();
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
        for(int i = 0; i < 24; i += 6){
            data.addAll(spriteVertices[i], spriteVertices[i + 1], z, spriteVertices[i + 3], spriteVertices[i + 4], colorPacked);
        }
        textureSeq.add(texture);
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){}

    @Override
    protected void flush(){

    }
}
