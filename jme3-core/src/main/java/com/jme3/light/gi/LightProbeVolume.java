package com.jme3.light.gi;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.export.Savable;
import com.jme3.light.Light;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.jme3.util.TempVars;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Light Probe Volume.
 * <p>
 * LightProbeVolume samples diffuse indirect illumination at arbitrary positions in the scene by creating light probe data,<br/>
 * and provides high quality diffuse global illumination for dynamic/static objects in the large scene, similar to lightmaps<br/>
 * (which only support static objects), LightProbeVolume is baked offline and stored, then loaded and used efficiently at runtime.
 * </p>
 * @see <a href=https://ubm-twvideo01.s3.amazonaws.com/o1/vault/gdc2012/slides/Programming%20Track/Cupisz_Robert_Light_Probe_Interpolation.pdf>https://ubm-twvideo01.s3.amazonaws.com/o1/vault/gdc2012/slides/Programming%20Track/Cupisz_Robert_Light_Probe_Interpolation.pdf</a><br/>
 * @see <a href=https://www.cse.chalmers.se/~uffe/xjobb/bowald_final_master_thesis_v2.pdf>https://www.cse.chalmers.se/~uffe/xjobb/bowald_final_master_thesis_v2.pdf</a><br/>
 * @see <a href=http://melancholytree.com/thesis.pdf>http://melancholytree.com/thesis.pdf</a><br/>
 * @author JohnKkk
 */
public class LightProbeVolume extends Light implements Savable {

    private static final Logger logger = Logger.getLogger(LightProbeVolume.class.getName());
    private boolean ready = false;

    // light probe origin
    private Vector3f probeOrigin = new Vector3f(0, 0, 0);
    // light probe count
    private Vector3f probeCount = new Vector3f(8, 8, 4);
    // light probe step
    private Vector3f probeStep = new Vector3f(15.6f / 2.0f, 8.0f / 2.0f, 5.35f);
    private int totalCount = 0;
    // intensity
    private float diffuseGIIntensity = 1.0f;
    // light probe volume center
    private Vector3f volumeCenter;
    // light probe locations
    private ArrayList<Vector3f> probeLocations;

    private TextureArray probeOctahedralIrradiances;
    private TextureArray probeOctahedralFilteredDistances;

    public void setIndirectMultiplier(float indirectMultiplier) {
        this.diffuseGIIntensity = indirectMultiplier;
    }

    public float getIndirectMultiplier() {
        return diffuseGIIntensity;
    }

    public void setProbeCount(Vector3f probeCount) {
        this.probeCount = probeCount;
    }

    public Vector3f getProbeCount() {
        return probeCount;
    }

    public void setProbeOrigin(Vector3f probeOrigin) {
        this.probeOrigin = probeOrigin;
    }

    public Vector3f getProbeOrigin() {
        return probeOrigin;
    }

    public void setProbeStep(Vector3f probeStep) {
        this.probeStep = probeStep;
    }

    public Vector3f getProbeStep() {
        return probeStep;
    }

    public boolean isReady() {
        return ready;
    }

    public ArrayList<Vector3f> getProbeLocations() {
        return probeLocations;
    }

    public void placeProbes(){
        if(ready)return;
        totalCount = (int) ((int)(probeCount.x) * (int)(probeCount.y) * (int)(probeCount.z));
        probeLocations = new ArrayList<>(totalCount);

        Vector3f location = new Vector3f();
        Vector3f diff = new Vector3f();
        Vector3f temp = new Vector3f();
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        for(int z = 0, zNum = (int)probeCount.z;z < zNum;z++){
            for(int y = 0, yNum = (int)probeCount.y;y < yNum;y++){
                for(int x = 0, xNum = (int)probeCount.x;x < xNum;x++){
                    temp.set(x, y, z);
                    maxX = Math.max(x, maxX);
                    maxY = Math.max(y, maxY);
                    maxZ = Math.max(z, maxZ);
                    minX = Math.min(x, minX);
                    minY = Math.min(y, minY);
                    minZ = Math.min(z, minZ);
                    temp.mult(probeStep, diff);
                    probeOrigin.add(diff, location);
                    probeLocations.add(location.clone());
                }
            }
        }
        volumeCenter = new Vector3f((maxX - minX) * 0.5f, (maxY - minY) * 0.5f, (maxZ - minZ) * 0.5f);
//        ready = true;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void setProbeOctahedralIrradiances(List<Image> images) {
        this.probeOctahedralIrradiances = new TextureArray(images);
        this.probeOctahedralIrradiances.setWrap(Texture.WrapMode.EdgeClamp);
        this.probeOctahedralIrradiances.setWrap(Texture.WrapMode.EdgeClamp);
//        this.probeOctahedralIrradiances.setMinFilter(Texture.MinFilter.BilinearNearestMipMap);
//        this.probeOctahedralIrradiances.setMagFilter(Texture.MagFilter.Bilinear);
    }

    public void setProbeOctahedralFilteredDistances(List<Image> images) {
        this.probeOctahedralFilteredDistances = new TextureArray(images);
        this.probeOctahedralFilteredDistances.setWrap(Texture.WrapMode.EdgeClamp);
        this.probeOctahedralFilteredDistances.setWrap(Texture.WrapMode.EdgeClamp);
//        this.probeOctahedralFilteredDistances.setMinFilter(Texture.MinFilter.BilinearNearestMipMap);
//        this.probeOctahedralFilteredDistances.setMagFilter(Texture.MagFilter.Bilinear);
    }

    public TextureArray getProbeOctahedralIrradiances() {
        return probeOctahedralIrradiances;
    }

    public TextureArray getProbeOctahedralFilteredDistances() {
        return probeOctahedralFilteredDistances;
    }

    public int getTotalCount() {
        return totalCount;
    }

    @Override
    public boolean intersectsBox(BoundingBox box, TempVars vars) {
        return true;
    }

    @Override
    public boolean intersectsSphere(BoundingSphere sphere, TempVars vars) {
        return true;
    }

    @Override
    public boolean intersectsFrustum(Camera camera, TempVars vars) {
        return true;
    }

    @Override
    protected void computeLastDistance(Spatial owner) {
        if (owner.getWorldBound() != null) {
            BoundingVolume bv = owner.getWorldBound();
            lastDistance = bv.distanceSquaredTo(volumeCenter);
        } else {
            lastDistance = owner.getWorldTranslation().distanceSquared(volumeCenter);
        }
    }

    @Override
    public Type getType() {
        return Type.LightProbeVolume;
    }
}
