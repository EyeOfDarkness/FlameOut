package flame;

import java.util.*;

@SuppressWarnings("unchecked")
public class HierarchyArray<T> implements Iterable<T>{
    public T[] array;
    public float[] scores;
    public int size = 0;

    HierarchyIterable<T> iterable;

    public HierarchyArray(int size){
        array = (T[])new Object[size];
        scores = new float[size];
    }

    public T get(int idx){
        if(idx >= size) return null;
        return array[idx];
    }

    public void add(T item, float score){
        if(size >= array.length) return;

        for(int i = 0; i < array.length; i++){
            T c = array[i];
            float s = scores[i];

            if(c == null){
                array[i] = item;
                scores[i] = score;
                size++;
                break;
            }else{
                if(score > s){
                    array[i] = item;
                    scores[i] = score;

                    item = c;
                    score = s;
                }
            }
        }
    }

    public void remove(T item){
        for(int i = 0; i < size; i++){
            T c = array[i];
            if(c == item){
                remove(i);
                break;
            }
        }
    }

    public void remove(int idx){
        for(int i = idx; i < size - 1; i++){
            T n = array[i + 1];
            float scr = scores[i + 1];
            array[i] = n;
            array[i + 1] = null;
            scores[i] = scr;
            scores[i + 1] = 0f;
        }
        array[size - 1] = null;
        scores[size - 1] = 0f;
        size--;
    }

    public void clear(){
        Arrays.fill(array, null);
        Arrays.fill(scores, 0f);
        size = 0;
    }

    @Override
    public Iterator<T> iterator(){
        if(iterable == null) iterable = new HierarchyIterable<>(this);
        return iterable.iterator();
    }

    public static class HierarchyIterable<T> implements Iterable<T>{
        final HierarchyArray<T> array;
        private final HierarchyIterator iterator1 = new HierarchyIterator(), iterator2 = new HierarchyIterator();

        HierarchyIterable(HierarchyArray<T> ar){
            array = ar;
        }

        @Override
        public Iterator<T> iterator(){
            if(iterator1.done){
                iterator1.index = 0;
                iterator1.done = false;
                return iterator1;
            }

            if(iterator2.done){
                iterator2.index = 0;
                iterator2.done = false;
                return iterator2;
            }

            return new HierarchyIterator();
        }

        private class HierarchyIterator implements Iterator<T>{
            int index = 0;
            boolean done = true;

            @Override
            public boolean hasNext(){
                if(index >= array.size) done = true;
                return index < array.size;
            }

            @Override
            public T next(){
                if(index >= array.size) throw new NoSuchElementException(String.valueOf(index));
                return array.array[index++];
            }

            @Override
            public void remove(){
                index--;
                array.remove(index);
            }
        }
    }
}
