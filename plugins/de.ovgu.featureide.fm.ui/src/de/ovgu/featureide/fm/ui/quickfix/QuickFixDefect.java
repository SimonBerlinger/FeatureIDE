/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.event.IEventListener;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.ui.editors.FeatureDiagramEditor;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class QuickFixDefect implements IMarkerResolution {

	protected IFeatureProject project;
	protected FeatureModelFormula featureModel;
	protected final FeatureModelManager fmManager;
	protected FeatureDiagramEditor diagramEditor;

	@Override
	public String getLabel() {
		return null;
	}

	@Override
	public void run(IMarker marker) {}

	public QuickFixDefect(final IMarker marker, FeatureModelManager manager) {
		fmManager = manager;
		if (marker != null) {
			project = CorePlugin.getFeatureProject(marker.getResource());
			if (project == null) {
				featureModel = null;
			} else {
				featureModel = project.getFeatureModelManager().getPersistentFormula();
			}
		} else {
			featureModel = null;
			project = null;
		}

		if (fmManager != null) {
			for (final IEventListener l : fmManager.getListeners()) {
				if (l.getClass().equals(FeatureDiagramEditor.class)) {
					diagramEditor = (FeatureDiagramEditor) l;
					break;
				}
			}
		}
	}

}
