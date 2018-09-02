package org.sourceheads.bridjfx.taskbar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.bridj.Pointer;
import org.bridj.cpp.com.COMRuntime;
import org.bridj.cpp.com.shell.ITaskbarList3;

import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * <p>Convenience wrapper for BridJ's {@link org.bridj.cpp.com.shell.ITaskbarList3} to be used with Java FX 8.</p>
 * <p>Calls to COM fail with code {@code 0x8001010e (RPC_E_WRONG_THREAD)} when performed from the Java FX UI thread.
 * This implementation is using a separate thread to process queued calls to the COM instance.</p>
 * <p>Usage:
 * <ul>
 * <li>instantiate from {@link javafx.application.Application#init()}</li>
 * <li>call {@link #init(javafx.stage.Stage)} from
 * {@link javafx.application.Application#start(javafx.stage.Stage)} <b>after</b> calling
 * {@link javafx.stage.Stage#show()}</li>
 * <li>make calls to {@link #setProgressState(ProgressState)} and {@link #setProgressValue(long, long)}</li>
 * <li>destroy from {@link javafx.application.Application#stop()}</li>
 * </ul></p>
 * <p>Note: This implementation is using reflection on deprecated JavaFX 8 methods to get the platform window's
 * native handle (see <a href="http://stackoverflow.com/a/19735944">this stackoverflow answer</a>).</p>
 *
 * @author Stefan Fiedler
 */
public class TaskbarList {

    private final ITaskbarList3 taskbarList;
    private volatile Pointer<Integer> hWnd;

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    private volatile Thread runner;

    /**
     * <p>Creates a new {@link org.bridj.cpp.com.shell.ITaskbarList3} wrapper.</p>
     */
    public TaskbarList() {
        try {
            taskbarList = COMRuntime.newInstance(ITaskbarList3.class);
        }
        catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * <p>Initialize the wrapper for the given primary stage window.</p>
     * <p>Make sure the stage is already shown (ie. this must be called after {@link javafx.stage.Stage#show()})
     * or else you'll get a {@code NullPointerException}.</p>
     *
     * @param primaryStage The primary stage
     */
    public void init(final Stage primaryStage) {
        hWnd = getWindowHwnd(primaryStage);

        runner = new Thread(() -> {
            while (running) {
                try {
                    final Runnable runnable = queue.take();
                    runnable.run();
                }
                catch (final InterruptedException ignore) {
                }
            }
        });
        runner.start();
    }

    /**
     * <p>Destroy the wrapper.</p>
     */
    public void destroy() {
        queue.clear();
        running = false;
        runner.interrupt();
        taskbarList.Release();
        hWnd.release();
    }

    /**
     * <p>Calls {@link org.bridj.cpp.com.shell.ITaskbarList3#SetProgressState(org.bridj.Pointer, org.bridj.ValuedEnum)}.
     * </p>
     * <p>See also
     * <a href="http://msdn.microsoft.com/en-us/library/windows/desktop/dd391697%28v=vs.85%29.aspx">ITaskbarList3::SetProgressState</a>
     * on MSDN.</p>
     *
     * @param state Progress state
     */
    public void setProgressState(final ProgressState state) {
        queue.offer(() -> taskbarList.SetProgressState(hWnd, state.getFlag()));
    }

    /**
     * <p>Calls {@link org.bridj.cpp.com.shell.ITaskbarList3#SetProgressValue(org.bridj.Pointer, long, long)}.</p>
     * <p>See also <a href="http://msdn.microsoft.com/en-us/library/windows/desktop/dd391698%28v=vs.85%29.aspx">ITaskbarList3::SetProgressValue</a>
     * on MSDN.</p>
     *
     * @param completed Completed value
     * @param total Total value
     */
    public void setProgressValue(final long completed, final long total) {
        queue.offer(() -> taskbarList.SetProgressValue(hWnd, completed, total));
    }

    private static Pointer<Integer> getWindowHwnd(final Window window) {
        try {
            final Method getPeer = Window.class.getDeclaredMethod("getPeer");
            getPeer.setAccessible(true);
            final Object tkStage = getPeer.invoke(window);
            final Method getPlatformWindow = tkStage.getClass().getDeclaredMethod("getPlatformWindow");
            getPlatformWindow.setAccessible(true);
            final Object platformWindow = getPlatformWindow.invoke(tkStage);
            final Method getNativeHandle = platformWindow.getClass().getMethod("getNativeHandle");
            final long hWnd = (Long) getNativeHandle.invoke(platformWindow);
            //noinspection Convert2MethodRef
            return Pointer.pointerToAddress(hWnd, Integer.class, p -> p.release());
        }
        catch (final IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
