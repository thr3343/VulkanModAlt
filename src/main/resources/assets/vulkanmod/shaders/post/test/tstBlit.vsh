#version 460



const vec2 tri[3] = {
    vec2(1.f,1.f),
    vec2(1.f,-3.f),
    vec2(-3.f,1.f)
};

void main() {
  gl_Position = vec4(tri[gl_VertexIndex], 0.0, 1.0);

}