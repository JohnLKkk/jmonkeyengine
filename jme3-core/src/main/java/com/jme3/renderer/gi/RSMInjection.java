package com.jme3.renderer.gi;

import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightFilter;
import com.jme3.light.LightList;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

import java.util.logging.Logger;

/**
 * @author JhonKkk
 */
public class RSMInjection {
    private float injectionFactor = 13.0f;
    private GeometryList rsmInjectionTargets;
    // 默认执行pass
    private static final String RSM_INJECTION_TECH = "RSMInjection";
    private static class InjectionLightFilter implements LightFilter{
        private Light targetLight;
        public InjectionLightFilter(Light light){
            targetLight = light;
        }

        @Override
        public void setCamera(Camera camera) {

        }

        @Override
        public void filterLights(Geometry geometry, LightList filteredLightList) {
            filteredLightList.add(targetLight);
        }
    }
    private static final Logger logger = Logger.getLogger(RSMInjection.class.getName());
    private Light light;
    private InjectionLightFilter injectionLightFilter;
    private FrameBuffer rsmFB;
    private ViewPort rsmVP;
    private RenderState innerRenderState;
    private Texture2D colorBuffer;
    private Texture2D positionBuffer;
    private Texture2D normalBuffer;
    public RSMInjection(Light light, int cellSize){
        if(light == null){
            logger.throwing(RSMInjection.class.getName(), "<RSMInjection Const>", new NullPointerException("light is Null!"));
        }
        Light.Type ltp = light.getType();
        if(ltp == Light.Type.Probe || ltp == Light.Type.Ambient){
            logger.throwing(RSMInjection.class.getName(), "<RSMInjection Const>", new UnsupportedClassVersionError("光源类型不支持"));
        }
        this.light = light;
        injectionLightFilter = new InjectionLightFilter(light);
        rsmInjectionTargets = new GeometryList(new OpaqueComparator());
        innerRenderState = new RenderState();
        setup(cellSize);
    }
    private final void setup(int size){
        colorBuffer = createRT(size);
        positionBuffer = createRT(size);
        normalBuffer = createRT(size);
        Texture2D depthTexture = new Texture2D(size, size, Image.Format.Depth32F);

        FrameBuffer.FrameBufferTextureTarget rt0 = FrameBuffer.FrameBufferTarget.newTarget(colorBuffer);
        FrameBuffer.FrameBufferTextureTarget rt1 = FrameBuffer.FrameBufferTarget.newTarget(positionBuffer);
        FrameBuffer.FrameBufferTextureTarget rt2 = FrameBuffer.FrameBufferTarget.newTarget(normalBuffer);
        FrameBuffer.FrameBufferTextureTarget depth = FrameBuffer.FrameBufferTarget.newTarget(depthTexture);

        rsmFB = new FrameBuffer(size, size, 1);
        rsmFB.addColorTarget(rt0);
        rsmFB.addColorTarget(rt1);
        rsmFB.addColorTarget(rt2);
        rsmFB.setDepthTarget(depth);
        rsmFB.setMultiTarget(true);

        rsmVP = new ViewPort("RSMCamera", new Camera(size, size));
        rsmVP.setClearFlags(true, true, true);
        rsmVP.setBackgroundColor(ColorRGBA.Black);
        rsmVP.setOutputFrameBuffer(rsmFB);
        rsmVP.getCamera().setParallelProjection(true);
    }

    public Texture2D getColorBuffer() {
        return colorBuffer;
    }

    public Texture2D getPositionBuffer() {
        return positionBuffer;
    }

    public Texture2D getNormalBuffer() {
        return normalBuffer;
    }

    private Texture2D createRT(int size) {
        Texture2D rt = new Texture2D(size, size, Image.Format.RGBA32F);
        rt.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        rt.setMagFilter(Texture.MagFilter.Nearest);
        rt.getImage().setMipmapsGenerated(true);
        return rt;
    }
    private final void updateRSMCamera(){
        if(light.getType() == Light.Type.Directional){
            int size = 100 / 2;
            rsmVP.getCamera().setFrustumFar(size);
            rsmVP.getCamera().getRotation().lookAt(((DirectionalLight)light).getDirection(), rsmVP.getCamera().getUp());
            rsmVP.getCamera().update();
            rsmVP.getCamera().setFrustum(-size, size, -size, size, size, -size);
            rsmVP.getCamera().updateViewProjection();
        }
    }

    public final void injection(Spatial scene, RenderManager renderManager){
        updateRSMCamera();
        rsmVP.clearScenes();
        rsmVP.attachScene(scene);
        light.getColor().multLocal(injectionFactor);
        scene.updateGeometricState();
        LightFilter defaultLightFilter = renderManager.getLightFilter();
        FrameBuffer currentFB = renderManager.getRenderer().getCurrentFrameBuffer();
        renderManager.setLightFilter(injectionLightFilter);
        Camera currentCam = renderManager.getCurrentCamera();
        RenderState currentRenderState = renderManager.getForcedRenderState();
        innerRenderState.setDepthTest(true);
        innerRenderState.setBlendMode(RenderState.BlendMode.Off);
        innerRenderState.setDepthFunc(RenderState.TestFunction.LessOrEqual);
        renderManager.setForcedRenderState(innerRenderState);
        renderManager.setCamera(rsmVP.getCamera(), false);
        String lastForcedTech = renderManager.getForcedTechnique();
        renderManager.setForcedTechnique(RSM_INJECTION_TECH);
        renderManager.renderViewPort(rsmVP, 0.16F);
        renderManager.setLightFilter(defaultLightFilter);
        renderManager.setForcedTechnique(lastForcedTech);
        if(currentCam != null)
            renderManager.setCamera(currentCam, false);
        renderManager.getRenderer().setFrameBuffer(currentFB);
        renderManager.setForcedRenderState(currentRenderState);
        rsmVP.clearScenes();
        light.getColor().multLocal(1.0f/injectionFactor);
    }
}
