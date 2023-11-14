package com.jme3.light.gi;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.environment.generation.JobProgressListener;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.shadow.AbstractShadowFilter;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * LightProbeVolumeBake is a tool class for baking LightProbeVolume, with two main functionalities:<br/>
 * 1. 360° capture light probe scene information (including radiance and distance)<br/>
 * 2. Pack radiance and distance into texture2DArray through octahedral mapping<br/>
 * @see LightProbeVolume
 * @author JohnKkk
 */
public class LightProbeVolumeBake extends BaseAppState {
    private static String _S_LIGHT_PROBE_DISTANCE_PASS = "LightProbeDistance";
    protected static Vector3f[] axisX = new Vector3f[6];
    protected static Vector3f[] axisY = new Vector3f[6];
    protected static Vector3f[] axisZ = new Vector3f[6];

    protected Image.Format imageFormat = Image.Format.RGB16F;

    public TextureCubeMap debugEnv;

    //Axis for cameras
    static {
        //PositiveX axis(left, up, direction)
        axisX[0] = Vector3f.UNIT_Z.mult(1f);
        axisY[0] = Vector3f.UNIT_Y.mult(-1f);
        axisZ[0] = Vector3f.UNIT_X.mult(1f);
        //NegativeX
        axisX[1] = Vector3f.UNIT_Z.mult(-1f);
        axisY[1] = Vector3f.UNIT_Y.mult(-1f);
        axisZ[1] = Vector3f.UNIT_X.mult(-1f);
        //PositiveY
        axisX[2] = Vector3f.UNIT_X.mult(-1f);
        axisY[2] = Vector3f.UNIT_Z.mult(1f);
        axisZ[2] = Vector3f.UNIT_Y.mult(1f);
        //NegativeY
        axisX[3] = Vector3f.UNIT_X.mult(-1f);
        axisY[3] = Vector3f.UNIT_Z.mult(-1f);
        axisZ[3] = Vector3f.UNIT_Y.mult(-1f);
        //PositiveZ
        axisX[4] = Vector3f.UNIT_X.mult(-1f);
        axisY[4] = Vector3f.UNIT_Y.mult(-1f);
        axisZ[4] = Vector3f.UNIT_Z;
        //NegativeZ
        axisX[5] = Vector3f.UNIT_X.mult(1f);
        axisY[5] = Vector3f.UNIT_Y.mult(-1f);
        axisZ[5] = Vector3f.UNIT_Z.mult(-1f);

    }
    protected Material precomputeLightProbeDistanceFallbackMat;

    // capture FBO
    protected Image images[];
    protected ViewPort[] viewports;
    protected FrameBuffer[] framebuffers;
    protected ByteBuffer[] buffers;

    // precompute FBO
    protected Texture2D irradianceTexture;
    protected FrameBuffer.FrameBufferTextureTarget irradianceRT;
    protected Texture2D filteredDistanceTexture;
    protected FrameBuffer.FrameBufferTextureTarget filteredDistanceRT;
    protected Image precomputeImage;
    protected ViewPort precomputeViewport;
    protected FrameBuffer precomputeFramebuffer;
    protected ByteBuffer precomputeBuffer;
    protected ByteBuffer precomputeBuffer2;

    protected Vector3f position = new Vector3f();
    protected ColorRGBA backGroundColor;

    private Material precomputeMat;
    private Picture precomputeGeo;

    private Texture2D sphereSamplesData;
    private ImageRaster sphereSamplesDataIO;

    /**
     * The size of capture cameras.
     */
    protected int captureSize = 256;

    /**
     * The size of octahedralMapping.
     */
    protected int irradianceOctahedralSize = 128;

    // bake parameters
    protected int irradianceNumSamples = 2048;
    protected float irradianceLobeSize = 0.99f;
    protected int filteredDistanceNumSamples = 128;
    protected float filteredDistanceLobSize = 0.12f;

    private final List<BakeJob> jobs = new ArrayList<>();

    /**
     * Creates an LightProbeVolumeBake with a size of 256
     */
    public LightProbeVolumeBake() {
    }

    public void setIrradianceNumSamples(int irradianceNumSamples) {
        this.irradianceNumSamples = irradianceNumSamples;
    }

    public int getIrradianceNumSamples() {
        return irradianceNumSamples;
    }

    public void setIrradianceLobeSize(float irradianceLobeSize) {
        this.irradianceLobeSize = irradianceLobeSize;
    }

    public float getIrradianceLobeSize() {
        return irradianceLobeSize;
    }

    public void setFilteredDistanceNumSamples(int filteredDistanceNumSamples) {
        this.filteredDistanceNumSamples = filteredDistanceNumSamples;
    }

    public int getFilteredDistanceNumSamples() {
        return filteredDistanceNumSamples;
    }

    public void setFilteredDistanceLobSize(float filteredDistanceLobSize) {
        this.filteredDistanceLobSize = filteredDistanceLobSize;
    }

    public float getFilteredDistanceLobSize() {
        return filteredDistanceLobSize;
    }

    /**
     * Takes a bake of the surrounding scene.
     *
     * @param scene the scene to bake.
     * @param lightProbeVolume the lightProbeVolume.
     * @param done a callback to call when the bake is done.
     */
    public void bakeLightProbeVolume(final Spatial scene, LightProbeVolume lightProbeVolume, final JobProgressListener<LightProbeVolume> done) {
        getApplication().enqueue(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                BakeJob job = new BakeJob(done, scene, lightProbeVolume);
                jobs.add(job);
                return null;
            }
        });
    }

    @Override
    public void render(final RenderManager renderManager) {
        if (isBusy()) {
            final BakeJob job = jobs.get(0);
            if(job.lightProbeVolume.isReady()){
                // clear old data
                job.lightProbeVolume.setReady(false);
            }
            int totalProbeCount = job.lightProbeVolume.getTotalCount();
            List<Image> irr_images = new ArrayList<>(totalProbeCount);
            List<Image> dis_images = new ArrayList<>(totalProbeCount);
            String oldTech = renderManager.getForcedTechnique();
            ViewPort mainViewPort = renderManager.getMainView("Default");
            if(mainViewPort != null){
                for(int i = 0;i < 6;i++){
                    for(SceneProcessor sceneProcessor : mainViewPort.getProcessors()){
                        if(sceneProcessor instanceof FilterPostProcessor){
                            FilterPostProcessor filterPostProcessor = (FilterPostProcessor)sceneProcessor;
                            for(Filter filter : filterPostProcessor.getFilterList()){
                                if(filter instanceof AbstractShadowFilter){
                                    if(filter instanceof DirectionalLightShadowFilter){
                                        DirectionalLightShadowFilter target = (DirectionalLightShadowFilter)filter;
                                        DirectionalLightShadowFilter directionalLightShadowFilter = new DirectionalLightShadowFilter(getApplication().getAssetManager(), target.getShadowMapSize(), 4);
                                        directionalLightShadowFilter.setLight(target.getLight());
                                        directionalLightShadowFilter.setShadowIntensity(target.getShadowIntensity());
                                        FilterPostProcessor filterPostProcessor1 = new FilterPostProcessor(getApplication().getAssetManager());
                                        filterPostProcessor1.addFilter(directionalLightShadowFilter);
                                        viewports[i].addProcessor(filterPostProcessor1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            renderManager.enableBakeLightProbeVolume(true);
            TextureCubeMap map = null;
            for(Vector3f probePosition : job.lightProbeVolume.getProbeLocations()){
                position.set(probePosition);
                capture(renderManager, job.scene, true);
                map = EnvMapUtils.makeCubeMap(images[0],
                        images[1], images[2], images[3], images[4], images[5],
                        imageFormat);
                map.getImage().setColorSpace(ColorSpace.Linear);
                precomputeMat.setBoolean("IsBuildIrradiance", true);
                precomputeImage = preComputeProbeData(irradianceTexture, irradianceRT, renderManager, map, irradianceNumSamples, irradianceLobeSize);
                precomputeImage.setColorSpace(ColorSpace.Linear);
                irr_images.add(precomputeImage);
                precomputeImage.dispose();
                map.getImage().dispose();

//                renderManager.setForcedTechnique(_S_LIGHT_PROBE_DISTANCE_PASS);
                renderManager.setForcedMaterial(precomputeLightProbeDistanceFallbackMat);
                capture(renderManager, job.scene, false);
                map = EnvMapUtils.makeCubeMap(images[0],
                        images[1], images[2], images[3], images[4], images[5],
                        imageFormat);
                map.getImage().setColorSpace(ColorSpace.Linear);
//                renderManager.setForcedTechnique(oldTech);
                renderManager.setForcedMaterial(null);

                precomputeMat.setBoolean("IsBuildIrradiance", false);
                precomputeImage = preComputeProbeData(filteredDistanceTexture, filteredDistanceRT, renderManager, map, filteredDistanceNumSamples, filteredDistanceLobSize);
                precomputeImage.setColorSpace(ColorSpace.Linear);
                dis_images.add(precomputeImage);
                precomputeImage.dispose();
                map.getImage().dispose();
            }

            job.lightProbeVolume.setProbeOctahedralIrradiances(irr_images);
            job.lightProbeVolume.setProbeOctahedralFilteredDistances(dis_images);
            job.lightProbeVolume.setReady(true);
            renderManager.enableBakeLightProbeVolume(false);
            job.callback.done(job.lightProbeVolume);
            jobs.remove(0);
        }
    }
    private final Image preComputeProbeData(Texture2D texture, FrameBuffer.FrameBufferTextureTarget rt, RenderManager renderManager, TextureCubeMap cubeMap, int numSamples, float lobeSize){
        // precompute data
        precomputeFramebuffer.clearColorTargets();
        precomputeFramebuffer.addColorTarget(rt);
        precomputeMat.setInt("NumSamples", numSamples);
        precomputeMat.setFloat("LobeSize", lobeSize);
        precomputeMat.setTexture("Radiance", cubeMap);
        renderManager.renderViewPort(precomputeViewport, 0.16f);
        ByteBuffer buff;
        if(texture == irradianceTexture){
            precomputeBuffer = BufferUtils.createByteBuffer(irradianceOctahedralSize * irradianceOctahedralSize * texture.getImage().getFormat().getBitsPerPixel() / 8);
            buff = precomputeBuffer;
        }
        else{
            precomputeBuffer2 = BufferUtils.createByteBuffer(irradianceOctahedralSize * irradianceOctahedralSize * texture.getImage().getFormat().getBitsPerPixel() / 8);
            buff = precomputeBuffer2;
        }
        buff.clear();
        renderManager.getRenderer().readFrameBufferWithFormat(
                precomputeFramebuffer, buff, texture.getImage().getFormat());
        precomputeImage = new Image(texture.getImage().getFormat(), irradianceOctahedralSize, irradianceOctahedralSize, buff,
                ColorSpace.Linear);
        return precomputeImage;
    }

    /**
     * capture scene.
     * @param renderManager
     * @param scene
     */
    private final void capture(RenderManager renderManager, Spatial scene, boolean postProcessor){
        for (int i = 0; i < 6; i++) {
            viewports[i].getCamera().setLocation(position);
            viewports[i].clearScenes();
            viewports[i].attachScene(scene);
            if(!postProcessor && viewports[i].getProcessors().size() > 0){
                FilterPostProcessor filterPostProcessor = (FilterPostProcessor) viewports[i].getProcessors().get(0);
                for(Filter filter : filterPostProcessor.getFilterList()){
                    filter.setEnabled(false);
                }
            }
            renderManager.renderViewPort(viewports[i], 0.16f);
            if(buffers[i] == null){
                buffers[i] = BufferUtils.createByteBuffer(
                        captureSize * captureSize * imageFormat.getBitsPerPixel() / 8);
            }
            buffers[i].clear();
            renderManager.getRenderer().readFrameBufferWithFormat(
                    framebuffers[i], buffers[i], imageFormat);
            images[i] = new Image(imageFormat, captureSize, captureSize, buffers[i],
                    ColorSpace.Linear);
            if(!postProcessor && viewports[i].getProcessors().size() > 0){
                FilterPostProcessor filterPostProcessor = (FilterPostProcessor) viewports[i].getProcessors().get(0);
                for(Filter filter : filterPostProcessor.getFilterList()){
                    filter.setEnabled(true);
                }
            }
        }
    }

    /**
     * Alter the background color of an initialized LightProbeVolumeBake.
     *
     * @param bgColor the desired color (not null, unaffected, default is the
     * background color of the application's default viewport)
     */
    public void setBackGroundColor(ColorRGBA bgColor) {
        if (!isInitialized()) {
            throw new IllegalStateException(
                    "The LightProbeVolumeBake is uninitialized.");
        }

        backGroundColor.set(bgColor);
        for (int i = 0; i < 6; ++i) {
            viewports[i].setBackgroundColor(bgColor);
        }
    }

    /**
     * Gets the size of bake cameras.
     *
     * @return the size of bake cameras.
     */
    public int getCaptureSize() {
        return captureSize;
    }

    /**
     * Sets the size of bake cameras and rebuild this state if it was initialized.
     *
     * @param captureSize the size of bake cameras.
     */
    public void setCaptureSize(final int captureSize) {
        this.captureSize = captureSize;
        rebuild();
    }

    private ArrayList<Vector3f> createPointsInSphere(int count){
        ArrayList<Vector3f> points = new ArrayList<>(count);

        for(int i = 0;i < count;i++){

            float x = 0, y = 0, z = 0;
            float lengthSquared = 0;

            do {
                x = (float) (Math.random() * 2.0f - 1.0f);
                y = (float) (Math.random() * 2.0f - 1.0f);
                z = (float) (Math.random() * 2.0f - 1.0f);
                lengthSquared = x * x + y * y + z * z;
            }while(lengthSquared >= 1.0f);

            float length = (float) Math.sqrt(lengthSquared);

            points.add(new Vector3f(x / length, y / length, z / length));
        }

        return points;
    }

    private void createSphereSamples(){
        if(sphereSamplesData == null){
            int size = 4096;
            sphereSamplesData = new Texture2D(size, 1, Image.Format.RGBA32F);
            sphereSamplesData.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            sphereSamplesData.setMagFilter(Texture.MagFilter.Nearest);
            sphereSamplesData.setWrap(Texture.WrapMode.EdgeClamp);
            ByteBuffer data = BufferUtils.createByteBuffer( (int) Math.ceil(Image.Format.RGBA32F.getBitsPerPixel() / 8.0) * size);
            Image convertedImage = new Image(Image.Format.RGBA32F, size, 1, data, null, ColorSpace.Linear);
            sphereSamplesData.setImage(convertedImage);
            sphereSamplesData.getImage().setMipmapsGenerated(false);
            sphereSamplesDataIO = ImageRaster.create(sphereSamplesData.getImage());

            ArrayList<Vector3f> points = createPointsInSphere(size);
            ColorRGBA temp = new ColorRGBA();
            int i = 0;
            for(Vector3f v : points){
                temp.set(v.x, v.y, v.z, 0);
                sphereSamplesDataIO.setPixel(i++, 0, temp);
            }
            sphereSamplesData.getImage().setUpdateNeeded();
        }
    }

    /**
     * Rebuild all bake cameras.
     */
    protected void rebuild() {

        if (!isInitialized()) {
            return;
        }

        cleanup(getApplication());
        initialize(getApplication());
    }

    /**
     * Returns an array of the 6 ViewPorts used to capture the bake.
     * Note that this will be null until after initialize() is called.
     * @return array of ViewPorts
     */
    public ViewPort[] getViewPorts(){
        return viewports;
    }

    /**
     * Test whether this LightProbeVolumeBake is busy. Avoid reconfiguring while
     * busy!
     *
     * @return true if busy, otherwise false
     */
    public boolean isBusy() {
        boolean result = !jobs.isEmpty();
        return result;
    }

    @Override
    protected void initialize(Application app) {
        this.backGroundColor = app.getViewPort().getBackgroundColor().clone();

        final Camera[] cameras = new Camera[6];
        final Texture2D[] textures = new Texture2D[6];

        viewports = new ViewPort[6];
        framebuffers = new FrameBuffer[6];
        buffers = new ByteBuffer[6];
        images = new Image[6];

        for (int i = 0; i < 6; i++) {
            cameras[i] = createOffCamera(captureSize, position, axisX[i], axisY[i], axisZ[i]);
            viewports[i] = createOffViewPort("CaptureView" + i, cameras[i]);
            viewports[i].setRenderPath(RenderManager.RenderPath.Forward);
            framebuffers[i] = createOffScreenFrameBuffer(captureSize, viewports[i]);
            textures[i] = new Texture2D(captureSize, captureSize, imageFormat);
            framebuffers[i].setColorTexture(textures[i]);
        }

        Camera precomputeCamera = new Camera(irradianceOctahedralSize, irradianceOctahedralSize);
        precomputeCamera.setFrustumPerspective(45.0f, 4.0f / 3.0f, 0.1f, 100.0f);
        precomputeViewport = createOffViewPort("PreComputeView", precomputeCamera);
        precomputeViewport.setBackgroundColor(ColorRGBA.Black);
        precomputeFramebuffer = createOffScreenFrameBuffer(irradianceOctahedralSize, precomputeViewport);
        irradianceTexture = new Texture2D(irradianceOctahedralSize, irradianceOctahedralSize, Image.Format.RGB111110F);
        irradianceTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        irradianceTexture.setMagFilter(Texture.MagFilter.Bilinear);
        irradianceTexture.setWrap(Texture.WrapMode.EdgeClamp);
        irradianceRT = FrameBuffer.FrameBufferTarget.newTarget(irradianceTexture);
        filteredDistanceTexture = new Texture2D(irradianceOctahedralSize, irradianceOctahedralSize, Image.Format.RG32F);
        filteredDistanceTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        filteredDistanceTexture.setMagFilter(Texture.MagFilter.Bilinear);
        filteredDistanceTexture.setWrap(Texture.WrapMode.EdgeClamp);
        filteredDistanceRT = FrameBuffer.FrameBufferTarget.newTarget(filteredDistanceTexture);

        precomputeMat = new Material(app.getAssetManager(), "Common/MatDefs/Gi/Irradiance.j3md");
        precomputeGeo = new Picture("PreComputeGeo");
        precomputeGeo.setWidth(1);
        precomputeGeo.setHeight(1);
        precomputeMat.selectTechnique("PackToOctahedral", app.getRenderManager());
        precomputeGeo.setMaterial(precomputeMat);
        precomputeLightProbeDistanceFallbackMat = new Material(app.getAssetManager(), "Common/MatDefs/Gi/LightProbeDistance.j3md");
        precomputeLightProbeDistanceFallbackMat.selectTechnique(_S_LIGHT_PROBE_DISTANCE_PASS, app.getRenderManager());

        createSphereSamples();
        precomputeMat.setTexture("SphereSamples", sphereSamplesData);

        precomputeViewport.attachScene(precomputeGeo);
        precomputeGeo.updateGeometricState();
        precomputeViewport.setRenderPath(RenderManager.RenderPath.Forward);
    }

    @Override
    protected void cleanup(Application app) {
        this.backGroundColor = null;

        for (final FrameBuffer frameBuffer : framebuffers) {
            frameBuffer.dispose();
        }

        for (final Image image : images) {
            if( image != null){
                image.dispose();
            }
        }
    }

    /**
     * returns the images format used for the generated maps.
     *
     * @return the enum value
     */
    public Image.Format getImageFormat() {
        return imageFormat;
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    /**
     * Creates an off camera
     *
     * @param mapSize the size
     * @param worldPos the position
     * @param axisX the x axis
     * @param axisY the y axis
     * @param axisZ tha z axis
     * @return a new instance
     */
    protected Camera createOffCamera(final int mapSize, final Vector3f worldPos, final Vector3f axisX, final Vector3f axisY, final Vector3f axisZ) {
        final Camera offCamera = new Camera(mapSize, mapSize);
        offCamera.setLocation(worldPos);
        offCamera.setAxes(axisX, axisY, axisZ);
        offCamera.setFrustumPerspective(90f, 1f, 0.1f, 100);
        offCamera.setLocation(position);
        return offCamera;
    }

    /**
     * creates an off-screen VP
     *
     * @param name the desired name for the offscreen viewport
     * @param offCamera the Camera to be used (alias created)
     * @return a new instance
     */
    protected ViewPort createOffViewPort(final String name, final Camera offCamera) {
        final ViewPort offView = new ViewPort(name, offCamera);
        offView.setClearFlags(true, true, true);
        offView.setBackgroundColor(backGroundColor);
        return offView;
    }

    /**
     * create an offscreen frame buffer.
     *
     * @param mapSize the desired size (pixels per side)
     * @param offView the off-screen viewport to be used (alias created)
     * @return a new instance
     */
    protected FrameBuffer createOffScreenFrameBuffer(int mapSize, ViewPort offView) {
        // create offscreen framebuffer
        final FrameBuffer offBuffer = new FrameBuffer(mapSize, mapSize, 1);
        offBuffer.setDepthBuffer(Image.Format.Depth);
        offView.setOutputFrameBuffer(offBuffer);
        return offBuffer;
    }

    /**
     * An inner class to keep track on a bake job.
     */
    protected class BakeJob {

        JobProgressListener<LightProbeVolume> callback;
        Spatial scene;
        LightProbeVolume lightProbeVolume;

        @SuppressWarnings("unchecked")
        public BakeJob(JobProgressListener callback, Spatial scene, LightProbeVolume lightProbeVolume) {
            this.callback = callback;
            this.scene = scene;
            this.lightProbeVolume = lightProbeVolume;
        }
    }
}
