package pw.lictex.osuplayer.activity;

import android.content.*;
import android.util.*;

import androidx.annotation.*;

public class RecyclerView extends androidx.recyclerview.widget.RecyclerView {
    public RecyclerView(@NonNull Context context) {
        super(context);
    }

    public RecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow, int type) {
        if (super.computeVerticalScrollOffset() == 0) return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
        return false;
    }
}
