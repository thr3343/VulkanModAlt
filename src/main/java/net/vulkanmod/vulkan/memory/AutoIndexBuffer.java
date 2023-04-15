package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class AutoIndexBuffer {
    private static MappedBuffer buffer;
    private static final int rowReOrderDepth = 6*16;
    int vertexCount;
    DrawType drawType;
    IndexBuffer indexBuffer;

    public AutoIndexBuffer(int vertexCount, DrawType type) {
        this.drawType = type;

        createIndexBuffer(vertexCount);
    }

    private void createIndexBuffer(int vertexCount) {
        this.vertexCount = vertexCount;
        int size;


        switch (drawType) {
            case QUADS -> {
                size = vertexCount * 3 / 2 * IndexBuffer.IndexType.SHORT.size;
                genQuadIdxs(vertexCount);
            }
            case TRIANGLE_FAN -> {
                size = (vertexCount - 2) * 3 * IndexBuffer.IndexType.SHORT.size;
                genTriangleFanIdxs(vertexCount);
            }
            case TRIANGLE_STRIP -> {
                size = (vertexCount - 2) * 3 * IndexBuffer.IndexType.SHORT.size;
                genTriangleStripIdxs(vertexCount);
            }
            default -> throw new RuntimeException("unknown drawType");
        }

        indexBuffer = new IndexBuffer(size, MemoryTypes.GPU_MEM);
        indexBuffer.copyBuffer(buffer.buffer());
    }

    public void checkCapacity(int vertexCount) {
        if(vertexCount > this.vertexCount) {
            int newVertexCount = this.vertexCount * 2;
            System.out.println("Reallocating AutoIndexBuffer from " + this.vertexCount + " to " + newVertexCount);

            //TODO: free old
            //Can't know when VBO will stop using it
            indexBuffer.freeBuffer();
            createIndexBuffer(newVertexCount);
        }
    }

    public static void genQuadIdxs(int vertexCount) {
        //short[] idxs = {0, 1, 2, 0, 2, 3};

        int indexCount = vertexCount * 3 / 2;
        buffer = MappedBuffer.AddMappedBuffer(indexCount * Short.BYTES);

        //short[] idxs = new short[indexCount];

        int j = 0;
        for(int i = 0; i < vertexCount; i += 4) {

            buffer.putInt(j, i + 1 <<16 |i);

            buffer.putInt(j + 4, i << 16 | i + 2);

            buffer.putInt(j + 8, i + 3 <<16 |i + 2);

            j += 12;
        }

        //this.type.copyIndexBuffer(this, bufferSize, idxs);
    }

    public static void genTriangleFanIdxs(int vertexCount) {
        int indexCount = (vertexCount - 2) * 3;
        buffer = MappedBuffer.AddMappedBuffer(indexCount * Short.BYTES);


        //short[] idxs = byteBuffer.asShortBuffer().array();

        int j = 0;
        for(int index = 0; index < vertexCount; index += 4* rowReOrderDepth) {

            for (int i = rowReOrderDepth - 1; i >= 0; i--) {
                j = addIdxBlock(j, index+(i*4));
            }


        }


        //this.type.copyIndexBuffer(this, bufferSize, idxs);
    }

    private static int addIdxBlock(int vtx, int idx) {
        {
            buffer.putShort(vtx, (short) (idx));
            buffer.putShort(vtx + 2, (short) (idx + 1));
            buffer.putShort(vtx + 4, (short) (idx + 2));
            buffer.putShort(vtx + 6, (short) (idx + 2));
            buffer.putShort(vtx + 8, (short) (idx + 3));
            buffer.putShort(vtx + 10, (short) (idx));

            vtx += 12;
        }
        return vtx;
    }

    public static void genTriangleStripIdxs(int vertexCount) {
        int indexCount = (vertexCount - 2) * 3;

        //TODO: free buffer
        buffer = MappedBuffer.AddMappedBuffer(indexCount * Short.BYTES);


        //short[] idxs = byteBuffer.asShortBuffer().array();

        int j = 0;
        for (int i = 0; i < vertexCount - 2; ++i) {
//            idxs[j] = 0;
//            idxs[j + 1] = (short) (i + 1);
//            idxs[j + 2] = (short) (i + 2);

            buffer.putShort(j, (short) i);
            buffer.putShort(j + 2, (short) (i + 1));
            buffer.putShort(j + 4, (short) (i + 2));

            j += 6;
        }


    }

    public IndexBuffer getIndexBuffer() { return indexBuffer; }

    public enum DrawType {
        QUADS(7),
        TRIANGLE_FAN(6),
        TRIANGLE_STRIP(5);

        public final int n;

        DrawType (int n) {
            this.n = n;
        }
    }
}
