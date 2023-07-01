package net.vulkanmod.vulkan.shader;

import static net.vulkanmod.vulkan.shader.ShaderUtil.RenderPassScope.*;

public class ShaderUtil {




    public enum ShaderStage
    {
        BASIC("gbuffers_basic", MAIN),

        COMPOSITE("composite", MAIN), //TODO: Combine Composite Stages...
        FINAL("final", POST);
        ShaderStage(String gbuffersBasic, RenderPassScope i) {

        }
    }

    enum RenderPassScope
    {
        MAIN,
        POST,
        DEFERRED,
        PRE_SETUP;
    }
}

/*TODO:
*  ftransform() =
*  gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
* */
/*TODO: gl_MultiTexCoord0 =
   * uniform mat4 MVP;
   *
   * layout (location = 0) in vec3 position;
   *
   * void main()
   * {
   *     //gl_Position = projMat * viewMat * modelMat * vec4(position, 1.0);
   *     gl_Position = MVP * vec4(position, 1.0);
   * }
   * This bit of shader code performs the
*/
/*TODO: gl_Vertex =
*  layout(location = 0) in vec3 Position;
* */
//=======
//public class ShaderUtil {
//    public enum shaderType
//    {
//        POSITION_COLOR()
//    }
//}
//>>>>>>> test-1.20-port
