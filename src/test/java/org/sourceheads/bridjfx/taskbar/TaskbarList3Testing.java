package org.sourceheads.bridjfx.taskbar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import org.bridj.Pointer;
import org.bridj.cpp.com.COMRuntime;
import org.bridj.cpp.com.shell.ITaskbarList3;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Testing play ground.
 *
 * @author Stefan Fiedler
 */
public class TaskbarList3Testing extends Application {

    private static ITaskbarList3 taskbarList;
    private static final BlockingQueue<Runnable> RUNNABLES = new LinkedBlockingQueue<>();
    private static volatile boolean running = true;
    private static volatile Thread runner;
    private static Pointer<Integer> hWnd;

    @Override
    public void start(final Stage primaryStage) throws Exception {
        // final Pointer<Integer> windowHwnd = getWindowHwnd(primaryStage);

        final Button btn25 = new Button("25");
        btn25.setOnAction(event -> call("25", () -> taskbarList.SetProgressValue(hWnd, 25, 100)));
        final Button btn50 = new Button("50");
        btn50.setOnAction(event -> call("50", () -> taskbarList.SetProgressValue(hWnd, 50, 100)));
        final Button btn75 = new Button("75");
        btn75.setOnAction(event -> call("75", () -> taskbarList.SetProgressValue(hWnd, 75, 100)));

        final Button btnIndet = new Button("Indet");
        btnIndet.setOnAction(event -> call("Indet", () -> taskbarList.SetProgressState(hWnd, ITaskbarList3.TbpFlag.TBPF_INDETERMINATE)));
        final Button btnPaused = new Button("Paused");
        btnPaused.setOnAction(event -> call("Paused", () -> taskbarList.SetProgressState(hWnd, ITaskbarList3.TbpFlag.TBPF_PAUSED)));
        final Button btnError = new Button("Error");
        btnError.setOnAction(event -> call("Error", () -> taskbarList.SetProgressState(hWnd, ITaskbarList3.TbpFlag.TBPF_ERROR)));
        final Button btnStop = new Button("Stop");
        btnStop.setOnAction(event -> call("Stop", () -> taskbarList.SetProgressState(hWnd, ITaskbarList3.TbpFlag.TBPF_NOPROGRESS)));

        final HBox hbButtons = new HBox(btn25, btn50, btn75, btnIndet, btnPaused, btnError, btnStop);
        hbButtons.setSpacing(8);

        final VBox vBox = new VBox(hbButtons);
        vBox.setPadding(new Insets(8, 8, 8, 8));

        final Scene scene = new Scene(vBox);
        primaryStage.setScene(scene);
        primaryStage.setTitle("TaskbarList testing");

        primaryStage.show();

        hWnd = getWindowHwnd(primaryStage);

        // taskbarList.HrInit();

        call("SetProgressState", () -> taskbarList.SetProgressState(hWnd, ITaskbarList3.TbpFlag.TBPF_NORMAL));
    }

    @Override
    public void init() throws Exception {
        super.init();
        taskbarList = COMRuntime.newInstance(ITaskbarList3.class);
        // taskbarList.HrInit();

        runner = new Thread(() -> {
            while (running) {
                try {
                    final Runnable take = RUNNABLES.take();
                    take.run();
                }
                catch (final InterruptedException ignore) {
                }
            }
            System.out.println("Runner terminated");
        });
        runner.start();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        running = false;
        runner.interrupt();
        call("Releasing", taskbarList::Release);
    }

    public static void main(final String[] args) throws ClassNotFoundException {
        launch(args);
        System.out.println("ok");
    }

    private static void call(final String info, final Supplier<Integer> supplier) {
        RUNNABLES.offer(() -> {
            final int result = supplier.get();
            System.out.println(info + ": 0x" + Integer.toHexString(result));
        });
    }

    private static Pointer<Integer> getWindowHwnd(final Window window) {
        try {
            final Method impl_getPeer = window.getClass().getMethod("impl_getPeer");
            final Object tkStage = impl_getPeer.invoke(window);
            final Method getPlatformWindow = tkStage.getClass().getDeclaredMethod("getPlatformWindow");
            getPlatformWindow.setAccessible(true);
            final Object platformWindow = getPlatformWindow.invoke(tkStage);
            final Method getNativeHandle = platformWindow.getClass().getMethod("getNativeHandle");
            final long hWnd = (Long) getNativeHandle.invoke(platformWindow);
            System.out.println("hWnd: " + hWnd);
            return Pointer.pointerToAddress(hWnd, Integer.class, p -> p.release());
        }
        catch (final IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
