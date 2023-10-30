package jme3test.pipeline;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.FGBuilderTool;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.FGGlobal;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.pass.*;
import com.jme3.scene.Geometry;
import com.jme3.system.AppSettings;
import com.jme3.util.TangentBinormalGenerator;
import jme3test.renderpath.RenderPathHelper;

/**
 * Custom pipeline, this example demonstrates the basic usage of CustomPipeline by customizing a GBufferPass.
 * @author JhonKkk
 */
public class TestCustomPipeline extends SimpleApplication {
    private FrameGraph customFrameGraph;
    // ------------------------------↓In this example, we only customize the GBufferPass, so get the other built-in Passes from the system
    private DeferredShadingPass defaultDeferredShadingPass;
    private TileDeferredShadingPass defaultTileDeferredShadingPass;
    private OpaquePass defaultOpaquePass;
    private SkyPass defaultSkyPass;
    private TransparentPass defaultTransparentPass;
    private GuiPass defaultGuiPass;
    private PostProcessorPass defaultPostProcessorPass;
    // ------------------------------↑In this example, we only customize the GBufferPass, so get the other built-in Passes from the system
    private CustomGBufferPass customGBufferPass;
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        if(customFrameGraph != null){
            // Switch rendering path per frame, reorganize FrameGraph, noteworthy is, no need to add all system FramePasses, just a minimal test case.
            customFrameGraph.reset();
            if(renderManager.getRenderPath() == RenderManager.RenderPath.Deferred){
                customFrameGraph.addPass(customGBufferPass);
                defaultDeferredShadingPass.setSinkLinkage(DeferredShadingPass.S_RT_0, customGBufferPass.getName() + "." + GBufferPass.S_RT_0);
                defaultDeferredShadingPass.setSinkLinkage(DeferredShadingPass.S_RT_1, customGBufferPass.getName() + "." + GBufferPass.S_RT_1);
                defaultDeferredShadingPass.setSinkLinkage(DeferredShadingPass.S_RT_4, customGBufferPass.getName() + "." + GBufferPass.S_RT_4);
                defaultDeferredShadingPass.setSinkLinkage(DeferredShadingPass.S_LIGHT_DATA, customGBufferPass.getName() + "." + GBufferPass.S_LIGHT_DATA);
                defaultDeferredShadingPass.setSinkLinkage(DeferredShadingPass.S_EXECUTE_STATE, customGBufferPass.getName() + "." + GBufferPass.S_EXECUTE_STATE);
                defaultDeferredShadingPass.setSinkLinkage(FGGlobal.S_DEFAULT_FB, customGBufferPass.getName() + "." + GBufferPass.S_FB);
                customFrameGraph.addPass(defaultDeferredShadingPass);
            }
            else if(renderManager.getRenderPath() == RenderManager.RenderPath.TiledDeferred){
                customFrameGraph.addPass(customGBufferPass);
                defaultTileDeferredShadingPass.setSinkLinkage(TileDeferredShadingPass.S_RT_0, customGBufferPass.getName() + "." + GBufferPass.S_RT_0);
                defaultTileDeferredShadingPass.setSinkLinkage(TileDeferredShadingPass.S_RT_1, customGBufferPass.getName() + "." + GBufferPass.S_RT_1);
                defaultTileDeferredShadingPass.setSinkLinkage(TileDeferredShadingPass.S_RT_4, customGBufferPass.getName() + "." + GBufferPass.S_RT_4);
                defaultTileDeferredShadingPass.setSinkLinkage(TileDeferredShadingPass.S_LIGHT_DATA, customGBufferPass.getName() + "." + GBufferPass.S_LIGHT_DATA);
                defaultTileDeferredShadingPass.setSinkLinkage(TileDeferredShadingPass.S_EXECUTE_STATE, customGBufferPass.getName() + "." + GBufferPass.S_EXECUTE_STATE);
                defaultTileDeferredShadingPass.setSinkLinkage(FGGlobal.S_DEFAULT_FB, customGBufferPass.getName() + "." + GBufferPass.S_FB);
                customFrameGraph.addPass(defaultTileDeferredShadingPass);
            }
            customFrameGraph.addPass(defaultOpaquePass);
            customFrameGraph.addPass(defaultSkyPass);
            customFrameGraph.addPass(defaultTransparentPass);
            customFrameGraph.addPass(defaultGuiPass);
            customFrameGraph.addPass(defaultPostProcessorPass);
        }
    }

    private final void setupScene(){
        Geometry teapot = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
        TangentBinormalGenerator.generate(teapot.getMesh(), true);

        teapot.setLocalScale(2f);
        renderManager.setSinglePassLightBatchSize(1);
        Material mat = new Material(assetManager, "jme3test/materials/MyCustomLighting.j3md");
        mat.setFloat("Shininess", 25);
        cam.setLocation(new Vector3f(0.015041917f, 0.4572918f, 5.2874837f));
        cam.setRotation(new Quaternion(-1.8875003E-4f, 0.99882424f, 0.04832061f, 0.0039016632f));

        mat.setColor("Ambient",  ColorRGBA.Black);
        mat.setColor("Diffuse",  ColorRGBA.Gray);
        mat.setColor("Specular", ColorRGBA.Gray);

        teapot.setMaterial(mat);
        rootNode.attachChild(teapot);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);
    }

    private final void setupCustomFrameGraph(){
        customFrameGraph = new FrameGraph(new FGRenderContext(renderManager, null, viewPort));
        defaultDeferredShadingPass = FGBuilderTool.findPass(DeferredShadingPass.class);
        defaultTileDeferredShadingPass = FGBuilderTool.findPass(TileDeferredShadingPass.class);
        // Redefine the overloaded material definition used by the custom ShadingModel here.
        if(defaultDeferredShadingPass != null){
            defaultDeferredShadingPass.setOverlyMat(new Material((MaterialDef) assetManager.loadAsset("jme3test/materials/MyCustomDeferredShading.j3md")));
            defaultDeferredShadingPass.reset();
            defaultDeferredShadingPass.getSinks().clear();
            defaultDeferredShadingPass.getBinds().clear();
            defaultDeferredShadingPass.init();
        }
        if(defaultTileDeferredShadingPass != null){
            defaultTileDeferredShadingPass.setOverlyMat(new Material((MaterialDef) assetManager.loadAsset("jme3test/materials/MyCustomTileBasedDeferredShading.j3md")));
            defaultTileDeferredShadingPass.reset();
            defaultTileDeferredShadingPass.getSinks().clear();
            defaultTileDeferredShadingPass.getBinds().clear();
            defaultTileDeferredShadingPass.init();
        }
        defaultOpaquePass = FGBuilderTool.findPass(OpaquePass.class);
        defaultSkyPass = FGBuilderTool.findPass(SkyPass.class);
        defaultTransparentPass = FGBuilderTool.findPass(TransparentPass.class);
        defaultGuiPass = FGBuilderTool.findPass(GuiPass.class);
        defaultPostProcessorPass = FGBuilderTool.findPass(PostProcessorPass.class);
        customGBufferPass = new CustomGBufferPass();
        FGBuilderTool.registerPass(CustomGBufferPass.class, customGBufferPass);
        viewPort.setFrameGraph(customFrameGraph);
        flyCam.setMoveSpeed(10.0f);
        renderManager.setRenderPath(RenderManager.RenderPath.Deferred);
        new RenderPathHelper(this, new Vector3f(10, cam.getHeight() - 10, 0), KeyInput.KEY_SPACE, "SPACE");
    }

    @Override
    public void simpleInitApp() {
        setupScene();
        setupCustomFrameGraph();
    }

    public static void main(String[] args) {
        TestCustomPipeline testCustomPipeline = new TestCustomPipeline();
        testCustomPipeline.start();
    }
}
