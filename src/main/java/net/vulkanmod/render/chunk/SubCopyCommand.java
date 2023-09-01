package net.vulkanmod.render.chunk;

public record SubCopyCommand(long id, long bufferId, int offset, long dstOffset, long bufferSize) {

}
