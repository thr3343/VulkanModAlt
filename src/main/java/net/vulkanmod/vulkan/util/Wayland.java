package net.vulkanmod.vulkan.util;

import org.lwjgl.system.Library;
import org.lwjgl.system.SharedLibrary;

public class Wayland {
    private static final SharedLibrary Wayland = Library.loadNative(Wayland.class, "net.vulkanmod.vulkan.util", null, "libWayland-client.so.0");

}
