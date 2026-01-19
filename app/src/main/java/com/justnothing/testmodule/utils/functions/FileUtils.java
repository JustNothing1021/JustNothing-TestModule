package com.justnothing.testmodule.utils.functions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Pair;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.io.RootProcessPool;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class FileUtils {

    public static final String TAG = "FileUtilsLogger";
    public static class FileUtilsLogger extends Logger {
        @Override
        public String getTag() {
            return TAG;
        }
    }

    private static final FileUtilsLogger logger = new FileUtilsLogger();

    public static boolean checkAndroidDirPermission() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean checkStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1145);
    }

    public static boolean saveObjectToFile(Object obj, String dst) {
        String data = (String) obj;
        File file = new File(dst);
        if (file.getParent() != null && !(new File(file.getParent()).isDirectory())) {
            if (!mkdir(file.getParent())) {
                logger.error("saveObjectToFile: 保存文件时创建目录" + file.getParent() +"失败");
            }
        }
        try {
            IOManager.writeFile(file.getAbsolutePath(), data);
            return true;
        } catch (Exception e) {
            logger.error("saveObjectToFile: 文件" + dst + "保存失败", e);
            return false;
        }
    }

    public static boolean saveObjectToFileByShell(Object obj, String dst) {
        String data = (String) obj;
        File file = new File(dst);
        try {
            if (file.getParent() != null && !(new File(file.getParent()).isDirectory())) {
                Pair<Boolean, CmdUtils.CommandOutput> res = mkdirBySuShell(file);
                if (!res.first) {
                    logger.warn(
                            "saveObjectToFileByShell: 保存文件时创建目录" + file.getParent() + "失败: "
                            + res.second.stdout()
                    );
                }
            }
            CmdUtils.runCommand(String.join(" ",
                    "echo", CmdUtils.quoted(data), ">>", CmdUtils.quoted(dst)
            ));
            return true;
        } catch (IOException | InterruptedException e) {
            logger.error("saveObjectToFileByShell: 文件" + dst + "保存失败", e);
            return false;
        }
    }

    public static boolean mkdir(String dst) {
        File file = new File(dst);
        if (file.isFile()) {
            logger.error("mkdir: 创建目录" + dst + "失败: 目标目录是一个文件");
            return false;
        } else {
            try {
                return file.mkdirs();
            } catch (Exception e) {
                logger.error("mkdir: 创建目录" + dst + "失败: 发生了其他错误", e);
                return false;
            }
        }
    }

    public static boolean isDir(String dst) {
        try {
            return new File(dst).isDirectory();
        } catch (Exception unused) {
            return false;
        }
    }

    public static boolean isDir(File dst) {
        try {
            return dst.isDirectory();
        } catch (Exception unused) {
            return false;
        }
    }

    public static boolean isParentADir(File dst) {
        try {
            return new File(Objects.requireNonNull(dst.getParent())).isDirectory();
        } catch (NullPointerException ignored) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static Pair<Boolean, CmdUtils.CommandOutput> createFileBySuShell(File dst) {
        try {
            IOManager.ProcessResult res = RootProcessPool.executeCommand("touch " + CmdUtils.quoted(dst.getPath()));
            CmdUtils.CommandOutput cmdOutput = new CmdUtils.CommandOutput(res.exitCode, res.stdout, res.stderr, res.stdout + res.stderr);
            return new Pair<>(res.exitCode == 0, cmdOutput);
        } catch (Exception e) {
            return new Pair<>(false, null);
        }
    }

    public static Pair<Boolean, CmdUtils.CommandOutput> mkdirBySuShell(File dst) {
        try {
            IOManager.ProcessResult res = RootProcessPool.executeCommand("mkdir " + CmdUtils.quoted(dst.getPath()));
            CmdUtils.CommandOutput cmdOutput = new CmdUtils.CommandOutput(res.exitCode, res.stdout, res.stderr, res.stdout + res.stderr);
            if (res.exitCode == 0) return new Pair<>(true, cmdOutput);
            Boolean succeed = res.stdout.toLowerCase().contains("file exists") && dst.isDirectory();
            return new Pair<>(succeed, cmdOutput);
        } catch (Exception e) {
            return new Pair<>(false, null);
        }
    }

    public static Pair<Boolean, CmdUtils.CommandOutput> copyByShell(File src, File dst) {
        try {
            CmdUtils.CommandOutput res = CmdUtils.runCommand(
                String.join(" ",
                        "cp", "-R",
                        CmdUtils.quoted(src.getPath()),
                        CmdUtils.quoted(dst.getPath())
                ));
            return new Pair<>(res.stat() == 0, res);
        } catch (Exception e) {
            return new Pair<>(false, null);
        }
    }

    public static Pair<Boolean, CmdUtils.CommandOutput> setContentByShell(File src, String content) {
        try {
            CmdUtils.CommandOutput res = CmdUtils.runCommand(
                String.join(" ",
                    "echo",
                    CmdUtils.quoted(content),
                    ">>",
                    CmdUtils.quoted(src.getPath())
                ));
            return new Pair<>(res.stat() == 0, res);
        } catch (Exception e) {
            return new Pair<>(false, null);
        }
    }

}
