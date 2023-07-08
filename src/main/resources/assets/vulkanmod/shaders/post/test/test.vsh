#version 460



const vec4 tri[3] = {
    vec4(1.f,1.f, 0.f, 1.f),
    vec4(1.f,-3.f, 0.f, 1.f),
    vec4(-3.f,1.f, 0.f, 1.f)
};

void main() {
  gl_Position = vec4(tri[gl_VertexIndex]);

}