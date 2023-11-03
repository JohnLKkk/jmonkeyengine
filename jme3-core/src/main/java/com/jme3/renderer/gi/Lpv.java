/*
 * Copyright (c) 2009-2023 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.gi;

import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.RenderState;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;

/**
 * @author JhonKkk
 */
public class Lpv {
    private static int nextId = 0;
    private int rsmSize = 512;
    private int lpvGridSize = 32;
    private boolean rebuildLPV = false;
    private boolean injectionFinished = false;
    private boolean geometryInjectionFinished = false;

    private FrameBuffer injectionFB;
    private Geometry injectionData;
    private Material injectionMat;
    private FrameBuffer.FrameBufferTextureTarget injectionRRT;
    private FrameBuffer.FrameBufferTextureTarget injectionGRT;
    private FrameBuffer.FrameBufferTextureTarget injectionBRT;

    private FrameBuffer geometryInjectionFB;
    private Geometry geometryInjectionData;
    private Material geometryInjectionMat;
    private FrameBuffer.FrameBufferTextureTarget geometryInjectionRRT;
    private FrameBuffer.FrameBufferTextureTarget geometryInjectionGRT;
    private FrameBuffer.FrameBufferTextureTarget geometryInjectionBRT;

    private FrameBuffer propagationFB;
    private Geometry propagationData;
    private Material propagationInjectionMat;
    private FrameBuffer.FrameBufferTextureTarget propagationRRT;
    private FrameBuffer.FrameBufferTextureTarget propagationGRT;
    private FrameBuffer.FrameBufferTextureTarget propagationBRT;

    private FrameBuffer accumulatedFB;
    private FrameBuffer.FrameBufferTextureTarget accumulatedRRT;
    private FrameBuffer.FrameBufferTextureTarget accumulatedGRT;
    private FrameBuffer.FrameBufferTextureTarget accumulatedBRT;

    private RenderManager renderManager;
    private RenderState innerRenderState;

    private ArrayList<MRTData> mrtDataList;
    public Lpv(int rsmSize, int lpvGridSize, RenderManager renderManager){
        this.rsmSize = rsmSize;
        this.lpvGridSize = lpvGridSize;
        this.renderManager = renderManager;
        innerRenderState = new RenderState();
        mrtDataList = new ArrayList<>();
        setup();
    }

    private final void setup(){
        geometryInjectionFB = createFB(lpvGridSize, false);
        geometryInjectionRRT = createRT(lpvGridSize);
        geometryInjectionGRT = createRT(lpvGridSize);
        geometryInjectionBRT = createRT(lpvGridSize);
        geometryInjectionFB.addColorTarget(geometryInjectionRRT);
        geometryInjectionFB.addColorTarget(geometryInjectionGRT);
        geometryInjectionFB.addColorTarget(geometryInjectionBRT);
        geometryInjectionData = createInjectionData(rsmSize, rsmSize);
        geometryInjectionMat = new Material((MaterialDef) VplVisualisation.getAssetManager().loadAsset("Common/MatDefs/Lpv/Lpv.j3md"));
        geometryInjectionMat.selectTechnique("GeometryInject", renderManager);
        geometryInjectionData.setMaterial(geometryInjectionMat);
        geometryInjectionFB.setMultiTarget(true);
//        geometryInjectionFB.setUpdateNeeded();

        injectionFB = createFB(lpvGridSize, false);
        injectionData = createInjectionData(rsmSize, rsmSize);
        injectionMat = geometryInjectionMat.clone();
        injectionMat.selectTechnique(TechniqueDef.DEFAULT_TECHNIQUE_NAME, renderManager);
        injectionData.setMaterial(injectionMat);
        injectionRRT = createRT(lpvGridSize);
        injectionGRT = createRT(lpvGridSize);
        injectionBRT = createRT(lpvGridSize);
        injectionFB.addColorTarget(injectionRRT);
        injectionFB.addColorTarget(injectionGRT);
        injectionFB.addColorTarget(injectionBRT);
        injectionFB.setMultiTarget(true);
//        injectionFB.setUpdateNeeded();

        propagationFB = createFB(lpvGridSize, false);
        propagationData = createInjectionData(lpvGridSize * lpvGridSize, lpvGridSize);
        propagationInjectionMat = geometryInjectionMat.clone();
        propagationInjectionMat.selectTechnique("LightPropagation", renderManager);
        propagationData.setMaterial(propagationInjectionMat);
        propagationRRT = createRT(lpvGridSize);
        propagationGRT = createRT(lpvGridSize);
        propagationBRT = createRT(lpvGridSize);
        propagationFB.addColorTarget(propagationRRT);
        propagationFB.addColorTarget(propagationGRT);
        propagationFB.addColorTarget(propagationBRT);
        propagationFB.setMultiTarget(true);
//        propagationFB.setUpdateNeeded();

        accumulatedFB = createFB(lpvGridSize, false);
        accumulatedRRT = createRT(lpvGridSize);
        accumulatedGRT = createRT(lpvGridSize);
        accumulatedBRT = createRT(lpvGridSize);
        accumulatedFB.addColorTarget(accumulatedRRT);
        accumulatedFB.addColorTarget(accumulatedGRT);
        accumulatedFB.addColorTarget(accumulatedBRT);
        accumulatedFB.setMultiTarget(true);
//        accumulatedFB.setUpdateNeeded();

        mrtDataList.add(new MRTData(injectionFB, injectionRRT, injectionGRT, injectionBRT));
        mrtDataList.add(new MRTData(propagationFB, propagationRRT, propagationGRT, propagationBRT));
    }

    private FrameBuffer createFB(int size, boolean useDefaultAttachments){
        FrameBuffer frameBuffer = new FrameBuffer(size * size, size, 1);
        if(useDefaultAttachments){
            frameBuffer.addColorTarget(createRT(size));
            frameBuffer.addColorTarget(createRT(size));
            frameBuffer.addColorTarget(createRT(size));
            frameBuffer.setMultiTarget(true);
        }
        return frameBuffer;
    }

    private FrameBuffer.FrameBufferTextureTarget createRT(int size){
        return FrameBuffer.FrameBufferTarget.newTarget(createSRV(size));
    }

    private Texture2D createSRV(int size) {
        Texture2D rt = new Texture2D(size * size, size, Image.Format.RGBA16F);
        rt.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        rt.setMagFilter(Texture.MagFilter.Bilinear);
        rt.getImage().setMipmapsGenerated(false);
        return rt;
    }

    public MRTData getInjectionMRTData() {
        return new MRTData(injectionFB, injectionRRT, injectionGRT, injectionBRT);
    }
    public MRTData getAccumulatedMRTData() {
        return new MRTData(accumulatedFB, accumulatedRRT, accumulatedGRT, accumulatedBRT);
    }

    public MRTData getGeometryInjectionMRTData() {
        return new MRTData(geometryInjectionFB, geometryInjectionRRT, geometryInjectionGRT, geometryInjectionBRT);
    }
    public final void clearAccumulatedBuffer(){
        accumulatedFB.clearColorTargets();
        accumulatedFB.addColorTarget(accumulatedRRT);
        accumulatedFB.addColorTarget(accumulatedGRT);
        accumulatedFB.addColorTarget(accumulatedBRT);
        accumulatedFB.setMultiTarget(true);
        accumulatedFB.setUpdateNeeded();
        clearBuffer(accumulatedFB);
    }
    public final void clearGeometryInjectionBuffer(){
        clearBuffer(geometryInjectionFB);
    }
    public final void clearInjectionBuffer(){
        injectionFB.clearColorTargets();
        injectionFB.addColorTarget(injectionRRT);
        injectionFB.addColorTarget(injectionGRT);
        injectionFB.addColorTarget(injectionBRT);
        injectionFB.setMultiTarget(true);
        injectionFB.setUpdateNeeded();
        clearBuffer(injectionFB);
    }

    public final void clearBuffer(FrameBuffer fb){
        Camera currentCam = renderManager.getCurrentCamera();
        renderManager.getRenderer().setViewPort(0, 0, lpvGridSize * lpvGridSize, lpvGridSize);
        FrameBuffer currentFB = renderManager.getRenderer().getCurrentFrameBuffer();
        renderManager.getRenderer().setFrameBuffer(fb);
        renderManager.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
        renderManager.getRenderer().setViewPort(0, 0, lpvGridSize * lpvGridSize, lpvGridSize);
        renderManager.getRenderer().clearBuffers(true, true, true);
        renderManager.getRenderer().setFrameBuffer(currentFB);
        renderManager.setCamera(currentCam, false);
        renderManager.getRenderer().setBackgroundColor(ColorRGBA.Black);
    }

    public final void lightInjection(RSMInjection rsmInjection){
        injectionFinished = false;

        Texture2D rsmFlux = rsmInjection.getColorBuffer();
        Texture2D rsmPositions = rsmInjection.getPositionBuffer();
        Texture2D rsmNormals = rsmInjection.getNormalBuffer();

        Camera currentCam = renderManager.getCurrentCamera();
        RenderState currentRenderState = renderManager.getForcedRenderState();
        FrameBuffer currentFB = renderManager.getRenderer().getCurrentFrameBuffer();
        innerRenderState.setDepthTest(false);
        innerRenderState.setDepthFunc(RenderState.TestFunction.Less);
        innerRenderState.setBlendMode(RenderState.BlendMode.Additive);
        innerRenderState.setCustomBlendFactors(RenderState.BlendFunc.One, RenderState.BlendFunc.One, RenderState.BlendFunc.One, RenderState.BlendFunc.One);
        renderManager.setForcedRenderState(innerRenderState);
        injectionFB.clearColorTargets();
        injectionFB.addColorTarget(injectionRRT);
        injectionFB.addColorTarget(injectionGRT);
        injectionFB.addColorTarget(injectionBRT);
        injectionFB.setMultiTarget(true);
        injectionFB.setUpdateNeeded();
        renderManager.getRenderer().setFrameBuffer(injectionFB);
        // 保险起见,在这里设置viewPort和clipRect
        renderManager.getRenderer().setViewPort(0, 0, lpvGridSize * lpvGridSize, lpvGridSize);
        renderManager.getRenderer().setClipRect(0, 0, lpvGridSize * lpvGridSize, lpvGridSize);
        injectionMat.setTexture("RsmFlux", rsmFlux);
        injectionMat.setTexture("RsmWorldPositions", rsmPositions);
        injectionMat.setTexture("RsmWorldNormals", rsmNormals);
        injectionMat.setInt("GridSize", lpvGridSize);
        injectionMat.setInt("RsmSize", rsmSize);
        injectionMat.render(injectionData, renderManager);
        renderManager.getRenderer().setFrameBuffer(currentFB);
        renderManager.setForcedRenderState(currentRenderState);
        renderManager.setCamera(currentCam, false);

        injectionFinished = true;
    }

    public final void geometryInjection(RSMInjection rsmInjection, Vector3f directionalLight){
        geometryInjectionFinished = false;

        Texture2D rsmFlux = rsmInjection.getColorBuffer();
        Texture2D rsmPositions = rsmInjection.getPositionBuffer();
        Texture2D rsmNormals = rsmInjection.getNormalBuffer();

        if(injectionFinished){
            Camera currentCam = renderManager.getCurrentCamera();
            RenderState currentRenderState = renderManager.getForcedRenderState();
            FrameBuffer currentFB = renderManager.getRenderer().getCurrentFrameBuffer();
            innerRenderState.setDepthTest(true);
            innerRenderState.setDepthFunc(RenderState.TestFunction.LessOrEqual);
            innerRenderState.setBlendMode(RenderState.BlendMode.Off);
            renderManager.setForcedRenderState(innerRenderState);
            renderManager.getRenderer().setFrameBuffer(geometryInjectionFB);
            // 保险起见,在这里设置viewPort和clipRect
            renderManager.getRenderer().setViewPort(0, 0, lpvGridSize * lpvGridSize, lpvGridSize);
            renderManager.getRenderer().setClipRect(0, 0, lpvGridSize * lpvGridSize, lpvGridSize);
            renderManager.getRenderer().setBackgroundColor(new ColorRGBA(0, 0, 0, 0));
            renderManager.getRenderer().clearBuffers(true, true, true);
            geometryInjectionMat.setTexture("RsmFlux", rsmFlux);
            geometryInjectionMat.setTexture("RsmWorldPositions", rsmPositions);
            geometryInjectionMat.setTexture("RsmWorldNormals", rsmNormals);
            geometryInjectionMat.setInt("TextureSize", lpvGridSize);
            geometryInjectionMat.setInt("RsmSize", rsmSize);
            geometryInjectionMat.setVector3("LightDirection", directionalLight);
            geometryInjectionMat.render(geometryInjectionData, renderManager);
            renderManager.getRenderer().setFrameBuffer(currentFB);
            renderManager.getRenderer().setBackgroundColor(new ColorRGBA(0, 0, 0, 1));
            renderManager.setForcedRenderState(currentRenderState);
            renderManager.setCamera(currentCam, false);

            geometryInjectionFinished = true;
        }

    }
    public final void lightPropagation(int propagationIterations){
        MRTData readLPV = null, nextIterationLPV = null;
        for(int i = 0, lpvIndex = 0;i < propagationIterations;i++){
            lpvIndex = i & 1;
            readLPV = mrtDataList.get(lpvIndex);
            nextIterationLPV = mrtDataList.get(lpvIndex ^ 1);

            nextIterationLPV.getFrameBuffer().clearColorTargets();
            nextIterationLPV.getFrameBuffer().addColorTarget(nextIterationLPV.getRt0());
            nextIterationLPV.getFrameBuffer().addColorTarget(nextIterationLPV.getRt1());
            nextIterationLPV.getFrameBuffer().addColorTarget(nextIterationLPV.getRt2());
            nextIterationLPV.getFrameBuffer().setUpdateNeeded();
            clearBuffer(nextIterationLPV.getFrameBuffer());
            lightPropagationIteration(i, readLPV, nextIterationLPV);
        }
    }
    private final void lightPropagationIteration(int iteration, MRTData readLPV, MRTData nextIterationLPV){
        if(injectionFinished && geometryInjectionFinished){
            accumulatedFB.clearColorTargets();
            accumulatedFB.addColorTarget(accumulatedRRT);
            accumulatedFB.addColorTarget(accumulatedGRT);
            accumulatedFB.addColorTarget(accumulatedBRT);
            accumulatedFB.addColorTarget(nextIterationLPV.getRt0());
            accumulatedFB.addColorTarget(nextIterationLPV.getRt1());
            accumulatedFB.addColorTarget(nextIterationLPV.getRt2());
            accumulatedFB.setMultiTarget(true);
            accumulatedFB.setUpdateNeeded();

            // Don't use occlusion in first step to prevent self-shadowing
            boolean firstIteration = iteration <= 0;

            Camera currentCam = renderManager.getCurrentCamera();
            RenderState currentRenderState = renderManager.getForcedRenderState();
            FrameBuffer currentFB = renderManager.getRenderer().getCurrentFrameBuffer();
            innerRenderState.setDepthTest(false);
            innerRenderState.setBlendMode(RenderState.BlendMode.Additive);
            innerRenderState.setCustomBlendFactors(RenderState.BlendFunc.One, RenderState.BlendFunc.One, RenderState.BlendFunc.One, RenderState.BlendFunc.One);
            renderManager.setForcedRenderState(innerRenderState);
            renderManager.getRenderer().setFrameBuffer(accumulatedFB);
            // 保险起见,在这里设置viewPort和clipRect
            renderManager.getRenderer().setViewPort(0, 0, lpvGridSize * lpvGridSize, lpvGridSize);
            renderManager.getRenderer().setClipRect(0, 0, lpvGridSize * lpvGridSize, lpvGridSize);
//            renderManager.getRenderer().setBackgroundColor(new ColorRGBA(0, 0, 0, 0));
//            renderManager.getRenderer().clearBuffers(true, true, true);
            propagationInjectionMat.setTexture("RedContribution", readLPV.getRt0().getTexture());
            propagationInjectionMat.setTexture("GreenContribution", readLPV.getRt1().getTexture());
            propagationInjectionMat.setTexture("BlueContribution", readLPV.getRt2().getTexture());
            propagationInjectionMat.setTexture("RedGeometryVolume", geometryInjectionRRT.getTexture());
            propagationInjectionMat.setTexture("GreenGeometryVolume", geometryInjectionGRT.getTexture());
            propagationInjectionMat.setTexture("BlueGeometryVolume", geometryInjectionBRT.getTexture());
            propagationInjectionMat.setInt("GridSize", lpvGridSize);
            propagationInjectionMat.setBoolean("FirstIteration", firstIteration);
            propagationInjectionMat.render(propagationData, renderManager);
            renderManager.getRenderer().setFrameBuffer(currentFB);
            renderManager.getRenderer().setBackgroundColor(new ColorRGBA(0, 0, 0, 1));
            renderManager.setForcedRenderState(currentRenderState);
            renderManager.setCamera(currentCam, false);
        }
    }

    private Geometry createInjectionData(int w, int h){
        Mesh vplData = new Mesh();
        Vector3f[] vertices = new Vector3f[w * h];

        int vplId = 0;
        for(int x = 0;x < w;x++){
            for(int y = 0;y < h;y++){
                vertices[vplId++] = new Vector3f(x, y, 0);
            }
        }

        vplData.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        vplData.setMode(Mesh.Mode.Points);
        vplData.updateBound();

        Geometry geo = new Geometry("injectionData_" + (nextId++), vplData);
        return geo;
    }
}
