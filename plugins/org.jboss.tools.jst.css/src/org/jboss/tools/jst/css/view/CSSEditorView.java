/*******************************************************************************
 * Copyright (c) 2007-2009 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.jst.css.view;

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IContributedContentsView;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.jboss.tools.jst.css.properties.CSSPropertyPage;

/**
 * @author Sergey Dzmitrovich
 * 
 */
public class CSSEditorView extends PropertySheet {

	static public String CONTRIBUTOR_ID = "org.eclipse.wst.css.core.csssource.source"; //$NON-NLS-1$

	@Override
	protected PageRec doCreatePage(final IWorkbenchPart part) {
		if (part instanceof PropertySheet) {
			return null;
		}
		IPropertySheetPage page = new CSSPropertyPage(
				new ITabbedPropertySheetPageContributor() {

					public String getContributorId() {
						return CONTRIBUTOR_ID;
					}
				}, this);
		if (page != null) {
			if (page instanceof IPageBookViewPage) {
				initPage((IPageBookViewPage) page);
			}
			page.createControl(getPageBook());
			return new PageRec(part, page);
		}

		return null;
	}

	@Override
	public SelectionProvider getSelectionProvider() {
		return super.getSelectionProvider();
	}
	public void postSelectionChanged(SelectionChangedEvent event){
		getSelectionProvider().postSelectionChanged(event);
	}

	@Override
	public Object getAdapter(Class key) {
		if (key == IContributedContentsView.class) {
			return new IContributedContentsView() {
				public IWorkbenchPart getContributingPart() {
					return getCurrentContributingPart();
				}
			};
		}
		return super.getAdapter(key);
	}
}
