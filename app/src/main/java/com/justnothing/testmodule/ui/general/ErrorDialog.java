package com.justnothing.testmodule.ui.general;

import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.color.MaterialColors;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.DialogErrorBinding;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorDialog {

    private final Context context;
    private final String errorMessage;
    private final Throwable throwable;
    private final boolean fatal;
    private DialogInterface.OnDismissListener onDismissListener;

    public ErrorDialog(Context context, String errorMessage, Throwable throwable) {
        this(context, errorMessage, throwable, false);
    }

    public ErrorDialog(Context context, String errorMessage, Throwable throwable, boolean fatal) {
        this.context = context;
        this.errorMessage = errorMessage;
        this.throwable = throwable;
        this.fatal = fatal;
    }

    public ErrorDialog setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.onDismissListener = listener;
        return this;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(fatal ? context.getString(R.string.general_fatal_error) : context.getString(R.string.general_error_title));

        DialogErrorBinding binding = DialogErrorBinding.inflate(LayoutInflater.from(context));
        TextView textMessage = binding.textErrorMessage;
        TextView textStackTrace = binding.textErrorStacktrace;

        textMessage.setText(errorMessage);

        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();
            
            if (!TextUtils.isEmpty(stackTrace)) {
                textStackTrace.setText(stackTrace);
                textStackTrace.setVisibility(View.VISIBLE);
            } else {
                textStackTrace.setVisibility(View.GONE);
            }
        } else {
            textStackTrace.setVisibility(View.GONE);
        }

        textStackTrace.setMovementMethod(new ScrollingMovementMethod());

        builder.setView(binding.getRoot());

        if (fatal) {
            builder.setPositiveButton(context.getString(R.string.general_exit_app), (dialog, which) -> {
                dialog.dismiss();
                System.exit(1);
            });
        } else {
            builder.setPositiveButton(context.getString(R.string.general_confirm), (dialog, which) -> dialog.dismiss());
        }

        AlertDialog dialog = builder.create();
        if (onDismissListener != null) {
            dialog.setOnDismissListener(onDismissListener);
        }
        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(MaterialColors.getColor(positiveButton, androidx.appcompat.R.attr.colorPrimary));
        }
    }

    public static void showError(Context context, String errorMessage, Throwable throwable) {
        new ErrorDialog(context, errorMessage, throwable).show();
    }

    public static void showError(Context context, String errorMessage, Throwable throwable, boolean fatal) {
        new ErrorDialog(context, errorMessage, throwable, fatal).show();
    }

    public static void showError(Context context, String errorMessage) {
        new ErrorDialog(context, errorMessage, null).show();
    }

    public static void showError(Context context, String errorMessage, boolean fatal) {
        new ErrorDialog(context, errorMessage, null, fatal).show();
    }

    public static void showError(Context context, Throwable throwable) {
        new ErrorDialog(context, throwable.getMessage(), throwable).show();
    }

    public static void showError(Context context, Throwable throwable, boolean fatal) {
        new ErrorDialog(context, throwable.getMessage(), throwable, fatal).show();
    }
}
