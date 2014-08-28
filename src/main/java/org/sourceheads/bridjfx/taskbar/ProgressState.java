package org.sourceheads.bridjfx.taskbar;

import org.bridj.cpp.com.shell.ITaskbarList3;

/**
 * <p>Progress states for calls to
 * {@link org.sourceheads.bridjfx.taskbar.TaskbarList#setProgressState(ProgressState)}.</p>
 *
 * @author Stefan Fiedler
 */
public enum ProgressState {

    NOPROGRESS(ITaskbarList3.TbpFlag.TBPF_NOPROGRESS),
    INDETERMINATE(ITaskbarList3.TbpFlag.TBPF_INDETERMINATE),
    NORMAL(ITaskbarList3.TbpFlag.TBPF_NORMAL),
    ERROR(ITaskbarList3.TbpFlag.TBPF_ERROR),
    PAUSED(ITaskbarList3.TbpFlag.TBPF_PAUSED);

    private final ITaskbarList3.TbpFlag flag;

    private ProgressState(final ITaskbarList3.TbpFlag flag) {
        this.flag = flag;
    }

    ITaskbarList3.TbpFlag getFlag() {
        return flag;
    }
}
