package com.jme3.material.logic;

import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.gi.LightProbeVolume;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;

public class LightProbeVolumeRender {
    private static String _g_ApplyGI = "g_ApplyGI";
    private static String _g_DiffuseGIIntensity = "g_DiffuseGIIntensity";
    private static String _g_ProbeCounts = "g_ProbeCounts";
    private static String _g_ProbeStartPosition = "g_ProbeStartPosition";
    private static String _g_ProbeStep = "g_ProbeStep";
    private static String _g_IrradianceProbeGrid = "IrradianceProbeGrid";
    private static String _g_MeanDistProbeGrid = "MeanDistProbeGrid";
    public static boolean setupLightProbeVolumes(Shader shader, Geometry geometry, LightList lightList, boolean removeLights) {
        // todo:Here find all LightProbeVolumes, calculate the light probes that need to be updated in the current view frustum
        for (int j = 0; j < lightList.size(); j++) {
            Light l = lightList.get(j);
            if (l instanceof LightProbeVolume) {
                if(removeLights){
                    lightList.remove(l);
                    LightProbeVolume lightProbeVolume = (LightProbeVolume)l;
                    if(lightProbeVolume.isReady()){
                        // todo:Assume there is only one LightProbeVolume for now
                        Uniform g_ProbeCounts = shader.getUniform(_g_ProbeCounts);
                        Uniform g_DiffuseGIIntensity = shader.getUniform(_g_DiffuseGIIntensity);
                        Uniform g_ProbeStartPosition = shader.getUniform(_g_ProbeStartPosition);
                        Uniform g_ProbeStep = shader.getUniform(_g_ProbeStep);
                        if(g_ProbeCounts != null){
                            g_ProbeCounts.setValue(VarType.Vector3, lightProbeVolume.getProbeCount());
                        }
                        if(g_DiffuseGIIntensity != null){
                            g_DiffuseGIIntensity.setValue(VarType.Float, lightProbeVolume.getIndirectMultiplier());
                        }
                        if(g_ProbeStartPosition != null){
                            g_ProbeStartPosition.setValue(VarType.Vector3, lightProbeVolume.getProbeOrigin());
                        }
                        if(g_ProbeStep != null){
                            g_ProbeStep.setValue(VarType.Vector3, lightProbeVolume.getProbeStep());
                        }
                        Material mat = geometry.getMaterial();
                        if(mat.getMaterialDef().getMaterialParam(_g_IrradianceProbeGrid) != null || mat.getTextureParam(_g_IrradianceProbeGrid) == null){
                            mat.setTexture(_g_IrradianceProbeGrid, lightProbeVolume.getProbeOctahedralIrradiances());
                        }
                        if(mat.getMaterialDef().getMaterialParam(_g_MeanDistProbeGrid) != null || mat.getTextureParam(_g_MeanDistProbeGrid) == null){
                            mat.setTexture(_g_MeanDistProbeGrid, lightProbeVolume.getProbeOctahedralFilteredDistances());
                        }
                        return true;
                    }
                    j--;
                }
            }
        }
        return false;
    }
    public static void enableGI(Shader shader, boolean enable){
        Uniform g_ApplyGI = shader.getUniform(_g_ApplyGI);
        if(g_ApplyGI != null){
            g_ApplyGI.setValue(VarType.Boolean, enable);
        }
    }
}
