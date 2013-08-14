package mon4h.framework.dashboard.common.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassUtils {
	private static boolean isSubClass(Class<?> c,Class<?> superClazz) {  
	    if (c == null) {  
	        return false;  
	    }  
	    if (c.isInterface()) {  
	        return false;  
	    }  
	    if (Modifier.isAbstract(c.getModifiers())) {  
	        return false;
	    }  
	    return superClazz.isAssignableFrom(c);  
	}
	
	private static List<File> listPaths(String path) {  
	    List<File> files = new ArrayList<File>();  
	    String jars = System.getProperty("java.class.path");  
	    if (jars == null) {  
	        System.err.println("java.class.path is null!");  
	        return files;  
	    } 
	    if(path != null){
	    	File givenPath = new File(path);
	    	if(givenPath.exists()){
	    		files.add(givenPath);
	    	}
	    }
	    String classPathRoot = null;
	    URL root = ClassUtils.class.getClassLoader().getResource("");  
	    if (root == null) {  
	        System.err.println("path root is null!");  
	        return files;  
	    }    
	    try {  
	    	classPathRoot = URLDecoder.decode(root.getFile(), "UTF-8");  
	    } catch (UnsupportedEncodingException e) {  
	        e.printStackTrace();  
	        return files;  
	    }  
	    File classRootDir = new File(classPathRoot);  
	    String[] array = (jars).split(";");  
	    if (array != null) {  
	        for (String s : array) {  
	            if (s == null) {  
	                continue;  
	            }  
	            File f = new File(s);  
	            if (f.exists()) {  
	                files.add(f);  
	            } else {
	                File jar = new File(classRootDir, s);  
	                if (jar.exists()) {  
	                    files.add(jar);  
	                }  
	            }  
	        }  
	    }  
	    return files;  
	}  
	
	private static <T> void dirWalker(String path, File file, List<Class<? extends T>> list,Class<T> superClazz) {  
	    if (file.exists()) {  
	        if (file.isDirectory()) {  
	            for (File f : file.listFiles()) {  
	            	ClassUtils.dirWalker(path, f, list,superClazz);  
	            }  
	        } else {  
	            ClassUtils.loadClassByFile(list,path, file,superClazz);  
	        }  
	    }  
	}  
	
	public static <T> List<Class<? extends T>> getClasses(String checkPath,String pkgPattern,Class<T> superClazz) {  
	    List<Class<? extends T>> list = new ArrayList<Class<? extends T>>();  
	    for (File f : ClassUtils.listPaths(checkPath)) {  
	        if (f.isDirectory()) {  
	            String path = pkgPattern.replace('.', File.separatorChar);  
	            ClassUtils.dirWalker(path, f, list,superClazz);  
	        } else { 
	        	parseJar(list,pkgPattern,f,superClazz);
	        }  
	    }  
	    return list;  
	}  
	
	@SuppressWarnings("unchecked")
	private static <T> void parseJar(List<Class<? extends T>> list,String pkgPattern, File jarFile,Class<T> superClazz){
		JarFile jar = null;  
        try {  
            jar = new JarFile(jarFile);  
        } catch (IOException e) {  
        }  
        if (jar == null) {  
            return;  
        }  
        String path = pkgPattern.replace(File.separatorChar, '.');  
        path = path.replace('.', '/');  
        Enumeration<JarEntry> entries = jar.entries();  
        while (entries.hasMoreElements()) {  
            JarEntry entry = entries.nextElement();  
            String name = entry.getName();  
            if (name.charAt(0) == '/') {  
                name = name.substring(1);  
            }  
            if (name.contains(path)) {  
                if (name.endsWith(".class") && !entry.isDirectory()) {  
                    name = name.replace("/", ".").substring(0,  
                            name.lastIndexOf("."));  
                    try {  
                    	Class<?> c = Class.forName(name);  
                        if (ClassUtils.isSubClass(c,superClazz)) {  
                            list.add((Class<T>)c);  
                        } 
                    } catch (Exception e) {  
 
                    }  
                }  
            }  
        }  
	}
	
	@SuppressWarnings("unchecked")
	private static <T> void loadClassByFile(List<Class<? extends T>> list,String pkgPattern, File file,Class<T> superClazz) {  
	    if (!file.isFile()) {  
	        return;  
	    }  
	    String name = file.getName();  
	    if (name.endsWith(".class")) {  
	        String ap = file.getAbsolutePath();  
	        if (!ap.contains(pkgPattern)) {  
	            return;  
	        }  
	        name = ap;  
	        if (name.startsWith(File.separator)) {  
	            name = name.substring(1);  
	        }  
	        String path = name.substring(0, name.lastIndexOf("."))  
	                .replace(File.separatorChar, '.');  
	        try {  
				Class<?> c = Class.forName(path);  
	            if (ClassUtils.isSubClass(c,superClazz)) {  
	                list.add((Class<T>)c);  
	            }  
	        } catch (ClassNotFoundException e) {  
	            // do nothing  
	        }  
	    }else{
	    	parseJar(list,pkgPattern,file,superClazz);
	    } 
	}  
}
