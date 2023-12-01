package flame;

import arc.math.*;
import arc.struct.*;

public class WeightedRandom<T>{
    float lastValue = 0f;
    Seq<T> items = new Seq<>();
    FloatSeq weights = new FloatSeq();

    public void add(T t, float weight){
        if(weight <= 0f) return;
        items.add(t);
        weights.add(lastValue + weight);
        lastValue += weight;
    }

    public T get(){
        double rnd = Mathf.rand.nextDouble() * lastValue;
        int size = items.size;
        for(int i = 0; i < size; i++){
            float lw = i <= 0 ? -1f : weights.items[i - 1];
            float w = weights.items[i];
            if(rnd > lw && rnd <= w){
                return items.items[i];
            }
        }
        return items.random();
    }

    public void clear(){
        items.clear();
        weights.clear();
        lastValue = 0f;
    }
}
