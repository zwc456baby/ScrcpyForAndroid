package org.client.scrcpy.utils;

import android.content.Context;
import android.os.Build;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

//@SuppressWarnings("ALL")
public class FileUtils {
    private FileUtils() {
    }

    public static String getAssetsData(Context context, String fileName) {
        String result = "";
        try {
            //获取输入流
            InputStream mAssets = context.getAssets().open(fileName);

            //获取文件的字节数
            int lenght = mAssets.available();
            //创建byte数组
            byte[] buffer = new byte[lenght];
            //将文件中的数据写入到字节数组中
            mAssets.read(buffer);
            mAssets.close();
            result = new String(buffer);
            return result;
        } catch (IOException e) {
            return result;
        }
    }

    public static byte[] getAssetsBytes(Context context, String fileName) {
        try {
            //获取输入流
            InputStream mAssets = context.getAssets().open(fileName);
            //获取文件的字节数
            int lenght = mAssets.available();
            //创建byte数组
            byte[] buffer = new byte[lenght];
            //将文件中的数据写入到字节数组中
            mAssets.read(buffer);
            mAssets.close();
            return buffer;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 从assets目录中复制整个文件夹内容,考贝到 /data/data/包名/files/目录中
     *
     * @param context  activity 使用CopyFiles类的Activity
     * @param filePath String  文件路径,如：/assets/aa
     */
    public static boolean copyAssetsDir2Phone(Context context, String filePath, String toPath) {

        String[] fileList = null;
        try {
            fileList = context.getAssets().list(filePath);
        } catch (IOException ignore) {
        }
        if (fileList != null && fileList.length > 0) {//如果是目录
            boolean suc = true;
            for (String fileName : fileList) {
                String nextFilePath = filePath + File.separator + fileName;
                suc = copyAssetsDir2Phone(context, nextFilePath, toPath) && suc;
            }
            return suc;
        } else {//如果是文件
            InputStream inputStream = null;
            FileOutputStream fos = null;
            try {
                inputStream = context.getAssets().open(filePath);
                File file = new File(toPath + File.separator + filePath);
                if (file.exists()) {
                    if (!deleteFileSafely(file)) return false;
                } else {
                    File parentFile = file.getParentFile();
                    if ((!parentFile.exists()) && (!parentFile.mkdirs())) return false;
                }
                fos = new FileOutputStream(file);
                int len;
                byte[] buffer = new byte[2048];
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fos != null) {
                        fos.flush();
                        fos.close();
                    }
                    if (inputStream != null) inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }

    public boolean copyFileToPath(String fromfile, String tofile) {
        File sourceFile = new File(fromfile);
        if (!sourceFile.exists() || !sourceFile.canRead()) { // 有权限且文件存在
            return false;
        }
        if (sourceFile.isDirectory()) { // 如果是文件夹，迭代
            File[] files = sourceFile.listFiles();
            boolean suc = true;
            for (File file : files) {
                suc = !copyFileToPath(file.getAbsolutePath(), tofile + File.separator + file.getName()) && suc;
            }
            return suc;
        } else {
            return copyFile(fromfile, tofile); // 如果是文件的话，直接传入路径复制文件
        }
    }

    public static boolean copyFile(String path, String toPath) {
        File file = new File(path);
        File toFile = new File(toPath);
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        if (!file.exists() || !file.canRead()) { // 有权限且文件存在
            return false;
        }
        if (!toFile.exists()) { // 如果文件不存在
            try {
                File dir = new File(toFile.getParent());
                if ((!dir.exists()) && !dir.mkdirs()) return false;
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        if (toFile.exists() && !deleteFileSafely(toFile)) return false;

        try {
            inputStream = new FileInputStream(file);
            outputStream = new FileOutputStream(toFile);
            byte[] buff = new byte[2048];
            int index;
            while ((index = inputStream.read(buff)) > 0) {
                outputStream.write(buff, 0, index);
            }
            return true;
        } catch (IOException e) {
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if (inputStream != null) inputStream.close();
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 将两个文件合并成一个新文件
     *
     * @param path
     * @param toPath
     * @return
     */
    public static boolean multiFile(String path, String path2, String toPath) {
        File file = new File(path);
        File file2 = new File(path2);
        File toFile = new File(toPath);
        FileInputStream inputStream = null;
        FileInputStream inputStream2 = null;
        FileOutputStream outputStream = null;
        if (!file.exists() || !file.canRead()) { // 有权限且文件存在
            return false;
        }
        if (!file2.exists() || !file2.canRead()) { // 有权限且文件存在
            return false;
        }
        if (!toFile.exists()) { // 如果文件不存在
            try {
                File dir = new File(toFile.getParent());
                if ((!dir.exists()) && !dir.mkdirs()) return false;
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        if (toFile.exists() && !deleteFileSafely(toFile)) return false;

        try {
            inputStream = new FileInputStream(file);
            inputStream2 = new FileInputStream(file2);
            outputStream = new FileOutputStream(toFile);
            byte[] buff = new byte[2048];
            int index;
            while ((index = inputStream.read(buff)) > 0) {
                outputStream.write(buff, 0, index);
            }
            while ((index = inputStream2.read(buff)) > 0) {
                outputStream.write(buff, 0, index);
            }
            return true;
        } catch (IOException e) {
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (inputStream2 != null) {
                    inputStream2.close();
                }
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static byte[] readFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists())
            return null;
        byte[] retBytes = new byte[(int) file.length()];
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(retBytes);
            return retBytes;
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String readFileToString(String filePath) {
        File file = new File(filePath);
        if (!file.exists())
            return null;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            StringBuilder readStringBuilder = new StringBuilder();
            String currentLine;
            boolean start = true;
            while ((currentLine = in.readLine()) != null) {
                if (!start) {
                    readStringBuilder.append("\n");
                } else {
                    start = false;
                }
                readStringBuilder.append(currentLine);
            }
            return readStringBuilder.toString();
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean putStringToFile(String path, String text) {
        File file = new File(path);
        if (file.exists() && (!deleteFileSafely(file)))
            return false;
        if (file.getParentFile() != null
                && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file), 2048);
            out.write(text);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean putBytes(String path, byte[] text) {
        File file = new File(path);
        if (file.exists() && (!deleteFileSafely(file)))
            return false;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(text);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param file 要删除的文件
     */

    public static boolean deleteFileSafely(File file) {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {  // 如果是文件夹，则递归删除其中的文件
                File[] files = file.listFiles();
                if (files != null && files.length > 0) {
                    for (File file1 : files) {
                        deleteFileSafely(file1);
                    }
                }
            }
            File tmp = getTmpFile(file, System.currentTimeMillis(), -1);
            if (file.renameTo(tmp)) { // 将源文件重命名
                return tmp.delete(); // 删除重命名后的文件
            } else {
                return file.delete();
            }
        }
        return false;
    }

    private static File getTmpFile(File file, long time, int index) {
        File tmp;
        if (index == -1) {
            tmp = new File(file.getParent() + File.separator + time);
        } else {
            tmp = new File(file.getParent() + File.separator + time + "(" + index + ")");
        }
        if (!tmp.exists()) {
            return tmp;
        } else {
            return getTmpFile(file, time, index >= 1000 ? index : ++index);
        }
    }


    /**
     * 压缩文件或者一个目录
     *
     * @param srcPath 资源文件或目录
     * @param dstPath 输出文件
     */
    public static boolean compressToZip(String srcPath, String dstPath) {
        File srcFile = new File(srcPath);
        File dstFile = new File(dstPath);
        if (!srcFile.exists()) {
            System.out.println(srcPath + " does not exist ！");
            return false;
        }

        try (FileOutputStream out = new FileOutputStream(dstFile);
             ZipOutputStream zipOut = new ZipOutputStream(new CheckedOutputStream(out, new CRC32()))) {
            String baseDir = "";
            compress(srcFile, zipOut, baseDir, true);

            // 关闭 entry
            zipOut.closeEntry();

            return true;
        } catch (IOException e) {
            System.out.println(" compress exception = " + e.getMessage());
            return false;
        }
    }


    /**
     * 解压文件
     *
     * @param zipPath 要解压的目标文件
     * @param descDir 指定解压目录
     * @return 解压结果：成功，失败
     */
    @SuppressWarnings("rawtypes")
    public static boolean decompressZip(String zipPath, String descDir) {
        File zipFile = new File(zipPath);
        boolean flag = false;
        if (!descDir.endsWith(File.separator)) {
            descDir = descDir + File.separator;
        }
        File pathFile = new File(descDir);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }

        ZipFile zip = null;
        try {
            // api level 24 才有此方法
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                zip = new ZipFile(zipFile, Charset.forName("gbk"));//防止中文目录，乱码
            } else {
                zip = new ZipFile(zipFile);
            }
            for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String zipEntryName = entry.getName();
                InputStream in = zip.getInputStream(entry);

                //指定解压后的文件夹+当前zip文件的名称
                String outPath = (descDir + zipEntryName).replace("/", File.separator);
                //判断路径是否存在,不存在则创建文件路径
                File file = new File(outPath.substring(0, outPath.lastIndexOf(File.separator)));

                if (!file.exists()) {
                    file.mkdirs();
                }
                //判断文件全路径是否为文件夹,如果是上面已经创建,不需要解压
                if (new File(outPath).isDirectory()) {
                    try {
                        in.close();
                    } catch (Exception ignore) {
                    }
                    continue;
                }

                //保存文件路径信息（可利用md5.zip名称的唯一性，来判断是否已经解压）
//                System.err.println("当前zip解压之后的路径为：" + outPath);
                //noinspection IOStreamConstructor
                OutputStream out = new FileOutputStream(outPath);
                byte[] buf1 = new byte[2048];
                int len;
                while ((len = in.read(buf1)) > 0) {
                    out.write(buf1, 0, len);
                }
                try {
                    in.close();
                } catch (Exception ignore) {
                }
                try {
                    out.close();
                } catch (Exception ignore) {
                }
            }
            flag = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }

    private static void compress(File file, ZipOutputStream zipOut, String baseDir, boolean isRootDir) throws IOException {
        if (file.isDirectory()) {
            compressDirectory(file, zipOut, baseDir, isRootDir);
        } else {
            compressFile(file, zipOut, baseDir);
        }
    }

    /**
     * 压缩一个目录
     */
    private static void compressDirectory(File dir, ZipOutputStream zipOut, String baseDir, boolean isRootDir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {  // 这两行加入将保留空目录
            ZipEntry entry = new ZipEntry(baseDir + dir.getName() + "/");
            zipOut.putNextEntry(entry);
            return;
        }
        for (File file : files) {
            String compressBaseDir = "";
            if (!isRootDir) {
                compressBaseDir = baseDir + dir.getName() + "/";
            }
            compress(file, zipOut, compressBaseDir, false);
        }
    }

    /**
     * 压缩一个文件
     */
    private static void compressFile(File file, ZipOutputStream zipOut, String baseDir) throws IOException {
        if (!file.exists()) {
            return;
        }

        //noinspection IOStreamConstructor
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            String fileName = file.getName();
            ZipEntry entry = new ZipEntry(baseDir + fileName);
            zipOut.putNextEntry(entry);
            int count;
            byte[] data = new byte[2048];
            while ((count = bis.read(data, 0, 2048)) != -1) {
                zipOut.write(data, 0, count);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
