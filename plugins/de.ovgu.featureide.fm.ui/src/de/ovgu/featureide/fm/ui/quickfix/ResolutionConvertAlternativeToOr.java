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

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class ResolutionConvertAlternativeToOr extends QuickFixDefect implements IMarkerResolution {

	private final IFeature alternativeParent;

	/**
	 * @param marker
	 * @param manager
	 */
	public ResolutionConvertAlternativeToOr(IMarker marker, FeatureModelManager manager, IFeature alternativeParent, String prefix, String postfix) {
		super(marker, manager);
		this.alternativeParent = alternativeParent;
	}

	@Override
	public String getLabel() {

		return prefix + "Change the alternative-relation below ''" + alternativeParent + "'' to an or-relation" + postfix;
	}

	@Override
	public void run(IMarker marker) {
		fmManager.overwrite();
		fmManager.editObject(featureModel -> {
			featureModel.getFeature(alternativeParent.getName()).getStructure().changeToOr();
			System.out.println(featureModel.getFeature(alternativeParent.getName()).getStructure().isOr() + " SUCCESS?");
		});

		fmManager.save();
		fmManager.overwrite();
	}

}
