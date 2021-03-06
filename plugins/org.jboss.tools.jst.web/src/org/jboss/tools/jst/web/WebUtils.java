/*******************************************************************************
 * Copyright (c) 2007 Exadel, Inc. and Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Exadel, Inc. and Red Hat, Inc. - initial API and implementation
 ******************************************************************************/ 
package org.jboss.tools.jst.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.jboss.tools.common.model.project.IModelNature;
import org.jboss.tools.common.model.util.EclipseResourceUtil;
import org.jboss.tools.jst.web.project.WebProject;

public class WebUtils {
	public static final String ATTR_WEB_SERVER_NAME = "org.jboss.tools.jst.web.web_server_name"; //$NON-NLS-1$
	public static final String WEB_SERVER_TOMCAT = "Tomcat"; //$NON-NLS-1$
	public static final String APPLICATION_SERVER_J2EE = "J2EE"; //$NON-NLS-1$
	public static final String APPLICATION_SERVER_JBOSS = "JBoss"; //$NON-NLS-1$
	
	public static String[] getServletVersions(String templateBase) {
		String location = templateBase + "/../lib/servlet"; //$NON-NLS-1$
		File f = new File(location);
		if(!f.isDirectory()) return new String[0];
		File[] fs = f.listFiles();
		Set<String> set = new TreeSet<String>();
		if(fs != null) for (int i = 0; i < fs.length; i++) {
			if(fs[i].isDirectory()) set.add(fs[i].getName());
		}
		return set.toArray(new String[0]);
	}
	
	public static String[] getServletLibraries(String templateBase, String servletVersion) {
		String location = templateBase + "/../lib/servlet/" + servletVersion; //$NON-NLS-1$
		File f = new File(location);
		if(!f.isDirectory()) return new String[0];

		List<String> jars = new ArrayList<String>();
		File[] fs = f.listFiles();
		if(fs != null) for (int i = 0; i < fs.length; i++) {
			if(!fs[i].isFile()) continue;
			String path = null;
			try {
				path = fs[i].getCanonicalPath();
			} catch (IOException e) {
				WebModelPlugin.getPluginLog().logError(e);
			}
			if(path != null) jars.add(path);
		}
		return jars.toArray(new String[jars.size()]);
	}

	public static String getWebRootPath(IProject project) {
		IModelNature modelNature = EclipseResourceUtil.getModelNature(project);
		return (modelNature != null) ? WebProject.getInstance(modelNature.getModel()).getWebRootLocation() : null;
	}

	public static IContainer[] getWebRootFolders(IProject project, boolean ignoreDerived) {
		return org.jboss.tools.common.web.WebUtils.getWebRootFolders(project, ignoreDerived);
	}

	public static IContainer[] getWebRootFolders(IProject project) {
		return getWebRootFolders(project, true);
	}

	public static String[] getServletLibraries(String natureId, String templateBase, String servletVersion) {
		String classPathVarName = findClassPathVarByNatureId(natureId);

		if (classPathVarName==null) return getServletLibraries(templateBase,servletVersion);

		String classPathVarValue = JavaCore.getClasspathVariable(classPathVarName).toOSString();
		
		String location = templateBase + "/../lib/servlet/" + servletVersion; //$NON-NLS-1$
		File f = new File(location);
		if(!f.isDirectory()) return new String[0];

		List<String> jars = new ArrayList<String>();
		File[] fs = f.listFiles();
		if(fs != null) for (int i = 0; i < fs.length; i++) {
			if(!fs[i].isFile()) continue;
			String path = null;
			try {
				path = fs[i].getCanonicalPath();
				if(!path.startsWith("/"))  //$NON-NLS-1$
					path="/"+path; //$NON-NLS-1$
				if(path.startsWith(classPathVarValue)) {
					path = classPathVarName + path.substring(classPathVarValue.length());
				}
			} catch (IOException e) {
				WebModelPlugin.getPluginLog().logError(e);
			}
			if(path != null) jars.add(path);
		}
		return jars.toArray(new String[jars.size()]);
	}
	
	public static String CLASS_PATH_VAR_EXTENSION_POINT_ID = "org.jboss.tools.jst.web.classPathVar"; //$NON-NLS-1$
	public static String VAR_TAG_NAME = "variable"; //$NON-NLS-1$
	public static String VAR_NAME_ATTR = "name"; //$NON-NLS-1$
	public static String NATURE_ID_ATTR = "natureId"; //$NON-NLS-1$

	public static String findClassPathVarByNatureId(String nId) {
	    IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(CLASS_PATH_VAR_EXTENSION_POINT_ID);
	    IExtension[] extensions = point.getExtensions();
	    for(int i=0; i<extensions.length; i++) {
	        IConfigurationElement[] configurationElements = extensions[i].getConfigurationElements();
	        for(int j=0; j<configurationElements.length; j++) {
	            if(VAR_TAG_NAME.equals(configurationElements[j].getName())) {
	                String name = configurationElements[j].getAttribute(VAR_NAME_ATTR);
	                String natureId = configurationElements[j].getAttribute(NATURE_ID_ATTR);
	                if(nId.equals(natureId)) {
	                    return name;
	                }
	            }
	        }
	    }
		return null;
	}

	public static IPath getRelativePath(IProject project, String path) {
		return EclipseResourceUtil.getRelativePath(project, path);
	}

	public static IClasspathEntry[] getDefaultJRELibrary() {
		return EclipseResourceUtil.getDefaultJRELibrary();
	}

	public static void changeTimeStamp(IProject project) throws CoreException {
		if(project == null || !project.isAccessible()) return;
		List<IFile> fs = getFilesToTouch(project);
		for (int i = 0; i < fs.size(); i++) {
			IFile f = (IFile)fs.get(i);
			f.setLocalTimeStamp(System.currentTimeMillis());
			f.touch(new NullProgressMonitor());	// done so deployers/listeners can detect the actual change.		
		}
	}

	private static List<IFile> getFilesToTouch(IProject project) {
		List<IFile> fs = new ArrayList<IFile>();
		if(project == null || !project.isAccessible()) return fs;
		boolean isWar = J2EEProjectUtilities.isDynamicWebProject(project);
		boolean isEar = J2EEProjectUtilities.isEARProject(project);

		boolean isReferencedByEar = false;
		if(!isEar) {
			IProject[] ps = J2EEProjectUtilities.getReferencingEARProjects(project);
			for (int i = 0; i < ps.length; i++) {
				fs.addAll(getFilesToTouch(ps[i]));
				isReferencedByEar = true;
			}
		}
		if(isEar) {
			IVirtualComponent component = ComponentCore.createComponent(project);
			IPath path = component.getRootFolder().getProjectRelativePath();
			IFile f = project.getFile(path.append("META-INF").append("application.xml")); //$NON-NLS-1$ //$NON-NLS-2$
			if(f != null && f.exists()) {
				fs.add(f);
			}
		}
		if(isWar && !isReferencedByEar) {
			IVirtualComponent component = ComponentCore.createComponent(project);
			IPath path = component.getRootFolder().getProjectRelativePath();
			IFile f = project.getFile(path.append("WEB-INF").append("web.xml")); //$NON-NLS-1$ //$NON-NLS-2$
			if(f != null && f.exists()) {
				fs.add(f);
			}
		}
		return fs;
	}	
}