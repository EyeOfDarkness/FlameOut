package flame.unit.empathy;

import arc.struct.*;

import java.lang.reflect.*;

public class ReflectUtils{
    static ObjectMap<Class<?>, ObjectMap<String, Field>> fieldMap = new ObjectMap<>();

    /** GlennFolker */
    static Class<?> findClassf(Class<?> type, String field){
        for(type = type.isAnonymousClass() ? type.getSuperclass() : type; type != null; type = type.getSuperclass()){
            try{
                type.getDeclaredField(field);
                break;
            }catch(NoSuchFieldException ignored){}
        }

        return type;
    }
    public static Field findField(Class<?> type, String field){
        //Field f2 = fieldMap.get(type);
        //if(f2 != null) return f2;
        try{
            Class<?> origin = type.isAnonymousClass() ? type.getSuperclass() : type;
            ObjectMap<String, Field> ar;
            Field f2;
            if((ar = fieldMap.get(origin)) != null && (f2 = ar.get(field)) != null){
                return f2;
            }

            Field f = findClassf(type, field).getDeclaredField(field);
            f.setAccessible(true);

            //fieldMap.put(type, f);
            //if(!fieldMap2.containsKey(field)) fieldMap2.put(field, new ObjectMap<>());
            ObjectMap<String, Field> ff = fieldMap.get(origin, new ObjectMap<>());
            ff.put(field, f);
            return f;
        }catch(NoSuchFieldException e){
            throw new RuntimeException(e);
        }
    }
}
