package me.inf32768.ultimate_scaler.util;

import net.minecraft.util.math.BlockPos;

/**
 * 用于伪传参法的 ThreadLocal 持有者。
 * 在调用链中传递原始 BlockPos，绕过打包/解包。
 * 
 * <p>使用方法：
 * <ol>
 *   <li>在调用目标方法前，调用 {@link #set(BlockPos)} 存入坐标</li>
 *   <li>在目标方法内部，调用 {@link #get()} 读取坐标</li>
 *   <li>在使用完毕后，调用 {@link #clear()} 清理，防止内存泄漏</li>
 * </ol>
 */
public final class CoordinateHolder {

    private static final ThreadLocal<BlockPos> POS_HOLDER = new ThreadLocal<>();

    private CoordinateHolder() {}

    /**
     * 设置当前线程的 BlockPos。
     *
     * @param pos 要传递的 BlockPos，不能为 null
     */
    public static void set(BlockPos pos) {
        POS_HOLDER.set(pos);
    }

    /**
     * 获取当前线程的 BlockPos。
     *
     * @return 之前存入的 BlockPos，如果未设置则返回 null
     */
    public static BlockPos get() {
        return POS_HOLDER.get();
    }

    /**
     * 清除当前线程的 BlockPos，防止内存泄漏。
     * 务必在 finally 块中调用。
     */
    public static void clear() {
        POS_HOLDER.remove();
    }
}
