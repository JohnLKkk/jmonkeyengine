package com.jme3.renderer.gi;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Sphere;
import com.jme3.shader.plugins.GLSLLoader;
import com.jme3.system.JmeSystem;
import com.jme3.util.TempVars;

/**
 * @author JhonKkk
 */
public class VplVisualisation {
    protected static AssetManager assetManager;
    private static void initAssetManager(){
        if(assetManager == null){
            assetManager = JmeSystem.newAssetManager();
            assetManager.registerLocator(".", FileLocator.class);
            assetManager.registerLocator("/", ClasspathLocator.class);
            assetManager.registerLoader(J3MLoader.class, "j3m");
            assetManager.registerLoader(J3MLoader.class, "j3md");
            assetManager.registerLoader(GLSLLoader.class, "vert", "frag","geom","tsctrl","tseval","glsllib","glsl");
        }
    }

    public static AssetManager getAssetManager() {
        if(assetManager == null){
            initAssetManager();
        }
        return assetManager;
    }

    public final static Node visualizeVpl(float cellSize, int gridSize){

        TempVars tempVars = TempVars.get();

        Vector3f origin = tempVars.vect3.set(0, 0, 0);
        Vector3f step = tempVars.vect1.set(cellSize, cellSize, cellSize);

        Vector3f halfGridSize = tempVars.vect2.set(gridSize / 2, gridSize / 2, gridSize / 2);
        Vector3f halfSize = tempVars.vect4;
        halfSize.set(step);
        halfSize.multLocal(halfGridSize);

        Vector3f bottomLeft = tempVars.vect5;
        bottomLeft.set(origin);
        bottomLeft.subtractLocal(halfSize);

        Vector3f diff = tempVars.vect6;
        Vector3f pos = tempVars.vect7;

        // todo:后续调整为vpl数据,暂时用sphere实例化渲染
        initAssetManager();
        Sphere vpl = new Sphere(12, 12, 0.1f);
        Geometry vplGeo = new Geometry("vplGeo", vpl);
        Material vplGeoMat = new Material((MaterialDef) assetManager.loadAsset("Common/MatDefs/Misc/Unshaded.j3md"));
        vplGeoMat.setColor("Color", ColorRGBA.White);
        vplGeoMat.setBoolean("UseInstancing", true);
        vplGeo.setMaterial(vplGeoMat);
        InstancedNode vpls = new InstancedNode("VPLs");
        for(int z = 0;z < gridSize;z++){
            for(int y = 0;y < gridSize;y++){
                for(int x = 0;x < gridSize;x++){
                    diff.set(step);
                    diff.multLocal(x, y, z);

                    pos.set(bottomLeft);
                    pos.addLocal(diff);
                    pos.addLocal(0.5f, 0.5f, 0.5f);

                    Geometry nextVPL = vplGeo.clone(false);
                    nextVPL.setLocalTranslation(pos);
                    vpls.attachChild(nextVPL);
                }
            }
        }
        vpls.instance();

        tempVars.release();

        return vpls;
    }
}
