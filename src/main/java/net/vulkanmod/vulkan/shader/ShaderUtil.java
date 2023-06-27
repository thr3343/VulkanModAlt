package net.vulkanmod.vulkan.shader;

import net.minecraft.client.renderer.ShaderInstance;

public class ShaderUtil {




    public enum ShaderStage
    {
        GBUFFERS_BASIC("gbuffers_basic", 1),


        FINAL("final", 1);
        ShaderStage(String gbuffersBasic, int i) {

        }
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
