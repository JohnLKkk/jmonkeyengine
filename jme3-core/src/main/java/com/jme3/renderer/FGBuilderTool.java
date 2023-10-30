package com.jme3.renderer;

import com.jme3.renderer.framegraph.FGPass;
import com.jme3.renderer.framegraph.FGRenderQueuePass;

import java.util.HashMap;
import java.util.Map;

/**
 * @author JhonKkk
 */
public class FGBuilderTool {
    private static Map<Class, FGPass> gPasses = new HashMap<>();
    public static final <T> void registerPass(Class<T> targetPassClass, FGPass targetPass){
        if(!gPasses.containsKey(targetPassClass)){
            gPasses.put(targetPassClass, targetPass);
        }else {
            throw new IllegalArgumentException(targetPassClass + " Already exists");
        }
    }
    public static final <T> void removePass(Class<T> targetRenderQueuePassClass){
        if(gPasses.containsKey(targetRenderQueuePassClass)){
            gPasses.remove(targetRenderQueuePassClass);
        }
    }
    public static final <T>T findPass(Class<T> targetPassClass){
        if(gPasses.containsKey(targetPassClass)){
            return (T) gPasses.get(targetPassClass);
        }else {
            throw new IllegalArgumentException("Unsupported class");
        }
    }
}
