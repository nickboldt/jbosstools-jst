/******************************************************************************* 
 * Copyright (c) 2013 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.jst.jsp.test.openon;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.jboss.tools.common.model.ui.editor.EditorPartWrapper;
import org.jboss.tools.common.model.ui.texteditors.XMLTextEditorStandAlone;
import org.jboss.tools.common.text.ext.hyperlink.AbstractHyperlink;
import org.jboss.tools.common.text.ext.hyperlink.IHyperlinkRegion;
import org.jboss.tools.common.text.ext.util.AxisUtil;
import org.jboss.tools.jst.web.ui.internal.editor.jspeditor.JSPMultiPageEditor;
import org.jboss.tools.jst.web.ui.internal.text.ext.hyperlink.internal.CreateNewFileHyperlink;
import org.jboss.tools.jst.web.ui.editors.WebCompoundEditor;

public class HyperlinkTestUtil extends TestCase{
	public static final String TEST_TEXT_EDITOR_ID="org.jboss.tools.jst.text.ext.test.TestTextEditor"; //$NON-NLS-1$
	
	public static void checkRegions(IProject project, String fileName, List<TestRegion> regionList, AbstractHyperlinkDetector elDetector) throws Exception {
		checkRegions(project, fileName, regionList, elDetector, false);
	}
	
	public static void checkRegions(IProject project, String fileName, List<TestRegion> regionList, AbstractHyperlinkDetector elDetector, boolean testOnlyExisting) throws Exception {
		IFile file = project.getFile(fileName);

		assertNotNull("The file \"" + fileName + "\" is not found", file); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("The file \"" + fileName + "\" is not found", file.isAccessible()); //$NON-NLS-1$ //$NON-NLS-2$

		FileEditorInput editorInput = new FileEditorInput(file);

		IDocumentProvider documentProvider = null;
			documentProvider = DocumentProviderRegistry.getDefault().getDocumentProvider(editorInput);

		assertNotNull("The document provider for the file \"" + fileName + "\" is not loaded", documentProvider); //$NON-NLS-1$ //$NON-NLS-2$

		documentProvider.connect(editorInput);
		try{
			IDocument document = documentProvider.getDocument(editorInput);
			
			assertNotNull("The document for the file \"" + fileName + "\" is not loaded", document); //$NON-NLS-1$ //$NON-NLS-2$
			
			if(regionList.get(0).region == null)
				loadRegions(regionList, document);
	
	
			int expected = 0;
			for(TestRegion testRegion : regionList)
				expected += testRegion.region.getLength()+1;
			
			IEditorPart part = openFileInEditor(file);
			ISourceViewer viewer = null;
			if(part instanceof JavaEditor){
				viewer = ((JavaEditor)part).getViewer();
				elDetector.setContext(new TestContext((ITextEditor)part));
			}else if(part instanceof EditorPartWrapper){
				if(((EditorPartWrapper)part).getEditor() instanceof WebCompoundEditor){
					WebCompoundEditor wce = (WebCompoundEditor)((EditorPartWrapper)part).getEditor();
					viewer = wce.getSourceEditor().getTextViewer();
					elDetector.setContext(new TestContext(wce.getSourceEditor()));
				}else if(((EditorPartWrapper)part).getEditor() instanceof XMLTextEditorStandAlone){
					XMLTextEditorStandAlone xtesa = (XMLTextEditorStandAlone)((EditorPartWrapper)part).getEditor();
					viewer = xtesa.getTextViewer();
					elDetector.setContext(new TestContext(xtesa));
				}else fail("unsupported editor type - "+((EditorPartWrapper)part).getEditor().getClass()); //$NON-NLS-1$
			}else if(part instanceof JSPMultiPageEditor){
				viewer = ((JSPMultiPageEditor)part).getJspEditor().getTextViewer();
				elDetector.setContext(new TestContext(((JSPMultiPageEditor)part).getJspEditor()));
			}else fail("unsupported editor type - "+part.getClass()); //$NON-NLS-1$
	
			
	
			int counter = 0;
			for (int i = 0; i < document.getLength(); i++) {
				int lineNumber = document.getLineOfOffset(i);
				int position = i - document.getLineOffset(lineNumber)+1;
				lineNumber++;
				
				TestData testData = new TestData(document, i);
				IHyperlink[] links = elDetector.detectHyperlinks(viewer, testData.getHyperlinkRegion(), true);
	
				boolean recognized = links != null;
	
				if (recognized) {
					counter++;
					TestRegion testRegion = findOffsetInRegions(i, regionList); 
					if(testRegion == null){
						String information = findRegionInformation(document, i, regionList);
						fail("Wrong detection for offset - "+i+" (line - "+lineNumber+" position - "+position+") "+information); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					}else{
						checkTestRegion(links, testRegion, testOnlyExisting);
					}
				} 
				else {
					for(TestRegion testRegion : regionList){
						if(i >= testRegion.region.getOffset() && i <= testRegion.region.getOffset()+testRegion.region.getLength()) {
							fail("Wrong detection for region - "+getRegionInformation(document, testRegion)+" offset - "+i+" (line - "+lineNumber+" position - "+position+")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						}
					}
				}
			}
	
			assertEquals("Wrong recognized region count: ", expected,  counter); //$NON-NLS-1$
		}finally{
			documentProvider.disconnect(editorInput);
		}
	}
	
	public static void checkHyperlinksInTextEditor(IProject project, String fileName, List<TestRegion> regionList, AbstractHyperlinkDetector elDetector) throws Exception {
		IFile file = project.getFile(fileName);

		assertNotNull("The file \"" + fileName + "\" is not found", file); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("The file \"" + fileName + "\" is not found", file.isAccessible()); //$NON-NLS-1$ //$NON-NLS-2$

		FileEditorInput editorInput = new FileEditorInput(file);

		IDocumentProvider documentProvider = null;
		documentProvider = DocumentProviderRegistry.getDefault().getDocumentProvider(editorInput);

		assertNotNull("The document provider for the file \"" + fileName + "\" is not loaded", documentProvider); //$NON-NLS-1$ //$NON-NLS-2$

		documentProvider.connect(editorInput);
		try{
			IDocument document = documentProvider.getDocument(editorInput);
			
			assertNotNull("The document for the file \"" + fileName + "\" is not loaded", document); //$NON-NLS-1$ //$NON-NLS-2$
			
			if(regionList.get(0).region == null)
				loadRegions(regionList, document);
	
	
			TestTextEditor part = openFileInTestTextEditor(file);
			ISourceViewer viewer = part.getTextSourceViewer();
			
	
			for (TestRegion testRegion : regionList) {
				int offset = testRegion.region.getOffset()+1;
				int lineNumber = document.getLineOfOffset(offset);
				int position = offset - document.getLineOffset(lineNumber)+1;
				lineNumber++;
				
				TestData testData = new TestData(document, offset);
				IHyperlink[] links = elDetector.detectHyperlinks(viewer, testData.getHyperlinkRegion(), true);
	
				boolean recognized = links != null;
	
				if (recognized) {
					for(IHyperlink link : links){
						if(link instanceof CreateNewFileHyperlink){
							CreateNewFileHyperlink l = (CreateNewFileHyperlink)link;
							IFile newFile = l.getFile();
							assertTrue("File should not be exist", !newFile.exists()); //$NON-NLS-1$
							
							l.test();
							
							newFile.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
							assertTrue("Hyperlink did not created the file", newFile.exists()); //$NON-NLS-1$
							newFile.delete(true, new NullProgressMonitor());
						}
					}
				} else {
					fail("Wrong detection for region - "+getRegionInformation(document, testRegion)+" offset - "+offset+" (line - "+lineNumber+" position - "+position+")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				}
			}
		}finally{
			documentProvider.disconnect(editorInput);
		}
	}
	
	public static TestTextEditor openFileInTestTextEditor(IFile input) throws PartInitException {
		if (input != null && input.exists()) {
			IWorkbenchPage page = PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow().getActivePage();
			return (TestTextEditor)IDE.openEditor(page, input, TEST_TEXT_EDITOR_ID, true);
		}
		return null;
	}

	private static void checkTestRegion(IHyperlink[] links, TestRegion testRegion, boolean testOnlyExisting){
		for(IHyperlink link : links){
			TestHyperlink testLink = findTestHyperlink(testRegion.hyperlinks, link);
			if(!testOnlyExisting){
				assertNotNull("Unexpected hyperlink - "+link.getHyperlinkText(), testLink); //$NON-NLS-1$
				assertEquals("Unexpected hyperlink type", testLink.hyperlink, link.getClass()); //$NON-NLS-1$
				if(testLink.fileName != null){
					assertTrue("HyperLink must be inherited from AbstractHyperlink", link instanceof AbstractHyperlink); //$NON-NLS-1$
					
					IFile f = ((AbstractHyperlink)link).getReadyToOpenFile();
					assertNotNull("HyperLink must return not null file", f); //$NON-NLS-1$
					assertEquals(testLink.fileName, f.getName());
					
				}
			}
		}
		
		for(TestHyperlink testLink : testRegion.hyperlinks){
			IHyperlink link = findHyperlink(links, testLink);
			assertNotNull("Hyperlink - "+testLink.name+" not found", link); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	private static void checkTestRegion(IHyperlink[] links, TestRegion testRegion){
		for(IHyperlink link : links){
			TestHyperlink testLink = findTestHyperlink(testRegion.hyperlinks, link);
			
			assertNotNull("Unexpected hyperlink - "+link.getHyperlinkText(), testLink);
			assertEquals("Unexpected hyperlink type", testLink.hyperlink, link.getClass());
			if(testLink.fileName != null){
				assertTrue("HyperLink must be inherited from AbstractHyperlink", link instanceof AbstractHyperlink);
				
				IFile f = ((AbstractHyperlink)link).getReadyToOpenFile();
				assertNotNull("HyperLink must return not null file", f);
				assertEquals(testLink.fileName, f.getName());
				
			}
		}
		
		for(TestHyperlink testLink : testRegion.hyperlinks){
			IHyperlink link = findHyperlink(links, testLink);
			assertNotNull("Hyperlink - "+testLink.name+" not found", link);
		}
	}

	private static TestHyperlink findTestHyperlink(List<TestHyperlink> testHyperlinks, IHyperlink link){
		for(TestHyperlink testLink : testHyperlinks){
			if(testLink.name.equals(link.getHyperlinkText()))
				return testLink;
		}
		return null;
	}

	private static IHyperlink findHyperlink(IHyperlink[] links, TestHyperlink testLink){
		for(IHyperlink link : links){
			if(testLink.name.equals(link.getHyperlinkText()))
				return link;
		}
		return null;
	}
	
	private static void loadRegions(List<TestRegion> regionList, IDocument document) throws BadLocationException{
		FindReplaceDocumentAdapter adapter = new FindReplaceDocumentAdapter(document);
		//IRegion region = adapter.find(0, "{", true, true, false, false);
		//if(region == null)
			IRegion region = new Region(0,0);
		for(TestRegion testRegion : regionList){
			IRegion newRegion = adapter.find(region.getOffset()+region.getLength(), testRegion.regionText, true, true, false, false);
			if(newRegion != null){
				testRegion.region = newRegion;
				region = newRegion;
			}else
				fail("Can not find string - "+testRegion.regionText); //$NON-NLS-1$
		}
		
		for(int i = regionList.size()-1; i >= 0; i--){
			TestRegion r = regionList.get(i);
			if(r.hyperlinks.size() == 0)
				regionList.remove(r);
		}
	}


	private static TestRegion findOffsetInRegions(int offset, List<TestRegion> regionList){
		for(TestRegion testRegion : regionList){
			if(offset >= testRegion.region.getOffset() && offset <= testRegion.region.getOffset()+testRegion.region.getLength())
				return testRegion;
		}
		return null;
	}
	
	private static String findRegionInformation(IDocument document, int offset, List<TestRegion> regionList) throws BadLocationException{
		if(regionList.size() == 0){
			return "empty list";
		}
		int index = 0;
		for(int i = 0; i < regionList.size(); i++){
			TestRegion testRegion = regionList.get(i);
			if(offset > testRegion.region.getOffset()+testRegion.region.getLength()){
				index = i;
			}
		}
		String info = "previous region - " + getRegionInformation(document, regionList.get(index)); //$NON-NLS-1$
		if(index+1 < regionList.size())
			info += " next region - " + getRegionInformation(document, regionList.get(index+1)); //$NON-NLS-1$
		return info;
	}
	
	private static String getRegionInformation(IDocument document, TestRegion region) throws BadLocationException{
		String info = ""; //$NON-NLS-1$
		int lineNumber = document.getLineOfOffset(region.region.getOffset());
		int position = region.region.getOffset() - document.getLineOffset(lineNumber)+1;
		lineNumber++;
		
		if(region.regionText != null)
			info += "<"+region.regionText+"> "; //$NON-NLS-1$ //$NON-NLS-2$
		
		info += region.region.getOffset()+" - "+(region.region.getOffset()+region.region.getLength())+" line - "+lineNumber+" position - "+position; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		return info;
	}

	public static IEditorPart openFileInEditor(IFile input) throws PartInitException {
		return openFileInEditor(input, null);
	}

	public static IEditorPart openFileInEditor(IFile input, String id) throws PartInitException {
		if (input != null && input.exists()) {
			if(id==null) {
				IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
				return IDE.openEditor(page, input, true);
			} else {
				IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
				return IDE.openEditor(page, input, id, true);
			}
		}
		return null;
	}

	static class TestData {
		IDocument document;
		int offset;
		IRegion region;
		String contentType;
		private IHyperlinkRegion hyperlinkRegion = null;

		TestData (IDocument document, int offset) {
			this.document = document;
			this.offset = offset;
			init();
		}

		private void init() {
			this.region = getDocumentRegion();
			this.contentType = getContentType();
			this.hyperlinkRegion = getHyperlinkRegion();
		}

		private IRegion getDocumentRegion() {
			IRegion region = JavaWordFinder.findWord(document, offset);

			return region;
		}

		public IHyperlinkRegion getHyperlinkRegion() {
			if (hyperlinkRegion != null)
				return hyperlinkRegion;

			return new IHyperlinkRegion() {
		        public String getAxis() {
                    return AxisUtil.getAxis(document, region.getOffset());
                }
                public String getContentType() {
                    return contentType;
                }
                public String getType() {
                    return region.toString();
                }
                public int getLength() {
                    return region.getLength();
                }
                public int getOffset() {
                    return region.getOffset();
                }
                public String toString() {
                	return "[" + getOffset() + "-" + (getOffset() + getLength() - 1) + ":" + getType() + ":" + getContentType() +  "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                }
            };
		}

		/**
		 * Returns the content type of document
		 * 
		 * @param document -
		 *            assumes document is not null
		 * @return String content type of given document
		 */
		private String getContentType() {
			String type = null;
	
			IModelManager mgr = StructuredModelManager.getModelManager();
			IStructuredModel model = null;
			try {
				model = mgr.getExistingModelForRead(document);
				if (model != null) {
					type = model.getContentTypeIdentifier();
				}
			} finally {
				if (model != null) {
					model.releaseFromRead();
				}
			}
			return type;
		}
	}

	static class TestContext implements IAdaptable{
		ITextEditor editor;

		public TestContext(ITextEditor editor){
			this.editor = editor;
		}

		public Object getAdapter(Class adapter) {
			if(adapter.equals(ITextEditor.class))
				return editor;
			return null;
		}
	}
	
	public static class TestRegion{
		IRegion region = null;
		String regionText = null;
		ArrayList<TestHyperlink> hyperlinks = new ArrayList<TestHyperlink>();
		
		public TestRegion(int offset, int length, TestHyperlink[] testHyperlinks){
			region = new Region(offset, length);
			for(TestHyperlink testHyperlink : testHyperlinks){
				hyperlinks.add(testHyperlink);
			}
		}
		
		public TestRegion(String regionText, TestHyperlink[] testHyperlinks){
			this.regionText = regionText;
			for(TestHyperlink testHyperlink : testHyperlinks){
				hyperlinks.add(testHyperlink);
			}
		}
	}
	
	public static class TestHyperlink{
		Class<? extends IHyperlink> hyperlink;
		String name;
		String fileName=null;
		
		public TestHyperlink(Class<? extends IHyperlink> hyperlink, String name, String fileName){
			this(hyperlink, name);
			this.fileName = fileName;
		}
		
		public TestHyperlink(Class<? extends IHyperlink> hyperlink, String name){
			this.hyperlink = hyperlink;
			this.name = name;
		}
		
	}

	public static void checkRegionsInTextEditor(IProject project, String fileName, List<TestRegion> regionList, AbstractHyperlinkDetector elDetector) throws Exception {
			IFile file = project.getFile(fileName);
	
			assertNotNull("The file \"" + fileName + "\" is not found", file);
			assertTrue("The file \"" + fileName + "\" is not found", file.isAccessible());
	
			FileEditorInput editorInput = new FileEditorInput(file);
	
			IDocumentProvider documentProvider = DocumentProviderRegistry.getDefault().getDocumentProvider(editorInput);
	
			assertNotNull("The document provider for the file \"" + fileName + "\" is not loaded", documentProvider);
	
			documentProvider.connect(editorInput);
			try{
				IDocument document = documentProvider.getDocument(editorInput);
				
				assertNotNull("The document for the file \"" + fileName + "\" is not loaded", document);
				
				if(regionList.size() > 0 && regionList.get(0).region == null){
					loadRegions(regionList, document);
				}
		
		
				TestTextEditor part = openFileInTestTextEditor(file);
				ISourceViewer viewer = part.getTextSourceViewer();
				
		
				for (int i = 0; i < document.getLength(); i++) {
					int lineNumber = document.getLineOfOffset(i);
					int position = i - document.getLineOffset(lineNumber)+1;
					lineNumber++;
					
					TestData testData = new TestData(document, i);
					IHyperlink[] links = elDetector.detectHyperlinks(viewer, testData.getHyperlinkRegion(), true);
		
					boolean recognized = links != null;
		
					if (recognized) {
						TestRegion testRegion = findOffsetInRegions(i, regionList); 
						if(testRegion == null){
							String information = findRegionInformation(document, i, regionList);
							fail("Wrong detection for offset - "+i+" (line - "+lineNumber+" position - "+position+") "+information);
						}else{
							checkTestRegion(links, testRegion);
						}
					} 
					else {
						for(TestRegion testRegion : regionList){
							if(i >= testRegion.region.getOffset() && i < testRegion.region.getOffset()+testRegion.region.getLength()) {
								fail("Wrong detection for region - "+getRegionInformation(document, testRegion)+" offset - "+i+" (line - "+lineNumber+" position - "+position+")");
							}
						}
					}
				}
			}finally{
				documentProvider.disconnect(editorInput);
			}
		}
}