package pw.lictex.osuplayer.activity;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.*;
import android.view.animation.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.*;

import lombok.*;

public class ImageSwitcher extends FrameLayout {
    private ImageView[] i = new ImageView[2];
    private boolean second = true;
    @Getter @Setter private long animationDuration;

    public ImageSwitcher(@NonNull Context context) {
        super(context, null, 0);
    }

    public ImageSwitcher(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public ImageSwitcher(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2) throw new RuntimeException();
        i[0] = (ImageView) getChildAt(0);
        i[1] = (ImageView) getChildAt(1);
    }

    public void to(Bitmap b) {
        i[second ? 0 : 1].setImageBitmap(b);
        if (second) i[0].setVisibility(VISIBLE);
        else i[0].postDelayed(() -> i[0].setVisibility(INVISIBLE), animationDuration);
        i[1].animate().setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(animationDuration).alpha(second ? 0 : 1).start();
        second = !second;
    }

    public void to(Drawable d) {
        i[second ? 0 : 1].setImageDrawable(d);
        if (second) i[0].setVisibility(VISIBLE);
        else i[0].postDelayed(() -> i[0].setVisibility(INVISIBLE), animationDuration);
        i[1].animate().setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(animationDuration).alpha(second ? 0 : 1).start();
        second = !second;
    }
}
