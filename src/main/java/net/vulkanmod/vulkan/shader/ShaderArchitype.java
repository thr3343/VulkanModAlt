package net.vulkanmod.vulkan.shader;

import org.lwjgl.util.spvc.Spv;

public class ShaderArchitype
{
//    ConPositeStages compositeStage;

    //idnentify overallping reagions,a nd elmitate and sutine overallping reducndnaices into an aggerated verabel self-transforing "Shader Architype", ehich adapts istelf based on teh sepcif cchanges ans and when required, , ensuring no overlap or redcucnaes occur or take place and tehfore nay ratwed repated or refertcifnreudnat work/(ie swatsred works ) and that no work of stages.pahes are wstesddd arw wated at and gieve dkidbed
//Elimiate Dpendies of exqutive , and ensure fl;alcite Out of Outder Execution at all times of an at all viable at andt patcia;et time
//    i.e. to avoid unnCERy chaining.stalls, pipeliening.statls and or barred dkcheiodebsjdhdkensismdusidnm
//    Spv.SPV_REVISION
    //negated stages...
//TOOD; EXECUTionDpencies, execution
//    e.g. rendeirng terrain and Water and the same time simulatneousl;y, as mper mroe clos;ey isif pmroe cplsoeold;papxonting to vanilal behavuipir e.g.

    /*TODO:
     * AFAIK Muitple rendperasses are not posible to be active at the same time concurrently
     * So hypthetcially ErdnerAccesses/Pahes need to be organsied based on restive RenderPass Scope, anmd efor abse don what resoruces are needed to be available wtin tat spe=renderPassPRScope
     * and form ther if it it also posbel tcertaina d combine ecrtain Satges/(terrain+Watre, but not Combining Waving and terrainExComposte may be much
     * Terrain-> Extend Waving, Water
     * Deferrs/->Dpeht only, Bloom ->Postprocessing cresular rays, Motion blur, A i.e. crossing renderPass Scope ->=-> Bad
     * This chages if Multile.(i.e. Cocnurnet )faremebuffers are Posisble howver
     * Mutile State, conurent, stae, AccexsorExcutorState,
     * Contourne -> To bypass State
     * MAy BE easier to setup PostPRocesing/dfeered shaders beof settinup Inp-place or geomery/And./or "InPass" Shader stages/PahienExecutonmndeedsinedexdpeeddedde
     * TODO: Setup ShadercinfurtaINstnemodule config:eerdits: Allo ShaderStages,Modules tobe added/removed -. Enabled-.Dibled at any time +(Add Vnlal COnfigrtaopaetsidcPaamterisic shaders to be aded dustsned eirveduntousunnanimus bstarced divevfcvertededndierved
     *  derived shadrs
     * SO i isnlt just "bolt On", ad Vanlla "Shedsr" are treated as "Core Inern shadesr"andexternvonststatcocnstepr,verstie likage e.g.
     *
     *  Eploid redprasocnfigutaunoruferUBournCoifigtaiosltos, imairly ot the FramebuggerCofnigREcormatcigsyste Used wihtit Tjhe FrameBufferMathign reifgrutaion
     * and tefor apply the same to Renderpahse Scoping

     */
    public ShaderArchitype(ShaderUtil.ShaderStage[] shaderStage, RenderStateShades neededStagesAccess, ShaderComponant shaderComponant) {
//    i/e. Also a bit like the comopsite ALyer syteme used currently for rendrlayers/-Compiste layers e.g.blic ShaderArchitype(neededStages, ) {

        //StageComponats.stages are dybam ical consituted from the various stages, based the onteh vien need, pruspeo, function and consitage.dpendied sistuatiooens of the Variosu Shaders imepe,msts e..fi.e.i.e..etc .msic l.e.c i.ek/stc
        //Optimising ShaderStage ->subsutive elmitaion
//        this.compositeStage = compositeStage;
    }
}
