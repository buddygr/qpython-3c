package com.quseit.util;

import java.io.File;
import java.util.Comparator;

public class FolderUtils {
    public static final Comparator<File> sortTypeByName = new Comparator<File>() {
        @Override
        public int compare(File arg00, File arg11) {
            String arg0 = arg00.toString();
            String arg1 = arg11.toString();
            String ext = null;
            String ext2 = null;
            int ret;

            try {
                ext = arg0.substring(arg0.lastIndexOf(".") + 1, arg0.length()).toLowerCase();
                ext2 = arg1.substring(arg1.lastIndexOf(".") + 1, arg1.length()).toLowerCase();
            } catch (IndexOutOfBoundsException e) {
                return 0;
            }
            ret = ext.compareTo(ext2);

            if (ret == 0)
                return arg0.toLowerCase().compareTo(arg1.toLowerCase());
            return ret;
        }
    };

    public static final Comparator<File> sortByName = new Comparator<File>() {
        @Override
        public int compare(File o1, File o2) {
            String arg0 = o1.getName();
            String arg1 = o2.getName();
            String ext;
            String ext2;
            int ret;

            try {
                ext = arg0.substring(arg0.lastIndexOf(".") + 1, arg0.length()).toLowerCase();
                ext2 = arg1.substring(arg1.lastIndexOf(".") + 1, arg1.length()).toLowerCase();

            } catch (IndexOutOfBoundsException e) {
                return 0;
            }
            ret = ext.compareTo(ext2);

            if (ret == 0)
                return arg0.toLowerCase().compareTo(arg1.toLowerCase());
            return ret;
        }
    };
}
