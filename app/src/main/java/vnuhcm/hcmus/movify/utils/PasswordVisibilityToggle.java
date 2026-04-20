package vnuhcm.hcmus.movify.utils;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.graphics.drawable.Drawable;
import android.widget.EditText;

public final class PasswordVisibilityToggle {

    private PasswordVisibilityToggle() {
    }

    public static void attach(EditText editText) {
        editText.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() != MotionEvent.ACTION_UP) {
                return false;
            }

            Drawable endDrawable = editText.getCompoundDrawablesRelative()[2];
            if (endDrawable == null) {
                return false;
            }

            float toggleAreaStart = editText.getWidth()
                    - editText.getPaddingEnd()
                    - endDrawable.getIntrinsicWidth();
            if (motionEvent.getX() < toggleAreaStart) {
                return false;
            }

            boolean isHidden = editText.getTransformationMethod() instanceof PasswordTransformationMethod;
            if (isHidden) {
                editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }

            editText.setSelection(editText.getText().length());
            view.performClick();
            return true;
        });
    }
}
