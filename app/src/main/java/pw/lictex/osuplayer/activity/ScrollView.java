package pw.lictex.osuplayer.activity;

import android.content.*;
import android.util.*;

import androidx.annotation.*;
import androidx.core.widget.*;

public class ScrollView extends NestedScrollView {
    public ScrollView(@NonNull Context context) {
        super(context);
    }

    public ScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow, int type) {
        if (super.computeVerticalScrollOffset() == 0) return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
        return false;
    }
}
