/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2019  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.ui.handlers;

import org.eclipse.swt.widgets.FileDialog;

import de.ovgu.featureide.fm.core.io.IFeatureModelFormat;
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat;
import de.ovgu.featureide.fm.core.preferences.DIMACSOmitRootPreference;
import de.ovgu.featureide.fm.ui.handlers.base.AbstractFMExportHandler;

/**
 * Exports the feature model in DIMACS CNF format.
 *
 * @author Sebastian Krieter
 */
public class ExportDIMACSHandler extends AbstractFMExportHandler {

	@Override
	protected IFeatureModelFormat getOutputFormat() {
		final DIMACSFormat format = new DIMACSFormat();
		format.setOmitDummyRoot(DIMACSOmitRootPreference.getInstance().get());
		return format;
	}

	@Override
	protected void configureFileDialog(FileDialog fileDialog) {
		super.configureFileDialog(fileDialog);
		fileDialog.setFileName("model.dimacs");
		fileDialog.setFilterExtensions(new String[] { "*.dimacs" });
		fileDialog.setFilterNames(new String[] { "DIMACS format *.dimacs" });
	}
}
